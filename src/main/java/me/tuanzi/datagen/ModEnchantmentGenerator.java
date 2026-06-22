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
    }

    @Override
    public String getName() {
        return "Mod Enchantment Generator";
    }
}
