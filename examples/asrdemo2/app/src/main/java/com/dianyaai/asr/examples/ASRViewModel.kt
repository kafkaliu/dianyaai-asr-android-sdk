package com.dianyaai.asr.examples

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dianyaai.asr.RealTimeTranscribeClient
import com.dianyaai.asr.RealTimeTranscribeMessageType
import com.dianyaai.asr.createRealTimeTranscribeClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ASRViewModel : ViewModel() {

    private val _transcriptText = MutableStateFlow("")
    val transcriptText: StateFlow<String> = _transcriptText.asStateFlow()

    private val _partialTranscriptText = MutableStateFlow("")
    val partialTranscriptText: StateFlow<String> = _partialTranscriptText.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _showAlert = MutableStateFlow(false)
    val showAlert: StateFlow<Boolean> = _showAlert.asStateFlow()

    private val _alertMessage = MutableStateFlow("")
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()

    private val _authToken = MutableStateFlow("")
    val authToken: StateFlow<String> = _authToken.asStateFlow()

    private val microphoneManager = MicrophoneManager()
    private var asrClient: RealTimeTranscribeClient? = null
    private var transcriptionJob: Job? = null
    private var audioJob: Job? = null

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

    fun startTranscription() {
        if (_authToken.value.isBlank()) {
            _alertMessage.value = "请在设置中输入认证令牌"
            _showAlert.value = true
            return
        }

        viewModelScope.launch {
            _transcriptText.value = ""
            _partialTranscriptText.value = ""
            _isPaused.value = false
            _isTranscribing.value = true

            asrClient = createRealTimeTranscribeClient(authToken = _authToken.value)
            asrClient?.connect()

            transcriptionJob = asrClient?.dataStream?.onEach { message ->
                Log.d("ASRViewModel", "Received message size: ${message.toString().toByteArray().size}")
                when (val data = message.data) {
                    is RealTimeTranscribeMessageType.AsrResult -> {
                        if (data.data.text.isNotEmpty()) {
                            _transcriptText.value += (if (_transcriptText.value.isNotEmpty()) "\n" else "") + data.data.text
                            _partialTranscriptText.value = ""
                        }
                    }
                    is RealTimeTranscribeMessageType.AsrResultPartial -> {
                        _partialTranscriptText.value = data.data.text
                    }
                    is RealTimeTranscribeMessageType.Error -> {
                        _alertMessage.value = "转录错误: ${data.data.data}"
                        _showAlert.value = true
                        stopTranscription()
                    }
                    is RealTimeTranscribeMessageType.Stop -> {
                        stopTranscription()
                    }
                }
            }?.launchIn(viewModelScope)

            audioJob = microphoneManager.startRecording().onEach { audioData ->
                if (!_isPaused.value) {
//                    Log.d("ASRViewModel", "Sending audio chunk size: ${audioData.size}")
                    asrClient?.sendAudioChunk(audioData)
                }
            }.launchIn(viewModelScope)
        }
    }

    fun pauseTranscription() {
        _isPaused.value = true
    }

    fun resumeTranscription() {
        _isPaused.value = false
    }

    fun stopTranscription() {
        viewModelScope.launch {
            transcriptionJob?.cancel()
            audioJob?.cancel()
            microphoneManager.stopRecording()
            asrClient?.disconnect()
            _isTranscribing.value = false
            _isPaused.value = false
//            _partialTranscriptText.value = ""
        }
    }

    fun dismissAlert() {
        _showAlert.value = false
    }
}