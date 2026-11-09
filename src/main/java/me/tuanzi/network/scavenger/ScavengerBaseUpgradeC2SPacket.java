package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端在基地整备台界面点击升级时，向服务端发起升级请求（C2S）。
 */
public record ScavengerBaseUpgradeC2SPacket(
        BlockPos stationPos,
        UpgradeTarget target
) implements CustomPacketPayload {

    public static final Type<ScavengerBaseUpgradeC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_base_upgrade_c2s"));

    public enum UpgradeTarget {
        WEIGHT_SYSTEM,
        BASE_STASH
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ScavengerBaseUpgradeC2SPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.stationPos());
                buf.writeEnum(pkt.target());
            },
            buf -> new ScavengerBaseUpgradeC2SPacket(
                    buf.readBlockPos(),
                    buf.readEnum(UpgradeTarget.class)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
