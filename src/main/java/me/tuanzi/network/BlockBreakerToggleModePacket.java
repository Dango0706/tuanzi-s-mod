package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BlockBreakerToggleModePacket(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlockBreakerToggleModePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block_breaker_toggle_mode"));

    public static final StreamCodec<FriendlyByteBuf, BlockBreakerToggleModePacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    BlockBreakerToggleModePacket::pos,
                    BlockBreakerToggleModePacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
