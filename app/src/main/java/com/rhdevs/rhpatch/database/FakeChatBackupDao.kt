package com.rhdevs.rhpatch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FakeChatBackupDao {
    @Query("SELECT * FROM fake_chat_backups WHERE messageId = :msgId LIMIT 1")
    fun getBackup(msgId: String): FakeChatBackup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(backup: FakeChatBackup)

    @Query("DELETE FROM fake_chat_backups WHERE messageId = :msgId")
    fun deleteByMsgId(msgId: String)
}
