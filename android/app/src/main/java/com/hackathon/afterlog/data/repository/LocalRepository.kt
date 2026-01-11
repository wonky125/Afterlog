package com.hackathon.afterlog.data.repository

import com.hackathon.afterlog.data.local.dao.LogDao
import com.hackathon.afterlog.data.local.dao.SessionDao
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.local.entities.SessionEntity
import com.hackathon.afterlog.data.model.PerspectiveGuideConfig
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

    suspend fun savePerspectiveGuide(sessionId: String, config: PerspectiveGuideConfig) {
        sessionDao.updatePerspectiveGuide(sessionId, config.toSerializedString())
    }

    suspend fun getPerspectiveGuide(sessionId: String): PerspectiveGuideConfig? {
        val json = sessionDao.getPerspectiveGuideJson(sessionId)
        return PerspectiveGuideConfig.fromSerializedString(json)
    }

    suspend fun getLastSavedPerspectiveGuide(): PerspectiveGuideConfig? {
        val lastSession = sessionDao.getLastSession() ?: return null
        return PerspectiveGuideConfig.fromSerializedString(lastSession.perspectiveGuideJson)
    }

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    suspend fun getSessionById(sessionId: String): SessionEntity? {
        return sessionDao.getSessionById(sessionId)
    }

    suspend fun getLastSessionId(): String? {
        return sessionDao.getLastSession()?.id
    }
    
    suspend fun getSessionStartTime(sessionId: String): Long? {
        return sessionDao.getSessionById(sessionId)?.startTime
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

    suspend fun deleteLogsBySessionAndType(sessionId: String, type: MediaType) {
        logDao.deleteLogsBySessionAndType(sessionId, type)
    }

    suspend fun deleteSessionData(sessionId: String) {
        logDao.deleteLogsBySession(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    fun getLogsForSession(sessionId: String): Flow<List<MediaLogEntity>> {
        return logDao.getLogsBySession(sessionId)
    }

    suspend fun getSessionLogs(sessionId: String): List<MediaLogEntity> {
        val targetId = if (sessionId == "last_session") {
            try {
                // Prefer the most recent session that actually has logs.
                val lastLoggedSessionId = logDao.getLastLoggedSessionId()
                if (!lastLoggedSessionId.isNullOrBlank()) {
                    lastLoggedSessionId
                } else {
                    val lastSession = sessionDao.getLastSession()
                    lastSession?.id ?: sessionId
                }
            } catch (e: Exception) {
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
