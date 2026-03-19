package com.yuerchu.remoteask.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yuerchu.remoteask.MainActivity
import com.yuerchu.remoteask.R
import com.yuerchu.remoteask.data.QuestionDatabase
import com.yuerchu.remoteask.data.SettingsDataStore
import com.yuerchu.remoteask.data.model.QuestionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AskFCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val settings = SettingsDataStore(applicationContext)
        serviceScope.launch {
            settings.saveFcmToken(token)
            // TODO: register device with worker if configured
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val questionId = data["question_id"] ?: return
        val question = data["question"] ?: return
        val optionsJson = data["options"]

        val options: List<String>? = if (!optionsJson.isNullOrBlank()) {
            try {
                json.decodeFromString(optionsJson)
            } catch (_: Exception) {
                null
            }
        } else null

        // Save to local database
        val db = QuestionDatabase.getInstance(applicationContext)
        serviceScope.launch {
            db.questionDao().insert(
                QuestionEntity(
                    id = questionId,
                    question = question,
                    options = options,
                    answer = null,
                    status = "pending"
                )
            )
        }

        // Show notification
        showNotification(questionId, question, options)
    }

    private fun showNotification(
        questionId: String,
        question: String,
        options: List<String>?
    ) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "远程问题",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "来自 Claude Code 的远程问题"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Intent to open answer screen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("question_id", questionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, questionId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Claude 需要你的输入")
            .setContentText(
                if (question.length > 80) question.take(77) + "..." else question
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(question))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add quick reply actions for options (max 3)
        options?.take(3)?.forEachIndexed { index, option ->
            val actionIntent = Intent(this, QuickReplyReceiver::class.java).apply {
                action = ACTION_QUICK_REPLY
                putExtra("question_id", questionId)
                putExtra("answer", option)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                this,
                questionId.hashCode() + index + 1,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, option, actionPendingIntent)
        }

        manager.notify(questionId.hashCode(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "remote_questions"
        const val ACTION_QUICK_REPLY = "com.yuerchu.remoteask.QUICK_REPLY"
    }
}
