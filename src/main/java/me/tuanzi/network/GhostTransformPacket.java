package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GhostTransformPacket(BlockPos offset, int rotation, boolean mirrored) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GhostTransformPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "ghost_transform"));

    public static final StreamCodec<FriendlyByteBuf, GhostTransformPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, GhostTransformPacket::offset,
        ByteBufCodecs.VAR_INT, GhostTransformPacket::rotation,
        ByteBufCodecs.BOOL, GhostTransformPacket::mirrored,
        GhostTransformPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
