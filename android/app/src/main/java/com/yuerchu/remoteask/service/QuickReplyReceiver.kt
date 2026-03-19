package com.yuerchu.remoteask.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yuerchu.remoteask.data.QuestionDatabase
import com.yuerchu.remoteask.data.QuestionRepository
import com.yuerchu.remoteask.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickReplyReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AskFCMService.ACTION_QUICK_REPLY) return

        val questionId = intent.getStringExtra("question_id") ?: return
        val answer = intent.getStringExtra("answer") ?: return

        val db = QuestionDatabase.getInstance(context)
        val settings = SettingsDataStore(context)
        val repository = QuestionRepository(db.questionDao(), settings)

        scope.launch {
            repository.submitAnswer(questionId, answer)
        }

        // Dismiss notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(questionId.hashCode())
    }
}
