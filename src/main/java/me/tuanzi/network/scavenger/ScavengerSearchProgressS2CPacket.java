package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.scavenger.loot.meta.ScavengerRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端下发给客户端的逐刻搜寻进度与品质高光阶段脉冲更新包（S2C）。
 */
public record ScavengerSearchProgressS2CPacket(
        BlockPos containerPos,
        int entryId,
        int progressTicks,
        int totalTicks,
        float pulseThreshold,
        ScavengerRarity rarity
) implements CustomPacketPayload {

    public static final Type<ScavengerSearchProgressS2CPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_search_progress_s2c"));

    public static final StreamCodec<FriendlyByteBuf, ScavengerSearchProgressS2CPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.containerPos());
                buf.writeVarInt(pkt.entryId());
                buf.writeVarInt(pkt.progressTicks());
                buf.writeVarInt(pkt.totalTicks());
                buf.writeFloat(pkt.pulseThreshold());
                ScavengerRarity.STREAM_CODEC.encode(buf, pkt.rarity());
            },
            buf -> new ScavengerSearchProgressS2CPacket(
                    buf.readBlockPos(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    ScavengerRarity.STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
