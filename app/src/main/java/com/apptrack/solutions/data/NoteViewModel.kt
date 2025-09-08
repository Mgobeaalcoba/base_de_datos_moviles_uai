package com.apptrack.solutions.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptrack.solutions.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    fun loadNotes() {
        viewModelScope.launch {
            val localNotes = repository.getLocalNotes()
            _notes.value = localNotes
        }
    }

    fun addNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
            loadNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
            loadNotes()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            loadNotes()
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            val cloudNotes = repository.fetchNotesFromCloud()
            cloudNotes.forEach { repository.insertNote(it) }
            loadNotes()
        }
    }
}
