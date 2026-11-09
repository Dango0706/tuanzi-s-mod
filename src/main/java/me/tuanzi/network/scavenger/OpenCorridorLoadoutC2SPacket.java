package me.tuanzi.network.scavenger;

import io.netty.buffer.ByteBuf;
import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端在拾荒回廊维度按下 E 键时，请求打开战术出装与随身背负装备网格界面（C2S）。
 */
public record OpenCorridorLoadoutC2SPacket() implements CustomPacketPayload {

    public static final Type<OpenCorridorLoadoutC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "open_corridor_loadout_c2s"));

    public static final StreamCodec<ByteBuf, OpenCorridorLoadoutC2SPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {},
            buf -> new OpenCorridorLoadoutC2SPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
