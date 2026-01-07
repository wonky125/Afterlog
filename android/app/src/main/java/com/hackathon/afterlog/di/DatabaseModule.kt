package com.hackathon.afterlog.di

import android.content.Context
import androidx.room.Room
import com.hackathon.afterlog.data.local.AppDatabase
import com.hackathon.afterlog.data.local.dao.LogDao
import com.hackathon.afterlog.data.local.dao.SessionDao
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
