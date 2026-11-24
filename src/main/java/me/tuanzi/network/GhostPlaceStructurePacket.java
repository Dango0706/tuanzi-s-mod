package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GhostPlaceStructurePacket(
        BlockPos basePos,
        BlockPos offset,
        int rotation,
        boolean mirrored,
        boolean useHeldBlueprint,
        CompoundTag blueprintData
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GhostPlaceStructurePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "ghost_place_structure"));

    public static final StreamCodec<FriendlyByteBuf, GhostPlaceStructurePacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GhostPlaceStructurePacket::basePos,
            BlockPos.STREAM_CODEC, GhostPlaceStructurePacket::offset,
            ByteBufCodecs.VAR_INT, GhostPlaceStructurePacket::rotation,
            ByteBufCodecs.BOOL, GhostPlaceStructurePacket::mirrored,
            ByteBufCodecs.BOOL, GhostPlaceStructurePacket::useHeldBlueprint,
            BlueprintTableImportPacket.COMPRESSED_COMPOUND_TAG, GhostPlaceStructurePacket::blueprintData,
            GhostPlaceStructurePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
