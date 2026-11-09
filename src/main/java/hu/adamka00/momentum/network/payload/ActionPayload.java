package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** An accepted server action. Kind 2/3 replaces y only; kind 4 replaces x/z and adds y. */
public record ActionPayload(long epoch, int kind, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeVarInt(p.kind); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); },
        b -> new ActionPayload(b.readLong(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
