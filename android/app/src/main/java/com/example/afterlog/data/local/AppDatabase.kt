package com.example.afterlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.afterlog.data.local.dao.LogDao
import com.example.afterlog.data.local.dao.SessionDao
import com.example.afterlog.data.local.entities.AnalysisResultEntity
import com.example.afterlog.data.local.entities.MediaLogEntity
import com.example.afterlog.data.local.entities.SessionEntity

@Database(
    entities = [SessionEntity::class, MediaLogEntity::class, AnalysisResultEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun logDao(): LogDao
}
