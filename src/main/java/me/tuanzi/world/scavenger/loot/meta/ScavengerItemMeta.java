package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 拾荒回廊物品搜刮与占地抽象元数据（不可变模型）
 * 严格遵循：最大堆叠恒为 1，最终标定总价值恒等于 baseValue（品质与面积不影响价值）。
 */
public record ScavengerItemMeta(
        Identifier itemId,
        ScavengerCategory category,
        ScavengerRarity rarity,
        GridSize gridSize,
        float weightKg,
        int baseValue,
        float baseSearchSeconds,
        List<String> allowedContainerTags,
        Optional<EquipmentGridSpec> equipmentGrid,
        ScavengerSoundProfile soundProfile,
        Optional<String> loreTranslationKey
) {

    public static final int FIXED_MAX_STACK = 1;

    public record GridSize(int width, int height) {
        public static final Codec<GridSize> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.intRange(1, 9).fieldOf("width").forGetter(GridSize::width),
                ExtraCodecs.intRange(1, 9).fieldOf("height").forGetter(GridSize::height)
        ).apply(instance, GridSize::new));

        public static final StreamCodec<ByteBuf, GridSize> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                GridSize::width,
                ByteBufCodecs.VAR_INT,
                GridSize::height,
                GridSize::new
        );

        public GridSize {
            width = Mth.clamp(width, 1, 9);
            height = Mth.clamp(height, 1, 9);
        }
    }

    public static final Codec<ScavengerItemMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("item").forGetter(ScavengerItemMeta::itemId),
            ScavengerCategory.CODEC.optionalFieldOf("category", ScavengerCategory.MISC).forGetter(ScavengerItemMeta::category),
            ScavengerRarity.CODEC.optionalFieldOf("rarity", ScavengerRarity.COMMON).forGetter(ScavengerItemMeta::rarity),
            GridSize.CODEC.fieldOf("grid_size").forGetter(ScavengerItemMeta::gridSize),
            Codec.floatRange(0.0f, 500.0f).fieldOf("weight_kg").forGetter(ScavengerItemMeta::weightKg),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("base_value").forGetter(ScavengerItemMeta::baseValue),
            Codec.floatRange(0.0f, 30.0f).fieldOf("base_search_seconds").forGetter(ScavengerItemMeta::baseSearchSeconds),
            Codec.STRING.listOf().optionalFieldOf("allowed_container_tags", List.of("*")).forGetter(ScavengerItemMeta::allowedContainerTags),
            EquipmentGridSpec.CODEC.optionalFieldOf("equipment_grid").forGetter(ScavengerItemMeta::equipmentGrid),
            ScavengerSoundProfile.CODEC.optionalFieldOf("sound_profile", ScavengerSoundProfile.DEFAULT_TACTICAL).forGetter(ScavengerItemMeta::soundProfile),
            Codec.STRING.optionalFieldOf("lore_translation_key").forGetter(ScavengerItemMeta::loreTranslationKey)
    ).apply(instance, ScavengerItemMeta::new));

    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, Optional<EquipmentGridSpec>> OPTIONAL_EQUIP_STREAM_CODEC =
            ByteBufCodecs.optional(EquipmentGridSpec.STREAM_CODEC);
    private static final StreamCodec<ByteBuf, Optional<String>> OPTIONAL_LORE_STREAM_CODEC =
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8);

    public static final StreamCodec<FriendlyByteBuf, ScavengerItemMeta> STREAM_CODEC = StreamCodec.of(
            (buf, meta) -> {
                Identifier.STREAM_CODEC.encode(buf, meta.itemId());
                ScavengerCategory.STREAM_CODEC.encode(buf, meta.category());
                ScavengerRarity.STREAM_CODEC.encode(buf, meta.rarity());
                GridSize.STREAM_CODEC.encode(buf, meta.gridSize());
                buf.writeFloat(meta.weightKg());
                buf.writeVarInt(meta.baseValue());
                buf.writeFloat(meta.baseSearchSeconds());
                STRING_LIST_STREAM_CODEC.encode(buf, meta.allowedContainerTags());
                OPTIONAL_EQUIP_STREAM_CODEC.encode(buf, meta.equipmentGrid());
                ScavengerSoundProfile.STREAM_CODEC.encode(buf, meta.soundProfile());
                OPTIONAL_LORE_STREAM_CODEC.encode(buf, meta.loreTranslationKey());
            },
            buf -> new ScavengerItemMeta(
                    Identifier.STREAM_CODEC.decode(buf),
                    ScavengerCategory.STREAM_CODEC.decode(buf),
                    ScavengerRarity.STREAM_CODEC.decode(buf),
                    GridSize.STREAM_CODEC.decode(buf),
                    buf.readFloat(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    STRING_LIST_STREAM_CODEC.decode(buf),
                    OPTIONAL_EQUIP_STREAM_CODEC.decode(buf),
                    ScavengerSoundProfile.STREAM_CODEC.decode(buf),
                    OPTIONAL_LORE_STREAM_CODEC.decode(buf)
            )
    );

    public ScavengerItemMeta {
        weightKg = Mth.clamp(weightKg, 0.0f, 500.0f);
        baseValue = Math.max(0, baseValue);
        baseSearchSeconds = Mth.clamp(baseSearchSeconds, 0.0f, 30.0f);
        allowedContainerTags = allowedContainerTags == null || allowedContainerTags.isEmpty()
                ? List.of("*")
                : List.copyOf(allowedContainerTags);
    }

    public int width() {
        return this.gridSize.width();
    }

    public int height() {
        return this.gridSize.height();
    }

    public boolean canRotate() {
        return this.gridSize.width() != this.gridSize.height();
    }

    public int effectiveWidth(boolean rotated) {
        return (rotated && canRotate()) ? this.gridSize.height() : this.gridSize.width();
    }

    public int effectiveHeight(boolean rotated) {
        return (rotated && canRotate()) ? this.gridSize.width() : this.gridSize.height();
    }

    public int cellCount() {
        return this.gridSize.width() * this.gridSize.height();
    }

    public double valuePerCell() {
        return (double) this.baseValue / Math.max(1, cellCount());
    }

    /**
     * 判断该物品是否符合指定容器的标签准入要求（"*" 为通配符）
     */
    public boolean matchesContainerTags(Collection<String> targetTags) {
        if (targetTags == null || targetTags.isEmpty() || targetTags.contains("*") || this.allowedContainerTags.contains("*")) {
            return true;
        }
        for (String tag : this.allowedContainerTags) {
            if (targetTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断物品在拾荒回廊维度内是否允许放入玩家快捷栏（1~9）：
     * 仅允许近战武器/工具类，或尺寸严格为 1x1 的非背包/非安全箱物品。
     */
    public boolean isAllowedInHotbar(ItemStack stack) {
        if (this.equipmentGrid.isPresent() || this.category == ScavengerCategory.BACKPACK || this.category == ScavengerCategory.SAFEBOX) {
            return false;
        }
        if (this.category == ScavengerCategory.TOOL || isWeaponOrToolStack(stack)) {
            return true;
        }
        return this.gridSize.width() == 1 && this.gridSize.height() == 1;
    }

    public static boolean isWeaponOrToolStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.KINETIC_WEAPON)
                || stack.has(DataComponents.PIERCING_WEAPON)
                || stack.has(DataComponents.BLOCKS_ATTACKS)) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof FishingRodItem
                || item instanceof ShearsItem
                || item instanceof FlintAndSteelItem) {
            return true;
        }
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (id != null) {
            String path = id.getPath();
            if (path.endsWith("_sword")
                    || path.endsWith("_axe")
                    || path.endsWith("_pickaxe")
                    || path.endsWith("_shovel")
                    || path.endsWith("_hoe")
                    || path.endsWith("_spear")
                    || path.endsWith("_mace")
                    || path.endsWith("_nodachi")
                    || path.equals("trident")
                    || path.equals("shield")
                    || path.equals("bow")
                    || path.equals("crossbow")) {
                return true;
            }
        }
        try {
            return stack.is(ItemTags.SWORDS)
                    || stack.is(ItemTags.AXES)
                    || stack.is(ItemTags.PICKAXES)
                    || stack.is(ItemTags.SHOVELS)
                    || stack.is(ItemTags.HOES)
                    || stack.is(ItemTags.SPEARS);
        } catch (IllegalStateException ignored) {
            // 数据包 Tags 尚未完成绑定时安全降级为基于组件与类型的判定
            return false;
        }
    }

    /**
     * 为未在数据包中显式声明的原版或模组物品生成安全默认元数据（1x1 / 普通 / 0.2kg）
     */
    public static ScavengerItemMeta fallback(Identifier itemId) {
        return new ScavengerItemMeta(
                itemId,
                ScavengerCategory.MISC,
                ScavengerRarity.COMMON,
                new GridSize(1, 1),
                0.20f,
                100,
                0.40f,
                List.of("#tuanzis_mod:general"),
                Optional.empty(),
                ScavengerSoundProfile.DEFAULT_TACTICAL,
                Optional.empty()
        );
    }
}
