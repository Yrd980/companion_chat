param(
    [ValidateSet("doctor", "bootstrap", "compile", "build", "time-build", "install", "download", "run", "restart", "deploy", "fresh", "model", "asr", "image", "logs", "smoke", "devices")]
    [string]$Task = "build",

    [string]$Serial = $env:ANDROID_SERIAL,

    [string]$PackageName = "com.companion.chat",

    [string]$Activity = ".MainActivity",

    [string]$ModelPath = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Resolve-Path (Join-Path $ScriptDir "..")).Path
$Gradle = Join-Path $RootDir "gradlew.bat"
$ApkPath = Join-Path $RootDir "app\build\outputs\apk\debug\app-debug.apk"
$RemoteModelPath = "/sdcard/Android/data/$PackageName/files/models/gemma-4-E2B-it.litertlm"
$SenseVoiceModelDir = Join-Path $RootDir "third_party\models\asr\sensevoice"
$ImageModelDir = Join-Path $RootDir "third_party\models\image\sd15-hypersd"

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$File,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    Write-Host "> $File $($Arguments -join ' ')"
    & $File @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $File $($Arguments -join ' ')"
    }
}

function Get-AdbArguments {
    param([string[]]$Arguments)

    if ([string]::IsNullOrWhiteSpace($Serial)) {
        return $Arguments
    }

    return @("-s", $Serial) + $Arguments
}

function Invoke-Gradle {
    param([string]$GradleTask)

    if (-not (Test-Path -LiteralPath $Gradle)) {
        throw "Gradle wrapper not found: $Gradle"
    }

    Invoke-Checked -File $Gradle -Arguments @("-p", $RootDir, $GradleTask)
}

function Invoke-Adb {
    param([string[]]$Arguments)

    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -eq $adb) {
        throw "adb was not found on PATH. Install Android platform-tools or add adb to PATH."
    }

    Invoke-Checked -File $adb.Source -Arguments (Get-AdbArguments $Arguments)
}

function Assert-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [string]$InstallHint = ""
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        if ([string]::IsNullOrWhiteSpace($InstallHint)) {
            throw "$Name was not found on PATH."
        }
        throw "$Name was not found on PATH. $InstallHint"
    }
    Write-Host "${Name}: $($command.Source)"
}

function Test-AndroidEnvironment {
    Assert-Command "git" "Install Git and retry."
    Assert-Command "adb" "Install Android platform-tools or add adb to PATH."

    if (-not (Test-Path -LiteralPath $Gradle)) {
        throw "Gradle wrapper not found: $Gradle"
    }
    Write-Host "gradle wrapper: $Gradle"

    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $java) {
        throw "java was not found on PATH. Install JDK 17 and set JAVA_HOME/PATH."
    }
    Write-Host "java: $($java.Source)"
    & $java.Source -version
    if ($LASTEXITCODE -ne 0) {
        throw "java -version failed."
    }

    if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME) -and [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        throw "ANDROID_HOME or ANDROID_SDK_ROOT is not set. Install Android SDK and set one of them."
    }
    $androidSdk = if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }
    if (-not (Test-Path -LiteralPath $androidSdk)) {
        throw "Android SDK path does not exist: $androidSdk"
    }
    Write-Host "android sdk: $androidSdk"

    $ninja = Get-Command ninja -ErrorAction SilentlyContinue
    if ($null -eq $ninja) {
        $sdkNinja = Get-ChildItem -Path (Join-Path $androidSdk "cmake") -Recurse -Filter "ninja.exe" -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -eq $sdkNinja) {
            throw "ninja was not found on PATH or under Android SDK CMake. Install Android SDK CMake 3.22.1 or add ninja to PATH."
        }
        Write-Host "ninja: $($sdkNinja.FullName)"
    } else {
        Write-Host "ninja: $($ninja.Source)"
    }
}

function Initialize-Repository {
    Test-AndroidEnvironment

    Invoke-Checked -File "git" -Arguments @("-C", $RootDir, "submodule", "sync", "--recursive")
    Invoke-Checked -File "git" -Arguments @("-C", $RootDir, "submodule", "update", "--init", "--recursive")

    $submoduleStatus = & git -C $RootDir submodule status --recursive
    if ($LASTEXITCODE -ne 0) {
        throw "git submodule status failed."
    }
    $notReady = $submoduleStatus | Where-Object { $_ -match "^[+-]" }
    if ($notReady) {
        $joined = $notReady -join "`n"
        throw "Some submodules are not initialized or are out of sync:`n$joined"
    }
    Write-Host "submodules: ready"
}

function Build-Apk {
    Invoke-Gradle ":app:assembleDebug"
    if (-not (Test-Path -LiteralPath $ApkPath)) {
        throw "APK was not produced at expected path: $ApkPath"
    }
    Write-Host "APK: $ApkPath"
}

function Measure-Build {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    Build-Apk
    $sw.Stop()
    Write-Host ("BUILD_SECONDS={0:N3}" -f $sw.Elapsed.TotalSeconds)
}

function Install-Apk {
    if (-not (Test-Path -LiteralPath $ApkPath)) {
        Build-Apk
    }

    $remoteApk = "/data/local/tmp/companionchat.apk"
    Invoke-Adb @("push", $ApkPath, $remoteApk)
    Invoke-Adb @("shell", "pm", "install", "-r", "-t", "--user", "0", $remoteApk)
}

function Run-App {
    Invoke-Adb @("shell", "am", "start", "-n", "$PackageName/$Activity")
}

function Restart-App {
    Invoke-Adb @("shell", "am", "force-stop", $PackageName)
    Run-App
}

function Push-Model {
    if ([string]::IsNullOrWhiteSpace($ModelPath)) {
        throw "ModelPath is required. Example: scripts\android-dev.bat model -ModelPath C:\models\gemma-4-E2B-it.litertlm"
    }
    $resolvedModel = (Resolve-Path -LiteralPath $ModelPath).Path
    Invoke-Adb @("shell", "mkdir", "-p", "/sdcard/Android/data/$PackageName/files/models")
    Invoke-Adb @("push", $resolvedModel, $RemoteModelPath)
}

function Push-SenseVoiceModels {
    $files = @("model.int8.onnx", "tokens.txt", "silero_vad.onnx")
    Invoke-Adb @("shell", "mkdir", "-p", "/sdcard/Android/data/$PackageName/files/models/asr/sensevoice")
    foreach ($file in $files) {
        $path = Join-Path $SenseVoiceModelDir $file
        if (-not (Test-Path -LiteralPath $path)) {
            throw "Missing SenseVoice model file: $path"
        }
        Invoke-Adb @("push", $path, "/sdcard/Android/data/$PackageName/files/models/asr/sensevoice/$file")
    }
}

function Push-ImageModels {
    if (-not (Test-Path -LiteralPath $ImageModelDir)) {
        throw "Missing image model directory: $ImageModelDir"
    }
    Invoke-Adb @("shell", "mkdir", "-p", "/sdcard/Android/data/$PackageName/files/models/image/sd15-hypersd")
    Invoke-Adb @("push", (Join-Path $ImageModelDir "."), "/sdcard/Android/data/$PackageName/files/models/image/sd15-hypersd/")
}

function Invoke-SmokeCheck {
    Invoke-Adb @("logcat", "-c")
    Restart-App
    Start-Sleep -Seconds 8
    Invoke-Adb @("logcat", "-d", "-v", "time")
}

switch ($Task) {
    "doctor" {
        Test-AndroidEnvironment
    }
    "bootstrap" {
        Initialize-Repository
    }
    "compile" {
        Invoke-Gradle ":app:compileDebugKotlin"
    }
    "build" {
        Build-Apk
    }
    "time-build" {
        Measure-Build
    }
    "install" {
        Install-Apk
    }
    "download" {
        Install-Apk
    }
    "run" {
        Run-App
    }
    "restart" {
        Restart-App
    }
    "deploy" {
        Build-Apk
        Install-Apk
        Run-App
    }
    "fresh" {
        Initialize-Repository
        Build-Apk
        Install-Apk
        Run-App
    }
    "model" {
        Push-Model
    }
    "asr" {
        Push-SenseVoiceModels
    }
    "image" {
        Push-ImageModels
    }
    "logs" {
        Invoke-Adb @("shell", "run-as", $PackageName, "cat", "files/viewmodel_log.txt")
    }
    "smoke" {
        Invoke-SmokeCheck
    }
    "devices" {
        Invoke-Adb @("devices", "-l")
    }
}
