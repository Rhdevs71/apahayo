package com.rhdevs.rhpatch.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jid: String,
    val contactName: String,
    val messageText: String,
    val mediaPath: String? = null,
    val mediaType: String? = null, // "IMAGE", "VIDEO", "DOCUMENT", "AUDIO", or null
    var scheduledTime: Long,
    val isRecurring: Boolean = false,
    val recurrenceType: String = "ONCE", // "ONCE", "DAILY", "WEEKLY", "MONTHLY", "SPECIFIC_DAYS"
    val recurrenceDays: String? = null, // Comma-separated numbers e.g. "1,2,5" (1=Sunday, 2=Monday...)
    var status: String = "PENDING", // "PENDING", "SENT", "FAILED"
    val autoDelete: Boolean = false,
    val targetPackage: String? = "BOTH"
) : Serializable
