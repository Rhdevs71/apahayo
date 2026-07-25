package com.rhdevs.rhpatch.scheduler.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "universal_tasks")
data class UniversalTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetApp: String, // "whatsapp", "telegram", "sms", "email"
    val recipientName: String,
    val recipientPhoneOrEmail: String,
    val message: String,
    val triggerTimeMillis: Long,
    val status: String // "PENDING", "COMPLETED", "FAILED"
)

@Entity(tableName = "universal_templates")
data class UniversalTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String
)

@Entity(tableName = "universal_recipients")
data class UniversalRecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneOrEmail: String,
    val groupName: String // e.g. "Family", "Work"
)
