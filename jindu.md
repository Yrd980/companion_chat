# 项目进度

## 2026-05-11 - CompanionChat v0.1.0 UI骨架

### 完成内容
- 创建 Android Compose 项目，包名 `com.companion.chat`
- 底部导航 4 个 Tab：首页、对话、记忆、设置
- 首页空壳占位（未来放海报/宣传）
- 对话页完整 UI：消息列表、用户/AI 气泡、输入栏、发送按钮、流式输出模拟、图片上传、语音输入按钮
- 记忆页空壳占位
- 设置页空壳占位（角色/模型/语音/外观/关于）
- 推理引擎/语音引擎/角色抽象接口预留
- APK 编译通过，ADB 推送到手机

### 关键文件
- 设计文档：`docs/plans/2026-05-11-android-chat-app-design.md`
- 实施计划：`docs/plans/2026-05-11-android-chat-app-plan.md`
- Android 项目：`CompanionChat/`
- 模型文件：`models/gemma-4-E2B-it.litertlm`（2.4GB，从 Edge Gallery 复制）
- 模型在手机上：`/sdcard/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/`

---

## 2026-05-11 - CompanionChat v0.1.0 Phase 2：模型推理 + 语音输入输出

### 完成内容

#### 依赖升级
- Kotlin 2.0.21 → 2.3.20（LiteRT-LM 要求 Kotlin metadata 2.2+）
- AGP 8.4.2 → 8.5.2
- `kotlinOptions` → `tasks.withType<KotlinCompile>().configureEach { compilerOptions {} }`（适配 2.3.x）
- JDK target 17（兼容 JDK 21 编译器）

#### LiteRT-LM 推理引擎
- 集成 `com.google.ai.edge.litertlm:litertlm-android:0.11.0` Maven 依赖
- 通过 `javap` 反编译 AAR 确认精确 API 签名（源码 vs 发布版有差异）
- 实现 `LiteRTLMInferenceEngine`，流程：`EngineConfig → Engine(config) → engine.initialize() → engine.createConversation(convConfig) → conversation.sendMessageAsync(text).collect {}`
- 默认模型路径：`/sdcard/Download/gemma-4-E2B-it.litertlm`
- 模型已通过 ADB 推送到手机（2.4GB，28.3 MB/s）

#### 语音输入引擎
- 实现 `AndroidVoiceInputEngine`，基于 Android `SpeechRecognizer`
- 支持中文普通话识别，`callbackFlow` 封装回调事件
- 自动停止检测、错误处理

#### 语音输出引擎
- 实现 `AndroidVoiceOutputEngine`，基于 Android `TextToSpeech`
- 支持中文 TTS，`MutableStateFlow` 跟踪播放状态
- 语速 1.0f，音调 1.0f

#### ChatViewModel 升级
- 改为 `AndroidViewModel`，直接创建真实引擎实例
- 引擎初始化在 ViewModel init 时自动触发
- 流式推理通过 `sendMessageStream().collect` 更新 UI
- 语音输入/输出生命周期随 ViewModel 管理

#### UI 更新
- ChatScreen 增加语音权限处理（`RECORD_AUDIO`）
- ChatInputBar 增加 TTS 播放中状态显示和停止按钮
- 状态栏显示模型初始化进度

### 验证
- APK 编译通过（BUILD SUCCESSFUL in 23s）
- ADB 安装到手机成功（aa972376）
- 模型文件就位：`/sdcard/Download/gemma-4-E2B-it.litertlm`

### 关键文件（新增/修改）
- 推理引擎：`CompanionChat/app/src/main/java/com/companion/chat/engine/LiteRTLMInferenceEngine.kt`
- 语音输入：`CompanionChat/app/src/main/java/com/companion/chat/engine/AndroidVoiceInputEngine.kt`
- 语音输出：`CompanionChat/app/src/main/java/com/companion/chat/engine/AndroidVoiceOutputEngine.kt`
- ViewModel：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
- 聊天页面：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/ChatScreen.kt`
- 输入栏：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/components/ChatInputBar.kt`
- 集成设计：`docs/plans/2026-05-11-model-voice-integration-design.md`

### 遇到的问题和解决方案
1. **LiteRT-LM 要求 Kotlin 2.2+**：从 2.0.21 升级到 2.3.20
2. **`kotlinOptions` 在 2.3.x 废弃**：改用 `tasks.withType<KotlinCompile>` 的 `compilerOptions`
3. **LiteRT-LM v0.11.0 API 与源码不一致**：用 `javap` 反编译 AAR 的 classes.jar 确认真实签名
4. **`Engine` 构造函数只有 1 个参数**（不是源码里的 2 个）
5. **`SamplerConfig` 的 topP/temperature 是 Double**（不是 Float）
6. **ADB 不在 PATH**：使用 `D:\AndroidstudioSDK\platform-tools\adb.exe` 完整路径

### 待做
- 实际运行测试模型推理效果
- 记忆系统
- 角色管理
- 模型/角色下载管理

---

## 2026-05-12 - CompanionChat v0.1.0 Bug 修复 + 图片多模态支持

### 问题修复

#### 1. 模型文件读取失败（Android 分区存储）
- **问题**：Android 10+ 分区存储导致无法读取 `/sdcard/Download/` 路径
- **解决**：改为使用 `context.getExternalFilesDir("models")` → `/sdcard/Android/data/com.companion.chat/files/models/`
- **部署流程**：卸载旧包 → 安装新包 → ADB 推送模型到应用目录

#### 2. MIUI 无法查看 Logcat
- **问题**：小米手机 MIUI 默认屏蔽应用日志输出，`adb logcat` 无法看到应用日志
- **解决**：在 `ChatUiState` 增加 `diagnosticLog` 字段，引擎日志直接显示在 UI 上；同时引擎用 `openFileOutput("engine_log.txt")` 写文件日志

#### 3. 第一次对话后无法继续
- **问题**：`callbackFlow` 的 `sendMessageAsync().collect` 完成后未调用 `close()`，Flow 永远不结束，`isGenerating` 永远为 true
- **解决**：在 `finally` 块中添加 `close()` 确保 Flow 正常终止

#### 4. 图片显示虚假（UI bug）
- **问题**：ChatInputBar 和 MessageBubble 显示灰色占位方块
- **解决**：集成 Coil 图片加载库（`coil-compose:3.2.0`），用 `AsyncImage` 替代手动 Box 绘制

#### 5. 图片无法被模型识别（核心功能）
- **问题**：`sendMessageStream()` 只发送文本 `lastUserMessage.content`，完全忽略 `images` 列表
- **解决**：
  - 通过 `javap` 反编译 AAR 确认图片 API：`Content.ImageBytes(byte[])`, `Content.Text(String)`, `Contents.of(Content...)`
  - 新增 `uriToImageBytes(uri)` 工具方法：URI → InputStream → Bitmap → 缩放到 1024px → PNG byte[]
  - `sendMessageStream()` 检测到图片时构建多模态消息：`Contents.of(ImageBytes, Text)` 发送
  - `EngineConfig` 增加 `visionBackend = Backend.GPU()` 和 `maxNumImages = 4` 启用视觉能力

### 关键文件（新增/修改）
- 推理引擎：`CompanionChat/app/src/main/java/com/companion/chat/engine/LiteRTLMInferenceEngine.kt`（图片转换+多模态发送）
- ViewModel：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`（诊断日志）
- 聊天页面：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/ChatScreen.kt`（诊断显示）
- 输入栏：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/components/ChatInputBar.kt`（Coil 图片预览）
- 消息气泡：`CompanionChat/app/src/main/java/com/companion/chat/ui/chat/components/MessageBubble.kt`（Coil 图片显示+全屏预览）
- 依赖：`gradle/libs.versions.toml` + `app/build.gradle.kts`（添加 Coil 库）

### 部署状态
- APK 编译通过（BUILD SUCCESSFUL in 1m 4s）
- 卸载旧包 → 安装新包 → 推送模型到应用目录完成
- 模型位置：`/sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm`

### 待做
- 测试图片识别效果（用户在手机上操作验证）
- 记忆系统
- 角色管理
- 模型/角色下载管理
