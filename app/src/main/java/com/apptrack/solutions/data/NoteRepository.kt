package com.apptrack.solutions.data

import android.util.Log
import com.apptrack.solutions.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NoteRepository(
    private val noteDao: NoteDao,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val userId: String? get() = auth.currentUser?.uid

    suspend fun getLocalNotes(): List<Note> = noteDao.getAllNotes()

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
        syncNoteToCloud(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        syncNoteToCloud(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        deleteNoteFromCloud(note)
    }

    suspend fun syncNoteToCloud(note: Note) {
        userId?.let { uid ->
            Log.d("FirestoreSync", "Sincronizando nota para UID: $uid, noteId: ${note.id}")
            db.collection("notes").document(uid)
                .collection("user_notes").document(note.id)
                .set(note)
                .await()
        } ?: Log.e("FirestoreSync", "userId es null, no se puede sincronizar")
    }

    suspend fun deleteNoteFromCloud(note: Note) {
        userId?.let { uid ->
            db.collection("notes").document(uid)
                .collection("user_notes").document(note.id)
                .delete()
                .await()
        }
    }

    suspend fun fetchNotesFromCloud(): List<Note> {
        userId?.let { uid ->
            val snapshot = db.collection("notes").document(uid)
                .collection("user_notes").get().await()
            return snapshot.documents.mapNotNull { it.toObject(Note::class.java) }
        }
        return emptyList()
    }
}
