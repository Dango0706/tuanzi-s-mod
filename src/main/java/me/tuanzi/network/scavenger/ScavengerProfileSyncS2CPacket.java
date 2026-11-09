package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 玩家个人拾荒装备档案与负重状态权威同步包（S2C）。
 */
public record ScavengerProfileSyncS2CPacket(
        ItemStack equippedBackpack,
        int backpackColumns,
        int backpackRows,
        List<GridItemEntry> backpackEntries,
        ItemStack equippedSafebox,
        int safeboxColumns,
        int safeboxRows,
        List<GridItemEntry> safeboxEntries,
        int weightLevel,
        int stashLevel,
        float totalWeightKg,
        boolean isOverloaded
) implements CustomPacketPayload {

    public static final Type<ScavengerProfileSyncS2CPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_profile_sync_s2c"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<GridItemEntry>> ENTRIES_STREAM_CODEC =
            GridItemEntry.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, ScavengerProfileSyncS2CPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pkt.equippedBackpack());
                buf.writeVarInt(pkt.backpackColumns());
                buf.writeVarInt(pkt.backpackRows());
                ENTRIES_STREAM_CODEC.encode(buf, pkt.backpackEntries());

                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pkt.equippedSafebox());
                buf.writeVarInt(pkt.safeboxColumns());
                buf.writeVarInt(pkt.safeboxRows());
                ENTRIES_STREAM_CODEC.encode(buf, pkt.safeboxEntries());

                buf.writeVarInt(pkt.weightLevel());
                buf.writeVarInt(pkt.stashLevel());
                buf.writeFloat(pkt.totalWeightKg());
                buf.writeBoolean(pkt.isOverloaded());
            },
            buf -> new ScavengerProfileSyncS2CPacket(
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    ENTRIES_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    ENTRIES_STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
