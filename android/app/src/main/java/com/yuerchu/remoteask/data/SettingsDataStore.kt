package com.yuerchu.remoteask.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_WORKER_URL = stringPreferencesKey("worker_url")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }

    val workerUrl: Flow<String> = context.dataStore.data.map { it[KEY_WORKER_URL] ?: "" }
    val authToken: Flow<String> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] ?: "" }
    val fcmToken: Flow<String> = context.dataStore.data.map { it[KEY_FCM_TOKEN] ?: "" }

    val isConfigured: Flow<Boolean> = context.dataStore.data.map {
        !it[KEY_WORKER_URL].isNullOrBlank() && !it[KEY_AUTH_TOKEN].isNullOrBlank()
    }

    suspend fun saveWorkerUrl(url: String) {
        context.dataStore.edit { it[KEY_WORKER_URL] = url.trim() }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { it[KEY_AUTH_TOKEN] = token.trim() }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { it[KEY_FCM_TOKEN] = token }
    }

    suspend fun getWorkerUrlSync(): String {
        return workerUrl.first()
    }

    suspend fun getAuthTokenSync(): String {
        return authToken.first()
    }
}
