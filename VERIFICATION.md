# Momentum 1.0.0 verification

Executed on 2026-11-09 using Minecraft 26.3, Loader 0.20.1, API 0.166.0+26.3,
Loom 1.19.4, Gradle 9.8.2, and Temurin Java 25.0.5+8 on Linux x86_64.
Official dependency evidence is in [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md).

## Results

| Check | Actual result |
| --- | --- |
| Gradle wrapper download and checksum | Passed; wrapper reports Gradle 9.8.2 |
| `./gradlew clean build` | Passed, including compilation, resources, JAR, unit tests, and server GameTests |
| `./gradlew test` | Passed; 28 tests executed during clean build; separate invocation confirmed the task |
| Unit failures / errors / skipped | 0 / 0 / 0 |
| Momentum server GameTests | All 5 passed; Fabric's additional built-in test also passed (6 total in runner) |
| Integrated-server real client scenario | Passed: second/third jump, fourth-jump rejection, dash, maximum sprint, modifier cleanup, HUD screenshot |
| Normal dedicated server | Reached `Done`, loaded Momentum in SERVER environment, then stopped through console and saved worlds cleanly |
| Two real simultaneous clients | Passed on that dedicated server with separate JVMs and graphics displays |
| Two-client jump budgets | Both clients received their second and third jumps |
| Two-client dash cooldowns | Both accepted 1 server ticks apart, within one cooldown window, confirmed in server log |
| Two-client sprint state | Both reached 1.35; A's modifier cleared after stopping while B retained full boost |
| JAR integrity / metadata | ZIP CRC, version, Minecraft target, entrypoints, resources, and Java 25 class version verified |
| Environment separation | Common classes contain no Minecraft client references; dedicated initialization and server GameTests succeeded |
| Test code in production JAR | None |

The localhost fixture used a disposable peaceful flat world, development identities MomentumA/MomentumB,
and a loopback-only offline-authentication server after the user accepted the Minecraft EULA.
Both players were connected at the same time. This was actual TCP multiplayer with real clients,
not just two mock state objects. Online account authentication was not tested.

## Unit coverage

- Configuration: 5 tests, including generated/distributed defaults, numeric repair, null sections, malformed JSON backup and recovery, and unified jump modes.
- Jumps: 10 tests, including initial state, double/triple limits, fresh presses, replay rejection, same-tick guards, supported landing confirmation, airborne/brief-contact behavior, delayed separation, and separate players.
- Sprint: 7 tests, including delay, progression, cap, decay, restricted states, separate players, packet timing gaps, and stale-motion expiry.
- Dash: 6 tests, including exact tick cooldowns, duplicate/same-tick rejection, rejected requests, separate players, jump-counter preservation, normalized/fallback directions, invalid values, and request budgets.

The five Minecraft server tests exercise momentum/fall-distance preservation, transient attribute
replacement and cleanup, wall collision, slab support versus walls/ceilings/spoofed ground reports,
and all six typed payload codec round trips. These tests use Minecraft server objects and world collision shapes.

## Findings resolved during verification

The two-client run exposed a sprint timer reset on server ticks without a fresh position packet.
A three-server-tick observed-motion grace period now tolerates packet timing gaps while current
forward input, sprint state, support, collision, and restrictions are still checked every tick.
Regression tests cover the gap and stale-motion expiry. The real two-client scenario then passed.

The headless host initially had no display. Private X displays with software EGL rendering enabled
the real client tests. Independent displays were needed to prevent SDL focus changes in one window
from clearing the other test client's held keys. This is a test setup requirement, not a shared-player state mechanism.
Audio-device and Minecraft Services discovery warnings occurred on the host; they did not prevent offline local tests.

## Remaining unverified behavior

- Injected 100/250 ms latency, measured correction/rubber-banding behavior, and internet authentication.
- LAN discovery/joining, macOS Apple Silicon execution, third-party movement/anti-cheat interoperability.
- Full interactive lifecycle matrix: death/respawn, dimension transitions, reconnect stress, and retained-memory profiling.
- Every potion, slime/honey, vehicle, fluid, elytra, Creative flight, high-fall-damage, fence/door/corner, and GUI-scale combination.
- Audible sound quality, all cosmetic preferences, manual key rebinding, and a Hungarian-language HUD session.

These paths are implemented or delegated to documented vanilla behavior, but are not claimed as fully runtime-verified.
Use [docs/MULTIPLAYER-CHECKLIST.md](docs/MULTIPLAYER-CHECKLIST.md) for the remaining procedures.
Client movement prediction is deliberately disabled, so ability response includes a server round trip.
The HUD uses server snapshots without a speculative cooldown countdown.

## Artifact

`momentum-1.0.0.jar`: 48,219 bytes.

SHA-256: `34b84aa9f724a92d83c046b342ce24a4111d41b36f38371c62527fa2ccaefd33`

Build/test logs, XML results, and real-client screenshots are included in the delivery's
`verification/` directory. The source ZIP includes all production/test sources, wrapper files,
defaults, README, changelog, license, dependency evidence, and test procedures. Minecraft files,
downloaded JDK/Gradle distributions, caches, test identities, and test worlds are excluded.
