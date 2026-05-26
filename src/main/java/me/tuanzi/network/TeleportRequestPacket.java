package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeleportRequestPacket(int slotIdx, boolean isMainHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleportRequestPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "teleport_request"));

    public static final StreamCodec<FriendlyByteBuf, TeleportRequestPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TeleportRequestPacket::slotIdx,
        ByteBufCodecs.BOOL,
        TeleportRequestPacket::isMainHand,
        TeleportRequestPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
