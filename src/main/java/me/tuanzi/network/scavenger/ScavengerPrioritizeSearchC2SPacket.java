package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端点击某未揭示条目时，请求服务端将其插队置顶为当前首选搜索目标（C2S）。
 */
public record ScavengerPrioritizeSearchC2SPacket(
        BlockPos containerPos,
        int entryId
) implements CustomPacketPayload {

    public static final Type<ScavengerPrioritizeSearchC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_prioritize_search_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScavengerPrioritizeSearchC2SPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.containerPos());
                buf.writeVarInt(pkt.entryId());
            },
            buf -> new ScavengerPrioritizeSearchC2SPacket(
                    buf.readBlockPos(),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
