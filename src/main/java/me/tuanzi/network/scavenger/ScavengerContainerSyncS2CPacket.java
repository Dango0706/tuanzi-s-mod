package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 服务端下发给客户端的搜刮容器全量脱敏快照（S2C）。
 */
public record ScavengerContainerSyncS2CPacket(
        Identifier containerDefId,
        BlockPos containerPos,
        int columns,
        int rows,
        List<RedactedGridEntryView> entries
) implements CustomPacketPayload {

    public static final Type<ScavengerContainerSyncS2CPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_container_sync_s2c"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<RedactedGridEntryView>> ENTRIES_STREAM_CODEC =
            RedactedGridEntryView.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, ScavengerContainerSyncS2CPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                Identifier.STREAM_CODEC.encode(buf, pkt.containerDefId());
                buf.writeBlockPos(pkt.containerPos());
                buf.writeVarInt(pkt.columns());
                buf.writeVarInt(pkt.rows());
                ENTRIES_STREAM_CODEC.encode(buf, pkt.entries());
            },
            buf -> new ScavengerContainerSyncS2CPacket(
                    Identifier.STREAM_CODEC.decode(buf),
                    buf.readBlockPos(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    ENTRIES_STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
