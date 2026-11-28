package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：画板界面操作请求。
 */
public record EaselActionC2SPacket(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int ACTION_SET_2D = 1;
    public static final int ACTION_SET_3D = 2;
    public static final int ACTION_ENTER_EDIT = 3;
    public static final int ACTION_SAVE_CANVAS = 4;
    public static final int ACTION_CYCLE_PERMISSION = 5;
    public static final int ACTION_RESET_CANVAS = 6;
    public static final int ACTION_EXPORT_CORRUPTED = 7;

    public static final CustomPacketPayload.Type<EaselActionC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "easel_action"));

    public static final StreamCodec<FriendlyByteBuf, EaselActionC2SPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EaselActionC2SPacket::pos,
            ByteBufCodecs.VAR_INT, EaselActionC2SPacket::action,
            EaselActionC2SPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
