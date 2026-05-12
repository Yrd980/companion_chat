# GGUF llama.cpp Runtime

The app now uses a CPU-only llama.cpp runtime for text chat. The first supported ABI is `arm64-v8a`.

## Repository Setup

Initialize the pinned llama.cpp submodule:

```bash
git submodule update --init --recursive third_party/llama.cpp
git -C third_party/llama.cpp checkout 1ec7ba0c14f33f17e980daeeda5f35b225d41994
```

## Model File

GGUF and LiteRT-LM model files are intentionally ignored by Git and should not be packaged into the APK.

Place the model on the device:

```bash
adb shell mkdir -p /sdcard/Android/data/com.companion.chat/files/models
adb push Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf /sdcard/Android/data/com.companion.chat/files/models/
```

Default runtime path:

```text
/sdcard/Android/data/com.companion.chat/files/models/Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf
```

## Build

```bash
cd CompanionChat
./gradlew :app:assembleDebug
```

The native build is configured through `app/src/main/cpp/CMakeLists.txt` and links llama.cpp as CPU-only with `n_gpu_layers=0`.

Default runtime settings favor shorter, faster responses:

```text
contextSize=2048
maxTokens=512
temperature=0.7
topK=40
topP=0.95
recentPromptMessages=6
```

## Diagnostics

Runtime logs are written to Android logcat under `LlamaCppEngine` and `CompanionLlamaJNI`. App diagnostic files include:

```text
llama_engine_log.txt
viewmodel_log.txt
```

Generation logs include prompt token count, prompt decode time, first token latency, generated token count, and tokens per second.

Images are not sent to the GGUF runtime in this first text-only version; image messages return a clear unsupported-input response.
