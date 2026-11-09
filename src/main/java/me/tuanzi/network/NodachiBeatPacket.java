package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端玩家使用野太刀攻击时向服务端发送的出刀节奏窗口状态包
 *
 * @param onBeat 本次出刀是否正处于准星节拍器的高亮 QTE 窗口内且冷却已满 100%
 */
public record NodachiBeatPacket(boolean onBeat) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NodachiBeatPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "nodachi_beat"));

    public static final StreamCodec<FriendlyByteBuf, NodachiBeatPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            NodachiBeatPacket::onBeat,
            NodachiBeatPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
