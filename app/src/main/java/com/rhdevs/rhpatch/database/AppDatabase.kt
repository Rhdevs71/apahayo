package com.rhdevs.rhpatch.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.rhdevs.rhpatch.scheduler.db.UniversalTaskEntity
import com.rhdevs.rhpatch.scheduler.db.UniversalTemplateEntity
import com.rhdevs.rhpatch.scheduler.db.UniversalRecipientEntity
import com.rhdevs.rhpatch.scheduler.db.UniversalSchedulerDao

@Database(
    entities = [
        ScheduledMessage::class, 
        AutoReplyRule::class, 
        FakeChatBackup::class,
        UniversalTaskEntity::class,
        UniversalTemplateEntity::class,
        UniversalRecipientEntity::class
    ], 
    version = 7, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun autoReplyRuleDao(): AutoReplyRuleDao
    abstract fun fakeChatBackupDao(): FakeChatBackupDao
    abstract fun universalSchedulerDao(): UniversalSchedulerDao

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
