package com.apptrack.solutions.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.apptrack.solutions.model.Tag

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: String): Tag?

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tags ON tags.id = note_tags.tagId
        WHERE note_tags.noteId = :noteId
        ORDER BY tags.name ASC
    """)
    fun getTagsForNote(noteId: String): Flow<List<Tag>>
}
