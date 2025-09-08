package com.apptrack.solutions.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apptrack.solutions.model.Note

@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
