# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- **Instagram Patching Engine**: Introduced `MetaUnobfuscator` using DexKit to dynamically scan and patch Instagram at runtime.
- **Instagram Hide Ads**: Ported Piko's `DisableAdsFingerprint` (`Is ad pod`) to block sponsored posts and stories.
- **Instagram Hook Flags**: Created foundation to intercept `MobileConfig` flags using Piko's `__fbt_null__` fingerprint.
- **GitHub Actions**: Added Discord Webhook notifications for build success and failure (`build-whatsapp.yml` & `build-business.yml`).

### Fixed
- **WhatsApp Startup Crash**: Fixed a fatal `Resources$NotFoundException` crash during initialization by ensuring `moduleContext` is loaded before querying module-specific resource arrays (e.g. `supported_versions_wpp`).
- **Compilation Error**: Fixed `Argument type mismatch` in `WppCore.kt` related to `Boolean::class.javaPrimitiveType`.
