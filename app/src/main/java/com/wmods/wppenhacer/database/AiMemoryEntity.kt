package com.wmods.wppenhacer.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Entity(tableName = "ai_memory")
data class AiMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jid: String,
    val role: String, // "user" or "model" / "assistant"
    val messageText: String,
    val timestamp: Long
)

@Dao
interface AiMemoryDao {
    @Insert
    fun insertMemory(memory: AiMemoryEntity)

    @Query("SELECT * FROM ai_memory WHERE jid = :jid ORDER BY timestamp ASC")
    fun getMemoriesByJid(jid: String): List<AiMemoryEntity>

    @Query("DELETE FROM ai_memory WHERE id IN (SELECT id FROM ai_memory WHERE jid = :jid ORDER BY timestamp ASC LIMIT 1)")
    fun deleteOldestMemory(jid: String)

    @Query("SELECT COUNT(*) FROM ai_memory WHERE jid = :jid")
    fun getMemoryCount(jid: String): Int
}
