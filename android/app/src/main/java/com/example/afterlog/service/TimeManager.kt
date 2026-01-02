package com.example.afterlog.service

import android.content.Context
import android.util.Log
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock
import com.lyft.kronos.SyncListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val kronosClock: KronosClock = AndroidClockFactory.createKronosClock(
        context,
        object : SyncListener {
            override fun onSuccess(ticksDelta: Long, responseTimeMs: Long) {
                Log.i("TimeManager", "NTP Sync Success. Delta: $ticksDelta, ResponseTime: $responseTimeMs")
            }

            override fun onError(host: String, throwable: Throwable) {
                Log.w("TimeManager", "NTP Sync Failed: $host", throwable)
            }

            override fun onStartSync(host: String) {
                Log.d("TimeManager", "NTP Sync Started: $host")
            }
        }
    )

    init {
        kronosClock.syncInBackground()
    }

    /**
     * Returns the current time in milliseconds.
     * Uses NTP time if synchronized, otherwise falls back to System.currentTimeMillis().
     */
    fun getCurrentTime(): Long {
        return kronosClock.getCurrentTimeMs()
    }
}
