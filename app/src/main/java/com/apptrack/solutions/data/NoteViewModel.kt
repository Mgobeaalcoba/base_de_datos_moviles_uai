package com.apptrack.solutions.data

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
        val currentUser = _selectedUser.value
        if (currentUser == null) {
            _errorMessage.value = "Debe seleccionar un usuario"
            return
        }

        if (title.isBlank()) {
            _errorMessage.value = "El título no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val note = Note(
                    userId = currentUser.id,
                    title = title.trim(),
                    content = content.trim()
                )

                // Insertar la nota
                repository.insertNote(note)

                // Asociar etiquetas a la nota
                selectedTags.forEach { tag ->
                    repository.insertNoteTag(NoteTag(noteId = note.id, tagId = tag.id))
                }

                // Agregar adjunto si existe
                if (!attachmentUri.isNullOrBlank()) {
                    val attachment = Attachment(
                        noteId = note.id,
                        uri = attachmentUri,
                        fileName = "attachment_${System.currentTimeMillis()}",
                        mimeType = "application/octet-stream"
                    )
                    repository.insertAttachment(attachment)
                }

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error creando nota: ${e.message}"
            } finally {
                _isLoading.value = false
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
}
