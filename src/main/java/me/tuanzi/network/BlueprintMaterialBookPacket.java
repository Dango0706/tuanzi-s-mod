package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求将材料清单写入大炮书槽中的书与笔
 */
public record BlueprintMaterialBookPacket(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintMaterialBookPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_material_book"));

    public static final StreamCodec<FriendlyByteBuf, BlueprintMaterialBookPacket> CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlueprintMaterialBookPacket::pos,
            BlueprintMaterialBookPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
