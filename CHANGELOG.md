# Changelog

## Unreleased — 1.1.0-dev (not Minecraft-verified)

- Added backward-compatible, validated configuration models for Slide, Wall Jump, Long Jump and future HUD controls.
- Connected Slide, Wall Jump and Long Jump to bounded server request processing, collision queries, replacement impulses, lifecycle sessions and authoritative snapshots.
- Added native settings GUI, independent O keybind, live singleplayer application, read-only remote gameplay settings, optional Mod Menu 21 adapter, localized controls/tooltips and configurable HUD. All Minecraft-dependent code remains uncompiled in this cloud instance.
- Added a dependency-light Java 25 core test runner. See `docs/MOVEMENT-UPDATE-PROGRESS.md` for current evidence and network blockers. This is not a completed Movement Update release; see the documented slide-jump fallback limitation and local verification requirements.

## 1.0.0 — 2026-11-09

- Added a unified, server-authoritative double/triple jump system with configurable strengths and confirmed landings.
- Added gradual sprint acceleration with a replaceable transient speed modifier and configurable decay.
- Added a rebindable directional dash with per-player tick cooldowns and normalized input.
- Added typed networking, replay protection, lifecycle epochs, bounded request processing, and server configuration synchronization.
- Added local cosmetic preferences, accepted-action particles/sounds, a compact HUD, and English/Hungarian translations.
- Added validated JSON defaults, malformed-file recovery, Java 25/Gradle 9.8.2 project files, unit tests, server/client GameTests, a two-process localhost client harness, and a multiplayer checklist.
- Targeted Minecraft 26.3 using unobfuscated Mojang names and Fabric Loader 0.20.1 / Fabric API 0.166.0+26.3.

See [VERIFICATION.md](VERIFICATION.md) for executed checks and unverified runtime scenarios. This version number does not imply every manual multiplayer acceptance check has passed.
