package com.apptrack.solutions.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class Attachment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis()
)
