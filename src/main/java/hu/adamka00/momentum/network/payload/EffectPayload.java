package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Cosmetics only; never changes another player's state. Kinds 5 and 9 are quiet sprint/slide trails; 6/7/8 are slide/wall/long initiations. */
public record EffectPayload(int entityId, int kind, boolean particles, boolean sounds) implements CustomPacketPayload {
    public static final Type<EffectPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> CODEC = StreamCodec.of(
        (b, p) -> { b.writeVarInt(p.entityId); b.writeVarInt(p.kind); b.writeBoolean(p.particles); b.writeBoolean(p.sounds); },
        b -> new EffectPayload(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
