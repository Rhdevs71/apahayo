# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- **Instagram Ghost Mode**: Ported Piko's Ghost Mode to hide read receipts for Direct Messages (`mark_thread_seen-`).
- **Instagram Downloader Fallback**: Added a long-click listener on the Instagram Share button as a reliable fallback to download media.
- **Instagram Patching Engine**: Introduced `MetaUnobfuscator` using DexKit to dynamically scan and patch Instagram at runtime.
- **Instagram Hide Ads**: Ported Piko's `DisableAdsFingerprint` (`Is ad pod`) to block sponsored posts and stories.
- **Instagram Hook Flags**: Created foundation to intercept `MobileConfig` flags using Piko's `__fbt_null__` fingerprint.
- **GitHub Actions**: Added Discord Webhook notifications for build success and failure (`build-whatsapp.yml` & `build-business.yml`).

### Fixed
- **Fake Settings Crash**: Wrapped `isMe()` and `getRawString()` calls in `FakeDisplayHook.kt` with `try-catch` blocks to prevent `NoSuchMethodError` and allow the Fake Settings menu to function correctly in newer WhatsApp versions.
- **Resource Not Found Crash**: Handled `Resources$NotFoundException` in `UnobfuscatorCache.kt` gracefully to prevent fatal crashes during module initialization when resources fail to inject via LSPosed.
- **WhatsApp Startup Crash**: Fixed a fatal `Resources$NotFoundException` crash during initialization by ensuring `moduleContext` is loaded before querying module-specific resource arrays (e.g. `supported_versions_wpp`).
- **Compilation Error**: Fixed `Argument type mismatch` in `WppCore.kt` related to `Boolean::class.javaPrimitiveType`.
