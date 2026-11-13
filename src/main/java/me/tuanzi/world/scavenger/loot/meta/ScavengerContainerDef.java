package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拾荒回廊搜刮容器定义模型（数据驱动 JSON 映射，不可变 Record）
 * 定义容器网格尺寸、容器标签、可存入过滤标签、迷雾开关与战利品生成规则。
 */
public record ScavengerContainerDef(
        Identifier containerId,
        String titleKey,
        int columns,
        int rows,
        List<String> containerTags,
        List<String> acceptedDepositTags,
        boolean requiresSearch,
        LootGenerationRule lootGeneration,
        String soundTheme
) {

    public static final Codec<ScavengerContainerDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("container_id").forGetter(ScavengerContainerDef::containerId),
            Codec.STRING.fieldOf("title_key").forGetter(ScavengerContainerDef::titleKey),
            ExtraCodecs.intRange(1, 12).fieldOf("columns").forGetter(ScavengerContainerDef::columns),
            ExtraCodecs.intRange(1, 64).fieldOf("rows").forGetter(ScavengerContainerDef::rows),
            Codec.STRING.listOf().optionalFieldOf("container_tags", List.of("general")).forGetter(ScavengerContainerDef::containerTags),
            Codec.STRING.listOf().optionalFieldOf("accepted_deposit_tags", List.of("*")).forGetter(ScavengerContainerDef::acceptedDepositTags),
            Codec.BOOL.optionalFieldOf("requires_search", true).forGetter(ScavengerContainerDef::requiresSearch),
            LootGenerationRule.CODEC.optionalFieldOf("loot_generation", LootGenerationRule.createDefault()).forGetter(ScavengerContainerDef::lootGeneration),
            Codec.STRING.optionalFieldOf("sound_theme", "metal_case").forGetter(ScavengerContainerDef::soundTheme)
    ).apply(instance, ScavengerContainerDef::new));
    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
    public static final StreamCodec<FriendlyByteBuf, ScavengerContainerDef> STREAM_CODEC = StreamCodec.of(
            (buf, def) -> {
                Identifier.STREAM_CODEC.encode(buf, def.containerId());
                buf.writeUtf(def.titleKey());
                buf.writeVarInt(def.columns());
                buf.writeVarInt(def.rows());
                STRING_LIST_STREAM_CODEC.encode(buf, def.containerTags());
                STRING_LIST_STREAM_CODEC.encode(buf, def.acceptedDepositTags());
                buf.writeBoolean(def.requiresSearch());
                LootGenerationRule.STREAM_CODEC.encode(buf, def.lootGeneration());
                buf.writeUtf(def.soundTheme());
            },
            buf -> new ScavengerContainerDef(
                    Identifier.STREAM_CODEC.decode(buf),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    STRING_LIST_STREAM_CODEC.decode(buf),
                    STRING_LIST_STREAM_CODEC.decode(buf),
                    buf.readBoolean(),
                    LootGenerationRule.STREAM_CODEC.decode(buf),
                    buf.readUtf()
            )
    );

    public ScavengerContainerDef {
        columns = Mth.clamp(columns, 1, 12);
        rows = Mth.clamp(rows, 1, 64);
        containerTags = (containerTags == null || containerTags.isEmpty())
                ? List.of("general")
                : List.copyOf(containerTags);
        acceptedDepositTags = (acceptedDepositTags == null || acceptedDepositTags.isEmpty())
                ? List.of("*")
                : List.copyOf(acceptedDepositTags);
        if (lootGeneration == null) {
            lootGeneration = LootGenerationRule.createDefault();
        }
        if (soundTheme == null || soundTheme.isBlank()) {
            soundTheme = "metal_case";
        }
    }

    public int totalCells() {
        return this.columns * this.rows;
    }

    /**
     * 判断目标物品在原朝向或旋转 90° 后，几何尺寸上是否有可能放进该容器空网格中。
     */
    public boolean canFitEver(ScavengerItemMeta meta) {
        int w = meta.width();
        int h = meta.height();
        return (w <= this.columns && h <= this.rows) || (h <= this.columns && w <= this.rows);
    }

    /**
     * 判断玩家是否允许将该物品手动拖入/转入此容器中。
     */
    public boolean acceptsDeposit(ScavengerItemMeta meta) {
        if (this.acceptedDepositTags.contains("*")) {
            return true;
        }
        String catLower = meta.category().getSerializedName().toLowerCase(java.util.Locale.ROOT);
        if (this.acceptedDepositTags.contains(catLower) || this.acceptedDepositTags.contains(meta.category().getSerializedName())) {
            return true;
        }
        return meta.matchesContainerTags(this.acceptedDepositTags);
    }

    /**
     * 判断该物品是否符合此容器初始搜刮战利品池的生成准入条件（几何尺寸 + 容器标签 + 池过滤标签）。
     */
    public boolean matchesLootCandidate(ScavengerItemMeta meta) {
        if (!canFitEver(meta)) {
            return false;
        }
        if (!meta.matchesContainerTags(this.containerTags)) {
            return false;
        }
        List<String> filterTags = this.lootGeneration.poolFilterTags();
        if (filterTags.contains("*")) {
            return true;
        }
        String catLower = meta.category().getSerializedName().toLowerCase(java.util.Locale.ROOT);
        if (filterTags.contains(catLower) || filterTags.contains(meta.category().getSerializedName())) {
            return true;
        }
        return meta.matchesContainerTags(filterTags);
    }

    /**
     * 容器战利品生成规则
     */
    public record LootGenerationRule(
            int minRolls,
            int maxRolls,
            float maxFillRatio,
            Map<ScavengerRarity, Float> rarityWeightModifiers,
            List<String> poolFilterTags
    ) {
        public static final Codec<LootGenerationRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.intRange(0, 64).optionalFieldOf("min_rolls", 1).forGetter(LootGenerationRule::minRolls),
                ExtraCodecs.intRange(0, 64).optionalFieldOf("max_rolls", 4).forGetter(LootGenerationRule::maxRolls),
                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("max_fill_ratio", 0.70f).forGetter(LootGenerationRule::maxFillRatio),
                Codec.unboundedMap(ScavengerRarity.CODEC, Codec.FLOAT)
                        .optionalFieldOf("rarity_weight_modifiers", defaultRarityModifiers())
                        .forGetter(LootGenerationRule::rarityWeightModifiers),
                Codec.STRING.listOf().optionalFieldOf("pool_filter_tags", List.of("*")).forGetter(LootGenerationRule::poolFilterTags)
        ).apply(instance, LootGenerationRule::new));
        private static final StreamCodec<ByteBuf, Map<ScavengerRarity, Float>> RARITY_MOD_STREAM_CODEC =
                ByteBufCodecs.map(HashMap::new, ScavengerRarity.STREAM_CODEC, ByteBufCodecs.FLOAT);
        private static final StreamCodec<ByteBuf, List<String>> TAG_LIST_STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
        public static final StreamCodec<FriendlyByteBuf, LootGenerationRule> STREAM_CODEC = StreamCodec.of(
                (buf, rule) -> {
                    buf.writeVarInt(rule.minRolls());
                    buf.writeVarInt(rule.maxRolls());
                    buf.writeFloat(rule.maxFillRatio());
                    RARITY_MOD_STREAM_CODEC.encode(buf, rule.rarityWeightModifiers());
                    TAG_LIST_STREAM_CODEC.encode(buf, rule.poolFilterTags());
                },
                buf -> new LootGenerationRule(
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readFloat(),
                        RARITY_MOD_STREAM_CODEC.decode(buf),
                        TAG_LIST_STREAM_CODEC.decode(buf)
                )
        );

        public LootGenerationRule {
            minRolls = Mth.clamp(minRolls, 0, 64);
            maxRolls = Math.max(minRolls, Mth.clamp(maxRolls, 0, 64));
            maxFillRatio = Mth.clamp(maxFillRatio, 0.0f, 1.0f);
            EnumMap<ScavengerRarity, Float> normalized = new EnumMap<>(ScavengerRarity.class);
            for (ScavengerRarity rarity : ScavengerRarity.values()) {
                float mod = rarityWeightModifiers != null && rarityWeightModifiers.containsKey(rarity)
                        ? Math.max(0.0f, rarityWeightModifiers.get(rarity))
                        : 1.0f;
                normalized.put(rarity, mod);
            }
            rarityWeightModifiers = Map.copyOf(normalized);
            poolFilterTags = (poolFilterTags == null || poolFilterTags.isEmpty())
                    ? List.of("*")
                    : List.copyOf(poolFilterTags);
        }

        public static LootGenerationRule createDefault() {
            return new LootGenerationRule(1, 4, 0.70f, defaultRarityModifiers(), List.of("*"));
        }

        public static Map<ScavengerRarity, Float> defaultRarityModifiers() {
            EnumMap<ScavengerRarity, Float> map = new EnumMap<>(ScavengerRarity.class);
            for (ScavengerRarity rarity : ScavengerRarity.values()) {
                map.put(rarity, 1.0f);
            }
            return Map.copyOf(map);
        }
    }
}
