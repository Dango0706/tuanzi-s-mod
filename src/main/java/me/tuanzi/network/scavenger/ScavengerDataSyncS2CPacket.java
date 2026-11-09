package me.tuanzi.network.scavenger;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.scavenger.loot.meta.ScavengerContainerDef;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 拾荒回廊搜刮元数据热重载全量同步 S2C 网络包
 * 在玩家加入服务器或管理员执行 /reload 后下发，确保客户端 GUI 网格、品质边框、Tooltip 与公式完全对齐。
 */
public record ScavengerDataSyncS2CPacket(
        List<ScavengerItemMeta> items,
        List<ScavengerContainerDef> containers,
        ScavengerProgressionConfig progression
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ScavengerDataSyncS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_data_sync"));

    private static final StreamCodec<FriendlyByteBuf, List<ScavengerItemMeta>> ITEM_LIST_CODEC =
            ScavengerItemMeta.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<FriendlyByteBuf, List<ScavengerContainerDef>> CONTAINER_LIST_CODEC =
            ScavengerContainerDef.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<FriendlyByteBuf, ScavengerDataSyncS2CPacket> CODEC = StreamCodec.composite(
            ITEM_LIST_CODEC,
            ScavengerDataSyncS2CPacket::items,
            CONTAINER_LIST_CODEC,
            ScavengerDataSyncS2CPacket::containers,
            ScavengerProgressionConfig.STREAM_CODEC,
            ScavengerDataSyncS2CPacket::progression,
            ScavengerDataSyncS2CPacket::new
    );

    public ScavengerDataSyncS2CPacket {
        items = items == null ? List.of() : List.copyOf(items);
        containers = containers == null ? List.of() : List.copyOf(containers);
        progression = progression == null ? ScavengerProgressionConfig.createDefault() : progression;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
