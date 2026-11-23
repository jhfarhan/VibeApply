# VibeApply — Persistent Project Context

Last updated: 2026-11-23
Repository: https://github.com/jhfarhan/VibeApply
GitHub account to use: jhfarhan (side account only)
Default branch: main

## Project purpose
Build VibeApply, an Android-first, offline-first document and application-preparation assistant. The goal is to help students, job applicants, and everyday users prepare common application documents directly on their phones without requiring a cloud account, paid AI API, or backend server.

## Product direction
Initial product concept: VibeApply / VibeDocs.
Core workflows to prioritize:
1. Image/document capture and crop/rotate.
2. Image-to-PDF and multi-page PDF creation.
3. PDF merge, split, and compression where feasible on-device.
4. Resize/compress images to a target KB/MB size.
5. Passport-photo and signature image resizing with configurable dimensions.
6. On-device OCR where supported.
7. Application document checklist and reusable local document packs.
8. Local-only document library with explicit privacy controls.

## Non-negotiable constraints
- Use ONLY the side-account repository jhfarhan/VibeApply for this project.
- Do NOT access, modify, push to, or create branches in the user's main GitHub account or the separate jhfarhanyt20-coder/VibeDownloader-Android repository.
- Android native stack: Kotlin + Jetpack Compose.
- Offline-first: local processing and local storage by default.
- No mandatory Firebase, Supabase, cloud database, paid API, or hosted backend.
- No account/login requirement for core features.
- Preserve a polished black/dark + red visual identity.
- Never claim a feature or test passed unless it was actually verified.
- Prefer small, reviewable commits and run build/emulator checks after meaningful changes.

## Development workflow
1. Inspect current repository state before changes.
2. Implement one coherent milestone at a time.
3. Commit/push to this repository only.
4. Build APK with GitHub Actions.
5. Run on Android emulator where available and collect screenshots/logs.
6. Inspect screenshots and fix visual/functional issues.
7. Update this file after each milestone, recording changed files, commit SHA, build/test results, known issues, and the exact next action.
8. In a new chat, read this file and recent commits/workflow runs before continuing.

## Current verified starting state (2026-11-23)
- Authenticated GitHub account is jhfarhan.
- jhfarhan/VibeApply exists, is public, and the authenticated account has admin/maintain/push/pull permissions.
- Default branch: main.
- Initial repository tree contained only README.md (11 bytes); no Android project or workflow exists yet.
- No VibeApply source code, APK, emulator run, or feature implementation has been completed yet.
- No files have been changed in the separate VibeDownloader repository as part of VibeApply work.

## Current status
Planning / repository bootstrap.
- Next: add an Android Gradle project skeleton and GitHub Actions build + emulator UI-check workflows.
- Then implement the first usable document workflow and verify it on a virtual Android device.

## Session handoff
When continuing, first verify the authenticated account is still jhfarhan, verify the target repo is still jhfarhan/VibeApply, inspect the latest commit and workflow runs, then continue from the latest recorded milestone. Do not rely on conversational memory alone.