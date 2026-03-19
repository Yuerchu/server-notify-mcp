package com.yuerchu.remoteask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.yuerchu.remoteask.data.ApiService
import com.yuerchu.remoteask.data.QuestionDatabase
import com.yuerchu.remoteask.data.QuestionRepository
import com.yuerchu.remoteask.data.SettingsDataStore
import com.yuerchu.remoteask.data.model.DeviceRegistration
import com.yuerchu.remoteask.ui.navigation.RemoteAskNavGraph
import com.yuerchu.remoteask.ui.theme.RemoteAskTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            registerFcmToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsDataStore = SettingsDataStore(applicationContext)
        val db = QuestionDatabase.getInstance(applicationContext)
        val repository = QuestionRepository(db.questionDao(), settingsDataStore)

        val questionId = intent?.getStringExtra("question_id")

        setContent {
            RemoteAskTheme {
                RemoteAskNavGraph(
                    settingsDataStore = settingsDataStore,
                    repository = repository,
                    initialQuestionId = questionId,
                    onSettingsSaved = { _, _ -> registerFcmToken() }
                )
            }
        }

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    registerFcmToken()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            registerFcmToken()
        }
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            activityScope.launch {
                settingsDataStore.saveFcmToken(token)

                // Register with worker if configured
                val workerUrl = settingsDataStore.workerUrl.first()
                val authToken = settingsDataStore.authToken.first()

                if (workerUrl.isNotBlank() && authToken.isNotBlank()) {
                    try {
                        val api = ApiService.create(workerUrl, authToken)
                        api.registerDevice(
                            DeviceRegistration(
                                fcm_token = token,
                                label = Build.MODEL
                            )
                        )
                    } catch (_: Exception) {
                        // Will retry later
                    }
                }
            }
        }
    }
}
