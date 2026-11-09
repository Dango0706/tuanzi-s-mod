package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NodachiSyncPacket(int momentum, int exhaustionTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NodachiSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "nodachi_sync"));

    public static final StreamCodec<FriendlyByteBuf, NodachiSyncPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        NodachiSyncPacket::momentum,
        ByteBufCodecs.VAR_INT,
        NodachiSyncPacket::exhaustionTicks,
        NodachiSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
