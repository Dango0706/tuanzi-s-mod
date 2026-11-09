package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 单条目搜索完成、迷雾消散时的物品揭示通知包（S2C）。
 */
public record ScavengerItemRevealedS2CPacket(
        BlockPos containerPos,
        int entryId,
        ItemStack stack
) implements CustomPacketPayload {

    public static final Type<ScavengerItemRevealedS2CPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_item_revealed_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScavengerItemRevealedS2CPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.containerPos());
                buf.writeVarInt(pkt.entryId());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pkt.stack());
            },
            buf -> new ScavengerItemRevealedS2CPacket(
                    buf.readBlockPos(),
                    buf.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
