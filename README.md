# Momentum 1.1.0-dev — unverified development branch

Development branch: [Momentum 1.1.0 progress and current limitations](docs/MOVEMENT-UPDATE-PROGRESS.md). Gameplay integration, native settings GUI, localization and HUD changes are implemented in source. Only dependency-light core code has compiled here; Minecraft integration is not yet verified. This is not a release. See [macOS verification instructions](docs/LOCAL-VERIFICATION.md).

Momentum adds extra jumps, gradual sprint acceleration, and a directional dash to **Minecraft Java Edition 26.3 with Fabric**. It supports Survival, Adventure, and grounded Creative players through the same server-authoritative movement system. Creative flight, spectator mode, elytra, swimming, climbing, and riding retain their normal controls.

The project includes production sources, a Gradle wrapper, JSON defaults, English and Hungarian translations, unit tests, Minecraft server GameTests, and a client GameTest. See [VERIFICATION.md](VERIFICATION.md) for what was actually executed and [docs/MULTIPLAYER-CHECKLIST.md](docs/MULTIPLAYER-CHECKLIST.md) for remaining runtime checks. A successful build alone does not establish multiplayer latency behavior.

## Compatibility and installation

| Component | Pinned build version |
| --- | --- |
| Minecraft Java Edition | 26.3 stable |
| Java | 25 |
| Fabric Loader | 0.20.1 or later |
| Fabric API | 0.166.0+26.3 |
| Fabric Loom | 1.19.4 |
| Gradle wrapper | 9.8.2 |
| Names/mappings | Unobfuscated Mojang names; no mapping dependency |

Install Java 25, Fabric Loader for **26.3**, and the matching Fabric API. Place `momentum-1.1.0-dev.jar` and Fabric API in the Minecraft instance's `mods/` directory. Install both mods on **every client and the dedicated server**. A client missing Momentum's networking channels is disconnected with an explanatory message. A vanilla server does not grant Momentum abilities.

For singleplayer and LAN, install on the host and each joining client. The integrated server enforces the host's configuration. Version numbers and official sources are documented in [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md). Other Minecraft versions are not supported by this build.

## Controls and features

| Control | Result |
| --- | --- |
| Space | Ordinary Minecraft jump |
| Release, then press Space in the air | Second jump |
| Release, then press Space again | Third jump, if enabled |
| Sprint and move forward continuously | Gradual speed boost after 20 ticks |
| R | Dash toward movement input; forward when stationary |
| Sprint + crouch | Slide, using vanilla crouch collision dimensions |
| Space while airborne near a wall | Wall jump before an ordinary air jump |
| Sprint + G | Long jump |
| O | Native Momentum settings |

Rebind Dash in **Options → Controls → Key Binds → Momentum**. Holding a key cannot repeatedly execute an ability. Auto-jump does not synthesize extra air jumps. Triple jump includes double jump and uses the same counter. Walking off a ledge consumes the ordinary ground jump, leaving two air jumps in the default mode.

Default air jump velocities are 0.42 and 0.46 blocks/tick; Jump Boost adds vanilla's jump bonus. Horizontal momentum is preserved at the moment the server's accepted jump reaches the client. No jump or dash clears fall distance. Using an extra jump late in a fall does **not** grant fall-damage immunity.

Sprint boost ramps from 1.00 to 1.35 over 60 eligible ticks after a 20-tick delay. A unique transient attribute modifier composes with Speed and other modifiers without changing base speed or saving a permanent bonus. Boost decays when walking, stopping, colliding, or jumping. The modifier is removed while airborne, in restricted states, at death, on respawn, dimension changes, disconnect, and server shutdown.

Observed horizontal movement has a three-server-tick grace period so ordinary packet timing gaps do not restart the sprint ramp. Forward input, sprint state, ground support, collision checks, and movement restrictions still apply every tick. Released input stops eligibility immediately; stale movement alone cannot sustain acceleration.

Dash replaces horizontal velocity with a normalized 1.2 blocks/tick burst and preserves vertical velocity by default. It uses normal Minecraft movement and collision processing, without teleporting through blocks. Its 40-tick cooldown is per player and follows server ticks. Jump and dash may both execute in the same tick, but each ability can execute only once in that tick.

The small lower-left HUD displays available **air** jumps, dash cooldown, and active sprint multiplier. HUD placement uses scaled GUI coordinates. F1, menus, death, and spectator mode hide it. Success particles and sounds follow server acceptance; nearby players receive cosmetic notifications as well.

## Configuration

`config/momentum.json` is created on first launch. [config/momentum.json](config/momentum.json) contains the complete defaults. Edit the file with the game/server stopped, then restart. The O key opens the native settings screen. Integrated-server gameplay changes are applied on its server thread after a successful save. Dedicated-server gameplay controls are read-only; local HUD and cosmetic settings remain editable. The implementation still needs full compilation and runtime validation.

Malformed JSON is logged, copied to `momentum.json.invalid`, and replaced with defaults where the filesystem permits. Missing sections receive defaults. Invalid numbers are clamped; nonfinite values use defaults. In-memory validated values are authoritative; ordinary valid files are not rewritten just to normalize ranges.

| Setting | Default | Meaning / permitted range |
| --- | --- | --- |
| `general.doubleJump` | true | Allow up to two total jumps when triple mode is off |
| `general.tripleJump` | true | Unified jump mode, bounded by `maximumTotalJumps` |
| `general.sprintBoost` | true | Enable sprint acceleration |
| `general.dash` | true | Enable dash |
| `general.particles` | true | Server permission for success particles |
| `general.sounds` | true | Server permission for success sounds |
| `general.debugLogging` | false | Log accepted jumps and dashes |
| `jump.maximumTotalJumps` | 3 | 1–3; includes the ordinary ground jump |
| `jump.secondStrength` | 0.42 | 0.1–1.5 blocks/tick, before Jump Boost |
| `jump.thirdStrength` | 0.46 | 0.1–1.5 blocks/tick, before Jump Boost |
| `jump.allowInCreative` | true | Allow air jumps while Creative flight is inactive |
| `jump.landingConfirmationTicks` | 2 | 2–10 consecutive supported landing ticks |
| `sprint.activationDelayTicks` | 20 | 0–1200 eligible ticks before ramp starts |
| `sprint.accelerationTicks` | 60 | 1–1200 additional ticks to maximum |
| `sprint.maximumMultiplier` | 1.35 | 1.0–2.0 multiplier over normal modified speed |
| `sprint.decelerationPerTick` | 0.025 | 0.001–1.0 multiplier points lost per ineligible tick |
| `sprint.particles` | false | Sparse quiet sprint trails, subject to general particle permission |
| `dash.horizontalImpulse` | 1.2 | 0.1–3.0 blocks/tick; diagonal input is normalized |
| `dash.cooldownTicks` | 40 | 1–1200 server ticks |
| `dash.allowAirDash` | true | Allow dash without valid ground contact |
| `dash.verticalImpulse` | 0 | −0.5–0.5 added blocks/tick |
| `client.showHud` | true | Local HUD visibility |
| `client.particles` | true | Local opt-out from Momentum particles |
| `client.sounds` | true | Local opt-out from Momentum sounds |
| `client.cooldownFeedback` | true | Local action-bar feedback on a dash attempt during cooldown |

To use double jump only, set `general.tripleJump` to false and keep `general.doubleJump` true. To turn off all extra jumps, set both false or set `jump.maximumTotalJumps` to 1. Triple mode includes the second jump even when the separate double-mode switch is false.

The server synchronizes enforced settings at login and lifecycle resets. Its movement settings override local movement settings. The `client` section is always read from the local instance; a server cannot force a HUD, particles, or sounds on someone who opted out. Cooldowns are displayed from server snapshots without speculative countdowns, so the HUD can trail by up to four server ticks plus network latency.

## Multiplayer implementation

The server owns separate UUID-indexed sessions. Jump requests carry press/release edges and a pre-movement air-jump intention; dash requests contain only bounded forward/strafe input. The server chooses strengths, yaw, counters, multipliers, and cooldowns. Monotonic request sequences, per-tick execution guards, and session epochs reject replayed requests and stale packets after respawn or dimension changes. A processing budget limits accepted protocol traffic to eight requests per player per tick.

Landing requires a reported ground contact, a nonascending observed position, and actual block collision beneath the feet for at least two ticks. An accepted air jump must separate from physical support before a subsequent landing can restore jumps. Touching walls or ceilings cannot reset the counter. Same-world teleports greater than 16 blocks between server samples reset the transient session as well.

All entity/world mutations run in Fabric's server-thread networking callbacks or server tick events. Sprint uses vanilla replicated input and observed movement, with no per-tick custom client requests. State snapshots are sent every four server ticks and on action requests. Accepted impulses are sent separately so only the intended velocity components change on the client. There is **no speculative client movement prediction**: an ability takes effect after the server accepts it. This favors consistency; ability response time includes a network round trip.

This validates Momentum's abilities, not all movement from a modified client. Vanilla collision and movement validation remain in charge. Third-party anti-cheat mods can impose additional restrictions; their interoperability needs testing. The server never loads Momentum's client initializer, HUD, key mapping, or particle renderer. No Mixins are needed.

## Building on macOS Apple Silicon

Install an **ARM64 JDK 25** (for example, Temurin), then open this directory as a Gradle project in IntelliJ IDEA. Choose JDK 25 for both the project and Gradle JVM. Rider installations need Java/Gradle support; IntelliJ IDEA is the recommended Java IDE.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew clean build
./gradlew test
./gradlew runGameTest
./gradlew runClient
./gradlew runServer --args="nogui"
```

The wrapper verifies the Gradle distribution SHA-256. Build output is `build/libs/momentum-1.1.0-dev.jar`; `momentum-1.1.0-dev-sources.jar` contains source code. Server GameTests are also part of `build`. The separate test mod is excluded from the production JAR. On Windows use `gradlew.bat` with Java 25.

The ordinary dedicated run directory is `run/server/`. Review the [Minecraft EULA](https://aka.ms/MinecraftEULA) and set `eula=true` in that directory's generated `eula.txt` if you accept it. Normal server runs do not automatically accept it. GameTest servers do not require a normal dedicated-world EULA file. Client tests run with:

```bash
./gradlew runClientGameTest
```

They need an active graphics display and compatible renderer. Client tests create disposable singleplayer worlds; they do not replace the two-client latency checklist. If using Loom's dedicated-client test facility separately, `-PacceptMinecraftEula=true` enables its EULA file only when you explicitly provide that property.

An additional two-process smoke test is included. Run `./gradlew writeClientTestLaunch`, then start an isolated dedicated server in another terminal. After accepting its EULA, use a disposable peaceful flat world, default Momentum settings, `server-ip=127.0.0.1`, `server-port=25565`, `online-mode=false`, `white-list=false`, and `difficulty=peaceful` for the development identities. Then run:

```bash
python3 scripts/two-client-smoke.py
```

The harness launches real clients named MomentumA and MomentumB. It checks that both can use their air jumps and dash, both can reach maximum sprint boost, and stopping A leaves B's modifier intact. Start with fresh test identities or reset their positions close together and replenish food before rerunning; they must remain inside each other's entity-tracking range at the initial barrier. Results and screenshots go to `build/run/twoClients/`. It requires Python 3 and enough memory for two clients in addition to the server. The exported launch file contains local paths and must be regenerated on each development machine. This localhost fixture does not test authentication or injected latency; retain normal online authentication for an actual hosted server.

For headless Linux tests with two virtual X displays, use `--second-display :100` (with the first client's `DISPLAY` already set). Independent displays prevent SDL focus changes in one test window from clearing the other's held inputs. On this build host, software rendering needed `SDL_VIDEO_FORCE_EGL=1`, and X's MIT-SHM extension was disabled because tool processes have separate IPC namespaces.

## Behavior and limitations

- Abilities are disabled while dead, spectating, flying, gliding, riding, sleeping, in water/lava, climbing, inside powder snow, or affected by Levitation. Jump limits and cooldowns do not refresh merely by entering these states.
- Slow Falling continues to use vanilla gravity and fall rules. Jump Boost augments air jump strength; Speed composes with sprint's attribute modifier. Honey/slime behavior and collisions remain vanilla; a bounce does not grant a ground reset until a confirmed landing.
- Very short ground contacts below the configured confirmation duration deliberately do not restore air jumps. Repeated automatic bunny hopping can therefore retain a spent air-jump budget until the player lands long enough.
- Air jumps and dashes preserve accumulated fall distance. This is intentional and can make a visually successful last-second air jump followed by landing still inflict fall damage.
- Airborne sprint acceleration is disabled. Jumped horizontal momentum remains subject to normal drag and input. Dash has a fixed horizontal burst rather than stacking on top of an existing boost.
- High latency delays abilities because prediction is disabled. Mod compatibility, LAN, and injected-latency behavior require separate validation. See the verification report for the two-client smoke test's actual outcome.

## Troubleshooting

- **Wrong Java / unsupported class version:** configure both the launcher and Gradle to use Java 25.
- **Missing dependency at startup:** use Minecraft 26.3 and the pinned Fabric API; do not install a 1.21.x API JAR.
- **Only one jump:** release Space between presses, verify server settings, and check that the player is not in a restricted state.
- **Creative double-tap starts flight:** this is normal vanilla behavior. Flight takes precedence; air jumps never override active flight.
- **Dash points forward while stationary:** this is the intended fallback. Check the Dash key mapping for conflicts.
- **Apparent landing did not refresh jumps:** wait for at least two supported server ticks. Wall/ceiling contacts and slime bounces are not automatically valid landings.
- **Rubber-banding with another mod:** reduce impulses and sprint maximum, then compare with only Fabric API and Momentum installed. Report server/client logs and measured latency.
- **Config defaults returned:** inspect the log and `config/momentum.json.invalid`. Fix types/JSON syntax and restart.

## License

[MIT](LICENSE). Minecraft, Fabric, Java, and their dependencies retain their respective licenses. Minecraft game files are downloaded by Loom and are not included in the source archive or mod JAR.
