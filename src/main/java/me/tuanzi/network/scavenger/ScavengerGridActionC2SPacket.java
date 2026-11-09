package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.scavenger.loot.meta.ScavengerSortOrder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端请求服务端执行的网格通用交互操作包（C2S）。
 * 覆盖：
 * <ul>
 *   <li>平移 / 旋转</li>
 *   <li>跨网格转移（放入/取出）</li>
 *   <li>单目标快速互换</li>
 *   <li>两阶段无损自动整理</li>
 *   <li>穿戴 / 卸下战术背包与安全箱</li>
 * </ul>
 */
public record ScavengerGridActionC2SPacket(
        ActionType actionType,
        GridSource source,
        GridSource destination,
        int entryId,
        int targetX,
        int targetY,
        boolean rotated,
        ScavengerSortOrder sortOrder
) implements CustomPacketPayload {

    public static final Type<ScavengerGridActionC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_grid_action_c2s"));

    public enum ActionType {
        MOVE_OR_ROTATE,
        QUICK_TRANSFER,
        AUTO_SORT,
        EQUIP_BACKPACK,
        UNEQUIP_BACKPACK,
        EQUIP_SAFEBOX,
        UNEQUIP_SAFEBOX,
        EQUIP_FROM_GRID,
        SWITCH_SAFEBOX
    }

    public enum GridSource {
        CONTAINER,
        BACKPACK,
        SAFEBOX,
        BASE_STASH,
        HOTBAR
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ScavengerGridActionC2SPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeEnum(pkt.actionType());
                buf.writeEnum(pkt.source());
                buf.writeEnum(pkt.destination());
                buf.writeVarInt(pkt.entryId());
                buf.writeVarInt(pkt.targetX());
                buf.writeVarInt(pkt.targetY());
                buf.writeBoolean(pkt.rotated());
                ScavengerSortOrder sort = pkt.sortOrder() != null ? pkt.sortOrder() : ScavengerSortOrder.BY_RARITY;
                ScavengerSortOrder.STREAM_CODEC.encode(buf, sort);
            },
            buf -> new ScavengerGridActionC2SPacket(
                    buf.readEnum(ActionType.class),
                    buf.readEnum(GridSource.class),
                    buf.readEnum(GridSource.class),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    ScavengerSortOrder.STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
