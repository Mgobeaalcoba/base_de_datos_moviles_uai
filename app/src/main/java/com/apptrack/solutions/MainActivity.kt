package com.apptrack.solutions

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.apptrack.solutions.data.AppDatabase
import com.apptrack.solutions.data.NoteRepository
import com.apptrack.solutions.data.NoteViewModel
import com.apptrack.solutions.ui.NotesScreen
import com.apptrack.solutions.ui.theme.AppTrackSolutionsGobeaTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    // ViewModelFactory para pasar el repositorio al ViewModel
    class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NoteViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Verificar autenticación
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Instanciar Room DB y repositorio
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "notes-db"
        ).fallbackToDestructiveMigration().build()

        val repository = NoteRepository(
            db.noteDao(),
            db.userDao(),
            db.tagDao(),
            db.attachmentDao(),
            db.noteTagDao(),
            applicationContext
        )

        // Obtener ViewModel con el repositorio
        val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory(repository) }

        setContent {
            AppTrackSolutionsGobeaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NotesScreen(
                        modifier = Modifier.padding(innerPadding),
                        noteViewModel = noteViewModel,
                        onSignOut = { signOut() }
                    )
                }
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
