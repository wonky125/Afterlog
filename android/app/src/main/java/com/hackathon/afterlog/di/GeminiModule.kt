package com.hackathon.afterlog.di

import android.content.Context
import com.hackathon.afterlog.BuildConfig
import com.hackathon.afterlog.data.remote.GeminiFilesApiClient
import com.hackathon.afterlog.data.repository.gemini.GeminiAudioUtils
import com.hackathon.afterlog.data.repository.gemini.GeminiLogUtils
import com.hackathon.afterlog.data.repository.gemini.GeminiParsers
import com.hackathon.afterlog.data.repository.gemini.GeminiPromptBuilder
import com.hackathon.afterlog.data.repository.gemini.GeminiRetryPolicy
import com.hackathon.afterlog.data.repository.gemini.GeminiVideoUtils
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {
    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-3-pro-preview",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 4096
            }
        )
    }

    @Provides
    @Singleton
    fun provideGeminiRetryPolicy(
        generativeModel: GenerativeModel
    ): GeminiRetryPolicy {
        return GeminiRetryPolicy(generativeModel)
    }

    @Provides
    @Singleton
    fun provideGeminiAudioUtils(
        filesApiClient: GeminiFilesApiClient,
        @ApplicationContext context: Context
    ): GeminiAudioUtils {
        return GeminiAudioUtils(filesApiClient, context)
    }

    @Provides
    fun provideGeminiPromptBuilder(): GeminiPromptBuilder = GeminiPromptBuilder

    @Provides
    fun provideGeminiParsers(): GeminiParsers = GeminiParsers

    @Provides
    fun provideGeminiVideoUtils(): GeminiVideoUtils = GeminiVideoUtils

    @Provides
    fun provideGeminiLogUtils(): GeminiLogUtils = GeminiLogUtils
}
