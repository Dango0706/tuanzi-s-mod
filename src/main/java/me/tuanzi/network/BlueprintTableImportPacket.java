package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public record BlueprintTableImportPacket(BlockPos tablePos, CompoundTag blueprintData, boolean updateExportSlot) implements CustomPacketPayload {
    public BlueprintTableImportPacket(BlockPos tablePos, CompoundTag blueprintData) {
        this(tablePos, blueprintData, false);
    }

    public static final int MAX_COMPRESSED_BYTES = 8 * 1024 * 1024;
    public static final long MAX_UNCOMPRESSED_NBT_BYTES = 32L * 1024 * 1024;

    public static final CustomPacketPayload.Type<BlueprintTableImportPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_table_import"));

    public static final StreamCodec<FriendlyByteBuf, CompoundTag> COMPRESSED_COMPOUND_TAG = new StreamCodec<>() {
        @Override
        public CompoundTag decode(FriendlyByteBuf buf) {
            byte[] compressed = buf.readByteArray(MAX_COMPRESSED_BYTES);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed)) {
                return NbtIo.readCompressed(bais, NbtAccounter.create(MAX_UNCOMPRESSED_NBT_BYTES));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to decompress blueprint NBT payload", e);
            }
        }

        @Override
        public void encode(FriendlyByteBuf buf, CompoundTag tag) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                NbtIo.writeCompressed(tag, baos);
                byte[] compressed = baos.toByteArray();
                if (compressed.length > MAX_COMPRESSED_BYTES) {
                    throw new IllegalArgumentException("Compressed blueprint payload exceeds limit: " + compressed.length);
                }
                buf.writeByteArray(compressed);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to compress blueprint NBT payload", e);
            }
        }
    };

    public static final StreamCodec<FriendlyByteBuf, BlueprintTableImportPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlueprintTableImportPacket::tablePos,
            COMPRESSED_COMPOUND_TAG, BlueprintTableImportPacket::blueprintData,
            ByteBufCodecs.BOOL, BlueprintTableImportPacket::updateExportSlot,
            BlueprintTableImportPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
