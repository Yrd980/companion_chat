# CompanionChat

CompanionChat，也叫 Anime Companion，是一个本地优先的 Android AI 伴侣应用。它探索的是围绕角色身份、长期记忆、语音优先交互、用户可控模型边界构建的陪伴体验。

这个仓库承载的是产品方向里的 Android 软件侧。它不是云端助手套壳，也不提交大模型文件。

## 产品方向

Anime Companion 面向私密、连续、可长期发展的 AI 陪伴：

- 尽量使用本地模型运行
- 稳定的角色身份
- 长期记忆与偏好学习
- 语音优先，文字可用
- 本地/云端后端由用户显式选择
- 未来面向可穿戴 Companion 设备，手机作为本地智能中枢

产品体验应该像一个持续陪伴的角色，而不是一次性聊天窗口或通用任务机器人。

## 当前已有能力

- Compose Android 应用，包含 Home、Chat、Memory、Helmet diagnostics、Settings
- Companion Turn 流程：接收/拒绝、流式输出、最终提交、语音播放、时间线事件、记忆刷新、偏好学习
- Room 持久化会话、消息、记忆、偏好、角色卡、Skills 和时间线事件
- Durable Memory：候选审核、置顶/确认记忆投影、下轮记忆注入
- Role Card 和 Skill 系统
- LiteRT-LM 和 llama.cpp runtime adapter
- 本地/远程语音和图片生成 provider 配置
- Cloud ASR、HTTP 语音克隆、HTTP 图片生成前的 Privacy Gate

## 仓库地图

```text
app/src/main/java/com/companion/chat/
  MainActivity.kt          应用入口和导航
  AppContainer.kt          依赖容器
  companion/turn/          Companion Turn 事务流程
  engine/                  模型 runtime、ASR/TTS、图片生成
  data/local/              Room 数据库、DAO、实体
  data/memory/             Durable Memory 模块
  memory/                  记忆提取和检索
  preference/              Preference Learning 辅助
  identity/                Role Card 仓库和 prompt
  ui/chat/                 聊天 UI 和 ChatViewModel
  ui/home/                 首页 dashboard 和发现目录
  ui/memory/               记忆与关系页面
  ui/settings/             profile、模型、语音、角色、skill 设置
docs/                      产品、架构和开发文档
scripts/                   Android 构建/部署脚本
third_party/               native/model runtime 源码依赖
```

## 运行链路

```text
Chat UI
-> ChatViewModel
-> CompanionTurnModule
-> ModelRuntimeLifecycle + InferenceEngine
-> 会话持久化
-> 语音播放 / 时间线事件 / Durable Memory 刷新 / Preference Learning
```

## 构建与运行

开发操作入口放在 [AGENTS.md](AGENTS.md) 和
[docs/android-dev-scripts.md](docs/android-dev-scripts.md)。

常用入口：

```powershell
scripts\android-dev.bat doctor
scripts\android-dev.bat build
scripts\android-dev.bat deploy
scripts\android-dev.bat logs
```

仓库不包含 LLM、ASR、TTS 或图片模型权重。运行时功能应该校验缺失的模型包，并给出清晰失败信息，而不是崩溃。

## 技术栈

- Kotlin
- Android SDK
- Jetpack Compose
- Navigation Compose
- Room + KSP
- LiteRT-LM Android
- llama.cpp
- sherpa-onnx
- ONNX Runtime Android
- stable-diffusion.cpp
- Android TextToSpeech
- Coil

## 文档

- [AGENTS.md](AGENTS.md)：代理/开发者操作笔记
- [README.md](README.md)：英文 README
- [docs/android-dev-scripts.md](docs/android-dev-scripts.md)：Android 脚本
- [docs/architecture-review-2026-06-11.md](docs/architecture-review-2026-06-11.md)：架构评审
- [docs/product-ui-ux.md](docs/product-ui-ux.md)：产品和 UI 方向
- [docs/frontend-backend-gaps.md](docs/frontend-backend-gaps.md)：实现差距
- [docs/waydroid.md](docs/waydroid.md)：Waydroid 笔记
