package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 拾荒回廊专属饰品槽 S2C 同步数据包
 * 向客户端同步槽位 0 (归厅信标)、槽位 1 (归室信标) 与槽位 2 (归市信标) 的物品数据。
 */
public record CorridorAccessorySyncPacket(ItemStack hallStack, ItemStack roomStack,
                                          ItemStack marketStack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CorridorAccessorySyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "corridor_accessory_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CorridorAccessorySyncPacket> CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            CorridorAccessorySyncPacket::hallStack,
            ItemStack.OPTIONAL_STREAM_CODEC,
            CorridorAccessorySyncPacket::roomStack,
            ItemStack.OPTIONAL_STREAM_CODEC,
            CorridorAccessorySyncPacket::marketStack,
            CorridorAccessorySyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
