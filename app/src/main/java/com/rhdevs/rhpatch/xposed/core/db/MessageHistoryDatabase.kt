package com.rhdevs.rhpatch.xposed.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rhdevs.rhpatch.xposed.core.db.dao.HideSeenDao
import com.rhdevs.rhpatch.xposed.core.db.dao.MessageDao
import com.rhdevs.rhpatch.xposed.core.db.entity.HideSeenEntity
import com.rhdevs.rhpatch.xposed.core.db.entity.MessageEntity

@Database(
    entities = [MessageEntity::class, HideSeenEntity::class],
    version = 5,
    exportSchema = false
)
abstract class MessageHistoryDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun hideSeenDao(): HideSeenDao
}
