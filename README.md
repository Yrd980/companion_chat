# CompanionChat

CompanionChat, also called Anime Companion, is a local-first Android AI
companion app. It explores a companion experience built around character
identity, long-term memory, voice-first interaction, and user-controlled model
boundaries.

This repository is the Android software side of the product direction. It is
not a cloud assistant wrapper and it does not commit large model files.

## Product Direction

Anime Companion is aimed at private, durable AI companionship:

- local model execution where practical
- persistent role identity
- long-term memory and preference learning
- voice-first chat with text fallback
- explicit local/cloud backend choices
- future wearable companion use, with the phone as the local intelligence hub

The product should feel like a continuing companion, not a disposable chat
window or a generic task bot.

## What Exists

- Compose Android app with Home, Chat, Memory, Helmet diagnostics, and Settings
- Companion Turn flow for accepted/rejected turns, streaming output, final
  commit, voice playback, timeline events, memory refresh, and preference
  learning
- Room-backed conversations, messages, memories, preferences, role cards,
  skills, and timeline events
- Durable Memory review, pinned/confirmed memory projections, and one-turn
  memory injection
- Role Card and Skill systems
- LiteRT-LM and llama.cpp runtime adapters
- Local/remote voice and image generation provider configuration
- Privacy Gate checks before cloud ASR, HTTP voice clone, and HTTP image
  generation

## Repository Map

```text
app/src/main/java/com/companion/chat/
  MainActivity.kt          app entry and navigation
  AppContainer.kt          dependency container
  companion/turn/          Companion Turn transaction flow
  engine/                  model runtime, ASR/TTS, image generation
  data/local/              Room database, DAOs, entities
  data/memory/             Durable Memory module
  memory/                  memory extraction and retrieval
  preference/              Preference Learning helpers
  identity/                Role Card repository and prompts
  ui/chat/                 Chat UI and ChatViewModel
  ui/home/                 Home dashboard and discover catalog
  ui/memory/               Memory and relationship UI
  ui/settings/             profile, model, voice, role, skill settings
docs/                      product, architecture, and developer notes
scripts/                   Android build/deploy helpers
third_party/               native/model runtime source dependencies
```

## Runtime Shape

```text
Chat UI
-> ChatViewModel
-> CompanionTurnModule
-> ModelRuntimeLifecycle + InferenceEngine
-> session persistence
-> voice playback / timeline events / Durable Memory refresh / Preference Learning
```

## Build And Run

Developer operations are kept in [AGENTS.md](AGENTS.md) and
[docs/android-dev-scripts.md](docs/android-dev-scripts.md).

Common entrypoints:

```powershell
scripts\android-dev.bat doctor
scripts\android-dev.bat build
scripts\android-dev.bat deploy
scripts\android-dev.bat logs
```

The repository does not include LLM, ASR, TTS, or image model weights. Runtime
features should validate missing model packages and fail clearly instead of
crashing.

## Tech Stack

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

## Docs

- [AGENTS.md](AGENTS.md): concise agent/developer operating notes
- [README_CN.md](README_CN.md): Chinese README
- [docs/android-dev-scripts.md](docs/android-dev-scripts.md): Android scripts
- [docs/architecture-review-2026-06-11.md](docs/architecture-review-2026-06-11.md): architecture review
- [docs/product-ui-ux.md](docs/product-ui-ux.md): product/UI direction
- [docs/frontend-backend-gaps.md](docs/frontend-backend-gaps.md): implementation gaps
- [docs/waydroid.md](docs/waydroid.md): Waydroid notes
