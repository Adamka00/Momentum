package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record DashRequest(long epoch, long sequence, int forward, int strafe) implements CustomPacketPayload {
    public static final Type<DashRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "dash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DashRequest> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeLong(p.sequence); b.writeByte(p.forward); b.writeByte(p.strafe); },
        b -> new DashRequest(b.readLong(), b.readLong(), b.readByte(), b.readByte()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
