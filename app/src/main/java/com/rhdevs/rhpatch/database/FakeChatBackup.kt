package com.rhdevs.rhpatch.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "fake_chat_backups")
data class FakeChatBackup(
    @PrimaryKey val messageId: String,
    val originalText: String,
    val originalTimestamp: Long
) : Serializable
