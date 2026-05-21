package me.tuanzi.datagen;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentTagProvider extends FabricTagsProvider<Enchantment> {
    public ModEnchantmentTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.ENCHANTMENT, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        builder(EnchantmentTags.TREASURE)
            .addOptional(ModEnchantments.SOULBOUND)
            .addOptional(ModEnchantments.SMELTING);

        builder(EnchantmentTags.IN_ENCHANTING_TABLE)
            .addOptional(ModEnchantments.EXPERIENCE);

        builder(EnchantmentTags.ON_RANDOM_LOOT)
            .addOptional(ModEnchantments.SOULBOUND)
            .addOptional(ModEnchantments.EXPERIENCE);

        builder(EnchantmentTags.TRADEABLE)
            .addOptional(ModEnchantments.SOULBOUND)
            .addOptional(ModEnchantments.EXPERIENCE);
            
        // 熔炼 (Smelting) 不加入 IN_ENCHANTING_TABLE, ON_RANDOM_LOOT, TRADEABLE (通用池)
        // 它将通过 ModLootTableModifiers 和 ModTrades 手动注入
    }
}
