package com.hackathon.afterlog.data.repository

import com.hackathon.afterlog.data.local.dao.LogDao
import com.hackathon.afterlog.data.local.dao.SessionDao
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.hackathon.afterlog.service.TimeManager

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

    suspend fun getSessionLogs(sessionId: String): List<MediaLogEntity> {
        val targetId = if (sessionId == "last_session") {
            try {
                // Determine the actual last session
                val lastSession = sessionDao.getLastSession()
                lastSession?.id ?: sessionId
            } catch (e: Exception) {
                // Fallback or log error if DAO method missing
                sessionId
            }
        } else {
            sessionId
        }
        
        return logDao.getLogsBySessionSuspend(targetId)
    }
    
    // Media Pipeline Helpers
    
    /**
     * Gets all video files for a session.
     * Returns both VIDEO_CHUNK and VIDEO_HIGHLIGHT types.
     */
    suspend fun getVideosBySession(sessionId: String): List<MediaLogEntity> {
        val allLogs = getSessionLogs(sessionId)
        return allLogs.filter { 
            it.type == MediaType.VIDEO_CHUNK || it.type == MediaType.VIDEO_HIGHLIGHT 
        }
    }
    
    /**
     * Gets the audio file for a session.
     * Returns the first AUDIO_CHUNK entry found.
     */
    suspend fun getAudioFileBySession(sessionId: String): MediaLogEntity? {
        val allLogs = getSessionLogs(sessionId)
        return allLogs.firstOrNull { it.type == MediaType.AUDIO_CHUNK }
    }
}
