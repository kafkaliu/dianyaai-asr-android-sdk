package com.dianyaai.asr.examples.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.dianyaai.asr.examples.ASRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ASRScreen(navController: NavController, viewModel: ASRViewModel) {
    val context = LocalContext.current
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val transcriptText by viewModel.transcriptText.collectAsState()
    val partialTranscriptText by viewModel.partialTranscriptText.collectAsState()
    val showAlert by viewModel.showAlert.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startTranscription()
        } else {
            // Handle permission denial
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setup(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("实时语音转写 Demo") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = transcriptText, style = MaterialTheme.typography.bodyLarge)
                Text(text = partialTranscriptText, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isTranscribing && !isPaused) {
                    Button(
                        onClick = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                                    viewModel.startTranscription()
                                }
                                else -> {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("开始")
                    }
                } else if (isPaused) {
                    Button(
                        onClick = { viewModel.resumeTranscription() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("恢复")
                    }
                } else if (isTranscribing) {
                    Button(
                        onClick = { viewModel.pauseTranscription() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("暂停")
                    }
                }

                if (isTranscribing || isPaused) {
                    Button(
                        onClick = { viewModel.stopTranscription() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("终止")
                    }
                }
            }
        }

        if (showAlert) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAlert() },
                title = { Text("错误") },
                text = { Text(alertMessage) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissAlert() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}
