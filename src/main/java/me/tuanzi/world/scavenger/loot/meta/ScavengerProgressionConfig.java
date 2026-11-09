package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import me.tuanzi.util.ModLog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拾荒回廊全局公式与基地成长配置模型（支持 JSON 热加载与 S2C 同步）
 * 包含：
 * 1. 动态搜索时长公式：T = base + rarity + (cells * perCell) + random(jitterMin, jitterMax)
 * 2. 8级负重成长系统：Lv.1 为 30kg/40kg，每升1级各 +5kg，最高8级（65kg/75kg）及分段移速惩罚
 * 3. 10级基地仓库扩容系统：固定9列，Lv.1 为 10行，每升1级 +5行，最高10级（55行）
 */
public record ScavengerProgressionConfig(
        SearchFormula searchFormula,
        WeightProgression weightProgression,
        StashProgression stashProgression
) {

    public record UpgradeCostEntry(Identifier itemId, int count) {
        public static final Codec<UpgradeCostEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("item").forGetter(UpgradeCostEntry::itemId),
                ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(UpgradeCostEntry::count)
        ).apply(instance, UpgradeCostEntry::new));

        public static final StreamCodec<FriendlyByteBuf, UpgradeCostEntry> STREAM_CODEC = StreamCodec.of(
                (buf, entry) -> {
                    Identifier.STREAM_CODEC.encode(buf, entry.itemId());
                    buf.writeVarInt(entry.count());
                },
                buf -> new UpgradeCostEntry(Identifier.STREAM_CODEC.decode(buf), buf.readVarInt())
        );

        public UpgradeCostEntry {
            count = Math.max(1, count);
        }
    }

    public record SearchFormula(
            float perCellSeconds,
            float randomJitterMin,
            float randomJitterMax,
            Map<ScavengerRarity, Float> rarityBonusSeconds
    ) {
        public static final Codec<SearchFormula> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(0.0f, 10.0f).optionalFieldOf("per_cell_seconds", 0.20f).forGetter(SearchFormula::perCellSeconds),
                Codec.floatRange(0.0f, 10.0f).optionalFieldOf("random_jitter_min", 0.05f).forGetter(SearchFormula::randomJitterMin),
                Codec.floatRange(0.0f, 10.0f).optionalFieldOf("random_jitter_max", 0.65f).forGetter(SearchFormula::randomJitterMax),
                Codec.unboundedMap(ScavengerRarity.CODEC, Codec.FLOAT)
                        .optionalFieldOf("rarity_bonus_seconds", defaultRarityMap())
                        .forGetter(SearchFormula::rarityBonusSeconds)
        ).apply(instance, SearchFormula::new));

        public SearchFormula {
            perCellSeconds = Math.max(0.0f, perCellSeconds);
            randomJitterMin = Math.max(0.0f, randomJitterMin);
            randomJitterMax = Math.max(randomJitterMin, randomJitterMax);
            EnumMap<ScavengerRarity, Float> normalized = new EnumMap<>(ScavengerRarity.class);
            for (ScavengerRarity r : ScavengerRarity.values()) {
                float bonus = rarityBonusSeconds != null && rarityBonusSeconds.containsKey(r)
                        ? Math.max(0.0f, rarityBonusSeconds.get(r))
                        : r.defaultBonusSearchSeconds();
                normalized.put(r, bonus);
            }
            rarityBonusSeconds = Map.copyOf(normalized);
        }

        public static Map<ScavengerRarity, Float> defaultRarityMap() {
            EnumMap<ScavengerRarity, Float> map = new EnumMap<>(ScavengerRarity.class);
            for (ScavengerRarity r : ScavengerRarity.values()) {
                map.put(r, r.defaultBonusSearchSeconds());
            }
            return Map.copyOf(map);
        }
    }

    public record WeightProgression(
            int maxLevel,
            float baseOverweightKg,
            float baseOverloadedKg,
            float perLevelBonusKg,
            Map<String, List<UpgradeCostEntry>> levelCosts
    ) {
        public static final Codec<WeightProgression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.intRange(1, 20).optionalFieldOf("max_level", 8).forGetter(WeightProgression::maxLevel),
                Codec.floatRange(1.0f, 500.0f).optionalFieldOf("base_overweight_kg", 30.0f).forGetter(WeightProgression::baseOverweightKg),
                Codec.floatRange(1.0f, 500.0f).optionalFieldOf("base_overloaded_kg", 40.0f).forGetter(WeightProgression::baseOverloadedKg),
                Codec.floatRange(0.0f, 100.0f).optionalFieldOf("per_level_bonus_kg", 5.0f).forGetter(WeightProgression::perLevelBonusKg),
                Codec.unboundedMap(Codec.STRING, UpgradeCostEntry.CODEC.listOf())
                        .optionalFieldOf("level_costs", Map.of())
                        .forGetter(WeightProgression::levelCosts)
        ).apply(instance, WeightProgression::new));

        public WeightProgression {
            maxLevel = Mth.clamp(maxLevel, 1, 20);
            baseOverweightKg = Math.max(1.0f, baseOverweightKg);
            baseOverloadedKg = Math.max(baseOverweightKg + 1.0f, baseOverloadedKg);
            perLevelBonusKg = Math.max(0.0f, perLevelBonusKg);
            levelCosts = levelCosts == null ? Map.of() : copyCostMap(levelCosts);
        }

        public List<UpgradeCostEntry> getCostForNextLevel(int targetLevel) {
            return this.levelCosts.getOrDefault(String.valueOf(targetLevel), List.of());
        }
    }

    public record StashProgression(
            int maxLevel,
            int columns,
            int baseRows,
            int perLevelBonusRows,
            Map<String, List<UpgradeCostEntry>> levelCosts
    ) {
        public static final Codec<StashProgression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.intRange(1, 20).optionalFieldOf("max_level", 10).forGetter(StashProgression::maxLevel),
                ExtraCodecs.intRange(1, 12).optionalFieldOf("columns", 9).forGetter(StashProgression::columns),
                ExtraCodecs.intRange(1, 30).optionalFieldOf("base_rows", 10).forGetter(StashProgression::baseRows),
                ExtraCodecs.intRange(1, 20).optionalFieldOf("per_level_bonus_rows", 5).forGetter(StashProgression::perLevelBonusRows),
                Codec.unboundedMap(Codec.STRING, UpgradeCostEntry.CODEC.listOf())
                        .optionalFieldOf("level_costs", Map.of())
                        .forGetter(StashProgression::levelCosts)
        ).apply(instance, StashProgression::new));

        public StashProgression {
            maxLevel = Mth.clamp(maxLevel, 1, 20);
            columns = Mth.clamp(columns, 1, 12);
            baseRows = Mth.clamp(baseRows, 1, 30);
            perLevelBonusRows = Mth.clamp(perLevelBonusRows, 1, 20);
            levelCosts = levelCosts == null ? Map.of() : copyCostMap(levelCosts);
        }

        public List<UpgradeCostEntry> getCostForNextLevel(int targetLevel) {
            return this.levelCosts.getOrDefault(String.valueOf(targetLevel), List.of());
        }
    }

    public static final Codec<ScavengerProgressionConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SearchFormula.CODEC.optionalFieldOf("search_formula", createDefault().searchFormula()).forGetter(ScavengerProgressionConfig::searchFormula),
            WeightProgression.CODEC.optionalFieldOf("weight_progression", createDefault().weightProgression()).forGetter(ScavengerProgressionConfig::weightProgression),
            StashProgression.CODEC.optionalFieldOf("stash_progression", createDefault().stashProgression()).forGetter(ScavengerProgressionConfig::stashProgression)
    ).apply(instance, ScavengerProgressionConfig::new));

    private static final StreamCodec<ByteBuf, Map<ScavengerRarity, Float>> RARITY_BONUS_MAP_STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ScavengerRarity.STREAM_CODEC, ByteBufCodecs.FLOAT);
    private static final StreamCodec<FriendlyByteBuf, Map<String, List<UpgradeCostEntry>>> COST_MAP_STREAM_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,
                    UpgradeCostEntry.STREAM_CODEC.apply(ByteBufCodecs.list())
            );

    public static final StreamCodec<FriendlyByteBuf, ScavengerProgressionConfig> STREAM_CODEC = StreamCodec.of(
            (buf, cfg) -> {
                SearchFormula sf = cfg.searchFormula();
                buf.writeFloat(sf.perCellSeconds());
                buf.writeFloat(sf.randomJitterMin());
                buf.writeFloat(sf.randomJitterMax());
                RARITY_BONUS_MAP_STREAM_CODEC.encode(buf, sf.rarityBonusSeconds());

                WeightProgression wp = cfg.weightProgression();
                buf.writeVarInt(wp.maxLevel());
                buf.writeFloat(wp.baseOverweightKg());
                buf.writeFloat(wp.baseOverloadedKg());
                buf.writeFloat(wp.perLevelBonusKg());
                COST_MAP_STREAM_CODEC.encode(buf, wp.levelCosts());

                StashProgression sp = cfg.stashProgression();
                buf.writeVarInt(sp.maxLevel());
                buf.writeVarInt(sp.columns());
                buf.writeVarInt(sp.baseRows());
                buf.writeVarInt(sp.perLevelBonusRows());
                COST_MAP_STREAM_CODEC.encode(buf, sp.levelCosts());
            },
            buf -> {
                float perCell = buf.readFloat();
                float jMin = buf.readFloat();
                float jMax = buf.readFloat();
                Map<ScavengerRarity, Float> rarityMap = RARITY_BONUS_MAP_STREAM_CODEC.decode(buf);
                SearchFormula sf = new SearchFormula(perCell, jMin, jMax, rarityMap);

                int wMaxLv = buf.readVarInt();
                float baseW1 = buf.readFloat();
                float baseW2 = buf.readFloat();
                float wBonus = buf.readFloat();
                Map<String, List<UpgradeCostEntry>> wCosts = COST_MAP_STREAM_CODEC.decode(buf);
                WeightProgression wp = new WeightProgression(wMaxLv, baseW1, baseW2, wBonus, wCosts);

                int sMaxLv = buf.readVarInt();
                int cols = buf.readVarInt();
                int baseRows = buf.readVarInt();
                int bonusRows = buf.readVarInt();
                Map<String, List<UpgradeCostEntry>> sCosts = COST_MAP_STREAM_CODEC.decode(buf);
                StashProgression sp = new StashProgression(sMaxLv, cols, baseRows, bonusRows, sCosts);

                return new ScavengerProgressionConfig(sf, wp, sp);
            }
    );

    /**
     * 入箱固化搜索时长计算（单位：ticks，20 ticks = 1 秒）
     * 公式：基础搜索时长 + 品质搜索时长 + 网格数搜索时长 + 随机浮动
     */
    public int computeSolidifiedSearchTicks(ScavengerItemMeta meta, RandomSource random) {
        float tBase = meta.baseSearchSeconds();
        float tRarity = this.searchFormula.rarityBonusSeconds().getOrDefault(meta.rarity(), meta.rarity().defaultBonusSearchSeconds());
        float tGrid = meta.cellCount() * this.searchFormula.perCellSeconds();
        float tRandom = this.searchFormula.randomJitterMax() > this.searchFormula.randomJitterMin()
                ? Mth.nextFloat(random, this.searchFormula.randomJitterMin(), this.searchFormula.randomJitterMax())
                : this.searchFormula.randomJitterMin();

        float totalSeconds = tBase + tRarity + tGrid + tRandom;
        int ticks = Math.max(5, Math.round(totalSeconds * 20.0f));

        ModLog.debug(String.format(
                "【拾荒回廊·搜索时长固化】item=%s | base=%.2fs + rarity(%s)=%.2fs + grid(%d格)=%.2fs + rand=%.3fs => %.2fs (%d ticks)",
                meta.itemId(), tBase, meta.rarity().getSerializedName(), tRarity, meta.cellCount(), tGrid, tRandom, totalSeconds, ticks
        ));
        return ticks;
    }

    public int clampWeightLevel(int level) {
        return Mth.clamp(level, 1, this.weightProgression.maxLevel());
    }

    public float getOverweightThresholdKg(int level) {
        int lv = clampWeightLevel(level);
        return this.weightProgression.baseOverweightKg() + (lv - 1) * this.weightProgression.perLevelBonusKg();
    }

    public float getOverloadedThresholdKg(int level) {
        int lv = clampWeightLevel(level);
        return this.weightProgression.baseOverloadedKg() + (lv - 1) * this.weightProgression.perLevelBonusKg();
    }

    /**
     * 计算当前全身总负重对应的移速乘数修正值（ADD_MULTIPLIED_TOTAL）：
     * - [0, W1]：0.0f（正常 100% 移速）
     * - (W1, W2]：线性从 0% 衰减至 -20%
     * - > W2：禁止疾跑，从 -25% 起每超 1kg 额外 -2%，最高封顶 -65%
     */
    public float computeMovementSpeedMultiplier(float totalWeightKg, int level) {
        float w1 = getOverweightThresholdKg(level);
        float w2 = getOverloadedThresholdKg(level);
        if (totalWeightKg <= w1) {
            return 0.0f;
        }
        if (totalWeightKg <= w2) {
            float span = Math.max(0.001f, w2 - w1);
            float ratio = (totalWeightKg - w1) / span;
            return -0.20f * ratio;
        }
        float excess = totalWeightKg - w2;
        float penalty = Math.min(0.65f, 0.25f + 0.02f * excess);
        return -penalty;
    }

    public boolean isSprintAllowed(float totalWeightKg, int level) {
        return totalWeightKg <= getOverloadedThresholdKg(level);
    }

    public int clampStashLevel(int level) {
        return Mth.clamp(level, 1, this.stashProgression.maxLevel());
    }

    public int getStashColumns() {
        return this.stashProgression.columns();
    }

    public int getStashRows(int level) {
        int lv = clampStashLevel(level);
        return this.stashProgression.baseRows() + (lv - 1) * this.stashProgression.perLevelBonusRows();
    }

    public static ScavengerProgressionConfig createDefault() {
        return new ScavengerProgressionConfig(
                new SearchFormula(0.20f, 0.05f, 0.65f, SearchFormula.defaultRarityMap()),
                new WeightProgression(8, 30.0f, 40.0f, 5.0f, Map.of()),
                new StashProgression(10, 9, 10, 5, Map.of())
        );
    }

    private static Map<String, List<UpgradeCostEntry>> copyCostMap(Map<String, List<UpgradeCostEntry>> src) {
        Map<String, List<UpgradeCostEntry>> copy = new HashMap<>();
        src.forEach((k, v) -> copy.put(k, v == null ? List.of() : List.copyOf(v)));
        return Map.copyOf(copy);
    }
}
