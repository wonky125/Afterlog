package com.hackathon.afterlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.local.entities.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MediaLogEntity)

    @Query("SELECT * FROM media_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsBySession(sessionId: String): Flow<List<MediaLogEntity>>

    @Query("SELECT * FROM media_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLogsBySessionSuspend(sessionId: String): List<MediaLogEntity>

    @Query("DELETE FROM media_logs WHERE sessionId = :sessionId")
    suspend fun deleteLogsBySession(sessionId: String)

    @Query("DELETE FROM media_logs WHERE sessionId = :sessionId AND type = :type")
    suspend fun deleteLogsBySessionAndType(sessionId: String, type: MediaType)

    @Query("SELECT sessionId FROM media_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLoggedSessionId(): String?
}
