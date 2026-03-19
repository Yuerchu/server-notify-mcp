package com.yuerchu.remoteask.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AskResponse(
    val question_id: String,
    val answer_url: String = "",
    val page_url: String = "",
    val push_sent: Boolean = false
)

@Serializable
data class AnswerResponse(
    val status: String,
    val answer: String? = null,
    val answered_at: String? = null,
    val error: String? = null
)

@Serializable
data class SubmitAnswerRequest(
    val answer: String,
    val answer_token: String? = null
)

@Serializable
data class StatusResponse(
    val status: String? = null,
    val error: String? = null
)

@Serializable
data class DeviceRegistration(
    val fcm_token: String,
    val label: String? = null
)

@Serializable
data class RemoteQuestion(
    val id: String,
    val question: String,
    val options: List<String>? = null,
    val answer: String? = null,
    val status: String,
    val createdAt: String,
    val answeredAt: String? = null
)

@Serializable
data class QuestionsListResponse(
    val questions: List<RemoteQuestion>
)

@Entity(tableName = "questions")
@TypeConverters(OptionsConverter::class)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val question: String,
    val options: List<String>?,
    val answer: String?,
    val status: String, // "pending" or "answered"
    val receivedAt: Long = System.currentTimeMillis(),
    val answeredAt: Long? = null,
    val pendingSubmit: Boolean = false // true if answer failed to submit
)

class OptionsConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toList(value: String?): List<String>? {
        return value?.let { json.decodeFromString(it) }
    }
}
