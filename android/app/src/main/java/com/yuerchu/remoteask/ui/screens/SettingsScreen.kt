package com.yuerchu.remoteask.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yuerchu.remoteask.data.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    isInitialSetup: Boolean = false,
    onSetupComplete: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSettingsSaved: ((url: String, token: String) -> Unit)? = null
) {
    val workerUrl by settingsDataStore.workerUrl.collectAsState(initial = "")
    val authToken by settingsDataStore.authToken.collectAsState(initial = "")
    val fcmToken by settingsDataStore.fcmToken.collectAsState(initial = "")

    var editUrl by remember { mutableStateOf("") }
    var editToken by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(workerUrl, authToken) {
        if (!initialized) {
            editUrl = workerUrl
            editToken = authToken
            if (workerUrl.isNotBlank() || authToken.isNotBlank()) {
                initialized = true
            }
        }
    }

    // Also initialize on first non-empty data
    LaunchedEffect(workerUrl) {
        if (workerUrl.isNotBlank() && editUrl.isBlank()) {
            editUrl = workerUrl
            initialized = true
        }
    }
    LaunchedEffect(authToken) {
        if (authToken.isNotBlank() && editToken.isBlank()) {
            editToken = authToken
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isInitialSetup) "欢迎使用 Remote Ask" else "设置")
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (isInitialSetup) {
                Text(
                    text = "配置你的 Worker 服务器连接",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = editUrl,
                onValueChange = { editUrl = it },
                label = { Text("Worker URL") },
                placeholder = { Text("https://remote-ask-worker.xxx.workers.dev") },
                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            OutlinedTextField(
                value = editToken,
                onValueChange = { editToken = it },
                label = { Text("Auth Token") },
                placeholder = { Text("你的 WORKER_AUTH_TOKEN") },
                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            if (tokenVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "切换可见"
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            AnimatedVisibility(
                visible = fcmToken.isNotBlank(),
                enter = fadeIn() + slideInVertically()
            ) {
                OutlinedTextField(
                    value = fcmToken,
                    onValueChange = {},
                    label = { Text("FCM Token") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (editUrl.isBlank() || editToken.isBlank()) {
                            snackbarHostState.showSnackbar("请填写 Worker URL 和 Auth Token")
                            return@launch
                        }
                        val savedUrl = editUrl.trimEnd('/')
                        val savedToken = editToken.trim()
                        settingsDataStore.saveWorkerUrl(savedUrl)
                        settingsDataStore.saveAuthToken(savedToken)
                        onSettingsSaved?.invoke(savedUrl, savedToken)
                        snackbarHostState.showSnackbar("保存成功")
                        if (isInitialSetup) {
                            onSetupComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isInitialSetup) "完成设置" else "保存",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
