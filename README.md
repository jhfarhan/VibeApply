# VibeApply

An offline-first Android document toolkit built with Kotlin and Jetpack Compose.

VibeApply helps prepare everyday application documents directly on a phone. Core processing is local: no account, cloud upload, Firebase, hosted backend, or paid AI API is required.

## Current tools

1. **Images to PDF** — combine multiple images into an A4 PDF.
2. **Image Compressor** — compress a photo toward a target KB size and save a new JPEG; the original is not modified.
3. **Photo Resizer** — export common ID/print sizes (35 × 45 mm, 2 × 2 inch, 4 × 6 inch), rotate by 90° and center-crop to the selected ratio.
4. **Application Checklist** — tick off common application documents, add custom items, and keep progress saved locally.
5. **Image Text Extractor (OCR)** — extract and copy printed Latin-script text on-device. Bangla OCR is not included in this build.
6. **PDF Page Extractor** — select page ranges such as `1-3,5` and save a new PDF.
7. **Merge PDFs** — combine multiple PDF files into one document.

## Important behavior and limitations

- Android's system document picker is used for input and output. The app does not request broad storage access.
- PDF extraction and merging render pages into images. The visual page content is retained, but searchable/selectable text and interactive form fields are not.
- PDF exports are limited to 30 pages per output to keep memory use under control.
- Photo resizing uses a centered crop; check that the subject remains correctly framed.
- Image compression targets are best-effort. Very small target sizes may require substantial quality reduction.
- OCR uses a bundled on-device Latin text model. This avoids a server/model download at first use, but increases APK size; Bengali-script OCR is not included.

## Tech

- Kotlin + Jetpack Compose + Material 3
- Android Storage Access Framework
- Android `Bitmap`, `PdfDocument` and `PdfRenderer`
- Google ML Kit bundled Text Recognition model
- minSdk 26, targetSdk 35, Java 17

## Build

GitHub Actions builds a **debug APK** from the `main` branch. The debug APK is for testing and is not a signed production release.

## Privacy

Files are selected and saved by the user through Android's system document picker. Image/PDF processing and checklist data stay on-device.

See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the durable project plan and session handoff.
