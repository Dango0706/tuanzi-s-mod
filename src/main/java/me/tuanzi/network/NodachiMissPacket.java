package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NodachiMissPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NodachiMissPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "nodachi_miss"));

    public static final StreamCodec<FriendlyByteBuf, NodachiMissPacket> CODEC = StreamCodec.unit(new NodachiMissPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
