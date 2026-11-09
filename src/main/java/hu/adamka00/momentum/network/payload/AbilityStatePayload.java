package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Versioned separately: 1.0 clients cannot accidentally decode a changed state layout. */
public record AbilityStatePayload(long epoch, long tick, int wallJumps, int longCooldown,
                                 int slideTicks, int slideCooldown) implements CustomPacketPayload {
    public static final Type<AbilityStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "abilities_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityStatePayload> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeLong(p.tick); b.writeVarInt(p.wallJumps); b.writeVarInt(p.longCooldown); b.writeVarInt(p.slideTicks); b.writeVarInt(p.slideCooldown); },
        b -> new AbilityStatePayload(b.readLong(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
