package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 -> 客户端：分片下发压缩画布 .painted 字节流供打印模型烘焙静态网格。
 */
public record CanvasDataChunkS2CPacket(
        String modelUuid,
        String dataHash,
        int chunkIndex,
        int totalChunks,
        byte[] payload
) implements CustomPacketPayload {
    public static final int CHUNK_SIZE = 65536;
    public static final int MAX_PACKET_BYTES = 256 * 1024;

    public static final CustomPacketPayload.Type<CanvasDataChunkS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "canvas_data_chunk"));

    public static final StreamCodec<FriendlyByteBuf, CanvasDataChunkS2CPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CanvasDataChunkS2CPacket::modelUuid,
            ByteBufCodecs.STRING_UTF8, CanvasDataChunkS2CPacket::dataHash,
            ByteBufCodecs.VAR_INT, CanvasDataChunkS2CPacket::chunkIndex,
            ByteBufCodecs.VAR_INT, CanvasDataChunkS2CPacket::totalChunks,
            ByteBufCodecs.byteArray(MAX_PACKET_BYTES), CanvasDataChunkS2CPacket::payload,
            CanvasDataChunkS2CPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
