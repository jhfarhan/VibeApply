# VibeApply — Persistent Project Context

Last updated: 2026-11-23
Repository: https://github.com/jhfarhan/VibeApply
GitHub account to use: jhfarhan (side account only)
Default branch: main

## Project purpose
Build VibeApply, an Android-first, offline-first document and application-preparation assistant. It helps students, job applicants, and everyday users prepare application documents directly on their phones without requiring a cloud account, paid AI API, or backend server.

## Non-negotiable constraints
- Use ONLY the side-account repository jhfarhan/VibeApply.
- Do NOT access, modify, push to, or create branches in the user's main GitHub account or jhfarhanyt20-coder/VibeDownloader-Android.
- Kotlin + Jetpack Compose.
- Offline-first: local processing and local storage by default.
- No mandatory Firebase, Supabase, cloud database, paid API, or hosted backend.
- No account/login requirement for core features.
- Preserve the black/dark + red visual identity.
- Never claim a feature or test passed unless actually verified.
- Prefer small reviewable commits and run build/emulator checks after meaningful changes.

## Current implemented features
1. **Images to PDF** — select multiple images; each becomes a fitted A4 page; save through Android Create Document picker.
2. **Image Compressor** — choose a target KB size (10–20,000 KB), encode locally to JPEG, and save a new file; original stays untouched.
3. **Photo Resizer** — 35 × 45 mm (413 × 531 px), 2 × 2 inch (600 × 600 px), 4 × 6 inch (1200 × 1800 px); center crop; rotate left/right in 90-degree steps; JPEG output.
4. **Application Checklist** — base document checklist, custom entries, checked state and custom items saved in SharedPreferences on-device.
5. **Image Text Extractor (OCR)** — Google ML Kit bundled Latin text recognition; processing on-device, editable result and clipboard copy. Bangla script is not supported in this build.
6. **PDF Page Extractor** — read page count with Android PdfRenderer, parse page ranges (e.g. 1-3,5), export selected pages.
7. **Merge PDFs** — combine pages from multiple selected PDFs.

## Important feature limitations
- PDF page extraction and merge rasterize pages into images. The visual content is rendered into the new PDF, but selectable/searchable text, links and interactive form fields are not preserved.
- PDF output is capped at 30 pages per file for memory safety.
- Photo Resizer uses a centered crop rather than a draggable crop-preview editor. Check subject framing before saving.
- Image compression target size is best-effort; tiny targets can require strong quality reduction.
- OCR is Latin-script only; Bengali OCR is not implemented.
- EXIF orientation is not automatically normalized across every input source.
- PDF/image operations still need thorough end-to-end emulator tests with real sample files.

## Build configuration
- Kotlin + Jetpack Compose.
- compileSdk 35, targetSdk 35, minSdk 26, Java 17.
- Uses Android Storage Access Framework; no broad storage permission.
- No account, backend, cloud database, Firebase/Supabase requirement, or paid API.
- Bundled ML Kit text model increases universal debug APK size to about 52 MB.

## GitHub Actions and verified checkpoints
- Authenticated GitHub user verified: jhfarhan.
- Repository verified: jhfarhan/VibeApply; authenticated user has admin/maintain/push/pull permissions.
- Default branch: main.
- Initial Android implementation: 0f60917a1c4760acdcf30300b10f9f19cbb4b84f.
- Project context checkpoint: 02d3ba4d56b59531d3eea4e7748d58f83920f48b.
- Commit 5ab9c82fac58e0cc163d887875c98fde98af910a included Photo Resizer, Application Checklist and bundled OCR; APK build run 45219740244 completed successfully for the later PDF-feature commit.
- Commit 062ca902e70dfe422ef34f9c1dbeff176cf65a29 added PDF Page Extractor and Merge PDFs.
- Build run 45220394762 completed successfully for commit 062ca902e70dfe422ef34f9c1dbeff176cf65a29; APK artifact ID 14840781043, artifact name VibeApply-debug-8, artifact archive 29,568,797 bytes. APK was downloaded and validated as an Android package; APK size was about 52 MB and SHA-256 was 498a8a7c4832f45c914a7535ab6c1a956f83030f5bc378f42e92100fdb6e21a7. It is a debug APK, not a signed production release.
- Emulator run 45220394735 (API 29 and API 34 matrix) was still in progress at last check; no emulator screenshots or final result had been confirmed.
- An earlier type-inference compile error in checklist state was fixed with explicit Set<String>/List<String> types; do not treat that failed pre-fix build as the current code status.
- No changes have been made to the separate VibeDownloader repository.

## Workflow
1. Verify authenticated GitHub user is jhfarhan and target is jhfarhan/VibeApply.
2. Inspect current branch HEAD and files before modifying.
3. Implement one coherent milestone at a time.
4. Commit/push only to this repository.
5. Build the debug APK with GitHub Actions.
6. Run on Android API 29 and 34 emulators; collect screenshots/logs.
7. Inspect screenshots and fix visual/functional issues.
8. Update this file after each meaningful milestone with exact test results and next actions.
9. In a new chat, read this file and recent commits/workflow runs before continuing. Do not rely on conversational memory alone.

## Next actions
1. Check the new emulator workflow after the latest changes; inspect both API 29 and API 34 screenshots and logcat before claiming runtime verification.
2. Exercise PDF page count, page selection, merge, and image export on emulator sample files.
3. Add tests for page-range parsing and PDF/image workflows.
4. Consider a Bangla-capable offline OCR option, EXIF auto-rotation, and a draggable crop preview.
5. If preparing for public release, create a properly signed release build; do not claim the debug APK is a production release.

## Session handoff
When continuing, first verify the authenticated account is still jhfarhan, verify the target repository is still jhfarhan/VibeApply, inspect the latest commit and workflow runs, then continue from the latest recorded milestone.
