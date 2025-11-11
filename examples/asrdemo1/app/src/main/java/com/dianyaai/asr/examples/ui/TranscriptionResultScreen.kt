package com.dianyaai.asr.examples.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.dianyaai.asr.FileTranscriptionDetail
import com.dianyaai.asr.FileTranscribeMessage
import com.dianyaai.asr.examples.ASRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionResultScreen(navController: NavController, viewModel: ASRViewModel) {
    val transcriptionStatus by viewModel.transcriptionStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("转录结果") })
        }
    ) { paddingValues ->
        transcriptionStatus?.let { status ->
            TabbedResultView(status = status, modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun TabbedResultView(status: FileTranscribeMessage, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = mutableListOf<String>()
    if (status.overviewMd != null) tabs.add("概述")
    if (status.summaryMd != null) tabs.add("总结")
    if (status.details != null) tabs.add("详情")

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        if (tabs.isNotEmpty()) {
            when (tabs[selectedTabIndex]) {
                "概述" -> status.overviewMd?.let { MarkdownView(markdown = it) }
                "总结" -> status.summaryMd?.let { MarkdownView(markdown = it) }
                "详情" -> status.details?.let { DetailsView(details = it) }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownView(markdown: String) {
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body {
                    font-family: -apple-system, sans-serif;
                    padding: 20px;
                }
            </style>
        </head>
        <body>
            <pre>${markdown}</pre>
        </body>
        </html>
    """.trimIndent()

    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    })
}

@Composable
fun DetailsView(details: List<*>) {
    LazyColumn {
        items(details) { detail ->
            val detailItem = detail as FileTranscriptionDetail
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${detailItem.startTime} - ${detailItem.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = detailItem.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            HorizontalDivider()
        }
    }
}
