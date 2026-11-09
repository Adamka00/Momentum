package hu.adamka00.momentum.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record ConfigPayload(long epoch, String json) implements CustomPacketPayload {
    public static final Type<ConfigPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("momentum", "config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.of(
        (b, p) -> { b.writeLong(p.epoch); b.writeUtf(p.json, 8192); },
        b -> new ConfigPayload(b.readLong(), b.readUtf(8192)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
