package com.apptrack.solutions

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import com.apptrack.solutions.ui.theme.AppTrackSolutionsGobeaTheme
import android.util.Log

class LoginActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var errorMessage by mutableStateOf<String?>(null)
    private var currentUser by mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null)
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AppTrackSolutionsGobeaTheme {
                LoginScreen(
                    errorMessage = errorMessage,
                    currentUser = currentUser,
                    isLoading = isLoading,
                    onLoginClick = { signInWithGoogle() },
                    onContinueClick = { continueWithCurrentUser() },
                    onExitClick = { finish() }
                )
            }
        }
    }

    private fun continueWithCurrentUser() {
        currentUser?.let { firebaseUser ->
            saveUserAndNavigate(firebaseUser)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(Exception::class.java)
                    firebaseAuthWithGoogle(account)
                } catch (e: Exception) {
                    errorMessage = "Error en el inicio de sesión de Google: ${e.message}"
                    isLoading = false
                }
            } else {
                errorMessage = "Inicio de sesión cancelado"
                isLoading = false
            }
        }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        currentUser = firebaseUser
                        saveUserAndNavigate(firebaseUser)
                    } else {
                        errorMessage = "No se pudo obtener el usuario de Firebase"
                        isLoading = false
                    }
                } else {
                    errorMessage = "Error de autenticación con Firebase: ${task.exception?.message}"
                    isLoading = false
                }
            }
    }

    private fun saveUserAndNavigate(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        Log.d("LoginActivity", "saveUserAndNavigate: Iniciando proceso de guardado para usuario: ${firebaseUser.uid}")
        Log.d("LoginActivity", "saveUserAndNavigate: Nombre: ${firebaseUser.displayName}")
        Log.d("LoginActivity", "saveUserAndNavigate: Email: ${firebaseUser.email}")

        lifecycleScope.launch {
            try {
                Log.d("LoginActivity", "saveUserAndNavigate: Creando instancia de Room DB")

                // Instanciar Room DB y repositorio
                val db = Room.databaseBuilder(
                    applicationContext,
                    com.apptrack.solutions.data.AppDatabase::class.java,
                    "notes-db"
                ).fallbackToDestructiveMigration().build()

                Log.d("LoginActivity", "saveUserAndNavigate: Creando repositorio")
                val repository = com.apptrack.solutions.data.NoteRepository(
                    db.noteDao(),
                    db.userDao(),
                    db.tagDao(),
                    db.attachmentDao(),
                    db.noteTagDao()
                )

                Log.d("LoginActivity", "saveUserAndNavigate: Creando objeto User")
                // Crear objeto User
                val user = com.apptrack.solutions.model.User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Usuario",
                    email = firebaseUser.email ?: ""
                )

                Log.d("LoginActivity", "saveUserAndNavigate: Insertando usuario en repositorio (Room + Firestore)")
                // Insertar usuario en Room y Firestore
                repository.insertUser(user)

                Log.d("LoginActivity", "saveUserAndNavigate: Usuario guardado exitosamente. Navegando a MainActivity")

                // Navegar a MainActivity tras sincronización
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Log.e("LoginActivity", "saveUserAndNavigate: Error guardando usuario", e)
                errorMessage = "Error guardando usuario: ${e.message}"
                isLoading = false
            }
        }
    }
}

@Composable
fun LoginScreen(
    errorMessage: String?,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onContinueClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Título de la app
                Text(
                    text = "AppTrack Solutions",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gestión de Notas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mostrar usuario actual si existe
                currentUser?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Usuario logueado:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.displayName ?: "Usuario",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = user.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón para continuar con usuario actual
                    Button(
                        onClick = onContinueClick,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Continuar como ${user.displayName}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón de login con Google
                Button(
                    onClick = onLoginClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading && currentUser == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (currentUser != null) "Cambiar cuenta" else "Iniciar sesión con Google")
                }

                // Mostrar errores
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExitClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salir", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}
