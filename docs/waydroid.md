# Waydroid local runbook

This project can run in an x86_64 Waydroid container after building the debug
APK. The current stable setup uses Waydroid full UI mode instead of multi-window
mode, because multi-window repeatedly caused the container to freeze or the app
window to disappear on KDE Wayland.

## Build

```bash
./gradlew :app:assembleDebug
```

The app module currently packages both Android native ABIs:

```text
arm64-v8a
x86_64
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run

Use the helper:

```bash
WAYDROID_ROOT_PASSWORD=yrd scripts/waydroid-companion.sh
```

If root credentials are already cached, this also works:

```bash
scripts/waydroid-companion.sh
```

The script:

- builds the debug APK when it is missing;
- applies the stable Waydroid display properties;
- installs the host ADB public key into Waydroid's `adb_keys`;
- starts the Waydroid session if needed;
- connects ADB to the Waydroid IP;
- installs the debug APK when needed;
- grants `android.permission.RECORD_AUDIO`;
- installs and selects the eSpeak system TTS engine when no usable engine is
  present;
- hard-links the required local model files into the app's external files
  directory;
- opens the full Waydroid UI and launches `com.companion.chat`.

## Stable Waydroid display settings

The helper applies these properties for the current 1920x1080 KDE Wayland
desktop:

```bash
waydroid prop set persist.waydroid.suspend false
waydroid prop set persist.waydroid.multi_windows false
waydroid prop set persist.waydroid.width 378
waydroid prop set persist.waydroid.height 837
waydroid prop set persist.waydroid.dpi 320
```

Notes:

- `persist.waydroid.suspend=false` avoids Waydroid freezing the container when
  it thinks no app window is visible.
- `persist.waydroid.multi_windows=false` keeps the full Waydroid UI path, which
  has been more stable than single-app multi-window mode.
- Avoid hand-written KWin rules that force Waydroid size and position. A forced
  KWin `position + size` rule made the Waydroid window layer unstable.

## ADB authorization

Waydroid may expose ADB as `unauthorized` even after `waydroid adb connect`.
The stable fix is to copy the host ADB public key into:

```text
~/.local/share/waydroid/data/misc/adb/adb_keys
```

The helper does this automatically from:

```text
~/.android/adbkey.pub
```

Expected verification:

```bash
adb devices -l
```

```text
192.168.240.112:5555   device product:lineage_waydroid_x86_64 ...
```

## Model files

The app reads external model files from Android's app-specific external storage:

```text
/sdcard/Android/data/com.companion.chat/files/models
```

In this Waydroid setup, that maps to host storage under:

```text
~/.local/share/waydroid/data/media/0/Android/data/com.companion.chat/files/models
```

Do not symlink that directory to the repo path. A symlink such as:

```text
/sdcard/.../models -> /home/yrd/projects/companion_chat/third_party/models
```

is visible as a broken link inside Android, because the container cannot resolve
the host `/home/yrd/...` path.

The helper uses hard links instead. This makes Android see normal files while
avoiding an extra multi-GB copy. It currently links the required startup set:

```text
models/Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf
models/mmproj-Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-f16.gguf
models/asr/sensevoice/model.int8.onnx
models/asr/sensevoice/silero_vad.onnx
models/asr/sensevoice/tokens.txt
```

After launch, logcat should include:

```text
LlamaCppEngine: 模型文件存在: true
LlamaCppEngine: 模型可读: true
LlamaCppEngine: === llama.cpp 引擎初始化完成，状态: Ready ===
VoiceInputEngine: local SenseVoice model ready
```

## System TTS

The stock Waydroid image has no usable system TTS engine:

```bash
adb shell cmd package query-services --brief -a android.intent.action.TTS_SERVICE
adb shell settings get secure tts_default_synth
```

Expected stock output:

```text
No services found
null
```

If no engine is installed, Android `TextToSpeech` reports:

```text
VoiceOutputEngine: TTS 初始化失败: -1
```

The helper installs the F-Droid eSpeak APK and selects it as the default engine:

```text
com.reecedunn.espeak/.TtsService
```

The app also declares a package visibility query for
`android.intent.action.TTS_SERVICE`. Without that manifest query, Android 13's
package visibility filter blocks the app from seeing the external TTS engine,
even when the engine is installed.

Expected verification after installing eSpeak and reinstalling the app:

```text
TextToSpeech: Sucessfully bound to com.reecedunn.espeak
VoiceOutputEngine: TTS 初始化成功
```

eSpeak may still log missing voice data until its own voice package is
initialized, but it is enough to prove that Android's system TTS binding path is
working. The app's MOSS local voice clone path is separate from Android's system
TTS fallback and still depends on the MOSS model package plus role voice
configuration.

## Debug commands

Launch and capture startup logs:

```bash
adb logcat -c
adb shell am force-stop com.companion.chat
adb shell am start -n com.companion.chat/.MainActivity
sleep 8
adb logcat -d -v time | rg "LlamaCppEngine|VoiceInputEngine|VoiceOutputEngine|FATAL EXCEPTION|AndroidRuntime|ANR"
```

Check foreground activity:

```bash
adb shell dumpsys activity activities | rg "mResumedActivity|mFocusedApp|com.companion.chat"
```

Dump the visible UI tree:

```bash
adb shell uiautomator dump /sdcard/window.xml
adb shell cat /sdcard/window.xml
```

## Recovery

If Waydroid gets stuck in `FROZEN` or `STOPPED`, restart the container once:

```bash
waydroid session stop
su -s /bin/sh -c 'systemctl restart waydroid-container'
WAYDROID_ROOT_PASSWORD=yrd scripts/waydroid-companion.sh
```
