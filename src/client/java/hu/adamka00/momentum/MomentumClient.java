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
    private KeyMapping dashKey;
    private MomentumConfig.Client preferences;
    @Override public void onInitializeClient() {
        preferences = ConfigManager.load(FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), MomentumMod.LOGGER).client;
        dashKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.momentum.dash", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("momentum", "movement"))));
        ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.TYPE, (payload, context) -> {
            enforced = ConfigManager.GSON.fromJson(payload.json(), MomentumConfig.class);
            enforced.validate();
            epoch = payload.epoch(); sequence = 0; jumpHeld = context.client().options.keyJump.isDown();
            dashHeld = dashKey.isDown(); pendingJumpChange = false; state = null;
        });
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) -> {
            if (payload.epoch() == epoch && (state == null || payload.tick() >= state.tick())) state = payload;
        });
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE, (payload, context) -> MovementEffects.play(context.client(), payload, preferences));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("momentum", "movement"),
            (graphics, delta) -> MovementHud.draw(graphics, state, enforced, preferences));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { epoch = -1; state = null; jumpHeld = false; });
        ClientPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            var player = context.client().player;
            if (player == null || payload.epoch() != epoch || !player.isAlive() || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying() || player.isSpectator()) return;
            var velocity = player.getDeltaMovement();
            if (payload.kind() == 2 || payload.kind() == 3) player.setDeltaMovement(velocity.x, payload.y(), velocity.z);
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
            if (client.player == null || epoch < 0) return;
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
