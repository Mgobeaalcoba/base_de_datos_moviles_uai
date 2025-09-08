package com.apptrack.solutions.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.apptrack.solutions.model.Note
import com.apptrack.solutions.model.Tag
import com.apptrack.solutions.model.User
import com.apptrack.solutions.model.Attachment
import com.apptrack.solutions.model.NoteTag

@Database(
    entities = [
        User::class,
        Note::class,
        Tag::class,
        Attachment::class,
        NoteTag::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun noteTagDao(): NoteTagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes-db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
