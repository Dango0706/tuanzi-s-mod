package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BlueprintTableImportPacket(BlockPos tablePos, CompoundTag blueprintData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintTableImportPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_table_import"));

    public static final StreamCodec<FriendlyByteBuf, BlueprintTableImportPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BlueprintTableImportPacket::tablePos,
        ByteBufCodecs.COMPOUND_TAG, BlueprintTableImportPacket::blueprintData,
        BlueprintTableImportPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
