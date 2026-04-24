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

        // 26.1 附魔定义
        Enchantment.EnchantmentDefinition definition = Enchantment.definition(
            items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
            1,    // weight (minimized)
            1,    // max level
            Enchantment.constantCost(10), 
            Enchantment.constantCost(50), 
            4,    // anvil cost
            EquipmentSlotGroup.ANY
        );

        // 26.1 使用 enchantment() 启动构建
        entries.add(ModEnchantments.SOULBOUND, Enchantment.enchantment(definition)
            .build(Identifier.fromNamespaceAndPath("tuanzis_mod", "soulbound")));
    }

    @Override
    public String getName() {
        return "Mod Enchantment Generator";
    }
}
