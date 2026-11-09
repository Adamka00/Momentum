# Manual multiplayer checklist

These are procedures, **not claims of successful manual testing**. Record the actual result, mod versions, latency, and logs. Test with only Fabric API and Momentum first. See `VERIFICATION.md` for automated results.

Use Minecraft 26.3, Java 25, Loader 0.20.1+, API 0.166.0+26.3, and Momentum 1.0.0 on both client A and client B. Use a separate dedicated server with the same mods. Back up any valuable worlds.

## Singleplayer and controls

- [ ] Vanilla first jump is unchanged. Holding Space performs no extra air jumps.
- [ ] Each release/press adds one air jump; the fourth total jump fails.
- [ ] Disable triple mode: only the second total jump succeeds. Disable both jump modes: no air jumps.
- [ ] Change strengths, restart, and confirm server-enforced values.
- [ ] Rebind Dash; holding the dash key does not repeat. Forward/back/side/diagonal input works, with equal diagonal magnitude.
- [ ] No input dashes horizontally toward yaw, even while looking up/down.
- [ ] HUD scales with GUI scale/resolution, hides with F1/menus/death/spectator, and respects local visibility.
- [ ] English/Hungarian labels, sounds, particles, and local cosmetic opt-outs work.
- [ ] Rejected jumps/dashes play no success sound or particles.

## Dedicated server and two clients

- [ ] Normal `runServer --args="nogui"` starts after the operator accepts the EULA, with no client-class errors.
- [ ] Client A and B connect simultaneously and both receive server configuration.
- [ ] A jumps twice in the air while B has an unused counter; B still has both air jumps.
- [ ] A dashes while B dashes in the same server tick; both succeed independently.
- [ ] A exhausts dash cooldown and repeatedly presses Dash; B can still dash immediately.
- [ ] A and B sprint for 80 eligible ticks; each reaches 1.35 independently. Stopping A does not affect B.
- [ ] Both players see the other's motion and accepted effects without unexpected corrections.
- [ ] A client missing Momentum is rejected clearly. A client on a vanilla server gets no Momentum abilities.
- [ ] Repeat with a LAN host and one joining player.

## Networking and abuse

- [ ] At 100 ms and 250 ms added round-trip latency, jumps/dashes wait for acceptance without double execution. Record response delay and any rubber-banding.
- [ ] Under server tick lag, cooldown still lasts exactly 40 server ticks; it does not expire early in wall-clock time.
- [ ] Replay identical sequences, send newer sequences in one tick, and resend packets after cooldown: limits still hold.
- [ ] Send invalid dash directions outside −1..1: no impulse or cooldown expenditure.
- [ ] Repeated supported/unsupported `onGround` transitions, wall contact, ceilings, and brief contacts cannot create infinite air jumps.
- [ ] Delay an accepted impulse while the player remains near support; no false landing refresh occurs before actual separation.
- [ ] Send old-epoch packets after death, respawn, dimension change, teleport, and reconnect: they do nothing.
- [ ] High packet rates do not produce more than one jump/dash per tick or unbounded response packets.

## Lifecycle and movement interactions

- [ ] Death/respawn resets transient state and removes the speed modifier.
- [ ] Nether/End transitions and same-world teleports behave consistently, without crashes or stale impulses.
- [ ] Disconnect/reconnect removes old state; long repeated join/leave tests do not grow retained player sessions.
- [ ] Sprint for several minutes, stop, disconnect, and restart: base movement speed is unchanged.
- [ ] Walking off a ledge leaves two default air jumps; touching a wall or ceiling restores none.
- [ ] Land on full blocks, slabs, stairs, snow layers, slime, and honey. Only confirmed supported landings refresh jumps.
- [ ] Bounce on slime and slide down honey; do not gain unlimited jumps from transient contacts.
- [ ] Enter water/lava, climb ladders/vines, ride a vehicle, use elytra, fly in Creative, or become a spectator: abilities are disabled appropriately.
- [ ] Creative double-tap flight remains usable; air jumps never override active flight. Adventure mode has normal ability behavior.
- [ ] Levitation disables abilities. Slow Falling retains vanilla gravity and fall rules. Jump Boost affects air-jump strength; Speed still affects normal sprint speed.
- [ ] Dash while jumping and during sprint boost; cooldown and jump counter remain independent.
- [ ] Dash directly into walls, corners, doors, fences, and low ceilings: no collision bypass.
- [ ] Fall from a high place and air-jump/dash late; fall distance is not cleared and fall damage is still possible.
- [ ] Repeat core controls and rendering on macOS Apple Silicon with an ARM64 JDK 25.
