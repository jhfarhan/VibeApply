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
- Initial repo had only README.md.
- Persistent project record was added in commit d7d7726968a8a0de9d287a80b56ffe67793caf16.
- First Android implementation is now being bootstrapped: Images to PDF and Image Compressor, both using Android's file picker and on-device processing.
- GitHub Actions workflows are being added for debug APK builds and Android emulator smoke tests on API 29 and 34.

## Current status
Milestone 1 implementation pushed; build and emulator checks pending.
- Images to PDF: select multiple images; each becomes a fitted A4 page; save through Android's Create Document picker.
- Image Compressor: select an image; choose a target size in KB; compress to JPEG locally; save through Android's Create Document picker.
- No broad storage permission or cloud account is requested.
- UI uses a black/dark + red theme.
- Next: verify GitHub Actions results, fix compile/runtime issues, inspect emulator screenshots, then implement further document tools.

## Session handoff
When continuing, first verify the authenticated account is still jhfarhan, verify the target repository is still jhfarhan/VibeApply, inspect the latest commit and workflow runs, then continue from the latest recorded milestone. Do not rely on conversational memory alone.
