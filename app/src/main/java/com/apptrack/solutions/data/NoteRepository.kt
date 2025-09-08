package com.apptrack.solutions.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import com.apptrack.solutions.model.Note
import com.apptrack.solutions.model.Tag
import com.apptrack.solutions.model.User
import com.apptrack.solutions.model.Attachment
import com.apptrack.solutions.model.NoteTag
import com.apptrack.solutions.util.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class NoteRepository(
    private val noteDao: NoteDao,
    private val userDao: UserDao,
    private val tagDao: TagDao,
    private val attachmentDao: AttachmentDao,
    private val noteTagDao: NoteTagDao,
    private val context: Context
) {

    companion object {
        private const val TAG = "NoteRepository"
        private const val USERS_COLLECTION = "users"
        private const val NOTES_COLLECTION = "notes"
        private const val TAGS_COLLECTION = "tags"
        private const val ATTACHMENTS_COLLECTION = "attachments"
    }

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val networkUtils = NetworkUtils(context)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Monitorear cambios de conectividad para sincronización automática
        syncScope.launch {
            networkUtils.wasDisconnected.collect { wasDisconnected ->
                if (wasDisconnected && networkUtils.isConnected.value) {
                    Log.d(TAG, "Connection restored - starting automatic sync")
                    syncUnsyncedNotes()
                    networkUtils.resetDisconnectedFlag()
                }
            }
        }
    }

    // User operations
    suspend fun insertUser(user: User) {
        Log.d(TAG, "insertUser: Iniciando inserción de usuario: ${user.id} - ${user.name}")

        try {
            // Guardar en Room (local) PRIMERO - esto siempre debe funcionar
            userDao.insertUser(user)
            Log.d(TAG, "insertUser: Usuario guardado en Room exitosamente")

            // Intentar guardar en Firestore (nube) - si falla, no es crítico
            try {
                val userMap = mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "email" to user.email,
                    "createdAt" to user.createdAt
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.id)
                    .set(userMap)
                    .await()

                Log.d(TAG, "insertUser: Usuario guardado en Firestore exitosamente")

            } catch (firestoreException: Exception) {
                Log.w(TAG, "insertUser: Error guardando en Firestore (continuando con Room solamente)", firestoreException)
                // No lanzar excepción - la operación local fue exitosa
            }

        } catch (e: Exception) {
            Log.e(TAG, "insertUser: Error crítico guardando usuario", e)
            throw e
        }
    }

    fun getAllUsers(): Flow<List<User>> {
        Log.d(TAG, "getAllUsers: Obteniendo usuarios de Room")
        return userDao.getAllUsers()
            .catch { e ->
                Log.e(TAG, "getAllUsers: Error obteniendo usuarios", e)
                throw e
            }
    }

    suspend fun getUserById(userId: String): User? {
        Log.d(TAG, "getUserById: Buscando usuario $userId")
        return userDao.getUserById(userId)
    }

    // Note operations
    suspend fun insertNote(note: Note) {
        Log.d(TAG, "insertNote: Iniciando inserción de nota: ${note.id} - ${note.title}")

        try {
            // Guardar en Room (local) PRIMERO - esto siempre debe funcionar
            val noteToInsert = note.copy(isSynced = false)
            noteDao.insertNote(noteToInsert)
            Log.d(TAG, "insertNote: Nota guardada en Room exitosamente")

            // Intentar guardar en Firestore (nube) - si falla, no es crítico
            if (networkUtils.isConnected.value) {
                try {
                    val noteMap = mapOf(
                        "id" to noteToInsert.id,
                        "userId" to noteToInsert.userId,
                        "title" to noteToInsert.title,
                        "content" to noteToInsert.content,
                        "createdAt" to noteToInsert.createdAt,
                        "updatedAt" to noteToInsert.updatedAt,
                        "isSynced" to true
                    )

                    firestore.collection(NOTES_COLLECTION)
                        .document(noteToInsert.id)
                        .set(noteMap)
                        .await()

                    // Marcar como sincronizada
                    noteDao.updateSyncStatus(noteToInsert.id, true)
                    Log.d(TAG, "insertNote: Nota guardada en Firestore y marcada como sincronizada")

                } catch (firestoreException: Exception) {
                    Log.w(TAG, "insertNote: Error guardando en Firestore (se sincronizará más tarde)", firestoreException)
                    // La nota queda marcada como no sincronizada para reintento posterior
                }
            } else {
                Log.d(TAG, "insertNote: Sin conexión, nota se sincronizará cuando haya conexión")
            }

        } catch (e: Exception) {
            Log.e(TAG, "insertNote: Error crítico guardando nota", e)
            throw e
        }
    }

    suspend fun updateNote(note: Note) {
        Log.d(TAG, "updateNote: Actualizando nota: ${note.id}")

        try {
            val updatedNote = note.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false // Marcar como no sincronizada al actualizar
            )

            // Actualizar en Room (local) PRIMERO
            noteDao.updateNote(updatedNote)
            Log.d(TAG, "updateNote: Nota actualizada en Room")

            // Intentar actualizar en Firestore
            if (networkUtils.isConnected.value) {
                try {
                    val noteMap = mapOf(
                        "id" to updatedNote.id,
                        "userId" to updatedNote.userId,
                        "title" to updatedNote.title,
                        "content" to updatedNote.content,
                        "createdAt" to updatedNote.createdAt,
                        "updatedAt" to updatedNote.updatedAt,
                        "isSynced" to true
                    )

                    firestore.collection(NOTES_COLLECTION)
                        .document(updatedNote.id)
                        .set(noteMap)
                        .await()

                    // Marcar como sincronizada
                    noteDao.updateSyncStatus(updatedNote.id, true)
                    Log.d(TAG, "updateNote: Nota actualizada en Firestore y marcada como sincronizada")

                } catch (firestoreException: Exception) {
                    Log.w(TAG, "updateNote: Error actualizando en Firestore (se sincronizará más tarde)", firestoreException)
                }
            } else {
                Log.d(TAG, "updateNote: Sin conexión, nota se sincronizará cuando haya conexión")
            }

        } catch (e: Exception) {
            Log.e(TAG, "updateNote: Error actualizando nota", e)
            throw e
        }
    }

    suspend fun deleteNote(note: Note) {
        Log.d(TAG, "deleteNote: Eliminando nota: ${note.id}")

        try {
            // Eliminar de Room (local) PRIMERO
            noteDao.deleteNote(note)
            Log.d(TAG, "deleteNote: Nota eliminada de Room")

            // Intentar eliminar de Firestore
            try {
                firestore.collection(NOTES_COLLECTION)
                    .document(note.id)
                    .delete()
                    .await()

                Log.d(TAG, "deleteNote: Nota eliminada de Firestore")

            } catch (firestoreException: Exception) {
                Log.w(TAG, "deleteNote: Error eliminando de Firestore (continuando con Room solamente)", firestoreException)
            }

        } catch (e: Exception) {
            Log.e(TAG, "deleteNote: Error eliminando nota", e)
            throw e
        }
    }

    fun getAllNotes(): Flow<List<Note>> {
        Log.d(TAG, "getAllNotes: Obteniendo todas las notas")
        return noteDao.getAllNotes()
            .catch { e ->
                Log.e(TAG, "getAllNotes: Error obteniendo notas", e)
                throw e
            }
    }

    fun getNotesByUser(userId: String): Flow<List<Note>> {
        Log.d(TAG, "getNotesByUser: Obteniendo notas para usuario $userId")
        return noteDao.getNotesByUser(userId)
            .catch { e ->
                Log.e(TAG, "getNotesByUser: Error obteniendo notas del usuario", e)
                throw e
            }
    }

    suspend fun getNoteById(noteId: String): Note? {
        Log.d(TAG, "getNoteById: Buscando nota $noteId")
        return noteDao.getNoteById(noteId)
    }

    // Tag operations
    suspend fun insertTag(tag: Tag) {
        Log.d(TAG, "insertTag: Insertando etiqueta: ${tag.id} - ${tag.name}")

        try {
            // Guardar en Room (local) PRIMERO - esto siempre debe funcionar
            tagDao.insertTag(tag)
            Log.d(TAG, "insertTag: Etiqueta guardada en Room exitosamente")

            // Intentar guardar en Firestore (nube) - si falla, no es crítico
            try {
                val tagMap = mapOf(
                    "id" to tag.id,
                    "name" to tag.name,
                    "color" to tag.color,
                    "createdAt" to tag.createdAt
                )

                firestore.collection(TAGS_COLLECTION)
                    .document(tag.id)
                    .set(tagMap)
                    .await()

                Log.d(TAG, "insertTag: Etiqueta guardada en Firestore exitosamente")

            } catch (firestoreException: Exception) {
                Log.w(TAG, "insertTag: Error guardando en Firestore (continuando con Room solamente)", firestoreException)
                // No lanzar excepción - la operación local fue exitosa
            }

        } catch (e: Exception) {
            Log.e(TAG, "insertTag: Error crítico guardando etiqueta", e)
            throw e
        }
    }

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)
    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    fun getAllTags(): Flow<List<Tag>> {
        Log.d(TAG, "getAllTags: Obteniendo todas las etiquetas")
        return tagDao.getAllTags()
            .catch { e ->
                Log.e(TAG, "getAllTags: Error obteniendo etiquetas", e)
                throw e
            }
    }

    suspend fun getTagById(tagId: String): Tag? = tagDao.getTagById(tagId)
    fun getTagsForNote(noteId: String): Flow<List<Tag>> = tagDao.getTagsForNote(noteId)

    // NoteTag operations
    suspend fun insertNoteTag(noteTag: NoteTag) {
        Log.d(TAG, "insertNoteTag: Asociando nota ${noteTag.noteId} con etiqueta ${noteTag.tagId}")

        try {
            // Guardar en Room (local) PRIMERO
            noteTagDao.insertNoteTag(noteTag)
            Log.d(TAG, "insertNoteTag: Asociación guardada en Room")

            // Intentar guardar en Firestore
            try {
                val noteTagMap = mapOf(
                    "noteId" to noteTag.noteId,
                    "tagId" to noteTag.tagId,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("note_tags")
                    .document("${noteTag.noteId}_${noteTag.tagId}")
                    .set(noteTagMap)
                    .await()

                Log.d(TAG, "insertNoteTag: Asociación guardada en Firestore")

            } catch (firestoreException: Exception) {
                Log.w(TAG, "insertNoteTag: Error guardando en Firestore (continuando con Room solamente)", firestoreException)
            }

        } catch (e: Exception) {
            Log.e(TAG, "insertNoteTag: Error guardando asociación", e)
            throw e
        }
    }

    suspend fun deleteNoteTag(noteTag: NoteTag) = noteTagDao.deleteNoteTag(noteTag)
    suspend fun deleteNoteTagsByNote(noteId: String) = noteTagDao.deleteNoteTagsByNote(noteId)
    suspend fun deleteNoteTagsByTag(tagId: String) = noteTagDao.deleteNoteTagsByTag(tagId)

    // Attachment operations
    suspend fun insertAttachment(attachment: Attachment) {
        Log.d(TAG, "insertAttachment: Insertando adjunto: ${attachment.id}")

        try {
            // Guardar en Room (local) PRIMERO
            attachmentDao.insertAttachment(attachment)
            Log.d(TAG, "insertAttachment: Adjunto guardado en Room")

            // Intentar guardar en Firestore
            try {
                val attachmentMap = mapOf(
                    "id" to attachment.id,
                    "noteId" to attachment.noteId,
                    "uri" to attachment.uri,
                    "fileName" to attachment.fileName,
                    "mimeType" to attachment.mimeType,
                    "createdAt" to attachment.createdAt
                )

                firestore.collection(ATTACHMENTS_COLLECTION)
                    .document(attachment.id)
                    .set(attachmentMap)
                    .await()

                Log.d(TAG, "insertAttachment: Adjunto guardado en Firestore")

            } catch (firestoreException: Exception) {
                Log.w(TAG, "insertAttachment: Error guardando en Firestore (continuando con Room solamente)", firestoreException)
            }

        } catch (e: Exception) {
            Log.e(TAG, "insertAttachment: Error guardando adjunto", e)
            throw e
        }
    }

    suspend fun updateAttachment(attachment: Attachment) = attachmentDao.updateAttachment(attachment)
    suspend fun deleteAttachment(attachment: Attachment) = attachmentDao.deleteAttachment(attachment)
    fun getAttachmentsForNote(noteId: String): Flow<List<Attachment>> = attachmentDao.getAttachmentsForNote(noteId)
    suspend fun getAttachmentById(attachmentId: String): Attachment? = attachmentDao.getAttachmentById(attachmentId)

    // ==================== FUNCIONES DE SINCRONIZACIÓN BIDIRECCIONAL ====================

    /**
     * Sincroniza todas las notas no sincronizadas con Firestore
     */
    suspend fun syncUnsyncedNotes() {
        if (!networkUtils.isConnected.value) {
            Log.d(TAG, "syncUnsyncedNotes: Sin conexión, cancelando sincronización")
            return
        }

        try {
            Log.d(TAG, "syncUnsyncedNotes: Iniciando sincronización de notas pendientes")
            val unsyncedNotes = noteDao.getUnsyncedNotes()
            Log.d(TAG, "syncUnsyncedNotes: Encontradas ${unsyncedNotes.size} notas por sincronizar")

            for (note in unsyncedNotes) {
                try {
                    val noteMap = mapOf(
                        "id" to note.id,
                        "userId" to note.userId,
                        "title" to note.title,
                        "content" to note.content,
                        "createdAt" to note.createdAt,
                        "updatedAt" to note.updatedAt,
                        "isSynced" to true
                    )

                    firestore.collection(NOTES_COLLECTION)
                        .document(note.id)
                        .set(noteMap)
                        .await()

                    // Marcar como sincronizada
                    noteDao.updateSyncStatus(note.id, true)
                    Log.d(TAG, "syncUnsyncedNotes: Nota ${note.id} sincronizada exitosamente")

                } catch (e: Exception) {
                    Log.e(TAG, "syncUnsyncedNotes: Error sincronizando nota ${note.id}", e)
                    // Continuar con la siguiente nota
                }
            }

            Log.d(TAG, "syncUnsyncedNotes: Sincronización completada")

        } catch (e: Exception) {
            Log.e(TAG, "syncUnsyncedNotes: Error general en sincronización", e)
        }
    }

    /**
     * Descarga notas desde Firestore y las sincroniza localmente
     * Implementa resolución de conflictos con "última escritura gana"
     */
    suspend fun downloadNotesFromFirestore(userId: String) {
        if (!networkUtils.isConnected.value) {
            Log.d(TAG, "downloadNotesFromFirestore: Sin conexión, cancelando descarga")
            return
        }

        try {
            Log.d(TAG, "downloadNotesFromFirestore: Descargando notas para usuario $userId")

            val querySnapshot = firestore.collection(NOTES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d(TAG, "downloadNotesFromFirestore: Encontradas ${querySnapshot.size()} notas en Firestore")

            for (document in querySnapshot.documents) {
                try {
                    val data = document.data ?: continue
                    
                    val firestoreNote = Note(
                        id = data["id"] as String,
                        userId = data["userId"] as String,
                        title = data["title"] as String,
                        content = data["content"] as String,
                        createdAt = (data["createdAt"] as Number).toLong(),
                        updatedAt = (data["updatedAt"] as Number).toLong(),
                        isSynced = true // Las notas de Firestore están sincronizadas
                    )

                    // Verificar si existe localmente
                    val localNote = noteDao.getNoteById(firestoreNote.id)

                    if (localNote == null) {
                        // Nota nueva, insertar directamente
                        noteDao.insertNote(firestoreNote)
                        Log.d(TAG, "downloadNotesFromFirestore: Nueva nota ${firestoreNote.id} insertada")
                    } else {
                        // Conflicto: aplicar "última escritura gana"
                        if (firestoreNote.updatedAt > localNote.updatedAt) {
                            noteDao.updateNote(firestoreNote)
                            Log.d(TAG, "downloadNotesFromFirestore: Nota ${firestoreNote.id} actualizada (Firestore más reciente)")
                        } else if (localNote.updatedAt > firestoreNote.updatedAt && !localNote.isSynced) {
                            // La versión local es más reciente y no está sincronizada
                            // Subir la versión local a Firestore
                            val noteMap = mapOf(
                                "id" to localNote.id,
                                "userId" to localNote.userId,
                                "title" to localNote.title,
                                "content" to localNote.content,
                                "createdAt" to localNote.createdAt,
                                "updatedAt" to localNote.updatedAt,
                                "isSynced" to true
                            )

                            firestore.collection(NOTES_COLLECTION)
                                .document(localNote.id)
                                .set(noteMap)
                                .await()

                            noteDao.updateSyncStatus(localNote.id, true)
                            Log.d(TAG, "downloadNotesFromFirestore: Nota ${localNote.id} local más reciente, subida a Firestore")
                        }
                        // Si las versiones son iguales o la local está sincronizada, no hacer nada
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "downloadNotesFromFirestore: Error procesando nota ${document.id}", e)
                }
            }

            Log.d(TAG, "downloadNotesFromFirestore: Descarga completada")

        } catch (e: Exception) {
            Log.e(TAG, "downloadNotesFromFirestore: Error general en descarga", e)
        }
    }

    /**
     * Realiza sincronización completa: sube notas pendientes y descarga cambios remotos
     */
    suspend fun performFullSync(userId: String) {
        Log.d(TAG, "performFullSync: Iniciando sincronización completa para usuario $userId")
        
        // Primero subir cambios locales
        syncUnsyncedNotes()
        
        // Luego descargar cambios remotos
        downloadNotesFromFirestore(userId)
        
        Log.d(TAG, "performFullSync: Sincronización completa finalizada")
    }

    /**
     * Obtiene el estado de conectividad
     */
    fun isConnected(): Boolean = networkUtils.isConnected.value

    /**
     * Limpia recursos de red
     */
    fun cleanup() {
        networkUtils.cleanup()
    }
}
