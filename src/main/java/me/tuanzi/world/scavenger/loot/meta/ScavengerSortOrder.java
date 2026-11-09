package me.tuanzi.world.scavenger.loot.meta;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Comparator;
import java.util.function.IntFunction;

/**
 * 拾荒回廊网格容器与仓库排序规则枚举
 */
public enum ScavengerSortOrder implements StringRepresentable {
    BY_RARITY("by_rarity", "scavenger.sort.tuanzis_mod.by_rarity"),
    BY_VALUE("by_value", "scavenger.sort.tuanzis_mod.by_value"),
    BY_VALUE_DENSITY("by_value_density", "scavenger.sort.tuanzis_mod.by_value_density"),
    BY_WEIGHT("by_weight", "scavenger.sort.tuanzis_mod.by_weight"),
    BY_CATEGORY("by_category", "scavenger.sort.tuanzis_mod.by_category");

    private static final IntFunction<ScavengerSortOrder> BY_ID = ByIdMap.continuous(
            Enum::ordinal,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, ScavengerSortOrder> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    /**
     * 确定性决胜比较链：品质降序 -> 基础价值降序 -> 占格面积降序 -> 类别枚举序 -> 注册ID字典序
     */
    private static final Comparator<ScavengerItemMeta> BASE_TIE_BREAKER = Comparator
            .comparingInt((ScavengerItemMeta m) -> m.rarity().ordinal()).reversed()
            .thenComparing(Comparator.comparingInt(ScavengerItemMeta::baseValue).reversed())
            .thenComparing(Comparator.comparingInt(ScavengerItemMeta::cellCount).reversed())
            .thenComparingInt(m -> m.category().ordinal())
            .thenComparing(ScavengerItemMeta::itemId);

    private final String serializedName;
    private final String translationKey;

    ScavengerSortOrder(String serializedName, String translationKey) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey);
    }

    public ScavengerSortOrder next() {
        ScavengerSortOrder[] all = values();
        return all[(this.ordinal() + 1) % all.length];
    }

    public Comparator<ScavengerItemMeta> comparator() {
        return switch (this) {
            case BY_RARITY -> BASE_TIE_BREAKER;
            case BY_VALUE -> Comparator.comparingInt(ScavengerItemMeta::baseValue).reversed()
                    .thenComparing(BASE_TIE_BREAKER);
            case BY_VALUE_DENSITY -> Comparator.comparingDouble(ScavengerItemMeta::valuePerCell).reversed()
                    .thenComparing(BASE_TIE_BREAKER);
            case BY_WEIGHT -> Comparator.comparingDouble((ScavengerItemMeta m) -> m.weightKg()).reversed()
                    .thenComparing(BASE_TIE_BREAKER);
            case BY_CATEGORY -> Comparator.comparingInt((ScavengerItemMeta m) -> m.category().ordinal())
                    .thenComparing(BASE_TIE_BREAKER);
        };
    }
}
