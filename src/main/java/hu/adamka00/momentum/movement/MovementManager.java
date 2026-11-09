package hu.adamka00.momentum.movement;

import hu.adamka00.momentum.config.ConfigManager;
import hu.adamka00.momentum.config.MomentumConfig;
import hu.adamka00.momentum.MomentumMod;
import hu.adamka00.momentum.network.payload.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owned exclusively by the logical server thread. Entries hold no disconnected players. */
public final class MovementManager {
    private final Map<UUID, Session> sessions = new HashMap<>();
    private MomentumConfig config;
    private long tick, nextEpoch;

    private static final class Session {
        final long epoch;
        final MovementState state = new MovementState();
        final SlideState slide = new SlideState();
        final WallJumpState wall = new WallJumpState();
        final LongJumpState longJump = new LongJumpState();
        final java.util.List<Object> pending = new java.util.ArrayList<>(8);
        boolean wasSprinting;
        double slideX, slideZ;
        long lastJumpSequence = -1, lastDashSequence = -1, lastLongSequence = -1;
        final ServerLevel level;
        double x, y, z;
        net.minecraft.world.phys.Vec3 observedMovement = net.minecraft.world.phys.Vec3.ZERO;
        boolean dead;
        Session(long epoch, ServerPlayer player) {
            this.epoch = epoch; level = player.level();
            x = player.getX(); y = player.getY(); z = player.getZ();
        }
    }
    public MovementManager(MomentumConfig config) { this.config = config; }

    public void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> config = ConfigManager.load(
            net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), MomentumMod.LOGGER));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, AbilityStatePayload.TYPE)) {
                handler.disconnect(Component.translatable("message.momentum.protocol"));
                return;
            }
            reset(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SprintHandler.clear(handler.player); sessions.remove(handler.player.getUUID());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> { SprintHandler.clear(oldPlayer); reset(newPlayer); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> server.getPlayerList().getPlayers().forEach(SprintHandler::clear));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { sessions.clear(); tick = 0; });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick++;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) update(player);
        });
    }

    private Session reset(ServerPlayer player) {
        SprintHandler.clear(player);
        Session session = new Session(++nextEpoch, player);
        sessions.put(player.getUUID(), session);
        ServerPlayNetworking.send(player, new ConfigPayload(session.epoch, ConfigManager.GSON.toJson(config)));
        sync(player, session);
        return session;
    }

    private void update(ServerPlayer player) {
        Session session = sessions.get(player.getUUID());
        if (session == null) return;
        // Detect dimension and same-world teleports; stale in-flight requests use the old epoch.
        double dx = player.getX() - session.x, dy = player.getY() - session.y, dz = player.getZ() - session.z;
        if (session.level != player.level() || dx * dx + dy * dy + dz * dz > 256) {
            session = reset(player);
            dx = 0; dy = 0; dz = 0;
        }
        if (!player.isAlive()) {
            if (!session.dead) { session = reset(player); session.dead = true; }
        } else {
            session.observedMovement = new net.minecraft.world.phys.Vec3(dx, dy, dz);
            boolean wasGrounded = session.state.grounded();
            session.state.observeGround(validGround(player, session.y), !hasSupport(player), tick, config.jump.landingConfirmationTicks);
            if (!wasGrounded && session.state.grounded()) session.wall.confirmedLanding();
            boolean action = processPending(player, session);
            if (action) sync(player, session);
            boolean sprinting = player.isSprinting();
            boolean startSlide = !action && !restricted(player) && validGround(player, session.y)
                && (sprinting || session.wasSprinting);
            double speed = Math.hypot(dx, dz);
            if (session.slide.tick(tick, player.getLastClientInput().shift(), startSlide,
                !restricted(player) && player.onGround() && hasSupport(player), speed, config.slide)) {
                session.slideX = dx / speed; session.slideZ = dz / speed;
                effects(player, 6);
            }
            if (session.slide.active()) {
                SprintHandler.clear(player);
                impulse(player, session, 6, session.slideX * session.slide.speed(),
                    session.observedMovement.y, session.slideZ * session.slide.speed());
                if (tick % 8 == 0) effects(player, 9);
            }
            session.wasSprinting = sprinting;
            boolean restricted = restricted(player) || !config.general.sprintBoost || session.slide.active();
            var input = player.getLastClientInput();
            session.state.observeHorizontalMotion(dx * dx + dz * dz > 0.0001, tick);
            boolean eligible = !restricted && player.onGround() && hasSupport(player) && player.isSprinting()
                && input.forward() && !input.shift() && !player.horizontalCollision && session.state.hasRecentHorizontalMotion(tick);
            session.state.tickSprint(eligible, restricted, config.sprint.activationDelayTicks, config.sprint.accelerationTicks,
                config.sprint.maximumMultiplier, config.sprint.decelerationPerTick);
            // Airborne velocity remains vanilla; the ground acceleration modifier is removed immediately.
            SprintHandler.apply(player, !restricted && player.onGround() ? session.state.sprintMultiplier() : 1);
            if (config.sprint.particles && eligible && session.state.sprintMultiplier() > 1.05 && tick % 8 == 0) effects(player, 5);
        }
        session.x = player.getX(); session.y = player.getY(); session.z = player.getZ();
        if (tick % 4 == 0) sync(player, session);
    }

    public void jump(ServerPlayer player, JumpRequest request) {
        Session session = sessionForRequest(player, request.epoch());
        if (session != null && request.sequence() >= 0 && request.sequence() > session.lastJumpSequence) {
            session.lastJumpSequence = request.sequence(); session.pending.add(request);
        }
    }
    public void dash(ServerPlayer player, DashRequest request) {
        Session session = sessionForRequest(player, request.epoch());
        if (session != null && request.sequence() >= 0 && request.sequence() > session.lastDashSequence) {
            session.lastDashSequence = request.sequence(); session.pending.add(request);
        }
    }
    public void longJump(ServerPlayer player, LongJumpRequest request) {
        Session session = sessionForRequest(player, request.epoch());
        if (session != null && request.sequence() >= 0 && request.sequence() > session.lastLongSequence) {
            session.lastLongSequence = request.sequence(); session.pending.add(request);
        }
    }

    /** One impulse per server tick: long jump > dash > wall/air jump > slide.
     * All queued edges are consumed even if another action won. Queue is bounded by admitRequest. */
    private boolean processPending(ServerPlayer player, Session session) {
        boolean accepted = false;
        var input = player.getLastClientInput();
        boolean allowed = !restricted(player);
        boolean ground = validGround(player, session.y);
        for (Object value : session.pending) if (value instanceof LongJumpRequest request) {
            if (session.longJump.request(request.sequence(), request.pressed(), tick, !accepted && allowed && ground,
                player.isSprinting() || session.slide.active(), input.forward(),
                session.slide.active(), session.observedMovement.horizontalDistance(), config.longJump)) {
                var horizontal = MovementMath.dash(player.getYRot(), 1, (input.left() ? 1 : 0) - (input.right() ? 1 : 0), config.longJump.horizontalStrength);
                session.slide.cancel(); session.state.takeoff(tick, false);
                player.setOnGround(false); SprintHandler.clear(player);
                impulse(player, session, 8, horizontal.x(), Math.min(3, config.longJump.verticalStrength + player.getJumpBoostPower()), horizontal.z());
                effects(player, 8); accepted = true;
            }
        }
        for (Object value : session.pending) if (value instanceof DashRequest request) {
            boolean valid = !accepted && allowed && config.general.dash && MovementMath.validInput(request.forward(), request.strafe())
                && (config.dash.allowAirDash || ground);
            if (session.state.requestDash(request.sequence(), tick, valid, config.dash.cooldownTicks)) {
                session.slide.cancel();
                var velocity = DashHandler.apply(player, session.observedMovement, request.forward(), request.strafe(), config);
                ServerPlayNetworking.send(player, new ActionPayload(session.epoch, 4, velocity.x(), config.dash.verticalImpulse, velocity.z()));
                effects(player, 4); accepted = true;
            }
        }
        for (Object value : session.pending) if (value instanceof JumpRequest request) {
            boolean jumpAllowed = !accepted && allowed && (!player.isCreative() || config.jump.allowInCreative)
                && (!session.slide.active() || config.slide.allowSlideJump);
            boolean physicalGround = !request.airborneIntent() || ground;
            var wall = jumpAllowed && !physicalGround ? detectWall(player, config.wallJump.wallDetectionDistance) : null;
            boolean wallAccepted = session.wall.request(request.sequence(), request.pressed(), tick,
                jumpAllowed && !physicalGround, wall, config.wallJump);
            int number = session.state.requestJump(request.sequence(), request.pressed(), tick,
                physicalGround, jumpAllowed && !wallAccepted, config.maximumJumps());
            if (wallAccepted) {
                session.state.takeoff(tick, config.wallJump.resetAirJumpsOnWallJump);
                double x = wall == WallJumpState.Wall.EAST ? -1 : wall == WallJumpState.Wall.WEST ? 1 : 0;
                double z = wall == WallJumpState.Wall.SOUTH ? -1 : wall == WallJumpState.Wall.NORTH ? 1 : 0;
                player.setOnGround(false);
                impulse(player, session, 7, x * config.wallJump.horizontalStrength,
                    Math.min(3, config.wallJump.verticalStrength + player.getJumpBoostPower()), z * config.wallJump.horizontalStrength);
                effects(player, 7); accepted = true;
            } else if (number != 0) {
                double vertical = JumpHandler.apply(player, session.observedMovement, number, config);
                ServerPlayNetworking.send(player, new ActionPayload(session.epoch, number, 0, vertical, 0));
                effects(player, number); accepted = true;
            }
            if (accepted || (request.pressed() && physicalGround && config.slide.allowSlideJump)) session.slide.cancel();
        }
        session.pending.clear();
        return accepted;
    }

    private void impulse(ServerPlayer player, Session session, int kind, double x, double y, double z) {
        player.setDeltaMovement(x, y, z);
        ServerPlayNetworking.send(player, new ActionPayload(session.epoch, kind, x, y, z));
    }

    /** Four thin torso slabs exclude floors/ceilings and use actual voxel collision shapes.
     * A deterministic normal wins in corners; the shared airtime budget still applies. */
    static WallJumpState.Wall detectWall(ServerPlayer player, double distance) {
        var box = player.getBoundingBox();
        double low = box.minY + 0.15, high = box.maxY - 0.15;
        if (high <= low) return null;
        var queries = new net.minecraft.world.phys.AABB[] {
            new net.minecraft.world.phys.AABB(box.minX + 0.03, low, box.minZ - distance, box.maxX - 0.03, high, box.minZ),
            new net.minecraft.world.phys.AABB(box.minX + 0.03, low, box.maxZ, box.maxX - 0.03, high, box.maxZ + distance),
            new net.minecraft.world.phys.AABB(box.maxX, low, box.minZ + 0.03, box.maxX + distance, high, box.maxZ - 0.03),
            new net.minecraft.world.phys.AABB(box.minX - distance, low, box.minZ + 0.03, box.minX, high, box.maxZ - 0.03)
        };
        var normals = WallJumpState.Wall.values();
        for (int i = 0; i < queries.length; i++) if (player.level().getBlockCollisions(player, queries[i]).iterator().hasNext()) return normals[i];
        return null;
    }

    private Session sessionForRequest(ServerPlayer player, long epoch) {
        Session session = sessions.get(player.getUUID());
        if (session == null) return null;
        if (session.level != player.level()) { reset(player); return null; }
        if (epoch != session.epoch || !session.state.admitRequest(tick)) return null;
        return session;
    }

    private void effects(ServerPlayer player, int kind) {
        if (config.general.debugLogging && kind != 5 && kind != 9)
            MomentumMod.LOGGER.info("Accepted Momentum action {} for {} at tick {}", kind, player.getUUID(), tick);
        boolean localParticles = switch (kind) { case 4 -> config.dash.particles; case 6, 9 -> config.slide.particles; case 7 -> config.wallJump.particles; case 8 -> config.longJump.particles; default -> true; };
        boolean localSounds = switch (kind) { case 4 -> config.dash.sounds; case 6 -> config.slide.sounds; case 7 -> config.wallJump.sounds; case 8 -> config.longJump.sounds; case 5, 9 -> false; default -> true; };
        boolean particles = config.general.particles && localParticles;
        boolean sounds = config.general.sounds && localSounds;
        if (!particles && !sounds) return;
        var payload = new EffectPayload(player.getId(), kind, particles, sounds);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
            if (watcher != player && ServerPlayNetworking.canSend(watcher, EffectPayload.TYPE)) ServerPlayNetworking.send(watcher, payload);
        }
    }

    static boolean validGround(ServerPlayer player, double previousY) {
        if (!player.onGround() || player.getY() > previousY + 0.001) return false;
        return hasSupport(player);
    }

    private static boolean hasSupport(ServerPlayer player) {
        // Thin foot slab: excludes walls and ceilings, works with slabs, stairs and honey/slime tops.
        var box = player.getBoundingBox();
        var feet = new net.minecraft.world.phys.AABB(box.minX + 0.03, box.minY - 0.035, box.minZ + 0.03,
                                                   box.maxX - 0.03, box.minY, box.maxZ - 0.03);
        return player.level().getBlockCollisions(player, feet).iterator().hasNext();
    }

    public static boolean restricted(ServerPlayer player) {
        return !player.isAlive() || player.isSpectator() || player.getAbilities().flying || player.isFallFlying()
            || player.isPassenger() || player.isSleeping() || player.isInWater() || player.isInLava()
            || player.onClimbable() || player.isInPowderSnow || player.hasEffect(MobEffects.LEVITATION);
    }

    /** Called only by a trusted integrated-server task, never a clientbound authorization claim. */
    public void applyConfig(net.minecraft.server.MinecraftServer server, MomentumConfig replacement) {
        replacement.validate();
        config = replacement;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Session session = sessions.get(player.getUUID());
            if (session == null) continue;
            session.slide.cancel(); session.pending.clear();
            session.state.tickSprint(false, true, 0, 1, 1, 1);
            SprintHandler.clear(player);
            ServerPlayNetworking.send(player, new ConfigPayload(session.epoch, ConfigManager.GSON.toJson(config)));
            sync(player, session);
        }
    }

    private void sync(ServerPlayer player, Session session) {
        ServerPlayNetworking.send(player, new AbilityStatePayload(session.epoch, tick,
            config.wallJump.enabled ? session.wall.remaining(config.wallJump.maximumWallJumps) : 0,
            session.longJump.cooldown(tick), session.slide.remainingTicks(), session.slide.cooldown(tick)));
        ServerPlayNetworking.send(player, new StatePayload(session.epoch, tick, session.state.usedJumps(),
            config.maximumJumps(), session.state.dashCooldown(tick), player.onGround() ? session.state.sprintMultiplier() : 1, restricted(player)));
    }
}
