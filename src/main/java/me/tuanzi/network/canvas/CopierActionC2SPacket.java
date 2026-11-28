package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：复印机执行复制请求。
 */
public record CopierActionC2SPacket(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int ACTION_COPY = 1;

    public static final CustomPacketPayload.Type<CopierActionC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "copier_action"));

    public static final StreamCodec<FriendlyByteBuf, CopierActionC2SPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CopierActionC2SPacket::pos,
            ByteBufCodecs.VAR_INT, CopierActionC2SPacket::action,
            CopierActionC2SPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
