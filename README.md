# DianyaaiASR Android SDK

DianyaaiASR is an Android SDK for integrating Dianya AI's Automatic Speech Recognition (ASR) services into your Android applications.

## Features

- File transcription: Transcribe audio files and get the results.
- Real-time transcription from a continuous audio stream.

## Installation

The SDK is distributed as a Maven package via GitHub Packages.

Add the repository to your project's `build.gradle` or `build.gradle.kts`:

**Groovy (`build.gradle`):**
```groovy
repositories {
    mavenCentral()
    maven { url 'https://kafkaliu.github.io/dianyaai-asr-android-sdk/' }
}
```

**Kotlin (`build.gradle.kts`):**
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://kafkaliu.github.io/dianyaai-asr-android-sdk/") }
}
```

Then, add the dependency:

**Groovy (`build.gradle`):**
```groovy
dependencies {
    implementation 'com.dianyaai.asr:dianyaai-asr-android-sdk:1.0.0' // TODO: Replace with the latest version
}
```

**Kotlin (`build.gradle.kts`):**
```kotlin
dependencies {
    implementation("com.dianyaai.asr:dianyaai-asr-android-sdk:1.0.0") // TODO: Replace with the latest version
}
```

## Usage

The SDK provides two main clients for speech recognition: `RealTimeTranscribeClient` for live audio streams and `FileTranscribeClient` for transcribing pre-recorded audio files. Both clients are designed using Kotlin Coroutines and Flow.

### Real-time Transcription

Use `RealTimeTranscribeClient` to get transcriptions from a continuous audio stream (e.g., a microphone). The client communicates over a WebSocket.

**Audio Format:** The client expects audio data in **16kHz, 16-bit, single-channel PCM** format.

**Example:**

```kotlin
import com.dianyaai.asr.createRealTimeTranscribeClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

// 1. Create a CoroutineScope.
// An activity's lifecycleScope or a viewModelScope is recommended.
val scope = CoroutineScope(Dispatchers.IO)

// 2. Initialize the real-time client.
val client = createRealTimeTranscribeClient(authToken = "YOUR_AUTH_TOKEN")

// 3. Set up jobs to listen to the state and data streams.
// It's crucial to start listening *before* calling connect().

// Listen for connection state changes
scope.launch {
    client.stateStream.collect { state ->
        println("Client state changed: $state")
        if (state is RealTimeTranscribeClient.ClientState.Stopped) {
            val error = state.error
            if (error != null) {
                println("Client stopped with error: ${error.localizedMessage}")
            } else {
                println("Client stopped gracefully.")
            }
        }
    }
}

// Listen for transcription messages from the server
scope.launch {
    client.dataStream.collect { message ->
        when (val data = message.data) {
            is RealTimeTranscribeMessageType.AsrResult -> {
                println("Final Result: ${data.data.text}")
            }
            is RealTimeTranscribeMessageType.AsrResultPartial -> {
                println("Partial Result: ${data.data.text}")
            }
            is RealTimeTranscribeMessageType.Error -> {
                println("Server Error: ${data.data.data}")
            }
            is RealTimeTranscribeMessageType.Stop -> {
                println("Server indicated end of transcription.")
                // The dataStream will finish after this message.
            }
        }
    }
    println("Data stream finished.")
}

// 4. Connect to the server.
// This is not a suspend function; it launches its own coroutine.
client.connect()

// 5. Send audio data.
// In a real app, you would get this data from a microphone manager.
// These are suspend functions and must be called from a coroutine.
scope.launch {
    // The client automatically buffers and sends data in correctly sized chunks.
    // The audio must be 16kHz, 16-bit, single-channel PCM.
    client.sendAudioChunk(someAudioData)

    // When you're done sending audio, call stopSendingAudio().
    // The client sends any remaining buffered audio and then a special
    // "end" message to the server. The connection remains open to receive
    // any final results.
    client.stopSendingAudio()
}


// 6. Disconnect.
// When you are completely finished, call disconnect() to close the
// WebSocket and release resources.
// client.disconnect()
```

### File Transcription

Use `FileTranscribeClient` to transcribe a local audio file. The client handles uploading the file and polling for the transcription status.

**Example:**

```kotlin
import android.content.Context
import android.net.Uri
import com.dianyaai.asr.createFileTranscribeClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

// 1. Get the Uri of the audio file you want to transcribe.
// This example assumes you have a file in your app's assets.
fun getUriFromAssets(context: Context, fileName: String): Uri {
    val file = File(context.cacheDir, fileName)
    context.assets.open(fileName).use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return Uri.fromFile(file)
}

val context: Context = // Your Android Context
val fileUri = getUriFromAssets(context, "myaudio.mp3")

// 2. Create a CoroutineScope.
val scope = CoroutineScope(Dispatchers.IO)

// 3. Create the file transcribe client.
val client = createFileTranscribeClient(
    context = context,
    authToken = "YOUR_AUTH_TOKEN",
    fileURL = fileUri
)

// 4. Listen for state changes and data updates from the streams.
scope.launch {
    client.stateStream.collect { state ->
        println("Client state changed: $state")
        if (state is FileTranscribeClient.ClientState.Failed) {
            println("Client failed with error: ${state.error.localizedMessage}")
        }
    }
}

scope.launch {
    client.dataStream.collect { message ->
        println("Received status: ${message.data}")
        
        // Check if the transcription is done
        if (message.data == FileTranscribeMessageType.DONE) {
            println("Transcription complete!")
            if (message.summaryMd != null) {
                println("--- Summary ---")
                println(message.summaryMd)
            }
            if (message.details != null) {
                 println("--- Details ---")
                message.details.forEach { detail ->
                    println("Speaker ${detail.speaker} (${detail.startTime}-${detail.endTime}s): ${detail.text}")
                }
            }
            // The dataStream will finish after this message.
        } else if (message.data.isFailed) {
            println("Transcription failed with status: ${message.status}")
        }
    }
    println("Data stream has finished.")
}

// 5. Start the transcription process.
// This is not a suspend function.
client.start()
```
