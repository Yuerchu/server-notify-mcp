package com.yuerchu.remoteask

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yuerchu.remoteask.service.RetryWorker
import java.util.concurrent.TimeUnit

class RemoteAskApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic retry for failed answer submissions
        val retryWork = PeriodicWorkRequestBuilder<RetryWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "retry_pending_answers",
            ExistingPeriodicWorkPolicy.KEEP,
            retryWork
        )
    }
}
