# Agent Notes

## Project Intent

Anime Companion is a local-first Android AI companion app. Preserve privacy,
durable memory, role identity, voice-first chat, and explicit user-controlled
model/backend settings. Large model files are not committed.

## Commands

- Check environment: `scripts\android-dev.bat doctor`
- Compile Kotlin: `scripts\android-dev.bat compile`
- Build debug APK: `scripts\android-dev.bat build`
- Install debug APK: `scripts\android-dev.bat download`
- Build/install/run: `scripts\android-dev.bat deploy`
- Full bootstrap/build/install/run: `scripts\android-dev.bat fresh`
- Launch app only: `scripts\android-dev.bat run`
- Restart app: `scripts\android-dev.bat restart`
- Logs: `scripts\android-dev.bat logs`
- Smoke logs: `scripts\android-dev.bat smoke`
- Devices: `scripts\android-dev.bat devices`

Target a device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android-dev.ps1 deploy -Serial emulator-5554
```

## UI Test Emulator

Use `Vivo_X100_UI` for lightweight UI testing.

- AVD: `Vivo_X100_UI`
- Size: `1080x2400`
- Density: `420`
- RAM: `1536`
- CPU: `2`
- Image: `system-images;android-35;default;x86_64`

Start it:

```powershell
C:\Users\Yrd98\scoop\apps\android-clt\current\emulator\emulator.exe -avd Vivo_X100_UI -scale 0.35 -no-snapshot-load -no-snapshot-save -gpu swiftshader_indirect
```

If the emulator window is off-screen, move it with Win32 `SetWindowPos`; do not
recreate the AVD just for window placement.

## Android UI Automation

Prefer ADB over desktop clicking for emulator UI tests.

```powershell
$adb = "C:\Users\Yrd98\scoop\apps\android-clt\current\platform-tools\adb.exe"
& $adb shell wm size
& $adb shell wm density
& $adb shell uiautomator dump /sdcard/window.xml
& $adb shell cat /sdcard/window.xml
& $adb shell input tap 1000 210
& $adb shell input text hello
& $adb shell input keyevent KEYCODE_BACK
& $adb shell input swipe 500 1800 500 500
```

Use desktop automation only for emulator window management or screenshots.

## Model Packages

- LLM on device:
  `/sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm`
- Push LLM:
  `powershell -ExecutionPolicy Bypass -File scripts\android-dev.ps1 model -ModelPath <path>`
- ASR local cache:
  `third_party\models\asr\sensevoice`
- Push ASR:
  `scripts\android-dev.bat asr`
- Image local cache:
  `third_party\models\image\sd15-hypersd`
- Push image package:
  `scripts\android-dev.bat image`

## Key Paths

- App entry/navigation: `app/src/main/java/com/companion/chat/MainActivity.kt`
- Dependency container: `app/src/main/java/com/companion/chat/AppContainer.kt`
- Chat UI/ViewModel: `app/src/main/java/com/companion/chat/ui/chat/`
- Companion Turn: `app/src/main/java/com/companion/chat/companion/turn/`
- Model runtime/engines: `app/src/main/java/com/companion/chat/engine/`
- Durable Memory: `app/src/main/java/com/companion/chat/data/memory/`
- Memory extraction/retrieval: `app/src/main/java/com/companion/chat/memory/`
- Preference learning: `app/src/main/java/com/companion/chat/preference/`
- Role cards: `app/src/main/java/com/companion/chat/identity/`
- Room database/DAO/entities: `app/src/main/java/com/companion/chat/data/local/`
- Settings UI: `app/src/main/java/com/companion/chat/ui/settings/`
- Dev script docs: `docs/android-dev-scripts.md`

## Constraints

- Keep changes small and traceable.
- Prefer existing repositories, engines, ViewModels, and Compose patterns.
- Do not add remote dependencies unless the user asks.
- Do not commit model files or generated build output.
- For UI-only emulator testing, do not push/run large model packages.
