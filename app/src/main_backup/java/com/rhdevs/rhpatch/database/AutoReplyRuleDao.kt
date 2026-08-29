package com.rhdevs.rhpatch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AutoReplyRuleDao {
    @Query("SELECT * FROM auto_reply_rules ORDER BY id DESC")
    fun getAll(): List<AutoReplyRule>

    @Query("SELECT * FROM auto_reply_rules WHERE isEnabled = 1")
    fun getActiveRules(): List<AutoReplyRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rule: AutoReplyRule): Long

    @Update
    fun update(rule: AutoReplyRule)

    @Delete
    fun delete(rule: AutoReplyRule)

    @Query("DELETE FROM auto_reply_rules WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT * FROM auto_reply_rules WHERE id = :id LIMIT 1")
    fun getById(id: Int): AutoReplyRule?
}
