package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenBookScreenS2CPacket(boolean isMainHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBookScreenS2CPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "open_book_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenBookScreenS2CPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        OpenBookScreenS2CPacket::isMainHand,
        OpenBookScreenS2CPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
