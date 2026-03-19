package com.yuerchu.remoteask.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yuerchu.remoteask.data.QuestionDatabase
import com.yuerchu.remoteask.data.QuestionRepository
import com.yuerchu.remoteask.data.SettingsDataStore

class RetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = QuestionDatabase.getInstance(applicationContext)
        val settings = SettingsDataStore(applicationContext)
        val repository = QuestionRepository(db.questionDao(), settings)

        return try {
            repository.retryPendingSubmits()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
