package com.apptrack.solutions.data

import kotlinx.coroutines.flow.Flow
import com.apptrack.solutions.model.Note
import com.apptrack.solutions.model.Tag
import com.apptrack.solutions.model.User
import com.apptrack.solutions.model.Attachment
import com.apptrack.solutions.model.NoteTag

class NoteRepository(
    private val noteDao: NoteDao,
    private val userDao: UserDao,
    private val tagDao: TagDao,
    private val attachmentDao: AttachmentDao,
    private val noteTagDao: NoteTagDao
) {
    // User operations
    suspend fun insertUser(user: User) = userDao.insertUser(user)

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)

    // Note operations
    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesByUser(userId: String): Flow<List<Note>> = noteDao.getNotesByUser(userId)

    suspend fun getNoteById(noteId: String): Note? = noteDao.getNoteById(noteId)

    // Tag operations
    suspend fun insertTag(tag: Tag) = tagDao.insertTag(tag)

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun getTagById(tagId: String): Tag? = tagDao.getTagById(tagId)

    fun getTagsForNote(noteId: String): Flow<List<Tag>> = tagDao.getTagsForNote(noteId)

    // NoteTag operations
    suspend fun insertNoteTag(noteTag: NoteTag) = noteTagDao.insertNoteTag(noteTag)

    suspend fun deleteNoteTag(noteTag: NoteTag) = noteTagDao.deleteNoteTag(noteTag)

    suspend fun deleteNoteTagsByNote(noteId: String) = noteTagDao.deleteNoteTagsByNote(noteId)

    suspend fun deleteNoteTagsByTag(tagId: String) = noteTagDao.deleteNoteTagsByTag(tagId)

    // Attachment operations
    suspend fun insertAttachment(attachment: Attachment) = attachmentDao.insertAttachment(attachment)

    suspend fun updateAttachment(attachment: Attachment) = attachmentDao.updateAttachment(attachment)

    suspend fun deleteAttachment(attachment: Attachment) = attachmentDao.deleteAttachment(attachment)

    fun getAttachmentsForNote(noteId: String): Flow<List<Attachment>> = attachmentDao.getAttachmentsForNote(noteId)

    suspend fun getAttachmentById(attachmentId: String): Attachment? = attachmentDao.getAttachmentById(attachmentId)
}
