package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DoubleJumpC2SPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DoubleJumpC2SPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "double_jump"));

    public static final StreamCodec<FriendlyByteBuf, DoubleJumpC2SPacket> CODEC = StreamCodec.unit(new DoubleJumpC2SPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
