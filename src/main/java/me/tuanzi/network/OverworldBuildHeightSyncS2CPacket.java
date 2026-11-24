package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 主世界最大建筑高度 S2C 热更新同步数据包。
 */
public record OverworldBuildHeightSyncS2CPacket(int overworldMaxBuildHeight) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OverworldBuildHeightSyncS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "overworld_build_height_sync"));

    public static final StreamCodec<FriendlyByteBuf, OverworldBuildHeightSyncS2CPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OverworldBuildHeightSyncS2CPacket::overworldMaxBuildHeight,
            OverworldBuildHeightSyncS2CPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
