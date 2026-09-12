package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChromaticSkullRequestPacket(String playerName, boolean isMainHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChromaticSkullRequestPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "chromatic_skull_request"));

    public static final StreamCodec<FriendlyByteBuf, ChromaticSkullRequestPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        ChromaticSkullRequestPacket::playerName,
        ByteBufCodecs.BOOL,
        ChromaticSkullRequestPacket::isMainHand,
        ChromaticSkullRequestPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
