package com.apptrack.solutions.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String = "#2196F3", // Color azul por defecto
    val createdAt: Long = System.currentTimeMillis()
)
