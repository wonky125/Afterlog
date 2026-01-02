package com.example.afterlog.data.repository

import com.example.afterlog.data.local.dao.LogDao
import com.example.afterlog.data.local.dao.SessionDao
import com.example.afterlog.data.local.entities.MediaLogEntity
import com.example.afterlog.data.local.entities.MediaType
import com.example.afterlog.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val logDao: LogDao,
    private val timeManager: TimeManager
) {
    // Session Operations
    suspend fun startNewSession(): String {
        val session = SessionEntity(
            startTime = timeManager.getCurrentTime()
        )
        sessionDao.insertSession(session)
        return session.id
    }

    suspend fun endSession(sessionId: String) {
        sessionDao.updateSessionEndTime(sessionId, timeManager.getCurrentTime())
    }

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    suspend fun getSessionById(sessionId: String): SessionEntity? {
        return sessionDao.getSessionById(sessionId)
    }

    // Log Operations
    suspend fun logMedia(
        sessionId: String,
        type: MediaType,
        filePath: String,
        decibel: Int? = null,
        timestamp: Long = timeManager.getCurrentTime()
    ) {
        val log = MediaLogEntity(
            sessionId = sessionId,
            type = type,
            filePath = filePath,
            timestamp = timestamp,
            decibel = decibel
        )
        logDao.insertLog(log)
    }

    fun getLogsForSession(sessionId: String): Flow<List<MediaLogEntity>> {
        return logDao.getLogsBySession(sessionId)
    }
}
