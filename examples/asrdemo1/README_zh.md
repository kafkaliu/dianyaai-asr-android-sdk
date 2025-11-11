# ASRDemo1: DianyaaiASR Android SDK 示例

该项目是一个简单的示例，演示了如何使用 [DianyaaiASR Android SDK](https://github.com/kafkaliu/dianyaai-asr-android-sdk) 进行音频转写。

## 主要功能

- 从您的设备中选择一个音频文件。
- 使用 DianyaaiASR SDK 转写音频文件。
- 显示转写结果。
- 配置认证令牌（`authToken`）。

## 如何使用

1.  **克隆代码库：**
    ```bash
    git clone https://github.com/kafkaliu/dianyaai-asr-android-sdk.git
    cd dianyaai-asr-android-sdk/examples/asrdemo1
    ```

2.  **在 Android Studio 中打开项目。**

3.  **配置认证令牌：**
    - 在模拟器或真实设备上运行 App。
    - 点击齿轮图标进入设置页面。
    - 输入您的 `authToken`。您可以从 [点呀AI](https://www.dianyaai.com) 获取令牌。
    - 点击“保存”。

4.  **转写音频文件：**
    - 返回主屏幕。
    - 点击“选择音频文件”按钮打开文件选择器。
    - 选择一个音频文件。
    - App 将开始转写，您将在屏幕上看到结果。

## 代码集成

该示例演示了 DianyaaiASR SDK 的基本集成方法。以下是代码中的关键部分：

### `ASRViewModel.kt`

该文件包含了与 DianyaaiASR SDK 交互的核心逻辑。

1.  **初始化：**
    在 `setup()` 方法中，我们从 `SharedPreferences` 中初始化 `authToken`。

    ```kotlin
    fun setup(context: Context) {
        val sharedPreferences = context.getSharedPreferences("asr_demo_prefs", Context.MODE_PRIVATE)
        _authToken.value = sharedPreferences.getString("auth_token", "") ?: ""
    }
    ```

2.  **转写：**
    `transcribeFile(uri: Uri, context: Context)` 方法创建并启动一个 `FileTranscribeClient`。它收集 `dataStream` 以获取实时状态更新。

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
                        _transcriptionResult.value = "转写失败: ${message.message ?: "未知错误"}"
                        _isTranscribing.value = false
                    } else {
                        _transcriptionResult.value = "转写状态: ${message.data}"
                    }
                }
            }

            client.start()
        }
    }
    ```

### `ASRScreen.kt`

这个 Composable 提供了文件选择和结果显示的用户界面。它使用 `rememberLauncherForActivityResult` 来让用户选择音频文件。

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
    Text("选择音频文件")
}
```

### `TranscriptionResultScreen.kt`

该视图负责以结构化和用户友好的格式显示最终的转写结果。它在一个标签页视图中显示 `FileTranscribeMessage` 对象中的转写文本和其他相关信息。
