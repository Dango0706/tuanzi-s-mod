package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PaintBucketColorPacket(int rgb, boolean isMainHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PaintBucketColorPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "paint_bucket_color"));

    public static final StreamCodec<FriendlyByteBuf, PaintBucketColorPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        PaintBucketColorPacket::rgb,
        ByteBufCodecs.BOOL,
        PaintBucketColorPacket::isMainHand,
        PaintBucketColorPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
