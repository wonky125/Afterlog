package com.example.afterlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.afterlog.data.local.entities.MediaLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MediaLogEntity)

    @Query("SELECT * FROM media_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsBySession(sessionId: String): Flow<List<MediaLogEntity>>

    @Query("DELETE FROM media_logs WHERE sessionId = :sessionId")
    suspend fun deleteLogsBySession(sessionId: String)
}
