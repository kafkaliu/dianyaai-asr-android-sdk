# DianyaaiASR Android SDK

DianyaaiASR 是一个 Android SDK，用于将电牙 AI 的自动语音识别（ASR）服务集成到您的 Android 应用程序中。

## 功能

- 文件转写：转写音频文件并获取结果。
- 来自连续音频流的实时转写。

## 安装

本 SDK 通过 GitHub Packages 以 Maven 包的形式分发。

将仓库地址添加到您项目的 `build.gradle` 或 `build.gradle.kts` 文件中：

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

然后，添加依赖：

**Groovy (`build.gradle`):**
```groovy
dependencies {
    implementation 'com.dianyaai.asr:dianyaai-asr-android-sdk:1.0.0' // TODO: 请替换为最新版本
}
```

**Kotlin (`build.gradle.kts`):**
```kotlin
dependencies {
    implementation("com.dianyaai.asr:dianyaai-asr-android-sdk:1.0.0") // TODO: 请替换为最新版本
}
```

## 使用方法

本 SDK 提供两个主要的客户端用于语音识别：`RealTimeTranscribeClient` 用于实时音频流，`FileTranscribeClient` 用于转写预先录制的音频文件。两者都使用 Kotlin 协程和 Flow 进行设计。

### 实时转写

使用 `RealTimeTranscribeClient` 从连续的音频流（例如麦克风）中获取转写结果。该客户端通过 WebSocket 进行通信。

**音频格式:** 客户端期望的音频数据格式为 **16kHz 采样率、16位深度、单声道 PCM**。

**示例:**

```kotlin
import com.dianyaai.asr.createRealTimeTranscribeClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

// 1. 创建一个协程作用域。
// 推荐使用 Activity 的 lifecycleScope 或 ViewModel 的 viewModelScope。
val scope = CoroutineScope(Dispatchers.IO)

// 2. 初始化实时客户端。
val client = createRealTimeTranscribeClient(authToken = "YOUR_AUTH_TOKEN")

// 3. 设置任务以监听状态和数据流。
// 在调用 connect() 之前开始监听是至关重要的。

// 监听连接状态变化
scope.launch {
    client.stateStream.collect { state ->
        println("客户端状态改变: $state")
        if (state is RealTimeTranscribeClient.ClientState.Stopped) {
            val error = state.error
            if (error != null) {
                println("客户端因错误停止: ${error.localizedMessage}")
            } else {
                println("客户端已正常停止。")
            }
        }
    }
}

// 监听来自服务器的转写消息
scope.launch {
    client.dataStream.collect { message ->
        when (val data = message.data) {
            is RealTimeTranscribeMessageType.AsrResult -> {
                println("最终结果: ${data.data.text}")
            }
            is RealTimeTranscribeMessageType.AsrResultPartial -> {
                println("部分结果: ${data.data.text}")
            }
            is RealTimeTranscribeMessageType.Error -> {
                println("服务器错误: ${data.data.data}")
            }
            is RealTimeTranscribeMessageType.Stop -> {
                println("服务器已表示转写结束。")
                // 此消息后，dataStream 将会结束。
            }
        }
    }
    println("数据流已结束。")
}

// 4. 连接到服务器。
// 这不是一个挂起函数，它会启动自己的协程。
client.connect()

// 5. 发送音频数据。
// 在真实的应用中，您会从麦克风管理器获取这些数据。
// 这些是挂起函数，必须在协程中调用。
scope.launch {
    // 客户端会自动缓冲并以正确大小的块发送数据。
    // 音频必须是 16kHz、16位、单声道 PCM 格式。
    client.sendAudioChunk(someAudioData)

    // 当您完成发送音频后，调用 stopSendingAudio()。
    // 客户端会发送所有剩余的缓冲音频，然后向服务器发送一个特殊的
    // “结束”消息。连接将保持打开以接收任何最终结果。
    client.stopSendingAudio()
}


// 6. 断开连接。
// 当您完全完成并希望关闭连接时，调用 disconnect()。
// 这将关闭 WebSocket 并释放资源。
// client.disconnect()
```

### 文件转写

使用 `FileTranscribeClient` 来转写一个本地音频文件。客户端会处理文件上传和状态轮询。

**示例:**

```kotlin
import android.content.Context
import android.net.Uri
import com.dianyaai.asr.createFileTranscribeClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

// 1. 获取您要转写的音频文件的 Uri。
// 此示例假设您在应用的 assets 目录中有一个文件。
fun getUriFromAssets(context: Context, fileName: String): Uri {
    val file = File(context.cacheDir, fileName)
    context.assets.open(fileName).use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return Uri.fromFile(file)
}

val context: Context = // 您的 Android Context
val fileUri = getUriFromAssets(context, "myaudio.mp3")

// 2. 创建一个协程作用域。
val scope = CoroutineScope(Dispatchers.IO)

// 3. 创建文件转写客户端。
val client = createFileTranscribeClient(
    context = context,
    authToken = "YOUR_AUTH_TOKEN",
    fileURL = fileUri
)

// 4. 监听来自流的状态变化和数据更新。
scope.launch {
    client.stateStream.collect { state ->
        println("客户端状态改变: $state")
        if (state is FileTranscribeClient.ClientState.Failed) {
            println("客户端因错误失败: ${state.error.localizedMessage}")
        }
    }
}

scope.launch {
    client.dataStream.collect { message ->
        println("收到状态: ${message.data}")
        
        // 检查转写是否完成
        if (message.data == FileTranscribeMessageType.DONE) {
            println("转写完成！")
            if (message.summaryMd != null) {
                println("--- 总结 ---")
                println(message.summaryMd)
            }
            if (message.details != null) {
                 println("--- 详情 ---")
                message.details.forEach { detail ->
                    println("说话人 ${detail.speaker} (${detail.startTime}-${detail.endTime}s): ${detail.text}")
                }
            }
            // 此消息后，dataStream 将会结束。
        } else if (message.data.isFailed) {
            println("转写失败，状态为: ${message.status}")
        }
    }
    println("数据流已结束。")
}

// 5. 启动转写流程。
// 这不是一个挂起函数。
client.start()
```
