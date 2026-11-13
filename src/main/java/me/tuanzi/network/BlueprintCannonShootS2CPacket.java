package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 蓝图大炮开火发射 S2C 网络数据包
 * 通知附近客户端某个大炮发射了方块，客户端立即触发转向、后坐力、炮口烟雾与抛物线飞行方块渲染。
 */
public record BlueprintCannonShootS2CPacket(
        BlockPos cannonPos,
        BlockPos targetPos,
        int blockStateId,
        int totalTicks,
        boolean isDestroy
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BlueprintCannonShootS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_cannon_shoot"));

    public static final StreamCodec<FriendlyByteBuf, BlueprintCannonShootS2CPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlueprintCannonShootS2CPacket::cannonPos,
            BlockPos.STREAM_CODEC, BlueprintCannonShootS2CPacket::targetPos,
            ByteBufCodecs.VAR_INT, BlueprintCannonShootS2CPacket::blockStateId,
            ByteBufCodecs.VAR_INT, BlueprintCannonShootS2CPacket::totalTicks,
            ByteBufCodecs.BOOL, BlueprintCannonShootS2CPacket::isDestroy,
            BlueprintCannonShootS2CPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
