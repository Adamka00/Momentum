# Local verification on macOS Apple Silicon

This is **1.1.0-dev source, not a verified build or release**. The cloud's Fabric Maven proxy returns HTTP 403; its configuration service could not confirm the requested official-host additions. No restriction was bypassed. The 45 passing Java core tests do not establish Minecraft compatibility. No JAR is supplied because the full build has not compiled.

Keep your working 1.0.0 JAR and back up `config/momentum.json` and any worlds. Use a disposable test world. Work from `feature/momentum-1.1.0` in your existing checkout. The WIP source archive contains current source and wrapper files, without `.git`, credentials, downloaded dependencies or worlds; compare it with your local work before copying files. Do not overwrite unrelated local changes. The original main history remains intact.

Use an ARM64 JDK 25 and run these commands **from the Momentum project root**. Do not source the cloud's `/workspace/.tools/java-env.sh` on your Mac.

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew --version
./gradlew clean build
./gradlew test
./gradlew runGameTest
```

These are required local checks, **not claimed passing here**. Retain complete error output if compilation fails; the cloud could not type-check Minecraft-dependent APIs. In particular, verify the native widgets/render extraction, source-set dependency configuration, HUD transforms and integrated-server calls. No API stubs were added to make tests pass. Build outputs, if successful, are `build/libs/momentum-1.1.0-dev.jar` and its source JAR. Inspect production JAR contents for absence of test classes before using it.

For dependency-light tests independently of Loom, Python 3 is also needed:

```bash
bash scripts/test-core.sh
```

To exercise a real client **without Mod Menu**, then with the optional dependency:

```bash
./gradlew runClient
./gradlew -PwithModMenu=true runClient
```

Mod Menu 21.0.0's official tagged source targets Minecraft 26.3. Its adapter API was inspected, but artifact resolution and optional loading have not run here. An installed Mod Menu runtime may also need its declared Placeholder API dependency; allow Gradle to resolve the published POM's transitive requirements. Do not install unrelated Minecraft versions to silence dependency failures.

For a normal dedicated server:

```bash
./gradlew runServer --args="nogui"
```

Review the generated EULA under `run/server/` and accept it only if you agree, then rerun. This work did not accept it for you. Ordinary server startup is separate from GameTests. For the existing two-client fixture use the documented disposable, loopback-only server procedure in the README and regenerate `./gradlew writeClientTestLaunch`; it tests original movement regressions, and is not yet an automated acceptance test for the new mechanics.

## Required manual checks

- Verify Space double/triple jumps, R dash and sprint acceleration first. New arbitration deliberately allows only one mod impulse per tick: long jump > dash > wall/air jump > slide; vanilla ground jumps retain priority over extra jumps.
- Sprint forward and press crouch. Slide should start only with physical foot support and sufficient observed speed, decay without stacking, stop on release (default), collision/low speed, duration expiry or restricted states, and remove the sprint modifier. Repeat below a low ceiling. Vanilla crouch dimensions are the fallback; one-block tunnels are not supported.
- Test wall jumps in all four directions, corners, stairs/slabs, fences, ceilings, floors and empty/non-solid space. One press should choose wall jump before an available air jump; held keys, replayed packets and direction changes must not refill the shared airtime budget. Only a confirmed supported landing replenishes it.
- Sprint + G should replace horizontal speed and use configured vertical impulse plus Jump Boost (capped at 3 blocks/tick). Speed affects running before takeoff, not the impulse magnitude. Long jump consumes the ground jump; remaining double/triple jumps must remain correct. Try G and R together in either packet order. Neither ability may clear fall distance.
- Press O without Mod Menu; verify every category, toggle, numeric value/range/unit, tooltip, scrolling/page navigation, Tab/Shift-Tab, GUI scale, validation, Apply, Save & close, Cancel/discard and reset confirmations. O must not open settings while typing in chat or another screen.
- In singleplayer, change gameplay and local visual settings; confirm save/reopen/restart persistence, live application and resynchronization without replenishing spent jumps/cooldowns. Verify failed saves give visible failure and retain the editable draft. No remote-edit packet exists.
- On a dedicated server, verify displayed gameplay values match that server and are read-only, including resets. Local HUD changes must persist without replacing local gameplay values with the remote server's configuration. Repeat with two simultaneous clients; one client's actions and preferences must not change the other's counters or settings.
- With Mod Menu installed, open Configure and verify it opens the same native screen. Repeat startup without Mod Menu. Switch English/Hungarian and inspect labels and tooltips, including long Hungarian labels.
- Verify authoritative HUD counters/cooldowns, position, scale, opacity, F1, spectator/death/menu hiding and global/local particle/sound settings. New ability snapshots should be absent rather than speculative before synchronization.
- Exercise death/respawn, dimension changes, >16-block same-world teleports, disconnect/reconnect, water/lava, climbing, elytra, flight, vehicles and shutdown. Confirm no stuck slide, forced unsafe pose, retained sprint modifier or stale-epoch action.

## Known remaining work

`allowSlideJump=false` currently gates mod jump handling but cannot suppress vanilla's ordinary ground jump. Vanilla crouch/camera transitions are used rather than a custom slide posture/camera. The new wall-query/codec GameTests are written but unrun; additional interaction, low-ceiling, lifecycle and real-client scenarios are still needed. Do not mark the Movement Update complete or release it until these gaps and all full-build/runtime failures are resolved.
