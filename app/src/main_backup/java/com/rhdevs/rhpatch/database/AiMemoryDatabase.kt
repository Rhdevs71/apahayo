package com.rhdevs.rhpatch.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AiMemoryEntity::class], version = 1, exportSchema = false)
abstract class AiMemoryDatabase : RoomDatabase() {
    abstract fun aiMemoryDao(): AiMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AiMemoryDatabase? = null

        fun getInstance(context: Context): AiMemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiMemoryDatabase::class.java,
                    "ai_memory.db"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
