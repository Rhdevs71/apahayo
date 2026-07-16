package com.wmods.wppenhacer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScheduledMessage::class, AutoReplyRule::class, FakeChatBackup::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun autoReplyRuleDao(): AutoReplyRuleDao
    abstract fun fakeChatBackupDao(): FakeChatBackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wa_enhancer_app.db"
                )
                    .allowMainThreadQueries() // Simplifies code for settings-based queries
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
