package com.dianyaai.asr.examples

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MicrophoneManager {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ByteArray> {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        isRecording = true
        audioRecord?.startRecording()

        return flow {
            val buffer = ByteArray(bufferSize)
            while (isRecording && currentCoroutineContext().isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size)
                if (read != null && read > 0) {
                    emit(buffer.copyOf(read))
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun stopRecording() {
        withContext(Dispatchers.IO) {
            if (isRecording) {
                isRecording = false
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            }
        }
    }
}
