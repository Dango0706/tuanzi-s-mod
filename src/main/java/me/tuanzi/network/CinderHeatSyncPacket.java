package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 日冕过热 (Arena Heat Gauge) S2C 同步数据包
 */
public record CinderHeatSyncPacket(int heat, boolean inArena) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CinderHeatSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_heat_sync"));

    public static final StreamCodec<FriendlyByteBuf, CinderHeatSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CinderHeatSyncPacket::heat,
            ByteBufCodecs.BOOL,
            CinderHeatSyncPacket::inArena,
            CinderHeatSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
