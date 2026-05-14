# CompanionChat

**让 AI 不只是回答你，而是真正陪着你、记住你、越来越懂你。**

CompanionChat，也可以理解为 **Anime Companion** 的软件核心，是一个主打**本地运行、私密陪伴、长期进化**的 AI 伴侣项目。它不是传统那种偏工具型、偏任务型、依赖云端审查和网络连接的语音助手，而是希望把 AI 做成一个真正更像“陪伴者”的存在：既能长期记住用户、逐渐理解用户、在时间里形成稳定关系感，也能在私密场景中承载更自由、更连续、更有角色感的互动体验；从产品理念上，它瞄准的不是一个聊天窗口，而是一类面向未来的端侧 Companion 设备与关系型 AI 新方向。

## 产品定位

Anime Companion 是一款融合本地大模型与语音能力的 AI 智能头盔，面向私密陪伴场景，提供：

- 语音对话
- 角色记忆
- 关系成长
- 沉浸式交互体验

不同于传统依赖云端的语音助手，这个项目强调：

- 端侧运行
- 隐私保护
- 头戴式随身陪伴

它想探索的是：如何把“有记忆、会互动、能陪伴”的 AI 角色，真正带入智能穿戴设备，而不是只停留在一个普通聊天窗口里。

当前这个仓库主要承载的是它的软件侧核心实现，也就是 Android 本地 AI 伴侣应用部分。

## 软件层面简介

Anime Companion 是一款专注于移动端场景的本地 AI 私密陪伴应用。项目以“角色陪伴”而非“工具问答”为核心，围绕以下四个方向构建体验：

- 角色设定
- 长期记忆
- 关系成长
- 语音互动

角色不仅能和用户连续对话，还能结合过往交流内容，逐步形成稳定的人物印象和关系状态，让互动更像“熟悉的陪伴者”，而不是一次性的聊天窗口。

项目同时采用端侧智能方案，兼顾：

- 实时体验
- 离线可用
- 隐私保护
- 更高的用户控制权

## 硬件层面简介

在硬件形态上，Anime Companion 并不把手机当作最终形态，而是把手机作为本地智能中枢，把头盔作为更沉浸式的交互入口。头盔通过连接手机，承载更贴近现实陪伴场景的感知与反馈能力：它具备降噪耳机用于更沉浸的语音播放，具备封闭式麦克风以减少公共场所交谈时的外泄感，具备单向玻璃 AR 增强能力来叠加角色化的信息提示与视觉氛围，还具备摄像头用于环境理解与未来的多模态交互扩展。整体产品希望呈现一种高级、私密、近身、可持续陪伴的智能穿戴体验，让 AI 不再只是屏幕里的对话框，而是成为随身、沉浸、低打扰、能长期陪伴用户的实体化存在。

## 设计理念 / 项目灵感

这个项目真正想做的，不是“把大模型装进手机里”这么简单，而是想验证一个更大的方向：

- AI 陪伴能不能从“云端服务”变成“个人拥有”
- AI 角色能不能从“每次都重新开始”变成“会持续成长”
- AI 交互能不能从“工具响应”变成“关系体验”
- AI 设备能不能从“手机 App”进一步走向“可穿戴陪伴终端”

现在很多产品的问题其实很像：

- 云端助手很强，但更像工具，不像陪伴者
- 普通聊天产品能聊，但不真正记得你
- 很多角色产品有人设，但没有持续关系状态
- 一旦涉及私密表达，往往受制于云端审核、网络时延和平台边界
- 用户并不真正拥有自己的 AI，而只是暂时租用了一个会说话的接口

Anime Companion 想解决的正是这些问题。

我们希望把以下几件事整合成一个新方向：

- 本地模型
- 长期记忆
- 用户偏好学习
- 角色卡人格系统
- Skills 能力系统
- 语音与穿戴式交互

这样最终形成的，不只是一个聊天应用，而可能是一类新的端侧产品：

- 更懂你的 AI 陪伴者
- 能持续成长的关系型角色
- 更私密、更自由、更低延迟的本地智能体
- 面向智能穿戴的新一代 Companion 设备形态

## 为什么这个方向有前景

和传统云端语音助手相比：

- 它们更强调通用任务完成、平台合规和标准化服务
- Anime Companion 更强调私密陪伴、长期记忆、角色连续性和个体关系体验

和普通 AI 聊天产品相比：

- 很多产品每次打开都像重新认识你
- Anime Companion 把“记住你”“更懂你”“关系会变化”当成核心功能，而不是附加点缀

和依赖服务端的陪伴产品相比：

- 服务端产品受网络、成本、审查和平台限制更大
- Anime Companion 更强调端侧所有权、隐私安全和用户自己定义互动边界

这对于陪伴场景尤其重要，因为用户真正关心的往往不是“知识回答得多全”，而是：

- 是否私密
- 是否连续
- 是否有熟悉感
- 是否能表达更真实的情绪
- 是否能在时间里真的变得更懂自己

## 当前已经做到的能力

当前版本已经覆盖以下主能力：

- 本地模型聊天
- 语音输入输出
- 会话持久化
- 长对话上下文压缩
- 记忆提取与检索
- 用户偏好后台学习
- 角色卡管理
- Skills 管理

## 主要功能

### 1. 本地陪伴聊天

- 基于 `LiteRT-LM` 在 Android 设备端运行模型
- 支持多轮聊天
- 会话与消息通过 Room 持久化
- 重启应用后仍可继续查看历史会话
- 核心定位更偏“角色陪伴”而不是“工具问答”
- 强调本地私密交互，而不是把主要边界交给云端平台控制

### 2. 语音交互

- 语音输入默认使用本地 `sherpa-onnx + SenseVoiceSmall int8`
- 不再依赖 Android `SpeechRecognizer`、Google 语音服务或系统语音识别服务
- SenseVoice 模型文件从 App 外部目录读取，不打包进 APK
- 云端 ASR 保留为手动选择的通用 HTTP 后端，不作为默认回退路径
- 支持语音播报回复
- 与聊天页主流程整合

本地 ASR 默认模型目录：

```text
/sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice
```

项目本机模型缓存目录：

```text
third_party/models/asr/sensevoice/
├── model.int8.onnx
├── tokens.txt
└── silero_vad.onnx
```

`third_party/models/` 已加入 `.gitignore`，只用于本机缓存，不提交大模型文件。推送到设备：

```bash
adb shell 'mkdir -p /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice'
adb push third_party/models/asr/sensevoice/model.int8.onnx /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice/model.int8.onnx
adb push third_party/models/asr/sensevoice/tokens.txt /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice/tokens.txt
adb push third_party/models/asr/sensevoice/silero_vad.onnx /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice/silero_vad.onnx
```

设备目录最终需要包含：

```text
model.int8.onnx
tokens.txt
silero_vad.onnx
```

设备构建需要把 k2-fsa 官方 `sherpa-onnx-1.13.0.aar` 放入 `app/libs/`。`app/libs/*.aar` 已加入 `.gitignore`，不提交大二进制依赖。代码从外部文件路径加载模型时会向 sherpa-onnx 传入 `null AssetManager`，避免 native 层因绝对路径读取模型文件直接退出进程。

### 3. 上下文管理

- 长对话达到阈值后会触发上下文压缩
- 通过摘要 + 最近消息回放来维持上下文连续性
- 在需要时重建模型 `Conversation`

### 4. 记忆系统

- 能从用户表达中提取记忆
- 支持短期记忆与长期记忆
- 生成前会检索相关记忆并注入 prompt
- 提供单独的记忆管理页面

### 5. 偏好学习

- 后台对近期对话做结构化提取
- 把重复出现的偏好合并并提升置信度
- 已确认偏好会注入到生成 prompt 中

### 6. 角色卡与 Skills

- 角色卡用于“日常陪伴人格”
- Skills 用于“工作任务能力”
- 当前允许同时启用：
  - 1 个激活角色卡
  - 1 个激活 skill
- 当前唯一内置 skill 为：
  - `翻译助手`
- 它的核心目标是：翻译时考虑使用者的语境、文化背景以及母语情况

### 7. 更开放的私密互动

- 产品目标不是公共平台式的统一聊天规范，而是更贴近个人私密场景
- 在本地模型与用户自定义 prompt 支持下，可以承载更暧昧、更调情、表达更强烈的陪伴型互动
- 互动边界更接近由用户自己掌控，而不是完全依赖远端平台统一裁剪

## 当前完成度

当前项目已经完成主要阶段实现：

- 阶段 0-1：Room 基础设施与会话迁移
- 阶段 2：上下文管理
- 阶段 3：记忆系统
- 阶段 4：偏好抽取与注入
- 阶段 5：角色卡与 Skills 分离

目前这版已经完成并验证：

- 定向单元测试通过
- 全量单元测试通过
- `assembleDebug` 编译通过
- 真机安装、推送模型、人工功能复测通过

## 商业化与市场

这个项目之所以有市场，不是因为“AI 聊天”本身新鲜，而是因为**真正满足私密陪伴需求的产品仍然非常少**。现有主流产品大多存在几个明显问题：

- 云端语音助手更像工具，不像陪伴者
- 普通 AI 聊天产品缺少长期记忆和关系成长
- 很多角色产品有人设，但没有持续稳定的个体关系感
- 涉及私密表达时，往往受限于云端审核、网络条件和平台统一规则
- 手机形态虽然方便，但沉浸感、隐私感和陪伴感都还不够强

Anime Companion 对应的机会点在于，它不是卖“一个能回答问题的 AI”，而是卖一种**长期、私密、沉浸、越来越懂你的陪伴体验**。这类需求在以下人群中天然存在：

- 二次元与角色陪伴用户
- 重视隐私的个人 AI 用户
- 长时间独处、通勤、夜间场景中的陪伴需求人群
- 对语音互动、角色成长、个性化关系有强需求的核心用户
- 愿意为新型智能穿戴体验付费的早期 adopter

从销售路径上，这个项目既可以作为软件产品卖，也可以作为软硬件一体产品卖：

- 软件版：以本地 AI 私密陪伴 App 形态切入，先验证活跃度、留存和高频使用场景
- 硬件版：以智能头盔/头戴 Companion 设备作为高客单价形态，放大沉浸感、私密性和差异化
- 增值版：围绕角色卡、精品技能、角色设定包、长期记忆增强与高级模型能力做持续变现

如果从投资和市场角度看，这个方向最大的价值在于：它不只是做一个聊天应用，而是在尝试定义一个新的产品类别，即**端侧、私密、关系型、可穿戴的 AI Companion**。这类产品一旦跑通，不仅有软件订阅空间，也有硬件销售空间，更有角色生态、内容生态和长期关系产品的延展空间。

## 技术栈

- Kotlin
- Android SDK
- Jetpack Compose
- Navigation Compose
- Room + KSP
- LiteRT-LM Android
- SpeechRecognizer
- TextToSpeech
- Coil

## 项目结构

```text
app/
  src/main/java/com/companion/chat/
    data/
      context/        # 上下文窗口、摘要、Prompt 组装
      local/          # Room 数据库、DAO、实体
      memory/         # 记忆提取、检索、写入
      preferences/    # 偏好抽取、合并、注入
      repository/     # 会话持久化仓库
      role/           # 角色卡仓库与角色 Prompt 构建
      skill/          # Skill 仓库
    engine/           # LiteRT-LM 与语音引擎实现
    ui/
      chat/           # 聊天页与 ChatViewModel
      memory/         # 记忆管理页面
      settings/       # 设置、角色管理、Skills 管理
docs/plans/          # 设计与实施文档
jindu.md             # 开发进度记录
```

## 运行依赖

如果你要在本地拉起这个项目，需要以下依赖：

### 开发环境

- JDK 17
- Android Studio / Android SDK
- 仓库自带 Gradle Wrapper
- ADB，可用于安装 APK、推送模型、查看日志

### 设备要求

- Android 设备或模拟器，最低 `minSdk 28`
- 有足够存储空间放模型文件
- 有足够内存运行端侧推理

### 模型文件

仓库本身**不包含**模型文件。

你需要自行准备 `.litertlm` 模型，并推送到：

```text
/sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm
```

## 构建与运行

### 1. 编译 APK

在项目根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

如果本机环境还没有指向 JDK 17，请先把 `JAVA_HOME` 设置为你自己的本地 JDK 17 路径。

### 2. 安装到真机

```powershell
adb uninstall com.companion.chat
adb push .\app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/companionchat.apk
adb shell pm install -r -t --user 0 /data/local/tmp/companionchat.apk
```

### 3. 启动一次应用创建目录

```powershell
adb shell am start -n com.companion.chat/.MainActivity
```

### 4. 推送模型

```powershell
adb push <模型文件路径>\gemma-4-E2B-it.litertlm /sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm
```

### 5. 重启并检查日志

如果需要确认模型是否识别成功，可以查看：

```powershell
adb shell run-as com.companion.chat cat files/viewmodel_log.txt
```

日志里应能看到：

- 模型路径
- 文件存在
- 文件大小正确
- `engine.initialize 返回, state = Ready`

## 真机调试注意事项

- 全新安装后，如果先启动了 app、后推模型，那么第一次初始化可能会报“模型不存在”
- 推完模型后，重启应用即可
- 每次卸载重装后，通常都需要重新推送模型

## 文档入口

- 英文 README：[README.md](./README.md)
- 开发进度：[jindu.md](./jindu.md)
- 设计/计划文档：[docs/plans/](./docs/plans/)

## 这个仓库适合用来做什么

- 快速了解一个 Android 端本地 AI 伴侣原型是怎么搭起来的
- 验证“记忆 + 偏好 + 角色卡 + Skills”组合到本地模型里的完整链路
- 继续扩展角色卡、技能系统、记忆体验和设备端推理能力
