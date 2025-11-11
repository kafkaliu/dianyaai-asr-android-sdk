package com.dianyaai.asr.examples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dianyaai.asr.examples.ui.ASRScreen
import com.dianyaai.asr.examples.ui.SettingsScreen
import com.dianyaai.asr.examples.ui.TranscriptionResultScreen
import com.dianyaai.asr.examples.ui.theme.ASRDemo1Theme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initTimberIfNeeded()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASRDemo1Theme {
                AppNavigation()
            }
        }
    }
}

private fun initTimberIfNeeded() {
    if (Timber.treeCount == 0) {
        Timber.plant(Timber.DebugTree())
        Timber.d("Timber initialized in MainActivity")
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: ASRViewModel = viewModel()

    NavHost(navController = navController, startDestination = "asr") {
        composable("asr") {
            ASRScreen(navController = navController, viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }
        composable("result") {
            TranscriptionResultScreen(navController = navController, viewModel = viewModel)
        }
    }
}