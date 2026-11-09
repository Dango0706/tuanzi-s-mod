package me.tuanzi.datagen;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentGenerator extends FabricDynamicRegistryProvider {
    public ModEnchantmentGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
        var items = registries.lookupOrThrow(Registries.ITEM);
        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);

        // Soulbound
        entries.add(ModEnchantments.SOULBOUND, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
                1,    // weight
                1,    // max level
                Enchantment.constantCost(10), 
                Enchantment.constantCost(50), 
                16,    // anvil cost
                EquipmentSlotGroup.ANY
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "soulbound")));

        // 阅历 (Experience)
        // 30级附魔最多只能附出3级 -> Level 4 min cost > 30.
        // Level 1: 5, Level 2: 15, Level 3: 25, Level 4: 35
        entries.add(ModEnchantments.EXPERIENCE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                5,    // weight (common)
                4,    // max level
                Enchantment.dynamicCost(5, 10), 
                Enchantment.dynamicCost(25, 10), 
                4,     // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "experience")));

        // 熔炼 (Smelting)
        entries.add(ModEnchantments.SMELTING, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.MINING_ENCHANTABLE),
                2,    // weight
                1,    // max level
                Enchantment.constantCost(15), 
                Enchantment.constantCost(65), 
                8,    // anvil cost
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.SILK_TOUCH)))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "smelting")));

        // 连锁采掘 (Chain Mining)
        entries.add(ModEnchantments.CHAIN_MINING, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.MINING_ENCHANTABLE),
                1,    // weight
                4,    // max level
                Enchantment.dynamicCost(15, 10), 
                Enchantment.dynamicCost(65, 10), 
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "chain_mining")));

        // 血怒 (Blood Rage)
        entries.add(ModEnchantments.BLOOD_RAGE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                1,    // weight (rare)
                1,    // max level
                Enchantment.constantCost(15), 
                Enchantment.constantCost(65), 
                8,    // anvil cost
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT)))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "blood_rage")));

        // 狂战士 (Berserker)
        entries.add(ModEnchantments.BERSERKER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                2,    // weight (rare)
                2,    // max level
                Enchantment.dynamicCost(15, 10), 
                Enchantment.dynamicCost(65, 10), 
                8,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        )
        .exclusiveWith(HolderSet.direct(
            enchantments.getOrThrow(Enchantments.PROTECTION),
            enchantments.getOrThrow(Enchantments.FIRE_PROTECTION),
            enchantments.getOrThrow(Enchantments.BLAST_PROTECTION),
            enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION),
            enchantments.getOrThrow(Enchantments.FEATHER_FALLING)
        ))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "berserker")));

        // 处决 (Execute)
        entries.add(ModEnchantments.EXECUTE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(HolderSet.direct(
            enchantments.getOrThrow(Enchantments.SHARPNESS),
            enchantments.getOrThrow(Enchantments.SMITE),
            enchantments.getOrThrow(Enchantments.BANE_OF_ARTHROPODS)
        ))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "execute")));

        // 连锁苦痛 (Chain Pain)
        entries.add(ModEnchantments.CHAIN_PAIN, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.AXES),
                2,    // weight (rare)
                1,    // max level (I)
                Enchantment.constantCost(15), 
                Enchantment.constantCost(65), 
                8,    // anvil cost
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(HolderSet.direct(
            enchantments.getOrThrow(Enchantments.SILK_TOUCH)
        ))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "chain_pain")));

        // 追踪箭 (Seeking Arrow)
        entries.add(ModEnchantments.SEEKING_ARROW, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                1,    // weight
                1,    // max level (I)
                Enchantment.constantCost(15), 
                Enchantment.constantCost(65), 
                8,    // anvil cost
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(HolderSet.direct(
            enchantments.getOrThrow(Enchantments.MULTISHOT)
        ))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "seeking_arrow")));

        // 共振脉冲 (Resonance Pulse)
        entries.add(ModEnchantments.RESONANCE_PULSE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SWORDS),
                2,    // weight
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_NORMALIZATION))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "resonance_pulse")));

        // 深渊律动 (Abyssal Rhythm)
        entries.add(ModEnchantments.ABYSSAL_RHYTHM, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SWORDS),
                2,    // weight
                4,    // max level (IV)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_NORMALIZATION))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "abyssal_rhythm")));

        // 蜂鸣节律 (Buzzing Rhythm)
        entries.add(ModEnchantments.BUZZING_RHYTHM, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SWORDS),
                2,    // weight
                4,    // max level (IV)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(HolderSet.direct(
            enchantments.getOrThrow(Enchantments.SWEEPING_EDGE),
            enchantments.getOrThrow(Enchantments.FIRE_ASPECT),
            enchantments.getOrThrow(Enchantments.KNOCKBACK)
        ))
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES,
            new net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect(
                Identifier.fromNamespaceAndPath("tuanzis_mod", "enchantment.buzzing_rhythm.reach"),
                net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(0.0625F),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            )
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "buzzing_rhythm")));

        // 虚无共鸣 (Void Resonance)
        entries.add(ModEnchantments.VOID_RESONANCE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE),
                2,    // weight
                5,    // max level (V)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_NORMALIZATION))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "void_resonance")));

        // 坚盾之赐 (Steel Shield Gift)
        entries.add(ModEnchantments.STEEL_SHIELD_GIFT, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE),
                2,    // weight
                4,    // max level (IV)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_NORMALIZATION))
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES,
            new net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect(
                Identifier.fromNamespaceAndPath("tuanzis_mod", "enchantment.steel_shield_gift.armor"),
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(0.5F),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            )
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "steel_shield_gift")));

        // 过载协议 (Overload Protocol)
        entries.add(ModEnchantments.OVERLOAD_PROTOCOL, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE),
                2,    // weight
                5,    // max level (V)
                Enchantment.dynamicCost(15, 9), 
                Enchantment.dynamicCost(65, 9), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_NORMALIZATION))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "overload_protocol")));

        // 古卷附魔-汲血 (Ancient Scroll - Blood Leech)
        entries.add(ModEnchantments.ANCIENT_SCROLL_BLOOD_LEECH, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                1,    // weight (最高稀有度)
                4,    // max level (IV)
                Enchantment.dynamicCost(30, 100), 
                Enchantment.dynamicCost(80, 100), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ancient_scroll_blood_leech")));

        // 古卷附魔-切金断玉 (Ancient Scroll - Metal Cleaver)
        entries.add(ModEnchantments.ANCIENT_SCROLL_METAL_CUTTER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                1,    // weight (最高稀有度)
                4,    // max level (IV)
                Enchantment.dynamicCost(30, 100), 
                Enchantment.dynamicCost(80, 100), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ancient_scroll_metal_cutter")));

        // 古卷附魔-贯革 (Ancient Scroll - Hide Piercer)
        entries.add(ModEnchantments.ANCIENT_SCROLL_LEATHER_PIERCER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(me.tuanzi.init.ModItemTags.BOW_AND_CROSSBOW_ENCHANTABLE),
                1,    // weight (最高稀有度)
                4,    // max level (IV)
                Enchantment.dynamicCost(30, 100), 
                Enchantment.dynamicCost(80, 100), 
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ancient_scroll_leather_piercer")));

        // 玄冥雨刃 (Rain Blade)
        entries.add(ModEnchantments.RAIN_BLADE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                10,   // weight (普通)
                5,    // max level (V)
                Enchantment.dynamicCost(1, 10), 
                Enchantment.dynamicCost(16, 10), 
                2,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_WEATHER_BLADES))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "rain_blade")));

        // 青女霜刃 (Frost Blade)
        entries.add(ModEnchantments.FROST_BLADE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                10,   // weight (普通)
                5,    // max level (V)
                Enchantment.dynamicCost(1, 10), 
                Enchantment.dynamicCost(16, 10), 
                2,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_WEATHER_BLADES))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "frost_blade")));

        // 雷公霆刃 (Thunder Blade)
        entries.add(ModEnchantments.THUNDER_BLADE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                10,   // weight (普通)
                5,    // max level (V)
                Enchantment.dynamicCost(1, 10), 
                Enchantment.dynamicCost(16, 10), 
                2,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_WEATHER_BLADES))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "thunder_blade")));

        // 金乌炽刃 (Sun Blade)
        entries.add(ModEnchantments.SUN_BLADE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                10,   // weight (普通)
                5,    // max level (V)
                Enchantment.dynamicCost(1, 10), 
                Enchantment.dynamicCost(16, 10), 
                2,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_WEATHER_BLADES))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "sun_blade")));

        // 羲和昼刃 (Day Blade)
        entries.add(ModEnchantments.DAY_BLADE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                2,    // weight (稀有/Rare)
                5,    // max level (V)
                Enchantment.dynamicCost(10, 8), 
                Enchantment.dynamicCost(60, 8), 
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_DAY_NIGHT_BLADES))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "day_blade")));

        // 望舒夜刃 (Night Blade)
        entries.add(ModEnchantments.NIGHT_BLADE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                2,    // weight (稀有/Rare)
                5,    // max level (V)
                Enchantment.dynamicCost(10, 8), 
                Enchantment.dynamicCost(60, 8), 
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_DAY_NIGHT_BLADES))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "night_blade")));

        // 游侠速装 (Ranger Reload)
        entries.add(ModEnchantments.RANGER_RELOAD, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                2,    // weight (rare)
                1,    // max level (I)
                Enchantment.constantCost(20),
                Enchantment.constantCost(50),
                4,    // anvil cost
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ranger_reload")));

        // 古卷附魔-预备弹仓 (Ancient Scroll - Reserved Chamber)
        entries.add(ModEnchantments.ANCIENT_SCROLL_RESERVED_CHAMBER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                1,    // weight (最高稀有度/古卷附魔)
                4,    // max level (IV)
                Enchantment.dynamicCost(30, 100),
                Enchantment.dynamicCost(80, 100),
                8,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        )
        .exclusiveWith(enchantments.getOrThrow(ModEnchantments.EXCLUSIVE_RESERVED_CHAMBER))
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ancient_scroll_reserved_chamber")));

        // 古卷附魔-屹立不倒 (Ancient Scroll - Standing Firm)
        entries.add(ModEnchantments.ANCIENT_SCROLL_STANDING_FIRM, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                1,    // weight (最高稀有度/古卷附魔)
                5,    // max level (V)
                Enchantment.dynamicCost(30, 100),
                Enchantment.dynamicCost(80, 100),
                8,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ancient_scroll_standing_firm")));

        // 古卷附魔-经验老辣 (Ancient Scroll - Seasoned)
        entries.add(ModEnchantments.ANCIENT_SCROLL_SEASONED, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                1,    // weight (最高稀有度/古卷附魔)
                4,    // max level (IV)
                Enchantment.dynamicCost(30, 100),
                Enchantment.dynamicCost(80, 100),
                8,    // anvil cost per level
                EquipmentSlotGroup.HEAD
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ancient_scroll_seasoned")));

        // 回流 (Flow State)
        entries.add(ModEnchantments.FLOW_STATE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(me.tuanzi.init.ModItemTags.NODACHI_ENCHANTABLE),
                2,    // weight (rare)
                4,    // max level (IV)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "flow_state")));

        // 绝式 (Finality) - 宝藏型附魔
        entries.add(ModEnchantments.FINALITY, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(me.tuanzi.init.ModItemTags.NODACHI_ENCHANTABLE),
                1,    // weight (very rare)
                1,    // max level (I)
                Enchantment.constantCost(30),
                Enchantment.constantCost(70),
                8,    // anvil cost
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "finality")));

        // 二段跳 (Double Jump)
        entries.add(ModEnchantments.DOUBLE_JUMP, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                1,    // weight (very rare)
                1,    // max level (I)
                Enchantment.constantCost(20),
                Enchantment.constantCost(50),
                8,    // anvil cost
                EquipmentSlotGroup.FEET
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "double_jump")));

        // 延展 (Extension)
        entries.add(ModEnchantments.EXTENSION, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                5,    // max level (V)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                4,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        )
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES,
            new net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect(
                Identifier.fromNamespaceAndPath("tuanzis_mod", "enchantment.extension.block_reach"),
                net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(1.05F, 0.3F),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            )
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "extension")));

        // 遥击 (Ranged Strike)
        entries.add(ModEnchantments.RANGED_STRIKE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                2,    // weight (rare)
                5,    // max level (V)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        )
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES,
            new net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect(
                Identifier.fromNamespaceAndPath("tuanzis_mod", "enchantment.ranged_strike.entity_reach"),
                net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(0.75F, 0.25F),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            )
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "ranged_strike")));

        // 旅者 (Wayfarer)
        entries.add(ModEnchantments.WAYFARER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.LEG_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                3,    // max level (III)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                4,    // anvil cost per level
                EquipmentSlotGroup.LEGS
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "wayfarer")));

        // 登阶 (Step Up)
        entries.add(ModEnchantments.STEP_UP, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                2,    // max level (II)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                4,    // anvil cost per level
                EquipmentSlotGroup.FEET
            )
        )
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES,
            new net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect(
                Identifier.fromNamespaceAndPath("tuanzis_mod", "enchantment.step_up"),
                net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(0.4F, 0.5F),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            )
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "step_up")));

        // 饱腹 (Satiation) - 宝藏附魔
        entries.add(ModEnchantments.SATIATION, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                8,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "satiation")));

        // 丰获 (Bountiful Harvest)
        entries.add(ModEnchantments.BOUNTIFUL_HARVEST, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HOES),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "bountiful_harvest")));

        // 经验亲和 (Experience Affinity)
        entries.add(ModEnchantments.EXPERIENCE_AFFINITY, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(me.tuanzi.init.ModItemTags.AXE_PICKAXE_HOE_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "experience_affinity")));

        // 海之馈赠 (Bounty of the Sea)
        entries.add(ModEnchantments.BOUNTY_OF_THE_SEA, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.FISHING_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "bounty_of_the_sea")));

        // 踏雪 (Snow Strider)
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        entries.add(ModEnchantments.SNOW_STRIDER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                3,    // max level (III)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                4,    // anvil cost per level
                EquipmentSlotGroup.FEET
            )
        )
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.LOCATION_CHANGED,
            new net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect(
                Identifier.fromNamespaceAndPath("tuanzis_mod", "enchantment.snow_strider"),
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(0.08F, 0.08F),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ),
            net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition.hasProperties(
                net.minecraft.world.level.storage.loot.LootContext.EntityTarget.THIS,
                net.minecraft.advancements.predicates.entity.EntityPredicate.Builder.entity()
                    .movementAffectedBy(
                        net.minecraft.advancements.predicates.LocationPredicate.Builder.location()
                            .setBlock(net.minecraft.advancements.predicates.BlockPredicate.Builder.block().of(blocks, me.tuanzi.init.ModBlockTags.SNOW_STRIDER_BLOCKS))
                    )
            )
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "snow_strider")));

        // 虚空锚 (Void Anchor) - 宝藏附魔
        entries.add(ModEnchantments.VOID_ANCHOR, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                1,    // weight (very rare)
                1,    // max level (I)
                Enchantment.constantCost(25),
                Enchantment.constantCost(75),
                8,    // anvil cost per level
                EquipmentSlotGroup.FEET
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "void_anchor")));

        // 交易大师 (Trading Master) - 宝藏附魔
        entries.add(ModEnchantments.TRADING_MASTER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                8,    // anvil cost per level
                EquipmentSlotGroup.HEAD
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "trading_master")));

        // 增效 (Boost) - 非宝藏附魔
        entries.add(ModEnchantments.BOOST, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(me.tuanzi.init.ModItemTags.ELYTRA_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        )
        .withEffect(
            net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ITEM_DAMAGE,
            new net.minecraft.world.item.enchantment.effects.RemoveBinomial(net.minecraft.world.item.enchantment.LevelBasedValue.perLevel(0.10F))
        )
        .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "boost")));

        // 回声定位 (Echolocation) - 宝藏附魔
        entries.add(ModEnchantments.ECHOLOCATION, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                1,    // weight (very rare)
                3,    // max level (III)
                Enchantment.dynamicCost(20, 10),
                Enchantment.dynamicCost(70, 10),
                8,    // anvil cost per level
                EquipmentSlotGroup.HEAD
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "echolocation")));

        // 抱朴 (Purity) - 非宝藏附魔
        entries.add(ModEnchantments.PURITY, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                3,    // max level (III)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                2,    // anvil cost per level
                EquipmentSlotGroup.HEAD
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "purity")));

        // 衔月 (Moon Embrace) - 宝藏附魔
        entries.add(ModEnchantments.MOON_EMBRACE, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.HEAD
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "moon_embrace")));

        // 听涛 (Wave Listener) - 非宝藏附魔
        entries.add(ModEnchantments.WAVE_LISTENER, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.TRIDENT_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "wave_listener")));

        // 归鸿 (Returning Swan) - 非宝藏附魔
        entries.add(ModEnchantments.RETURNING_SWAN, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "returning_swan")));

        // 花间 (Among Flowers) - 非宝藏附魔
        entries.add(ModEnchantments.AMONG_FLOWERS, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.LEG_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                3,    // max level (III)
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(60, 8),
                2,    // anvil cost per level
                EquipmentSlotGroup.LEGS
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "among_flowers")));

        // 石心 (Stone Heart) - 非宝藏附魔
        entries.add(ModEnchantments.STONE_HEART, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.PICKAXES),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 9),
                Enchantment.dynamicCost(65, 9),
                4,    // anvil cost per level
                EquipmentSlotGroup.MAINHAND
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "stone_heart")));

        // 通惠 (Tonghui) - 无生存获取方式，附魔需要 20 级等级，重置物品累积惩罚为 0
        entries.add(ModEnchantments.TONGHUI, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.VANISHING_ENCHANTABLE),
                1,    // weight
                1,    // max level
                Enchantment.constantCost(20),
                Enchantment.constantCost(50),
                20,   // anvil cost
                EquipmentSlotGroup.ANY
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "tonghui")));

        // 药理 (Pharmacology) - 非宝藏附魔，附魔台 25~30 级
        entries.add(ModEnchantments.PHARMACOLOGY, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                2,    // weight (rare)
                3,    // max level (III)
                Enchantment.dynamicCost(25, 2),
                Enchantment.dynamicCost(50, 2),
                4,    // anvil cost per level
                EquipmentSlotGroup.HEAD
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "pharmacology")));

        // 休眠修复 (Dormant Repair) - 宝藏附魔，任意耐久物品
        entries.add(ModEnchantments.DORMANT_REPAIR, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
                1,    // weight (very rare)
                3,    // max level (III)
                Enchantment.dynamicCost(20, 10),
                Enchantment.dynamicCost(50, 10),
                8,    // anvil cost per level
                EquipmentSlotGroup.ANY
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "dormant_repair")));

        // 牧养 (Husbandry) - 非宝藏附魔，附魔台 15~25 级
        entries.add(ModEnchantments.HUSBANDRY, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.LEG_ARMOR_ENCHANTABLE),
                5,    // weight (uncommon)
                3,    // max level (III)
                Enchantment.dynamicCost(15, 4),
                Enchantment.dynamicCost(45, 4),
                2,    // anvil cost per level
                EquipmentSlotGroup.LEGS
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "husbandry")));

        // 代谢 (Metabolism) - 宝藏附魔，胸甲
        entries.add(ModEnchantments.METABOLISM, Enchantment.enchantment(
            Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                1,    // weight (very rare)
                3,    // max level (III)
                Enchantment.dynamicCost(20, 10),
                Enchantment.dynamicCost(50, 10),
                8,    // anvil cost per level
                EquipmentSlotGroup.CHEST
            )
        ).build(Identifier.fromNamespaceAndPath("tuanzis_mod", "metabolism")));
    }

    @Override
    public String getName() {
        return "Mod Enchantment Generator";
    }
}
