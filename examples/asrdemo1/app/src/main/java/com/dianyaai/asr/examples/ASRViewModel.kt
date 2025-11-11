package com.dianyaai.asr.examples

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dianyaai.asr.FileTranscribeClient
import com.dianyaai.asr.FileTranscribeMessage
import com.dianyaai.asr.createFileTranscribeClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ASRViewModel : ViewModel() {
    private val _transcriptionResult = MutableStateFlow("点击下方按钮开始转录")
    val transcriptionResult: StateFlow<String> = _transcriptionResult.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcriptionStatus = MutableStateFlow<FileTranscribeMessage?>(null)
    val transcriptionStatus: StateFlow<FileTranscribeMessage?> = _transcriptionStatus.asStateFlow()

    private val _authToken = MutableStateFlow("")
    val authToken: StateFlow<String> = _authToken.asStateFlow()

    private val _showAlert = MutableStateFlow(false)
    val showAlert: StateFlow<Boolean> = _showAlert.asStateFlow()

    private val _alertMessage = MutableStateFlow("")
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()

    fun setup(context: Context) {
        val sharedPreferences = context.getSharedPreferences("asr_demo_prefs", Context.MODE_PRIVATE)
        _authToken.value = sharedPreferences.getString("auth_token", "") ?: ""
    }

    fun saveAuthToken(context: Context) {
        val sharedPreferences = context.getSharedPreferences("asr_demo_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("auth_token", _authToken.value)
            apply()
        }
    }

    fun onAuthTokenChange(newToken: String) {
        _authToken.value = newToken
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = it.getString(displayNameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path?.let { File(it).name } ?: "temp_audio_file_${System.currentTimeMillis()}"
        }
        return fileName!!
    }
    private fun getFileFromUri(context: Context, uri: Uri): Uri {
        // 使用新方法获取原始文件名
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)

        // 使用 .use 块来自动关闭流，这是更安全、更简洁的写法
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Log.e("ASRViewModel", "Failed to copy file from Uri", e)
            // 如果失败，可以返回一个空Uri或抛出异常，这里我们打印日志并继续
        }

        return Uri.fromFile(tempFile)
    }

    fun transcribeFile(uri: Uri, context: Context) {
        if (_authToken.value.isBlank()) {
            _alertMessage.value = "未设置认证令牌"
            _showAlert.value = true
            return
        }

        viewModelScope.launch {
            _isTranscribing.value = true
            _transcriptionStatus.value = null
            val fileUri = getFileFromUri(context, uri)
            val client = createFileTranscribeClient(
                context = context,
                authToken = _authToken.value,
                fileURL = fileUri
            )

            launch {
                client.stateStream.collect { state ->
                    Log.d("ASRViewModel", "Client state changed: $state")
                    if (state is FileTranscribeClient.ClientState.Failed) {
                        _transcriptionResult.value = "客户端失败: ${state.error.localizedMessage}"
                        _isTranscribing.value = false
                    }
                }
            }

            launch {
                client.dataStream.collect { message ->
                    Log.d("ASRViewModel", "Received message: $message")
                    if (message.data == com.dianyaai.asr.FileTranscribeMessageType.DONE) {
                        _transcriptionStatus.value = message
                        _isTranscribing.value = false
                    } else if (message.data.isFailed) {
                        _transcriptionResult.value = "转录失败: ${message.message ?: "未知错误"}"
                        _isTranscribing.value = false
                    } else {
                        _transcriptionResult.value = "转录状态: ${message.data}"
                    }
                }
            }

            client.start()
        }
    }

    fun dismissAlert() {
        _showAlert.value = false
    }
}
