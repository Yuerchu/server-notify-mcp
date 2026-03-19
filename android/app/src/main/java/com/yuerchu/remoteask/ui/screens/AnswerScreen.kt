package com.yuerchu.remoteask.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yuerchu.remoteask.data.QuestionRepository
import com.yuerchu.remoteask.data.model.QuestionEntity
import com.yuerchu.remoteask.ui.components.OptionButton
import kotlinx.coroutines.launch

enum class SubmitState { Idle, Submitting, Success, Error }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerScreen(
    questionId: String,
    repository: QuestionRepository,
    onBack: () -> Unit
) {
    var question by remember { mutableStateOf<QuestionEntity?>(null) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var freeText by remember { mutableStateOf("") }
    var submitState by remember { mutableStateOf(SubmitState.Idle) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(questionId) {
        question = repository.getById(questionId)
    }

    val currentQuestion = question

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回答问题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimatedContent(
            targetState = submitState,
            transitionSpec = {
                (fadeIn(spring()) + scaleIn(
                    spring(dampingRatio = 0.6f),
                    initialScale = 0.8f
                )).togetherWith(fadeOut(spring()))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "submitState"
        ) { state ->
            when (state) {
                SubmitState.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "回答已提交",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Claude 将继续工作",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    if (currentQuestion == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (currentQuestion.status == "answered") {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "已回答",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    currentQuestion.answer ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Claude 的问题",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Option buttons
                            if (!currentQuestion.options.isNullOrEmpty()) {
                                currentQuestion.options.forEach { option ->
                                    OptionButton(
                                        text = option,
                                        isSelected = selectedOption == option,
                                        onClick = {
                                            selectedOption = if (selectedOption == option) null else option
                                            freeText = ""
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "或自由输入",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Free text input
                            OutlinedTextField(
                                value = freeText,
                                onValueChange = {
                                    freeText = it
                                    if (it.isNotBlank()) selectedOption = null
                                },
                                label = { Text("输入你的回答") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        val answer = freeText.trim().ifBlank { selectedOption } ?: return@KeyboardActions
                                        scope.launch {
                                            submitState = SubmitState.Submitting
                                            val result = repository.submitAnswer(questionId, answer)
                                            submitState = if (result.isSuccess) SubmitState.Success else SubmitState.Error
                                            if (result.isFailure) {
                                                snackbarHostState.showSnackbar(
                                                    "提交失败: ${result.exceptionOrNull()?.message}，已保存本地"
                                                )
                                            }
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val answer = freeText.trim().ifBlank { selectedOption }
                                    if (answer.isNullOrBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("请选择一个选项或输入回答") }
                                        return@Button
                                    }
                                    scope.launch {
                                        submitState = SubmitState.Submitting
                                        val result = repository.submitAnswer(questionId, answer)
                                        submitState = if (result.isSuccess) SubmitState.Success else SubmitState.Error
                                        if (result.isFailure) {
                                            snackbarHostState.showSnackbar(
                                                "提交失败: ${result.exceptionOrNull()?.message}，已保存本地"
                                            )
                                        }
                                    }
                                },
                                enabled = submitState != SubmitState.Submitting &&
                                        (freeText.isNotBlank() || selectedOption != null),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (submitState == SubmitState.Submitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("提交回答", style = MaterialTheme.typography.titleMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
