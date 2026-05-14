# Local AARs

Place the official `sherpa-onnx-1.13.0.aar` Android release from k2-fsa in this directory before building a device package with local SenseVoice ASR enabled.

The AAR is intentionally not committed because it is large. ASR model files are also not packaged in the APK; the app reads SenseVoice files from:

`context.getExternalFilesDir("models/asr/sensevoice")`
