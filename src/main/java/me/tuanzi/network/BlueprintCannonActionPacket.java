package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BlueprintCannonActionPacket(BlockPos pos, String action, boolean destroyMode, boolean exactMatch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintCannonActionPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_cannon_action"));

    public static final StreamCodec<FriendlyByteBuf, BlueprintCannonActionPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BlueprintCannonActionPacket::pos,
        ByteBufCodecs.STRING_UTF8, BlueprintCannonActionPacket::action,
        ByteBufCodecs.BOOL, BlueprintCannonActionPacket::destroyMode,
        ByteBufCodecs.BOOL, BlueprintCannonActionPacket::exactMatch,
        BlueprintCannonActionPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
