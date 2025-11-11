package com.dianyaai.asr.examples.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dianyaai.asr.examples.ASRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ASRScreen(navController: NavController, viewModel: ASRViewModel) {
    val context = LocalContext.current
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcriptionResult by viewModel.transcriptionResult.collectAsState()
    val transcriptionStatus by viewModel.transcriptionStatus.collectAsState()
    val showAlert by viewModel.showAlert.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.transcribeFile(it, context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setup(context)
    }

    LaunchedEffect(transcriptionStatus) {
        if (transcriptionStatus?.data == com.dianyaai.asr.FileTranscribeMessageType.DONE) {
            navController.navigate("result")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件转录演示") },
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isTranscribing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = transcriptionResult)
            } else {
                Text(text = transcriptionResult, modifier = Modifier.padding(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { filePickerLauncher.launch("audio/*") }) {
                Text("选择音频文件")
            }
        }

        if (showAlert) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAlert() },
                title = { Text("提示") },
                text = { Text(alertMessage) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissAlert() }) {
                        Text("好的")
                    }
                }
            )
        }
    }
}
