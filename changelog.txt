# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- **Telegram Notifications**: Set up a GitHub Action workflow to automatically send updates to a private Telegram channel.
- **Repository Cleanup**: Removed legacy script files, obsolete localization configurations, and completely modernized the README.md documentation.
- **Instagram Ghost Mode (Piko)**: Ported Piko's Ghost Mode to hide read receipts for Direct Messages (`mark_thread_seen-`) and Stories.
- **Instagram Downloader Fallback**: Added a long-click listener on the Instagram Share button as a reliable fallback to download media.
- **Instagram Patching Engine**: Introduced `MetaUnobfuscator` using DexKit to dynamically scan and patch Instagram at runtime.
- **Instagram Hide Ads**: Ported Piko's `DisableAdsFingerprint` (`Is ad pod`) to block sponsored posts and stories.
- **Instagram Hook Flags**: Created foundation to intercept `MobileConfig` flags using Piko's `__fbt_null__` fingerprint.

### Changed
- **Unified Build System**: Removed product flavors (whatsapp/business) to create a single, unified "Apahayo" APK output.

### Fixed
- **WhatsApp Crash (Fake Settings)**: Disabled direct SQLite `INSERT INTO` injections for "Fake Messages" and "Fake Call Logs" to prevent fatal crashes and database corruption.
- **WhatsApp Crash (Message Scheduler)**: Fixed `BadParcelableException` when scheduling messages by parsing `contact_names` as primitive String arrays instead of serialized objects.
- **Google Photos Spoofing**: Moved the device spoofing patch to run immediately during `handleLoadPackage` (before `Application.onCreate`) so Google Photos initializes with Pixel XL identifiers.
- **Instagram Downloader**: Fixed missing download button by updating `GhostModePatch.kt` logic and adding `BottomSheetDialog.show` hook fallback.
- **Fake Settings Crash**: Wrapped `isMe()` and `getRawString()` calls in `FakeDisplayHook.kt` with `try-catch` blocks to prevent `NoSuchMethodError` and allow the Fake Settings menu to function correctly in newer WhatsApp versions.
- **Resource Not Found Crash**: Handled `Resources$NotFoundException` in `UnobfuscatorCache.kt` gracefully to prevent fatal crashes during module initialization when resources fail to inject via LSPosed.
- **WhatsApp Startup Crash**: Fixed a fatal `Resources$NotFoundException` crash during initialization by ensuring `moduleContext` is loaded before querying module-specific resource arrays (e.g. `supported_versions_wpp`).
- **Compilation Error**: Fixed `Argument type mismatch` in `WppCore.kt` related to `Boolean::class.javaPrimitiveType`.
