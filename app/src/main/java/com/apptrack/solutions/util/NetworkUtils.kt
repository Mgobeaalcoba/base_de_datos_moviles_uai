package com.apptrack.solutions.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkUtils(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkUtils"
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _wasDisconnected = MutableStateFlow(false)
    val wasDisconnected: StateFlow<Boolean> = _wasDisconnected.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            val wasDisconnectedBefore = !_isConnected.value
            _isConnected.value = true
            
            if (wasDisconnectedBefore) {
                _wasDisconnected.value = true
                Log.d(TAG, "Connection restored - triggering sync flag")
            }
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            _isConnected.value = false
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            Log.d(TAG, "Network capabilities changed. Has internet: $hasInternet")
            val wasDisconnectedBefore = !_isConnected.value
            _isConnected.value = hasInternet
            
            if (hasInternet && wasDisconnectedBefore) {
                _wasDisconnected.value = true
                Log.d(TAG, "Internet connection restored - triggering sync flag")
            }
        }
    }

    init {
        // Verificar estado inicial
        _isConnected.value = isCurrentlyConnected()
        Log.d(TAG, "Initial connection state: ${_isConnected.value}")
        
        // Registrar callback para cambios de red
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun isCurrentlyConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun resetDisconnectedFlag() {
        _wasDisconnected.value = false
        Log.d(TAG, "Disconnected flag reset")
    }

    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Network callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }
    }
}
