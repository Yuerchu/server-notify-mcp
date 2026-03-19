package com.yuerchu.remoteask.data

import android.util.Log
import com.yuerchu.remoteask.data.model.QuestionEntity
import com.yuerchu.remoteask.data.model.SubmitAnswerRequest
import kotlinx.coroutines.flow.Flow

private const val TAG = "RemoteAsk"

class QuestionRepository(
    private val dao: QuestionDao,
    private val settingsDataStore: SettingsDataStore
) {
    val allQuestions: Flow<List<QuestionEntity>> = dao.getAllQuestions()

    suspend fun saveQuestion(entity: QuestionEntity) {
        dao.insert(entity)
    }

    suspend fun getById(id: String): QuestionEntity? = dao.getById(id)

    suspend fun submitAnswer(questionId: String, answer: String): Result<Unit> {
        return try {
            val api = createApi() ?: return Result.failure(Exception("未配置服务器"))
            val response = api.submitAnswer(questionId, SubmitAnswerRequest(answer = answer))

            if (response.isSuccessful) {
                dao.getById(questionId)?.let { entity ->
                    dao.update(
                        entity.copy(
                            answer = answer,
                            status = "answered",
                            answeredAt = System.currentTimeMillis(),
                            pendingSubmit = false
                        )
                    )
                }
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "HTTP ${response.code()}"
                // Mark as pending submit for retry
                dao.getById(questionId)?.let { entity ->
                    dao.update(entity.copy(answer = answer, pendingSubmit = true))
                }
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            // Save answer locally for retry
            dao.getById(questionId)?.let { entity ->
                dao.update(entity.copy(answer = answer, pendingSubmit = true))
            }
            Result.failure(e)
        }
    }

    suspend fun retryPendingSubmits() {
        val pending = dao.getPendingSubmits()
        val api = createApi() ?: return

        for (entity in pending) {
            val answer = entity.answer ?: continue
            try {
                val response = api.submitAnswer(entity.id, SubmitAnswerRequest(answer = answer))
                if (response.isSuccessful) {
                    dao.update(
                        entity.copy(
                            status = "answered",
                            answeredAt = System.currentTimeMillis(),
                            pendingSubmit = false
                        )
                    )
                }
            } catch (_: Exception) {
                // Will retry next time
            }
        }
    }

    suspend fun fetchFromServer(): Result<Int> {
        Log.d(TAG, "fetchFromServer called")
        return try {
            val api = createApi()
            if (api == null) {
                Log.e(TAG, "createApi returned null - settings not configured")
                return Result.failure(Exception("未配置服务器"))
            }
            Log.d(TAG, "Calling API getQuestions...")
            val response = api.getQuestions()
            Log.d(TAG, "API response code: ${response.code()}")
            if (!response.isSuccessful) {
                Log.e(TAG, "API failed: HTTP ${response.code()}")
                return Result.failure(Exception("HTTP ${response.code()}"))
            }
            val remoteQuestions = response.body()?.questions ?: emptyList()
            var newCount = 0
            for (rq in remoteQuestions) {
                val existing = dao.getById(rq.id)
                if (existing == null) {
                    dao.insert(
                        QuestionEntity(
                            id = rq.id,
                            question = rq.question,
                            options = rq.options,
                            answer = rq.answer,
                            status = rq.status,
                            receivedAt = System.currentTimeMillis()
                        )
                    )
                    newCount++
                } else if (existing.status == "pending" && rq.status == "answered") {
                    // Sync answer that was submitted via web page
                    dao.update(existing.copy(
                        answer = rq.answer,
                        status = "answered",
                        answeredAt = System.currentTimeMillis(),
                        pendingSubmit = false
                    ))
                }
            }
            Log.d(TAG, "fetchFromServer done, new questions: $newCount")
            Result.success(newCount)
        } catch (e: Exception) {
            Log.e(TAG, "fetchFromServer error", e)
            Result.failure(e)
        }
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    private suspend fun createApi(): ApiService? {
        val url = settingsDataStore.getWorkerUrlSync()
        val token = settingsDataStore.getAuthTokenSync()
        Log.d(TAG, "createApi: url='${url.take(30)}...', token=${if (token.isBlank()) "EMPTY" else "SET(${token.length})"}")
        if (url.isBlank() || token.isBlank()) return null
        return ApiService.create(url, token)
    }
}
