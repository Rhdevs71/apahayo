package com.rhdevs.rhpatch.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "auto_reply_rules")
data class AutoReplyRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keywords: String, // Comma-separated or single word
    val matchingType: String = "EXACT", // "EXACT", "CONTAINS", "REGEX"
    val replyText: String,
    val quoteOriginal: Boolean = false,
    val delaySeconds: Int = 0,
    val targetType: String = "ALL", // "ALL", "CONTACTS", "GROUPS", "NON_CONTACTS"
    val activeHoursStart: String? = null, // "HH:mm" format or null
    val activeHoursEnd: String? = null, // "HH:mm" format or null
    val isEnabled: Boolean = true,
    val isForward: Boolean = false,
    val forwardJid: String? = null,
    val isAi: Boolean = false,
    val ignoreCase: Boolean = true,
    val targetContacts: String? = null,
    val replyType: String = "TEXT", // "TEXT", "IMAGE", "MULTIPLE", "RANDOM"
    val aiProvider: String? = null, // "gemini", "chatgpt", "groq"
    val aiPrompt: String? = null,
    val attachmentUri: String? = null
) : Serializable
