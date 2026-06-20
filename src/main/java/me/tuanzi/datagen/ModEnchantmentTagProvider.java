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
            .addOptional(ModEnchantments.SMELTING)
            .addOptional(ModEnchantments.CHAIN_PAIN)
            .addOptional(ModEnchantments.SEEKING_ARROW);

        builder(EnchantmentTags.IN_ENCHANTING_TABLE)
            .addOptional(ModEnchantments.EXPERIENCE)
            .addOptional(ModEnchantments.BLOOD_RAGE)
            .addOptional(ModEnchantments.BERSERKER)
            .addOptional(ModEnchantments.EXECUTE)
            .addOptional(ModEnchantments.BUZZING_RHYTHM)
            .addOptional(ModEnchantments.RESONANCE_PULSE);

        builder(EnchantmentTags.ON_RANDOM_LOOT)
            .addOptional(ModEnchantments.EXPERIENCE)
            .addOptional(ModEnchantments.BLOOD_RAGE)
            .addOptional(ModEnchantments.BERSERKER)
            .addOptional(ModEnchantments.EXECUTE)
            .addOptional(ModEnchantments.CHAIN_PAIN)
            .addOptional(ModEnchantments.SEEKING_ARROW)
            .addOptional(ModEnchantments.BUZZING_RHYTHM)
            .addOptional(ModEnchantments.RESONANCE_PULSE);

        builder(EnchantmentTags.TRADEABLE)
            .addOptional(ModEnchantments.EXPERIENCE)
            .addOptional(ModEnchantments.BLOOD_RAGE)
            .addOptional(ModEnchantments.BERSERKER)
            .addOptional(ModEnchantments.EXECUTE)
            .addOptional(ModEnchantments.BUZZING_RHYTHM)
            .addOptional(ModEnchantments.RESONANCE_PULSE);
            
        // 熔炼 (Smelting) 不加入 IN_ENCHANTING_TABLE, ON_RANDOM_LOOT, TRADEABLE (通用池)
        // 它将通过 ModLootTableModifiers 和 ModTrades 手动注入

        builder(ModEnchantments.EXCLUSIVE_NORMALIZATION)
            .addOptional(ModEnchantments.RESONANCE_PULSE)
            .addOptional(ModEnchantments.ABYSSAL_RHYTHM)
            .addOptional(ModEnchantments.BUZZING_RHYTHM);
    }
}
