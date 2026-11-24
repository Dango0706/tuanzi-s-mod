package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 「背水一战」创伤值 S2C 同步数据包。
 */
public record DesperateStandSyncPacket(float trauma) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DesperateStandSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "desperate_stand_sync"));

    public static final StreamCodec<FriendlyByteBuf, DesperateStandSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            DesperateStandSyncPacket::trauma,
            DesperateStandSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
