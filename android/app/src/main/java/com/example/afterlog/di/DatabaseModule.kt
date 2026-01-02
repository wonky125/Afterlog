package com.example.afterlog.di

import android.content.Context
import androidx.room.Room
import com.example.afterlog.data.local.AppDatabase
import com.example.afterlog.data.local.dao.LogDao
import com.example.afterlog.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "afterlog_db"
        )
        .fallbackToDestructiveMigration() // 개발 중 편의를 위해 마이그레이션 실패 시 DB 초기화 (프로덕션에서는 주의)
        .build()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideLogDao(database: AppDatabase): LogDao {
        return database.logDao()
    }
}
