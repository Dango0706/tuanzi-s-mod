package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldSculptorsPenModePacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WorldSculptorsPenModePacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "world_sculptors_pen_mode"));

    public static final StreamCodec<FriendlyByteBuf, WorldSculptorsPenModePacket> CODEC = 
        StreamCodec.unit(new WorldSculptorsPenModePacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
