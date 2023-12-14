package com.example.flashcardapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.flashcardapplication.dao.ApplicationDao
import com.example.flashcardapplication.models.Folder
import com.example.flashcardapplication.models.Terminology
import com.example.flashcardapplication.models.Topic
import com.example.flashcardapplication.models.TopicFolderCrossRef

@Database(
    entities = [Terminology::class,
        Topic::class,
        Folder::class,
        TopicFolderCrossRef::class],
    version = 1,
    exportSchema = false)
abstract class RoomDb : RoomDatabase() {
    abstract fun ApplicationDao(): ApplicationDao
    companion object {
        @Volatile
        private var INSTANCE: RoomDb? = null
        private const val DATABASE_NAME = "flashcard_database"
        fun getDatabase(context: Context): RoomDb {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoomDb::class.java,
                    DATABASE_NAME
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}