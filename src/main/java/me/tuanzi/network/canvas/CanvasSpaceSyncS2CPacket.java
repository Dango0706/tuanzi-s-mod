package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 -> 客户端：同步当前玩家画布编辑空间边界、只读状态与模式信息。
 */
public record CanvasSpaceSyncS2CPacket(
        boolean inCanvasSpace,
        BlockPos origin,
        int sizeX,
        int sizeY,
        int sizeZ,
        boolean showBorder,
        boolean readOnly,
        boolean creativeMode
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CanvasSpaceSyncS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "canvas_space_sync"));

    public static final StreamCodec<FriendlyByteBuf, CanvasSpaceSyncS2CPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CanvasSpaceSyncS2CPacket::inCanvasSpace,
            BlockPos.STREAM_CODEC, CanvasSpaceSyncS2CPacket::origin,
            ByteBufCodecs.VAR_INT, CanvasSpaceSyncS2CPacket::sizeX,
            ByteBufCodecs.VAR_INT, CanvasSpaceSyncS2CPacket::sizeY,
            ByteBufCodecs.VAR_INT, CanvasSpaceSyncS2CPacket::sizeZ,
            ByteBufCodecs.BOOL, CanvasSpaceSyncS2CPacket::showBorder,
            ByteBufCodecs.BOOL, CanvasSpaceSyncS2CPacket::readOnly,
            ByteBufCodecs.BOOL, CanvasSpaceSyncS2CPacket::creativeMode,
            CanvasSpaceSyncS2CPacket::new
    );

    public static CanvasSpaceSyncS2CPacket cleared() {
        return new CanvasSpaceSyncS2CPacket(false, BlockPos.ZERO, 0, 0, 0, false, false, false);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
