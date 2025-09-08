package com.apptrack.solutions.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.apptrack.solutions.model.Note
import com.apptrack.solutions.model.Tag
import com.apptrack.solutions.model.User
import com.apptrack.solutions.model.Attachment
import com.apptrack.solutions.model.NoteTag

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadUsers()
        loadAllTags()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getAllUsers().collect { userList ->
                    _users.value = userList
                    // Si solo hay un usuario, seleccionarlo automáticamente
                    if (userList.size == 1 && _selectedUser.value == null) {
                        selectUser(userList.first())
                    }
                    // Importante: Salir del estado de loading una vez que se cargan los usuarios
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando usuarios: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun selectUser(user: User) {
        _selectedUser.value = user
        loadNotesForUser(user.id)
        // Realizar sincronización completa al seleccionar usuario
        performFullSync(user.id)
    }

    private fun loadNotesForUser(userId: String) {
        viewModelScope.launch {
            try {
                repository.getNotesByUser(userId).collect { noteList ->
                    _notes.value = noteList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando notas: ${e.message}"
            }
        }
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            try {
                repository.getAllTags().collect { tagList ->
                    _tags.value = tagList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando etiquetas: ${e.message}"
            }
        }
    }

    fun createNote(title: String, content: String, selectedTags: List<Tag>, attachmentUri: String?) {
        Log.d("NoteViewModel", "createNote: Iniciando creación de nota")
        Log.d("NoteViewModel", "createNote: Título: '$title'")
        Log.d("NoteViewModel", "createNote: Contenido: '${content.take(50)}...'")
        Log.d("NoteViewModel", "createNote: Etiquetas seleccionadas: ${selectedTags.size}")
        Log.d("NoteViewModel", "createNote: URI adjunto: $attachmentUri")

        val currentUser = _selectedUser.value
        if (currentUser == null) {
            Log.e("NoteViewModel", "createNote: Error - No hay usuario seleccionado")
            _errorMessage.value = "Debe seleccionar un usuario"
            return
        }

        Log.d("NoteViewModel", "createNote: Usuario actual: ${currentUser.id} - ${currentUser.name}")

        if (title.isBlank()) {
            Log.e("NoteViewModel", "createNote: Error - Título vacío")
            _errorMessage.value = "El título no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("NoteViewModel", "createNote: Estado loading activado")

                val note = Note(
                    userId = currentUser.id,
                    title = title.trim(),
                    content = content.trim()
                )

                Log.d("NoteViewModel", "createNote: Nota creada con ID: ${note.id}")

                // Insertar la nota
                Log.d("NoteViewModel", "createNote: Insertando nota en repositorio")
                repository.insertNote(note)
                Log.d("NoteViewModel", "createNote: Nota insertada exitosamente")

                // Asociar etiquetas a la nota
                Log.d("NoteViewModel", "createNote: Asociando ${selectedTags.size} etiquetas")
                selectedTags.forEach { tag ->
                    Log.d("NoteViewModel", "createNote: Asociando etiqueta: ${tag.name} (${tag.id})")
                    repository.insertNoteTag(NoteTag(noteId = note.id, tagId = tag.id))
                }
                Log.d("NoteViewModel", "createNote: Etiquetas asociadas exitosamente")

                // Agregar adjunto si existe
                if (!attachmentUri.isNullOrBlank()) {
                    Log.d("NoteViewModel", "createNote: Agregando adjunto")
                    val attachment = Attachment(
                        noteId = note.id,
                        uri = attachmentUri,
                        fileName = "attachment_${System.currentTimeMillis()}",
                        mimeType = "application/octet-stream"
                    )
                    repository.insertAttachment(attachment)
                    Log.d("NoteViewModel", "createNote: Adjunto agregado exitosamente")
                }

                Log.d("NoteViewModel", "createNote: Nota creada completamente. Limpiando errores")
                _errorMessage.value = null

            } catch (e: Exception) {
                Log.e("NoteViewModel", "createNote: Error creando nota", e)
                _errorMessage.value = "Error creando nota: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d("NoteViewModel", "createNote: Estado loading desactivado")
            }
        }
    }

    fun createTag(tagName: String): Tag? {
        if (tagName.isBlank()) {
            _errorMessage.value = "El nombre de la etiqueta no puede estar vacío"
            return null
        }

        val trimmedName = tagName.trim()

        // Verificar si ya existe una etiqueta con ese nombre
        val existingTag = _tags.value.find { it.name.equals(trimmedName, ignoreCase = true) }
        if (existingTag != null) {
            return existingTag
        }

        val newTag = Tag(name = trimmedName)

        viewModelScope.launch {
            try {
                repository.insertTag(newTag)
            } catch (e: Exception) {
                _errorMessage.value = "Error creando etiqueta: ${e.message}"
            }
        }

        return newTag
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                repository.deleteNote(note)
            } catch (e: Exception) {
                _errorMessage.value = "Error eliminando nota: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== FUNCIONES DE SINCRONIZACIÓN ====================

    /**
     * Realiza sincronización completa para el usuario seleccionado
     */
    private fun performFullSync(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("NoteViewModel", "performFullSync: Iniciando sincronización para usuario $userId")
                repository.performFullSync(userId)
                Log.d("NoteViewModel", "performFullSync: Sincronización completada")
            } catch (e: Exception) {
                Log.e("NoteViewModel", "performFullSync: Error en sincronización", e)
                // No mostrar error al usuario para no interrumpir la experiencia
            }
        }
    }

    /**
     * Sincroniza manualmente las notas pendientes
     */
    fun syncPendingNotes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.syncUnsyncedNotes()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error sincronizando notas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Obtiene el estado de conectividad
     */
    fun isConnected(): Boolean = repository.isConnected()

    /**
     * Limpia recursos cuando el ViewModel se destruye
     */
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
