package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：按需请求打印模型对应的压缩画布体素数据。
 */
public record CanvasDataRequestC2SPacket(String modelUuid, String dataHash) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CanvasDataRequestC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "canvas_data_request"));

    public static final StreamCodec<FriendlyByteBuf, CanvasDataRequestC2SPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CanvasDataRequestC2SPacket::modelUuid,
            ByteBufCodecs.STRING_UTF8, CanvasDataRequestC2SPacket::dataHash,
            CanvasDataRequestC2SPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
