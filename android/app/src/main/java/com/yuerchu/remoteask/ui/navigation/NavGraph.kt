package com.yuerchu.remoteask.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yuerchu.remoteask.data.QuestionRepository
import com.yuerchu.remoteask.data.SettingsDataStore
import com.yuerchu.remoteask.ui.screens.AnswerScreen
import com.yuerchu.remoteask.ui.screens.QuestionListScreen
import com.yuerchu.remoteask.ui.screens.SettingsScreen

object Routes {
    const val SETUP = "setup"
    const val QUESTION_LIST = "questions"
    const val ANSWER = "answer/{questionId}"
    const val SETTINGS = "settings"

    fun answer(questionId: String) = "answer/$questionId"
}

@Composable
fun RemoteAskNavGraph(
    settingsDataStore: SettingsDataStore,
    repository: QuestionRepository,
    navController: NavHostController = rememberNavController(),
    initialQuestionId: String? = null,
    onSettingsSaved: ((url: String, token: String) -> Unit)? = null
) {
    val isConfigured by settingsDataStore.isConfigured.collectAsState(initial = false)

    val startDestination = if (isConfigured) Routes.QUESTION_LIST else Routes.SETUP

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETUP) {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                isInitialSetup = true,
                onSetupComplete = {
                    navController.navigate(Routes.QUESTION_LIST) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onSettingsSaved = onSettingsSaved
            )
        }

        composable(Routes.QUESTION_LIST) {
            // Navigate to answer screen if launched from notification
            if (initialQuestionId != null) {
                navController.navigate(Routes.answer(initialQuestionId)) {
                    launchSingleTop = true
                }
            }

            QuestionListScreen(
                questionsFlow = repository.allQuestions,
                repository = repository,
                onQuestionClick = { questionId ->
                    navController.navigate(Routes.answer(questionId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.ANSWER) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: return@composable
            AnswerScreen(
                questionId = questionId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                onBack = { navController.popBackStack() },
                onSettingsSaved = onSettingsSaved
            )
        }
    }
}
