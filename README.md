# CompanionChat

**Make AI more than something that answers you. Make it remember you, grow with you, and truly accompany you.**

CompanionChat, also presented as **Anime Companion**, is a local, privacy-first, continuously evolving AI companion project. It is not meant to be another tool-first, cloud-moderated voice assistant. The goal is to build something closer to a true companion: an AI that remembers the user over time, becomes more familiar through ongoing interaction, supports more expressive private conversation, and points toward a future product category built around edge-native companion devices rather than disposable chat windows.

## Product Positioning

Anime Companion is an AI smart helmet concept that combines:

- on-device large language models
- voice interaction
- persistent character memory
- relationship growth
- immersive wearable companionship

The hardware vision is a head-worn companion device for private interaction scenarios. Instead of treating AI as a cloud tool that only answers commands, the project explores how an AI character with memory, emotion-like continuity, and evolving familiarity can live inside a personal wearable product.

This repository currently focuses on the mobile/software side of that vision: the Android local AI stack, memory system, prompt system, role cards, and skill management.

## Software Overview

Anime Companion is a local AI private companion application designed for mobile usage scenarios.

The product is centered on **companionship instead of utility Q&A**. It is built around four core directions:

- character identity
- long-term memory
- relationship growth
- voice interaction

The goal is to create an experience where the character does not behave like a disposable chat window. Instead, it gradually forms a stable impression of the user, keeps continuity across conversations, and feels more like a familiar companion than a one-off assistant.

Because the system runs on-device, it also aims to balance:

- privacy protection
- offline availability
- low-latency interaction
- user-controlled conversational boundaries

## Hardware Overview

On the hardware side, Anime Companion does not treat the phone as the final form. The phone acts as the local intelligence hub, while the helmet acts as the immersive interaction layer. The helmet is designed to connect to the phone and provide a more advanced companion experience through real-world sensing and private feedback channels: noise-reduction audio playback for immersive voice output, a closed microphone design so speech is less noticeable to people nearby in public spaces, one-way-glass AR enhancement for character overlays and visual atmosphere, and a camera for environment understanding and future multimodal interaction. The overall direction is to create a premium, private, wearable companion product where AI is no longer just a screen-based chat box, but a persistent presence carried with the user.

## Design Philosophy

The long-term vision of this project is not just "put an LLM into an app". The real ambition is to open a new direction for edge AI companions:

- from cloud dependence to personal ownership
- from generic assistants to persistent characters
- from stateless replies to relationship continuity
- from moderated public-assistant behavior to user-controlled private interaction
- from phone-only interaction to wearable embodied companionship

In most current products, AI conversation still breaks in the same places:

- the model forgets who you are
- the personality resets between sessions
- privacy-sensitive dialogue must leave the device
- interaction is shaped by platform moderation rather than user intent
- the assistant behaves like a tool, not a companion

Anime Companion attempts to solve these problems by combining local inference, memory, preference learning, role conditioning, and wearable intimacy into one system.

The ceiling of this direction is much higher than a chat app:

- a companion that becomes more familiar with the user over time
- an AI relationship that feels continuous rather than episodic
- a wearable device that can host private, expressive, and emotionally persistent interaction
- a new product category for edge-native companion intelligence

## Why This Direction Matters

Compared with traditional cloud voice assistants:

- cloud assistants prioritize compliance, generality, and task completion
- Anime Companion prioritizes privacy, persistence, emotional continuity, and character presence

Compared with ordinary AI chat apps:

- ordinary chat apps often reset context and treat each session as disposable
- Anime Companion treats memory and relationship state as first-class product features

Compared with server-side AI companion products:

- server-side products depend on network quality, cloud cost, and platform content controls
- Anime Companion pushes toward local ownership, lower privacy risk, and more user-defined interaction space

This is especially important for companionship scenarios, where users often care about:

- privacy
- emotional continuity
- expressive freedom
- lower friction
- a sense of being known over time

## What It Does Today

The current implementation already covers the main product loop:

- local chat
- voice-first chat interaction
- context compression
- memory retrieval and storage
- background preference extraction
- discover role catalog and import flow
- role card management
- image generation provider configuration
- skills management

## Core Features

### 1. Local AI companion chat

- Uses `LiteRT-LM` on Android for on-device inference
- Supports multi-turn conversation with persisted sessions
- Keeps the latest active conversation available after app restarts
- Optimized for companionship-style conversation rather than pure task Q&A
- Designed for private local interaction without depending on cloud moderation as the primary control layer

### 2. Voice input and output

- Voice input defaults to local `sherpa-onnx + SenseVoiceSmall int8`
- Voice capture uses Android `AudioRecord`, then runs local VAD and SenseVoice transcription
- SenseVoice model files are loaded from the app external files directory instead of being bundled into the APK
- `silero_vad.onnx` is used by sherpa-onnx Silero VAD for on-device speech segmentation before SenseVoice transcription; it is not just a presence-check file
- Cloud ASR remains available as a manually selected generic HTTP backend, not as an automatic fallback path
- When Cloud ASR is selected, voice capture no longer depends on local SenseVoice model files; it records a short fixed window and sends the resulting audio to the configured HTTP backend
- The chat screen is voice-first: tapping the compact microphone button records speech, inserts the recognized transcript, automatically sends it, and automatically reads the assistant response for that voice-triggered turn
- Text input, image upload, and manual replay of the latest assistant response remain available as secondary controls
- Text-to-speech for assistant responses
- Role voice clone mode now tries local `moss-tts-nano` ONNX synthesis first, writes a WAV into app-private storage, and falls back to Android system TTS when models, reference audio, or inference are unavailable
- Integrated into the main chat screen

Default local ASR model directory:

```text
/sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice
```

Local project model cache:

```text
third_party/models/asr/sensevoice/
├── model.int8.onnx
├── tokens.txt
└── silero_vad.onnx
```

`third_party/models/` is ignored by Git and is only used as a local cache. Push the files to the device with:

```bash
adb shell 'mkdir -p /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice'
adb push third_party/models/asr/sensevoice/model.int8.onnx /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice/model.int8.onnx
adb push third_party/models/asr/sensevoice/tokens.txt /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice/tokens.txt
adb push third_party/models/asr/sensevoice/silero_vad.onnx /sdcard/Android/data/com.companion.chat/files/models/asr/sensevoice/silero_vad.onnx
```

The device directory must contain:

```text
model.int8.onnx
tokens.txt
silero_vad.onnx
```

Device builds require the official k2-fsa `sherpa-onnx-1.13.0.aar` in `app/libs/`. `app/libs/*.aar` is ignored by Git so the large binary dependency is not committed. When loading SenseVoice and Silero VAD models from external absolute paths, the app passes `null` for sherpa-onnx's `AssetManager`; passing a non-null asset manager for SD-card model paths can make sherpa-onnx terminate the process from native code.

Default local voice clone model directory:

```text
/sdcard/Android/data/com.companion.chat/files/models/tts/moss-tts-nano
```

Expected device layout:

```text
tts/
├── browser_poc_manifest.json
├── tokenizer.model
├── tts_browser_onnx_meta.json
├── moss_tts_prefill.onnx
├── moss_tts_decode_step.onnx
├── moss_tts_local_decoder.onnx
├── moss_tts_local_cached_step.onnx
├── moss_tts_local_fixed_sampled_frame.onnx
├── moss_tts_global_shared.data
└── moss_tts_local_shared.data
audio_tokenizer/
├── codec_browser_onnx_meta.json
├── moss_audio_tokenizer_encode.onnx
├── moss_audio_tokenizer_encode.data
├── moss_audio_tokenizer_decode_full.onnx
├── moss_audio_tokenizer_decode_step.onnx
└── moss_audio_tokenizer_decode_shared.data
```

The app does not commit or bundle MOSS model files. A local cache can live at `third_party/models/tts/moss-tts-nano/`; push that directory into the app external files directory and set a role to `CLONE` with a readable reference WAV/URI. The current Android path validates the real OpenMOSS browser ONNX bundle and falls back to system TTS until the autoregressive ONNX runner is completed.

### 3. Context management

- Compresses long conversations
- Rebuilds the model conversation when context gets too large
- Replays recent messages and injects summary when needed

### 4. Memory system

- Extracts memories from user messages
- Stores long-term and short-term memory in Room
- Retrieves relevant memory before generation
- Provides a memory management screen

### 5. Preference learning

- Runs background extraction for user preferences
- Merges repeated preferences into confirmed preferences
- Injects confirmed preferences into the system prompt

### 6. Role cards and skills

- Role cards are for daily companion identity and personality
- Skills are for task-oriented behavior
- One active role card + one active skill can work together
- The built-in skill is currently:
  - `Translator`, designed to consider the user's context, culture, and native language

### 7. Discover catalog and role import

- The home screen is now a discover catalog with search, tag filters, mature-content visibility, and hot/new/name sorting
- Seeded discover roles include persona, opening message, image style, voice summary, content rating, and generation presets
- Users can favorite, unlock, inspect details, and copy discover roles into their own role cards
- Tapping start chat from a role detail imports and activates the role before navigating into chat
- Fixed the runtime crash caused by `DiscoverViewModel` missing the explicit `Application` constructor required by `AndroidViewModelFactory`

### 8. Image generation and role media

- Image generation now supports HTTP, local DreamLite package checking, and local SD1.5 Hyper-SD generation through `stable-diffusion.cpp`
- Model settings expose image Base URL, API key, model name, request template, response field path, timeout, local model path, local image size, steps, CFG scale, seed, and Vulkan toggle
- Chat scene image generation routes through the provider selector and reports failures back into UI state
- The role editor is split into Basic, Persona, Image, and Voice tabs for avatar images, galleries, image prompts, voice mode, and voice reference URIs
- Images generated from discover roles can be appended to imported role galleries
- Local SD1.5 Hyper-SD uses `third_party/stable-diffusion.cpp` as a Git submodule and builds a `companion_sd` JNI library. Model files are expected under `/sdcard/Android/data/com.companion.chat/files/models/image/sd15-hypersd` and are not committed.
- Minimal local SD config:

```json
{
  "model_name": "SD1.5 Hyper-SD",
  "runtime": "stable-diffusion.cpp",
  "model_path": "sd15.safetensors",
  "lora_paths": ["hypersd_lora.safetensors"],
  "required_files": ["sd15.safetensors", "hypersd_lora.safetensors"],
  "default_width": 512,
  "default_height": 512,
  "default_steps": 4,
  "default_cfg_scale": 1.0,
  "use_vulkan": true
}
```

Push a prepared package with:

```bash
adb shell 'mkdir -p /sdcard/Android/data/com.companion.chat/files/models/image/sd15-hypersd'
adb push third_party/models/image/sd15-hypersd/. /sdcard/Android/data/com.companion.chat/files/models/image/sd15-hypersd/
```

- DreamLite source is tracked as a Git submodule at `third_party/DreamLite`; model files are expected under `/sdcard/Android/data/com.companion.chat/files/models/image/dreamlite` and are not committed. The Android path currently checks the DreamLite model package and returns a clear runtime-not-connected error until real on-device inference is wired in.
- OpenMOSS reader/runtime reference code is tracked as a Git submodule at `third_party/MOSS-TTS-Nano-Reader`; Android integration keeps model files under `third_party/models/tts/moss-tts-nano/` as local cache only.

### 9. More open private interaction

- Built for private local use rather than public-platform conversation norms
- Suitable for flirtatious, emotionally expressive, and more intimate interaction styles when the selected local model and prompts support it
- Keeps those interaction boundaries closer to the user instead of outsourcing them to a remote service

## Current Status

The project currently includes:

- Room-backed conversations, messages, memories, preferences, role cards, and skills
- Context compression, recent-message replay, memory retrieval, and confirmed preference prompt injection
- Role-card identity, skill prompts, discover role import, and fresh role-specific chat sessions
- Local SenseVoice ASR, Android system TTS, HTTP voice clone, and local MOSS voice clone fallback
- LiteRT-LM CPU/GPU/NPU backend selection with accelerator fallback and benchmark logging
- llama.cpp GGUF runtime with image prompt support through a configured projector model
- HTTP image generation, DreamLite package status checks, and local SD1.5 Hyper-SD generation through `stable-diffusion.cpp`
- Material 3 screens for chat, discover, memory, model settings, voice settings, role cards, and skills

The current build has passed:

- targeted unit tests
- full unit test run
- `assembleDebug`
- device install + model push + manual functional validation
- UI refresh verification with `:app:compileDebugKotlin`, `:app:assembleDebug`, device install, and manual screenshot checks across the four main tabs
- chat hardening verification with `:app:compileDebugKotlin`, `:app:assembleDebug`, targeted RoleAware voice output tests, targeted MOSS model-package tests, device install, and device MOSS file presence checks

## Build Notes

- Android Gradle Plugin is managed through `gradle/libs.versions.toml` and is currently set to `8.6.0`
- The Gradle wrapper uses Gradle `8.7`, matching the AGP 8.6.x compatibility requirements
- The app targets `compileSdk = 35`

## Business And Market

This project has market potential not because "AI chat" is novel by itself, but because products that truly satisfy private companionship needs are still rare. Most current products run into the same limits:

- cloud voice assistants feel useful, but not companion-like
- ordinary AI chat products do not preserve long-term memory or relationship growth
- many character products have style, but not durable relational continuity
- privacy-sensitive interaction is often constrained by cloud moderation, network dependency, and platform rules
- phone-only experiences still lack immersion, privacy, and emotional presence

Anime Companion is not trying to sell "an AI that can answer questions." It is trying to sell a **long-term, private, immersive, increasingly personalized companionship experience**. That value proposition naturally fits several user groups:

- anime and character-companion users
- privacy-conscious personal AI users
- people with strong companionship needs during solo time, commuting, or late-night use
- users who care about voice interaction, relationship growth, and personalized character dynamics
- early adopters willing to pay for a new kind of wearable AI experience

From a commercialization perspective, the project can be sold in multiple layers:

- Software tier: a local AI private companion app that validates engagement, retention, and high-frequency use cases
- Hardware tier: a premium smart helmet / wearable companion device that amplifies immersion, privacy, and product differentiation
- Premium tier: monetization through role packs, curated skills, character settings, enhanced memory features, and stronger local model capabilities

From an investor-style perspective, the larger opportunity is that this is not just another chat app. It is an attempt to define a new category: a **private, edge-native, relationship-oriented, wearable AI companion**. If successful, it opens room for software subscriptions, hardware sales, character ecosystems, content ecosystems, and long-term relationship products built on top of local AI.

## Tech Stack

- Kotlin
- Android SDK
- Jetpack Compose
- Navigation Compose
- Room + KSP
- LiteRT-LM Android
- sherpa-onnx
- ONNX Runtime Android
- llama.cpp
- stable-diffusion.cpp
- Android TextToSpeech
- Coil

## Project Structure

```text
app/
  src/main/java/com/companion/chat/
    data/
      context/        # context window, prompt assembly, summary flow
      discover/       # discover role seeds plus favorite/unlock/import state
      image/          # image generation config, provider routing, local placeholder
      local/          # Room database, dao, entities
      memory/         # memory extraction, retrieval, storage
      preferences/    # preference extraction and merge flow
      repository/     # chat session persistence
      role/           # role card repository and prompt builder
      skill/          # skill repository
      voice/          # ASR/TTS/voice clone configuration and fallback selection
    engine/           # LiteRT-LM and voice engine implementations
    ui/
      chat/           # chat screen, ChatViewModel, quiet input panel, message components
      memory/         # memory management UI with filters, lightweight tags, and compact actions
      settings/       # settings, role management, skills management, grouped Material rows
docs/plans/          # design and implementation documents
docs/progress/jindu.md             # progress log
```

## Runtime Requirements

To build and run this project, you need:

### Development environment

- JDK 17
- Android Studio / Android SDK
- Gradle wrapper included in this repository
- ADB for install and device operations
- `ninja` on `PATH` for the Vulkan image backend shader generator. Android SDK CMake includes one, for example:
  `PATH=$ANDROID_HOME/cmake/3.22.1/bin:$PATH ./gradlew :app:assembleDebug`

### Android requirements

- Android device or emulator with `minSdk 28+`
- Enough storage for the model file
- Enough memory for on-device inference

### Model requirement

This repository does **not** include the LLM model file.

You need to prepare a `.litertlm` model manually and push it to:

```text
/sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm
```

## Build And Run

### 1. Build the debug APK

From the project root:

```powershell
.\gradlew.bat :app:assembleDebug
```

If your environment does not already point to JDK 17, set `JAVA_HOME` to your local JDK 17 installation before building.

### 2. Install to device

```powershell
adb uninstall com.companion.chat
adb push .\app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/companionchat.apk
adb shell pm install -r -t --user 0 /data/local/tmp/companionchat.apk
```

### 3. Launch once to create app directories

```powershell
adb shell am start -n com.companion.chat/.MainActivity
```

### 4. Push the model

```powershell
adb push <path-to-model>\gemma-4-E2B-it.litertlm /sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm
```

### 5. Restart and verify

After the model is pushed, restart the app and check runtime logs if needed.

Example log check:

```powershell
adb shell run-as com.companion.chat cat files/viewmodel_log.txt
```

You should see the model path detected and the inference engine entering `Ready`.

## Notes For Device Testing

- On a fresh install, the app may start once before the model is pushed, so the first initialization can report that the model file is missing.
- After pushing the model, restart the app and check logs again.
- If you reinstall the app, you usually need to push the model again.

## Documentation

- Chinese README: [README_CN.md](./README_CN.md)
- Progress log: [docs/progress/jindu.md](./docs/progress/jindu.md)
- Design and implementation docs: [`docs/plans/`](./docs/plans/)

## Repository Purpose

This repository is mainly for:

- demonstrating an on-device AI companion workflow on Android
- experimenting with memory and preference-aware local AI interaction
- validating role-card and skill-based prompt composition on mobile
