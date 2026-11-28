package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：3D 打印机操作请求（选择缩放倍率 / 执行打印）。
 */
public record Printer3DActionC2SPacket(BlockPos pos, int action, int scale) implements CustomPacketPayload {
    public static final int ACTION_SELECT_SCALE = 1;
    public static final int ACTION_PRINT = 2;

    public static final CustomPacketPayload.Type<Printer3DActionC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "printer_3d_action"));

    public static final StreamCodec<FriendlyByteBuf, Printer3DActionC2SPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Printer3DActionC2SPacket::pos,
            ByteBufCodecs.VAR_INT, Printer3DActionC2SPacket::action,
            ByteBufCodecs.VAR_INT, Printer3DActionC2SPacket::scale,
            Printer3DActionC2SPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
