package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record JumpRequest(long epoch, long sequence, boolean pressed, boolean airborneIntent) implements CustomPacketPayload {
    public static final Type<JumpRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JumpRequest> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeLong(p.sequence); b.writeBoolean(p.pressed); b.writeBoolean(p.airborneIntent); },
        b -> new JumpRequest(b.readLong(), b.readLong(), b.readBoolean(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
