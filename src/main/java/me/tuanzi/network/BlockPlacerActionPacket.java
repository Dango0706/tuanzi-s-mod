package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 方块放置器 GUI 设置操作 C2S 数据包
 */
public record BlockPlacerActionPacket(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int ACTION_CYCLE_MODE = 0;
    public static final int ACTION_CYCLE_INTERVAL = 1;
    public static final int ACTION_TOGGLE_SNEAK = 2;
    public static final int ACTION_TOGGLE_EXTRACT = 3;

    public static final CustomPacketPayload.Type<BlockPlacerActionPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block_placer_action"));

    public static final StreamCodec<FriendlyByteBuf, BlockPlacerActionPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    BlockPlacerActionPacket::pos,
                    ByteBufCodecs.VAR_INT,
                    BlockPlacerActionPacket::action,
                    BlockPlacerActionPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
