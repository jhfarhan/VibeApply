# VibeApply — Persistent Project Context

Last updated: 2026-11-23
Repository: https://github.com/jhfarhan/VibeApply
GitHub account to use: jhfarhan (side account only)
Default branch: main

## Project purpose
Build VibeApply, an Android-first, offline-first document and application-preparation assistant. It helps students, job applicants, and everyday users prepare application documents directly on their phones without requiring a cloud account, paid AI API, or backend server.

## Product direction
- Image/document capture and crop/rotate.
- Image-to-PDF and multi-page PDF creation.
- PDF merge, split, and compression where feasible on-device.
- Resize/compress images to a target KB/MB size.
- Passport-photo and signature image resizing.
- On-device OCR where supported.
- Application document checklists and reusable local document packs.
- Local-only document library with explicit privacy controls.

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

## Workflow
1. Verify authenticated GitHub user is jhfarhan and target is jhfarhan/VibeApply.
2. Inspect current repository and latest commit before modifying.
3. Implement one coherent milestone at a time.
4. Commit/push only to this repository.
5. Build APK with GitHub Actions.
6. Run on an Android emulator; collect screenshots/logs.
7. Inspect screenshots and fix visual/functional issues.
8. Update this file after each meaningful milestone, recording changed files, commit SHA, test results, known issues, and next action.
9. In a new chat, read this file and recent commits/workflow runs before continuing.

## Current verified state
- Authenticated GitHub account: jhfarhan.
- Repository: jhfarhan/VibeApply (public); authenticated account has admin/maintain/push/pull permissions.
- Default branch: main.
- Persistent project record first added in commit d7d7726968a8a0de9d287a80b56ffe67793caf16.
- First Android implementation commit: 0f60917a1c4760acdcf30300b10f9f19cbb4b84f.
- Project Context checkpoint commit: 02d3ba4d56b59531d3eea4e7748d58f83920f48b.
- Added Kotlin + Jetpack Compose Android project with minSdk 26, targetSdk 35, and black/red theme.
- Added Images to PDF: multi-image selection, one fitted A4 page per image, save destination chosen through Android Create Document picker.
- Added Image Compressor: select image, choose a target KB size, locally encode JPEG, save as a new file; original is not overwritten.
- Uses Android Storage Access Framework; no broad storage permission, account, database server, Firebase, Supabase, or paid API.
- Added GitHub Actions build-apk workflow and emulator-test workflow (Android API 29 and 34).
- APK build run 45217302405 completed successfully for commit 02d3ba4d56b59531d3eea4e7748d58f83920f48b. All build steps passed, including Gradle assembleDebug and artifact upload.
- APK artifact: VibeApply-debug-2, artifact ID 14838468202, size 9,086,216 bytes; downloaded and verified as an Android APK at /mnt/data/vibeapply-build-2/app-debug.apk in this session. This is a debug APK, not a signed production release.
- Emulator smoke test run 45217302316 was still in progress at last check. Both API 29 and API 34 emulator jobs had reached the app-run step; no final result or screenshot artifact was confirmed at that check.
- Initial implementation run 45216958774 / 45216958721 was cancelled when the context checkpoint triggered new workflow runs; do not treat those cancelled runs as failures of the latest build.
- No changes have been made to the separate VibeDownloader repository as part of VibeApply work.

## Current status
Milestone 1 APK compiles and is available as a debug artifact. Emulator verification is pending.
Implemented:
- Images to PDF: select multiple images; each becomes a fitted A4 page; save via Android Create Document picker.
- Image Compressor: select image; choose target size (10–20,000 KB); compress to JPEG locally; save via Android Create Document picker.
Known limitations:
- Image rotation/EXIF orientation handling is not yet implemented.
- Target size is best-effort; extremely low targets may not be achievable without substantial quality loss.
- PDF/image workflows still need end-to-end emulator validation.
- Only Images to PDF and Image Compressor are implemented; other tools remain planned.

## Next actions
1. Recheck emulator run 45217302316 and inspect its API 29/API 34 job results.
2. Download and visually inspect screenshots/logs when the emulator workflow finishes.
3. If emulator checks fail or reveal defects, fix them in a separate commit and rerun both workflows.
4. Add tests for PDF creation and image compression, including edge cases.
5. Add document crop/rotate and OCR in a separate milestone after the core flows are verified.

## Session handoff
When continuing, first verify the authenticated account is still jhfarhan, verify the target repository is still jhfarhan/VibeApply, inspect the latest commit and workflow runs, then continue from the latest recorded milestone. Do not rely on conversational memory alone.
