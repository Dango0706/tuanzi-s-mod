package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

/**
 * 拾荒回廊 6 阶物品品质枚举
 * 主导网格边框颜色、默认掉落池权重、附加搜索时长与高光悬念阶段阈值（不影响基础标定价值）。
 */
public enum ScavengerRarity implements StringRepresentable {
    COMMON("COMMON", "scavenger.rarity.tuanzis_mod.common", 0xC8CDD4, 500, 0.00f, new float[0]),
    UNCOMMON("UNCOMMON", "scavenger.rarity.tuanzis_mod.uncommon", 0x2ECC71, 280, 0.30f, new float[0]),
    RARE("RARE", "scavenger.rarity.tuanzis_mod.rare", 0x3498DB, 140, 0.75f, new float[]{0.70f}),
    EPIC("EPIC", "scavenger.rarity.tuanzis_mod.epic", 0x9B59B6, 60, 1.30f, new float[]{0.65f}),
    LEGENDARY("LEGENDARY", "scavenger.rarity.tuanzis_mod.legendary", 0xF1C40F, 17, 2.00f, new float[]{0.60f}),
    MYTHIC("MYTHIC", "scavenger.rarity.tuanzis_mod.mythic", 0xE74C3C, 3, 3.20f, new float[]{0.45f, 0.75f});

    public static final Codec<ScavengerRarity> CODEC = new StringRepresentable.EnumCodec<>(
            values(),
            name -> {
                if (name == null) return null;
                String upper = name.toUpperCase(Locale.ROOT);
                for (ScavengerRarity v : values()) {
                    if (v.serializedName.equals(upper)) return v;
                }
                return null;
            }
    );

    private static final IntFunction<ScavengerRarity> BY_ID = ByIdMap.continuous(
            Enum::ordinal,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, ScavengerRarity> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;
    private final String translationKey;
    private final int colorRgb;
    private final int defaultPoolWeight;
    private final float defaultBonusSearchSeconds;
    private final float[] stagePulseThresholds;

    ScavengerRarity(
            String serializedName,
            String translationKey,
            int colorRgb,
            int defaultPoolWeight,
            float defaultBonusSearchSeconds,
            float[] stagePulseThresholds
    ) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
        this.colorRgb = colorRgb;
        this.defaultPoolWeight = defaultPoolWeight;
        this.defaultBonusSearchSeconds = defaultBonusSearchSeconds;
        this.stagePulseThresholds = stagePulseThresholds;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public int colorRgb() {
        return this.colorRgb;
    }

    public int argbBorder(int alpha) {
        return ((alpha & 0xFF) << 24) | (this.colorRgb & 0xFFFFFF);
    }

    public int defaultPoolWeight() {
        return this.defaultPoolWeight;
    }

    public float defaultBonusSearchSeconds() {
        return this.defaultBonusSearchSeconds;
    }

    public float[] stagePulseThresholds() {
        return this.stagePulseThresholds.clone();
    }

    public boolean isHighValueClimax() {
        return this == EPIC || this == LEGENDARY || this == MYTHIC;
    }

    public MutableComponent displayName() {
        return Component.translatable(this.translationKey).withStyle(Style.EMPTY.withColor(this.colorRgb));
    }
}
