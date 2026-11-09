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
    private final MomentumConfig config;
    private long tick, nextEpoch;

    private static final class Session {
        final long epoch;
        final MovementState state = new MovementState();
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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, ConfigPayload.TYPE)) {
                handler.disconnect(Component.literal("Momentum 1.0.0 is required on the client."));
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
            session.state.observeGround(validGround(player, session.y), !hasSupport(player), tick, config.jump.landingConfirmationTicks);
            boolean restricted = restricted(player) || !config.general.sprintBoost;
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
        if (session == null) return;
        int number = session.state.requestJump(request.sequence(), request.pressed(), tick,
            !request.airborneIntent() || validGround(player, session.y), !restricted(player) && (!player.isCreative() || config.jump.allowInCreative), config.maximumJumps());
        if (number != 0) {
            double vertical = JumpHandler.apply(player, session.observedMovement, number, config);
            ServerPlayNetworking.send(player, new ActionPayload(session.epoch, number, 0, vertical, 0));
            effects(player, number);
            if (config.general.debugLogging) MomentumMod.LOGGER.info("Accepted jump {} for {} at tick {}", number, player.getUUID(), tick);
        }
        sync(player, session);
    }

    public void dash(ServerPlayer player, DashRequest request) {
        Session session = sessionForRequest(player, request.epoch());
        if (session == null) return;
        boolean valid = MovementMath.validInput(request.forward(), request.strafe());
        boolean allowed = valid && config.general.dash && !restricted(player)
            && (config.dash.allowAirDash || validGround(player, session.y));
        if (session.state.requestDash(request.sequence(), tick, allowed, config.dash.cooldownTicks)) {
            var velocity = DashHandler.apply(player, session.observedMovement, request.forward(), request.strafe(), config);
            ServerPlayNetworking.send(player, new ActionPayload(session.epoch, 4, velocity.x(), config.dash.verticalImpulse, velocity.z()));
            effects(player, 4);
            if (config.general.debugLogging) MomentumMod.LOGGER.info("Accepted dash for {} at tick {}", player.getUUID(), tick);
        }
        sync(player, session);
    }

    private Session sessionForRequest(ServerPlayer player, long epoch) {
        Session session = sessions.get(player.getUUID());
        if (session == null) return null;
        if (session.level != player.level()) { reset(player); return null; }
        if (epoch != session.epoch || !session.state.admitRequest(tick)) return null;
        return session;
    }

    private void effects(ServerPlayer player, int kind) {
        boolean particles = config.general.particles;
        boolean sounds = config.general.sounds && kind != 5;
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

    private void sync(ServerPlayer player, Session session) {
        ServerPlayNetworking.send(player, new StatePayload(session.epoch, tick, session.state.usedJumps(),
            config.maximumJumps(), session.state.dashCooldown(tick), player.onGround() ? session.state.sprintMultiplier() : 1, restricted(player)));
    }
}
