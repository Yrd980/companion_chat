# Android Dev Scripts

Windows entrypoint:

```powershell
scripts\android-dev.bat build
```

PowerShell entrypoint:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android-dev.ps1 build
```

## Tasks

- `doctor`: checks Git, JDK, Android SDK, ADB, Gradle wrapper, and `ninja`
- `bootstrap`: runs `doctor`, then initializes all Git submodules recursively
- `compile`: runs `:app:compileDebugKotlin`
- `build`: runs `:app:assembleDebug` and prints the APK path
- `time-build`: runs `build` and prints elapsed seconds
- `install`: pushes the debug APK to `/data/local/tmp/companionchat.apk`, then installs it with `pm install -r -t --user 0`
- `download`: alias for `install`
- `run`: starts `com.companion.chat/.MainActivity`
- `restart`: force-stops and starts `com.companion.chat/.MainActivity`
- `deploy`: build, install, then run
- `fresh`: bootstrap, build, install, then run
- `model`: pushes a `.litertlm` model to the app model path
- `asr`: pushes the local SenseVoice ASR model package from `third_party\models\asr\sensevoice`
- `image`: pushes the local SD1.5 Hyper-SD image model package from `third_party\models\image\sd15-hypersd`
- `logs`: prints `files/viewmodel_log.txt` through `run-as`
- `smoke`: clears logcat, restarts the app, waits briefly, then prints logcat
- `devices`: runs `adb devices -l`

## Examples

Build only:

```powershell
scripts\android-dev.bat build
```

Fresh clone setup, build, install, and launch:

```powershell
scripts\android-dev.bat fresh
```

Build and install to the connected device:

```powershell
scripts\android-dev.bat download
```

Build, install, and launch:

```powershell
scripts\android-dev.bat deploy
```

Push the required local model:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android-dev.ps1 model -ModelPath C:\models\gemma-4-E2B-it.litertlm
```

Push local ASR and image model packages:

```powershell
scripts\android-dev.bat asr
scripts\android-dev.bat image
```

Target a specific device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android-dev.ps1 deploy -Serial emulator-5554
```

The debug APK is expected at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

The local LLM model is expected on device at:

```text
/sdcard/Android/data/com.companion.chat/files/models/gemma-4-E2B-it.litertlm
```

The ASR and image package tasks expect local cache directories at:

```text
third_party\models\asr\sensevoice
third_party\models\image\sd15-hypersd
```

The repository uses Git submodules under `third_party/`. `fresh` and
`bootstrap` run:

```powershell
git submodule sync --recursive
git submodule update --init --recursive
```
