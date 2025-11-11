# ASRDemo1: DianyaaiASR Android SDK Example

This project is a simple demonstration of how to use the [DianyaaiASR Android SDK](https://github.com/kafkaliu/dianyaai-asr-android-sdk) for audio transcription.

## Features

- Select an audio file from your device.
- Transcribe the audio file using the DianyaaiASR SDK.
- Display the transcription result.
- Configure the authentication token.

## How to Use

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/kafkaliu/dianyaai-asr-android-sdk.git
    cd dianyaai-asr-android-sdk/examples/asrdemo1
    ```

2.  **Open the project in Android Studio.**

3.  **Configure Authentication Token:**
    - Run the app on a simulator or a real device.
    - Navigate to the settings screen by tapping the gear icon.
    - Enter your `authToken`. You can obtain a token from [Dianyaai](https://www.dianyaai.com).
    - Tap "Save".

4.  **Transcribe an Audio File:**
    - Go back to the main screen.
    - Tap the "Select Audio File" button to open the file picker.
    - Select an audio file.
    - The app will start the transcription, and you will see the result on the screen.

## Code Integration

This example demonstrates the basic integration of the DianyaaiASR SDK. Here are the key parts in the code:

### `ASRViewModel.kt`

This file contains the core logic for interacting with the DianyaaiASR SDK.

1.  **Initialization:**
    In the `setup()` method, we initialize the `authToken` from `SharedPreferences`.

    ```kotlin
    fun setup(context: Context) {
        val sharedPreferences = context.getSharedPreferences("asr_demo_prefs", Context.MODE_PRIVATE)
        _authToken.value = sharedPreferences.getString("auth_token", "") ?: ""
    }
    ```

2.  **Transcription:**
    The `transcribeFile(uri: Uri, context: Context)` method creates and starts a `FileTranscribeClient`. It collects the `dataStream` to get real-time status updates.

    ```kotlin
    fun transcribeFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isTranscribing.value = true
            _transcriptionStatus.value = null
            val client = createFileTranscribeClient(
                context = context,
                authToken = _authToken.value,
                fileURL = uri
            )

            launch {
                client.dataStream.collect { message ->
                    if (message.data.isDone) {
                        _transcriptionStatus.value = message
                        _isTranscribing.value = false
                    } else if (message.data.isFailed) {
                        _transcriptionResult.value = "Transcription failed: ${message.message ?: "Unknown error"}"
                        _isTranscribing.value = false
                    } else {
                        _transcriptionResult.value = "Transcription status: ${message.data}"
                    }
                }
            }

            client.start()
        }
    }
    ```

### `ASRScreen.kt`

This Composable provides the user interface for file selection and displaying the result. It uses `rememberLauncherForActivityResult` to allow users to pick an audio file.

```kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    uri?.let {
        viewModel.transcribeFile(it, context)
    }
}

// ...

Button(onClick = { filePickerLauncher.launch("audio/*") }) {
    Text("Select Audio File")
}
```

### `TranscriptionResultScreen.kt`

This view is responsible for displaying the final transcription result in a structured and user-friendly format. It shows the transcribed text and other relevant information from the `FileTranscribeMessage` object in a tabbed view.
