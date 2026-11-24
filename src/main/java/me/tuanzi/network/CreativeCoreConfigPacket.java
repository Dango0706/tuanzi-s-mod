package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CreativeCoreConfigPacket(int intervalTicks, int shotsPerCycle, boolean isMainHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CreativeCoreConfigPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "creative_core_config"));

    public static final StreamCodec<FriendlyByteBuf, CreativeCoreConfigPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CreativeCoreConfigPacket::intervalTicks,
            ByteBufCodecs.VAR_INT,
            CreativeCoreConfigPacket::shotsPerCycle,
            ByteBufCodecs.BOOL,
            CreativeCoreConfigPacket::isMainHand,
            CreativeCoreConfigPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
