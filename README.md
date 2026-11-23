# VibeApply

An offline-first document toolkit for Android.

VibeApply helps people prepare everyday application documents directly on their phone. Core processing happens locally: no account, cloud upload, Firebase, or paid API is required.

## First milestone
- **Images to PDF:** combine multiple images into an A4 PDF, then choose where to save it.
- **Image Compressor:** compress a photo toward a target KB size and save a new JPEG without modifying the original.

## Tech
- Kotlin
- Jetpack Compose + Material 3
- Android Storage Access Framework
- Android PdfDocument and Bitmap APIs

## Build
Open the project in Android Studio (JDK 17, Android SDK 35), or use the GitHub Actions workflows to build a debug APK.

## Privacy
The app does not request broad storage access. Users select input files through Android's system document picker and select the output location themselves. Processing stays on-device.

See PROJECT_CONTEXT.md for the durable project plan and session handoff.
