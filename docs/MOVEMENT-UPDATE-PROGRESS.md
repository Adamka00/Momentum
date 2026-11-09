# Momentum 1.1.0 — work in progress

This branch is not an implementation of the complete Movement Update and is not a release candidate.
The runtime and artifact version are `1.1.0-dev`. The original 1.0.0 history on main is untouched. Do not distribute this branch as a finished release.

## Implemented source

- Backward-compatible `slide`, `wallJump`, and `longJump` configuration sections, numeric repair, bounded strengths and timers. Existing names and valid values are preserved. Loading a valid 1.0.0 file adds defaults in memory without rewriting the file.
- Local HUD preference fields for position, scale, opacity and individual indicators. Dash cosmetic preference fields.
- Pure, per-player `SlideState`, `WallJumpState`, and `LongJumpState` models with held-key protection, tick guards, cooldowns, and bounded slide magnitude. Wall-jump budget is shared across directions; same-direction cooldowns are independent. Only the confirmed-landing callback refreshes that budget, preserving replay and key history.
- A genuine 1.0.0 defaults migration fixture, custom-preference migration checks, null-section and nonfinite-number repair, detached-edit serialization/save tests, and pure movement-state tests.

Additional source changes now connect these models to server gameplay, packet registration, client acknowledgements and HUD snapshots. Long jumps use server-observed support/input/speed; wall normals come from four bounded voxel-collision queries. A bounded eight-request-per-tick queue arbitrates long jump > dash > wall/air jump > slide. Accepted long/wall jumps mark takeoff without resetting ordinary air jumps unless explicitly configured. Dimensions, teleport, death and disconnect drop old session queues and epochs. Slides cancel on unsupported/restricted states; no custom pose or permanent modifier is installed.

The native screen exposes every reflected configuration field using toggles or validated numeric inputs, categories, pages/scroll, tooltips, reset/discard confirmations, dirty tracking, Apply/Save/Cancel and feedback. Only the actual integrated server can receive gameplay edits, through an in-process task on its server thread; no remote gameplay-edit packet is registered. A successful apply preserves spent jumps/cooldowns, cancels slides, removes sprint modifiers and resynchronizes settings without resetting client sequence history. Remote preferences are persisted into the local configuration without overwriting it with remote gameplay values.

Mod Menu's official `v21.0.0` source declares Minecraft 26.3 and confirms `ModMenuApi#getModConfigScreenFactory` and `ConfigScreenFactory#create(Screen)`. The optional adapter is only referenced by the `modmenu` entrypoint; compile-only client dependency, no bundled API stubs, no required Mod Menu runtime dependency. See [upstream properties](https://github.com/TerraformersMC/ModMenu/blob/v21.0.0/gradle.properties) and [API](https://github.com/TerraformersMC/ModMenu/blob/v21.0.0/src/main/java/com/terraformersmc/modmenu/api/ModMenuApi.java). Native screen patterns were inspected in the same tag. Fabric's official 26.3 source also confirms HUD matrix calls, four-argument screen scroll callbacks and `Minecraft#getSingleplayerServer`.

**Only core configuration/math/state/metadata code has compiled and run here. Minecraft-dependent source, native screen, Mod Menu adapter, networking and HUD have not compiled, loaded or run in this instance.** Source inspection does not establish binary compatibility or working gameplay. New collision/codec GameTests have been written but not executed. See [local verification](LOCAL-VERIFICATION.md).

Known implementation limitation: slide uses vanilla crouching and vanilla camera transition. It cannot pass through one-block-high openings. No pose is forced on exit, so vanilla continues to decide whether standing fits. `allowSlideJump=false` gates mod jump handling during a slide, but does not suppress vanilla's ordinary ground jump; that part of the requested behavior remains incomplete. A dedicated camera transition, broader collision/lifecycle GameTests and new real-client scenarios also remain outstanding.

## Proposed units and bounds

Configuration values below define the model; gameplay integration is pending.

| Section / field | Default | Range / meaning |
| --- | --- | --- |
| slide.enabled | true | Boolean |
| slide.durationTicks | 20 | 1–200 server ticks |
| slide.initialSpeedMultiplier | 1.15 | 1–2; initial magnitude capped at 3 blocks/tick |
| slide.friction | 0.92 | 0.1–0.99; fraction of slide speed retained each tick |
| slide.minimumActivationSpeed | 0.15 | 0.01–1 blocks/tick |
| slide.minimumContinueSpeed | 0.06 | 0.001–activation speed, blocks/tick |
| slide.cooldownTicks | 30 | 1–1200 ticks, starts on activation; cancellation does not refund |
| slide.cancelOnCrouchRelease / allowSlideJump | true / true | Booleans |
| slide.particles / sounds | true / true | Booleans |
| wallJump.enabled | true | Boolean |
| wallJump.verticalStrength / horizontalStrength | 0.42 / 0.45 | 0.1–1.5 blocks/tick |
| wallJump.maximumWallJumps | 2 | 0–8 per airtime; no corner reset |
| wallJump.wallDetectionDistance | 0.15 | 0.01–0.3 blocks |
| wallJump.sameWallCooldownTicks | 10 | 2–1200 ticks per wall normal |
| wallJump.resetAirJumpsOnWallJump | false | Boolean; integration pending |
| wallJump.particles / sounds | true / true | Booleans |
| longJump.enabled / requireSprint / allowFromSlide | true / true / true | Booleans |
| longJump.horizontalStrength | 0.9 | 0.1–3 blocks/tick; replacement impulse planned |
| longJump.verticalStrength | 0.34 | 0.1–1.5 blocks/tick |
| longJump.minimumActivationSpeed | 0.15 | 0.01–1 blocks/tick |
| longJump.cooldownTicks | 40 | 1–1200 ticks |
| longJump.particles / sounds | true / true | Booleans |
| client.hudX / hudY | 0 / 1 | 0–1 fraction of available screen space |
| client.hudScale / hudOpacity | 1 / 1 | 0.5–3 / 0.1–1 |
| client.showAirJumps / showWallJumps / showDash / showLongJump / showSlide / showSprint | all true | Individual indicator toggles |
| dash.particles / sounds | true / true | Booleans |

## Current validation and blocker

In this cloud instance, Java 25 core compilation and JUnit execution passed **45 tests**, including the original 28. No tests were skipped. Run `bash scripts/test-core.sh` with Java 25 active to reproduce. It downloads JUnit Platform 1.13.4, Gson 2.13.2 and SLF4J API 2.0.17 over HTTPS from Maven Central into ignored build directories. These explicit core-check dependencies do not prove that Loom's resolved Minecraft dependency graph compiles. Results go to a fresh `build/core-tests/run-*/results` directory.

`./gradlew clean build`, `./gradlew test` and `./gradlew runGameTest` each failed before compilation because the proxy denies `maven.fabricmc.net` with HTTP 403. No GameTests, dedicated server, real client, GUI or multiplayer checks have run for these changes. The existing root verification report describes the earlier 1.0.0 author's results.

The environment configuration service also fails to verify the draft (`INVALID_ARGUMENT`), so no network configuration save is confirmed. Environment settings need these additional hosts, retaining existing rules and the package-manager preset:

`maven.fabricmc.net`, `meta.fabricmc.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`, `maven.terraformersmc.com`.

The last host is for resolving the Mod Menu 21.0.0 client compile dependency and optional runtime test. After access is restored, retry the full build with a fresh connection, resolve any compilation/API errors, complete the known slide limitation and missing integration tests, and verify real gameplay/GUI and multiplayer before release delivery. No release JAR, release source ZIP, push, PR, merge, GitHub Release or environment publication has been performed for this work.
