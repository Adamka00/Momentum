package hu.adamka00.momentum;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import hu.adamka00.momentum.config.*;
import hu.adamka00.momentum.network.payload.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import hu.adamka00.momentum.hud.MovementHud;
import hu.adamka00.momentum.effect.MovementEffects;
import net.minecraft.network.chat.Component;
public final class MomentumClient implements ClientModInitializer {
    private long epoch = -1, sequence;
    private boolean jumpHeld;
    private boolean dashHeld;
    private boolean pendingJumpChange, pendingJumpPressed, pendingAirborne;
    private MomentumConfig enforced = new MomentumConfig();
    private static StatePayload state;
    /** Immutable latest server snapshot, confined to the Minecraft client thread. */
    static StatePayload movementState() { return state; }
    private KeyMapping dashKey, longKey, settingsKey;
    private boolean longHeld;
    private static MomentumClient instance;
    private static AbilityStatePayload abilities;
    private MomentumConfig localConfig;
    public static MomentumClient instance() { return instance; }
    public MomentumConfig settings() {
        var result = ConfigManager.GSON.fromJson(ConfigManager.GSON.toJson(epoch >= 0 ? enforced : localConfig), MomentumConfig.class);
        result.client = ConfigManager.GSON.fromJson(ConfigManager.GSON.toJson(preferences), MomentumConfig.Client.class);
        return result;
    }
    public boolean gameplayReadOnly() { return net.minecraft.client.Minecraft.getInstance().player != null && net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null; }
    public void saveSettings(MomentumConfig edited, java.util.function.Consumer<Boolean> completion) {
        var client = net.minecraft.client.Minecraft.getInstance();
        edited.validate();
        var server = client.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                try {
                    MomentumMod.updateLocal(server, edited);
                    client.execute(() -> { localConfig = edited; preferences = edited.client; completion.accept(true); });
                } catch (Exception error) {
                    MomentumMod.LOGGER.error("Cannot apply Momentum settings", error);
                    client.execute(() -> completion.accept(false));
                }
            });
        } else {
            var saved = ConfigManager.GSON.fromJson(ConfigManager.GSON.toJson(localConfig), MomentumConfig.class);
            if (client.player == null) saved = edited;
            else saved.client = edited.client;
            try {
                ConfigManager.save(FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), saved);
                localConfig = saved; preferences = saved.client; completion.accept(true);
            } catch (java.io.IOException error) { MomentumMod.LOGGER.error("Cannot save Momentum settings", error); completion.accept(false); }
        }
    }
    private MomentumConfig.Client preferences;
    @Override public void onInitializeClient() {
        instance = this;
        localConfig = ConfigManager.load(FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), MomentumMod.LOGGER);
        preferences = localConfig.client;
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("momentum", "movement"));
        longKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.longJump", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G, category));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.settings", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_O, category));
        dashKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.dash", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R, category));
        ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.TYPE, (payload, context) -> {
            enforced = ConfigManager.GSON.fromJson(payload.json(), MomentumConfig.class);
            enforced.validate();
            if (epoch == payload.epoch()) return;
            abilities = null; longHeld = longKey.isDown();
            epoch = payload.epoch(); sequence = 0; jumpHeld = context.client().options.keyJump.isDown();
            dashHeld = dashKey.isDown(); pendingJumpChange = false; state = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(AbilityStatePayload.TYPE, (payload, context) -> {
            if (payload.epoch() == epoch && (abilities == null || payload.tick() >= abilities.tick())) abilities = payload;
        });
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) -> {
            if (payload.epoch() == epoch && (state == null || payload.tick() >= state.tick())) state = payload;
        });
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE, (payload, context) -> MovementEffects.play(context.client(), payload, preferences));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("momentum", "movement"),
            (graphics, delta) -> MovementHud.draw(graphics, state, abilities, enforced, preferences));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { epoch = -1; state = null; abilities = null; jumpHeld = false; longHeld = false; });
        ClientPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            var player = context.client().player;
            if (player == null || payload.epoch() != epoch || !player.isAlive() || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying() || player.isSpectator()
                || player.isSleeping() || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.isInPowderSnow || player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) return;
            if (payload.kind() == 6 && !player.onGround()) return;
            var velocity = player.getDeltaMovement();
            if (payload.kind() == 2 || payload.kind() == 3) player.setDeltaMovement(velocity.x, payload.y(), velocity.z);
            else if (payload.kind() >= 6 && payload.kind() <= 8) player.setDeltaMovement(payload.x(), payload.y(), payload.z());
            else if (payload.kind() == 4) player.setDeltaMovement(payload.x(), velocity.y + payload.y(), payload.z());
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || epoch < 0 || !ClientPlayNetworking.canSend(JumpRequest.TYPE)) return;
            boolean pressed = client.gui.screen() == null && client.options.keyJump.isDown();
            if (pressed != jumpHeld) {
                jumpHeld = pressed;
                pendingJumpChange = true; pendingJumpPressed = pressed; pendingAirborne = !client.player.onGround();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean openSettings = false;
            while (settingsKey.consumeClick()) openSettings = true;
            if (openSettings && client.gui.screen() == null) client.gui.setScreen(new hu.adamka00.momentum.gui.MomentumSettingsScreen(null));
            if (client.player == null || epoch < 0) return;
            boolean longPressed = client.gui.screen() == null && longKey.isDown();
            if (longPressed != longHeld && ClientPlayNetworking.canSend(LongJumpRequest.TYPE)) {
                ClientPlayNetworking.send(new LongJumpRequest(epoch, ++sequence, longPressed));
            }
            longHeld = longPressed;
            while (longKey.consumeClick()) { }

            if (pendingJumpChange) {
                pendingJumpChange = false;
                ClientPlayNetworking.send(new JumpRequest(epoch, ++sequence, pendingJumpPressed,
                    pendingAirborne && !client.player.getAbilities().flying));
            }
            boolean dashPressed = client.gui.screen() == null && dashKey.isDown();
            if (dashPressed && !dashHeld && ClientPlayNetworking.canSend(DashRequest.TYPE)) {
                // 26.3 uses Input accessors rather than the old forwardImpulse/leftImpulse fields.
                var input = client.player.input.keyPresses;
                int forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
                int strafe = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
                ClientPlayNetworking.send(new DashRequest(epoch, ++sequence, forward, strafe));
                if (preferences.cooldownFeedback && state != null && state.dashCooldown() > 0) {
                    client.player.sendOverlayMessage(Component.translatable("message.momentum.cooldown", state.dashCooldown()));
                }
            }
            dashHeld = dashPressed;
            while (dashKey.consumeClick()) { /* Drain OS key repeat; actions use rising edges only. */ }
        });
    }
}
