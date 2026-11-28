package me.tuanzi.network.canvas;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 -> 服务端：碎纸机执行损毁画布请求（仅二次确认 {@code confirmed=true} 后生效）。
 */
public record PaperShredderActionC2SPacket(BlockPos pos, boolean confirmed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PaperShredderActionC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "paper_shredder_action"));

    public static final StreamCodec<FriendlyByteBuf, PaperShredderActionC2SPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PaperShredderActionC2SPacket::pos,
            ByteBufCodecs.BOOL, PaperShredderActionC2SPacket::confirmed,
            PaperShredderActionC2SPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
