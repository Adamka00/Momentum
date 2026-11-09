package hu.adamka00.momentum.network;
import hu.adamka00.momentum.movement.MovementManager;
import hu.adamka00.momentum.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
public final class MomentumNetworking {
    private MomentumNetworking() { }
    public static void register(MovementManager manager) {
        PayloadTypeRegistry.serverboundPlay().register(LongJumpRequest.TYPE, LongJumpRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AbilityStatePayload.TYPE, AbilityStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(JumpRequest.TYPE, JumpRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DashRequest.TYPE, DashRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigPayload.TYPE, ConfigPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActionPayload.TYPE, ActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EffectPayload.TYPE, EffectPayload.CODEC);
        // Fabric invokes these handlers on the server thread (including integrated servers).
        ServerPlayNetworking.registerGlobalReceiver(LongJumpRequest.TYPE, (payload, context) -> manager.longJump(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(JumpRequest.TYPE, (payload, context) -> manager.jump(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(DashRequest.TYPE, (payload, context) -> manager.dash(context.player(), payload));
    }
}
