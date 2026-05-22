package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChainMiningKeyPacket(boolean holding) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChainMiningKeyPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "chain_mining_key"));

    public static final StreamCodec<FriendlyByteBuf, ChainMiningKeyPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        ChainMiningKeyPacket::holding,
        ChainMiningKeyPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
