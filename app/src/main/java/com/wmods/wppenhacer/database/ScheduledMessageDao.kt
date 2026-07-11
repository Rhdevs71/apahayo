package com.wmods.wppenhacer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledTime ASC")
    fun getAll(): List<ScheduledMessage>

    @Query("SELECT * FROM scheduled_messages WHERE status = :status ORDER BY scheduledTime ASC")
    fun getByStatus(status: String): List<ScheduledMessage>

    @Query("SELECT * FROM scheduled_messages WHERE scheduledTime <= :time AND status = 'PENDING'")
    fun getPendingBefore(time: Long): List<ScheduledMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: ScheduledMessage): Long

    @Update
    fun update(message: ScheduledMessage)

    @Delete
    fun delete(message: ScheduledMessage)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT * FROM scheduled_messages WHERE id = :id LIMIT 1")
    fun getById(id: Int): ScheduledMessage?
}
