package com.rhdevs.rhpatch.scheduler.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UniversalSchedulerDao {
    // Tasks
    @Query("SELECT * FROM universal_tasks ORDER BY triggerTimeMillis ASC")
    fun getAllTasks(): List<UniversalTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: UniversalTaskEntity): Long

    @Update
    fun updateTask(task: UniversalTaskEntity)

    @Delete
    fun deleteTask(task: UniversalTaskEntity)

    // Templates
    @Query("SELECT * FROM universal_templates")
    fun getAllTemplates(): List<UniversalTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTemplate(template: UniversalTemplateEntity)

    @Delete
    fun deleteTemplate(template: UniversalTemplateEntity)

    // Recipients
    @Query("SELECT * FROM universal_recipients")
    fun getAllRecipients(): List<UniversalRecipientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipient(recipient: UniversalRecipientEntity)

    @Delete
    fun deleteRecipient(recipient: UniversalRecipientEntity)
}
