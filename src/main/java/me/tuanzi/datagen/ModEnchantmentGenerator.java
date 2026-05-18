package me.tuanzi.datagen;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentGenerator extends FabricDynamicRegistryProvider {
    public ModEnchantmentGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
        var items = registries.lookupOrThrow(Registries.ITEM);

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
    }

    @Override
    public String getName() {
        return "Mod Enchantment Generator";
    }
}
