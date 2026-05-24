#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.companion.chat"
MODEL_SRC="$ROOT_DIR/third_party/models"
WAYDROID_FILES="$HOME/.local/share/waydroid/data/media/0/Android/data/$PACKAGE_NAME/files"
WAYDROID_MODELS="$WAYDROID_FILES/models"
ADB_KEYS_DIR="$HOME/.local/share/waydroid/data/misc/adb"
ADB_KEYS_FILE="$ADB_KEYS_DIR/adb_keys"
ADB_SERIAL=""
ESPEAK_PACKAGE="com.reecedunn.espeak"
ESPEAK_APK_URL="${ESPEAK_APK_URL:-https://f-droid.org/repo/com.reecedunn.espeak_22.apk}"
ESPEAK_APK_CACHE="${ESPEAK_APK_CACHE:-/tmp/companion-tts/espeak.apk}"

WIDTH="${WAYDROID_WIDTH:-378}"
HEIGHT="${WAYDROID_HEIGHT:-837}"
DPI="${WAYDROID_DPI:-320}"
WAYDROID_IP="${WAYDROID_IP:-}"
INSTALL_ESPEAK_TTS="${INSTALL_ESPEAK_TTS:-true}"

log() {
  printf '[waydroid-companion] %s\n' "$*"
}

have_sudo() {
  sudo -n true >/dev/null 2>&1
}

root_shell() {
  local script="$1"

  if [[ "$(id -u)" == "0" ]]; then
    sh -c "$script"
    return
  fi

  if have_sudo; then
    sudo sh -c "$script"
    return
  fi

  if [[ -n "${WAYDROID_ROOT_PASSWORD:-}" ]]; then
    printf '%s\n' "$WAYDROID_ROOT_PASSWORD" | su -s /bin/sh -c "$script"
    return
  fi

  su -s /bin/sh -c "$script"
}

ensure_apk() {
  if [[ -f "$APK_PATH" ]]; then
    return
  fi

  log "APK missing; building debug APK"
  "$ROOT_DIR/gradlew" -p "$ROOT_DIR" :app:assembleDebug
}

configure_props() {
  log "configuring Waydroid display: ${WIDTH}x${HEIGHT}, dpi ${DPI}"
  waydroid prop set persist.waydroid.suspend false
  waydroid prop set persist.waydroid.multi_windows false
  waydroid prop set persist.waydroid.width "$WIDTH"
  waydroid prop set persist.waydroid.height "$HEIGHT"
  waydroid prop set persist.waydroid.dpi "$DPI"
}

ensure_model_link() {
  if [[ ! -d "$MODEL_SRC" ]]; then
    log "model cache not found: $MODEL_SRC"
    return
  fi

  if root_shell "
    test -f '$WAYDROID_MODELS/Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf' &&
    test -f '$WAYDROID_MODELS/mmproj-Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-f16.gguf' &&
    test -f '$WAYDROID_MODELS/asr/sensevoice/model.int8.onnx' &&
    test -f '$WAYDROID_MODELS/image/sd15-hypersd/sd_config.json'
  " >/dev/null 2>&1; then
    return
  fi

  log "hard-linking required models into Waydroid app storage"
  root_shell "
    set -e
    dst='$WAYDROID_MODELS'
    src='$MODEL_SRC'
    rm -rf \"\$dst\"
    mkdir -p \"\$dst/asr/sensevoice\"
    mkdir -p \"\$dst/image/sd15-hypersd\"
    ln \"\$src/gguf/Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf\" \"\$dst/Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-Q4_K_P.gguf\"
    ln \"\$src/gguf/mmproj-Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-f16.gguf\" \"\$dst/mmproj-Gemma-4-E2B-Uncensored-HauhauCS-Aggressive-f16.gguf\"
    ln \"\$src/asr/sensevoice/model.int8.onnx\" \"\$dst/asr/sensevoice/model.int8.onnx\"
    ln \"\$src/asr/sensevoice/silero_vad.onnx\" \"\$dst/asr/sensevoice/silero_vad.onnx\"
    ln \"\$src/asr/sensevoice/tokens.txt\" \"\$dst/asr/sensevoice/tokens.txt\"
    image_src=\"\$src/image/sd15-hypersd-fast\"
    if [ ! -f \"\$image_src/sd_config.json\" ]; then
      image_src=\"\$src/image/sd15-hypersd\"
    fi
    if [ -f \"\$image_src/sd_config.json\" ]; then
      find \"\$image_src\" -maxdepth 1 -type f -exec ln {} \"\$dst/image/sd15-hypersd/\" \\;
    fi
    chown -R 1023:1023 \"\$dst\"
  "
}

ensure_adb_auth() {
  if [[ ! -f "$HOME/.android/adbkey.pub" ]]; then
    log "host adb public key not found; run adb once to create ~/.android/adbkey.pub"
    return
  fi

  if [[ -f "$ADB_KEYS_FILE" ]] && cmp -s "$HOME/.android/adbkey.pub" "$ADB_KEYS_FILE"; then
    return
  fi

  if mkdir -p "$ADB_KEYS_DIR" 2>/dev/null; then
    install -m 0640 "$HOME/.android/adbkey.pub" "$ADB_KEYS_FILE"
    chgrp 2000 "$ADB_KEYS_FILE" 2>/dev/null || true
    log "installed host adb key into Waydroid adb_keys"
  else
    log "installing host adb key into Waydroid adb_keys with root"
    root_shell "
      set -e
      mkdir -p '$ADB_KEYS_DIR'
      cp '$HOME/.android/adbkey.pub' '$ADB_KEYS_FILE'
      chmod 0640 '$ADB_KEYS_FILE'
      chown 1000:2000 '$ADB_KEYS_FILE'
    "
  fi
}

start_session() {
  local status
  status="$(waydroid status 2>/dev/null || true)"

  if ! grep -q 'Session:[[:space:]]*RUNNING' <<<"$status"; then
    log "starting Waydroid session"
    waydroid session start >/tmp/waydroid-session.log 2>&1 &
    sleep 8
  fi

  if waydroid status 2>/dev/null | grep -q 'Container:[[:space:]]*FROZEN'; then
    log "container is frozen; restarting session"
    waydroid session stop || true
    sleep 2
    waydroid session start >/tmp/waydroid-session.log 2>&1 &
    sleep 8
  fi
}

install_apk() {
  if waydroid app list 2>/dev/null | grep -q "packageName: $PACKAGE_NAME"; then
    return
  fi

  log "installing APK"
  waydroid app install "$APK_PATH"
}

connect_adb() {
  local ip="$WAYDROID_IP"
  if [[ -z "$ip" ]]; then
    ip="$(waydroid status 2>/dev/null | awk -F: '/IP address/ { gsub(/^[ \t]+/, "", $2); print $2; exit }')"
  fi

  if [[ -z "$ip" ]]; then
    log "Waydroid IP not available; skipping adb connection"
    return
  fi

  adb start-server >/dev/null
  adb connect "$ip:5555" >/dev/null || true
  if adb devices | grep -q "^$ip:5555[[:space:]]*device"; then
    ADB_SERIAL="$ip:5555"
    log "adb connected: $ip:5555"
  else
    log "adb not authorized yet: $ip:5555"
  fi
}

grant_runtime_permissions() {
  if [[ -z "$ADB_SERIAL" ]]; then
    return
  fi

  adb -s "$ADB_SERIAL" shell pm grant "$PACKAGE_NAME" android.permission.RECORD_AUDIO >/dev/null 2>&1 || true
}

ensure_tts_engine() {
  if [[ "$INSTALL_ESPEAK_TTS" != "true" || -z "$ADB_SERIAL" ]]; then
    return
  fi

  if ! adb -s "$ADB_SERIAL" shell cmd package query-services --brief -a android.intent.action.TTS_SERVICE |
    grep -q "$ESPEAK_PACKAGE"; then
    log "installing eSpeak TTS engine"
    mkdir -p "$(dirname "$ESPEAK_APK_CACHE")"
    if [[ ! -f "$ESPEAK_APK_CACHE" ]]; then
      curl -L --fail -o "$ESPEAK_APK_CACHE" "$ESPEAK_APK_URL"
    fi
    adb -s "$ADB_SERIAL" install -r "$ESPEAK_APK_CACHE" >/dev/null
  fi

  adb -s "$ADB_SERIAL" shell settings put secure tts_default_synth "$ESPEAK_PACKAGE" >/dev/null
  adb -s "$ADB_SERIAL" shell settings put secure tts_default_locale zh_CN >/dev/null
}

launch_app() {
  log "opening Waydroid UI"
  waydroid show-full-ui >/tmp/waydroid-ui.log 2>&1 &
  sleep 3

  log "launching $PACKAGE_NAME"
  waydroid app launch "$PACKAGE_NAME"
}

main() {
  ensure_apk
  configure_props
  ensure_adb_auth
  start_session
  connect_adb
  ensure_model_link
  install_apk
  grant_runtime_permissions
  ensure_tts_engine
  launch_app
  waydroid status
}

main "$@"
