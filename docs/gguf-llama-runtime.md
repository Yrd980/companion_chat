# GGUF llama.cpp Runtime

The root `app/` module is the canonical Android app. It can use a CPU-only llama.cpp runtime for GGUF text chat, with LiteRT-LM kept as an optional backend. The first supported native ABI is `arm64-v8a`.

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

Default GGUF runtime path:

```text
/sdcard/Android/data/com.companion.chat/files/models/Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf
```

Optional LiteRT-LM runtime path:

```text
/sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm
```

If an old checkout still has `CompanionChat/app/src/main/assets/models/*.gguf`, move it to a local ignored cache such as `third_party/models/gguf/` and push it to the device from there. Do not keep GGUF files under Android assets.

## Build

```bash
./gradlew :app:assembleDebug
```

The native build is configured through root `app/src/main/cpp/CMakeLists.txt` and links `third_party/llama.cpp` as CPU-only with `n_gpu_layers=0`.

Default runtime settings favor shorter, faster responses:

```text
contextSize=2048
maxTokens=256
temperature=0.7
topK=40
topP=0.95
recentPromptMessages=6
```

The GGUF runtime clamps each response to the remaining context window before decoding. If `llama_decode` returns a non-zero status during token generation, the runtime logs the status and ends the current response instead of surfacing a hard chat error. Generated Gemma turn markers such as `<end_of_turn>` and `<start_of_turn>` are treated as stop markers and are filtered from the chat UI.

## Diagnostics

Runtime logs are written to Android logcat under `LlamaCppEngine` and `CompanionLlamaJNI`. App diagnostic files include:

```text
llama_engine_log.txt
viewmodel_log.txt
```

Generation logs include prompt token count, prompt decode time, first token latency, generated token count, and tokens per second.

Images are not sent to the GGUF runtime in this first text-only version; image messages return a clear unsupported-input response.
