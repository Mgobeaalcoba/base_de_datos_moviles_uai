package com.apptrack.solutions.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.apptrack.solutions.model.NoteTag

@Dao
interface NoteTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTag(noteTag: NoteTag)

    @Delete
    suspend fun deleteNoteTag(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteNoteTagsByNote(noteId: String)

    @Query("DELETE FROM note_tags WHERE tagId = :tagId")
    suspend fun deleteNoteTagsByTag(tagId: String)

    @Query("SELECT * FROM note_tags WHERE noteId = :noteId")
    fun getNoteTagsByNote(noteId: String): Flow<List<NoteTag>>

    @Query("SELECT * FROM note_tags WHERE tagId = :tagId")
    fun getNoteTagsByTag(tagId: String): Flow<List<NoteTag>>
}
