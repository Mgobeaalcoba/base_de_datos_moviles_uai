package com.apptrack.solutions.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.apptrack.solutions.model.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotesByUser(userId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): Note?

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteNotesByUser(userId: String)

    // Queries específicas para sincronización
    @Query("SELECT * FROM notes WHERE isSynced = 0 ORDER BY updatedAt DESC")
    suspend fun getUnsyncedNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND isSynced = 0 ORDER BY updatedAt DESC")
    suspend fun getUnsyncedNotesByUser(userId: String): List<Note>

    @Query("UPDATE notes SET isSynced = :isSynced WHERE id = :noteId")
    suspend fun updateSyncStatus(noteId: String, isSynced: Boolean)
}
