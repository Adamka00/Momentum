package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record StatePayload(long epoch, long tick, int usedJumps, int maximumJumps,
                           int dashCooldown, double sprintMultiplier, boolean restricted) implements CustomPacketPayload {
    public static final Type<StatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeLong(p.tick); b.writeVarInt(p.usedJumps); b.writeVarInt(p.maximumJumps);
                    b.writeVarInt(p.dashCooldown); b.writeDouble(p.sprintMultiplier); b.writeBoolean(p.restricted); },
        b -> new StatePayload(b.readLong(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
