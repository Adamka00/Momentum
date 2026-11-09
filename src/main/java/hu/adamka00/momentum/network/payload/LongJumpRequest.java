package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record LongJumpRequest(long epoch, long sequence, boolean pressed) implements CustomPacketPayload {
    public static final Type<LongJumpRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "long_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LongJumpRequest> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeLong(p.sequence); b.writeBoolean(p.pressed); },
        b -> new LongJumpRequest(b.readLong(), b.readLong(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
