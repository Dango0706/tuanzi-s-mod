package me.tuanzi.datagen;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

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
            .addOptional(ModEnchantments.SEEKING_ARROW)
            .addOptional(ModEnchantments.FINALITY);

        builder(EnchantmentTags.IN_ENCHANTING_TABLE)
            .addOptional(ModEnchantments.EXPERIENCE)
            .addOptional(ModEnchantments.BLOOD_RAGE)
            .addOptional(ModEnchantments.BERSERKER)
            .addOptional(ModEnchantments.EXECUTE)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_BLOOD_LEECH)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_METAL_CUTTER)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_LEATHER_PIERCER)
            .addOptional(ModEnchantments.RAIN_BLADE)
            .addOptional(ModEnchantments.FROST_BLADE)
            .addOptional(ModEnchantments.THUNDER_BLADE)
            .addOptional(ModEnchantments.SUN_BLADE)
            .addOptional(ModEnchantments.DAY_BLADE)
            .addOptional(ModEnchantments.NIGHT_BLADE)
            .addOptional(ModEnchantments.RANGER_RELOAD)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_RESERVED_CHAMBER)
            .addOptional(ModEnchantments.FLOW_STATE);

        builder(ModEnchantments.ANCIENT_SCROLL)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_BLOOD_LEECH)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_METAL_CUTTER)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_LEATHER_PIERCER)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_RESERVED_CHAMBER);

        builder(EnchantmentTags.ON_RANDOM_LOOT)
            .addOptional(ModEnchantments.EXPERIENCE)
            .addOptional(ModEnchantments.BLOOD_RAGE)
            .addOptional(ModEnchantments.BERSERKER)
            .addOptional(ModEnchantments.EXECUTE)
            .addOptional(ModEnchantments.CHAIN_PAIN)
            .addOptional(ModEnchantments.SEEKING_ARROW)
            .addOptional(ModEnchantments.RAIN_BLADE)
            .addOptional(ModEnchantments.FROST_BLADE)
            .addOptional(ModEnchantments.THUNDER_BLADE)
            .addOptional(ModEnchantments.SUN_BLADE)
            .addOptional(ModEnchantments.DAY_BLADE)
            .addOptional(ModEnchantments.NIGHT_BLADE)
            .addOptional(ModEnchantments.RANGER_RELOAD)
            .addOptional(ModEnchantments.FLOW_STATE)
            .addOptional(ModEnchantments.FINALITY);

        builder(EnchantmentTags.TRADEABLE)
            .addOptional(ModEnchantments.EXPERIENCE)
            .addOptional(ModEnchantments.BLOOD_RAGE)
            .addOptional(ModEnchantments.BERSERKER)
            .addOptional(ModEnchantments.EXECUTE)
            .addOptional(ModEnchantments.RAIN_BLADE)
            .addOptional(ModEnchantments.FROST_BLADE)
            .addOptional(ModEnchantments.THUNDER_BLADE)
            .addOptional(ModEnchantments.SUN_BLADE)
            .addOptional(ModEnchantments.DAY_BLADE)
            .addOptional(ModEnchantments.NIGHT_BLADE)
            .addOptional(ModEnchantments.RANGER_RELOAD)
            .addOptional(ModEnchantments.FLOW_STATE)
            .addOptional(ModEnchantments.FINALITY);
            
        // 熔炼 (Smelting) 不加入 IN_ENCHANTING_TABLE, ON_RANDOM_LOOT, TRADEABLE (通用池)
        // 它将通过 ModLootTableModifiers 和 ModTrades 手动注入

        builder(ModEnchantments.EXCLUSIVE_NORMALIZATION)
            .addOptional(ModEnchantments.RESONANCE_PULSE)
            .addOptional(ModEnchantments.ABYSSAL_RHYTHM)
            .addOptional(ModEnchantments.BUZZING_RHYTHM)
            .addOptional(ModEnchantments.VOID_RESONANCE)
            .addOptional(ModEnchantments.STEEL_SHIELD_GIFT)
            .addOptional(ModEnchantments.OVERLOAD_PROTOCOL);

        builder(ModEnchantments.EXCLUSIVE_WEATHER_BLADES)
            .addOptional(ModEnchantments.RAIN_BLADE)
            .addOptional(ModEnchantments.FROST_BLADE)
            .addOptional(ModEnchantments.THUNDER_BLADE)
            .addOptional(ModEnchantments.SUN_BLADE);

        builder(ModEnchantments.EXCLUSIVE_DAY_NIGHT_BLADES)
            .addOptional(ModEnchantments.DAY_BLADE)
            .addOptional(ModEnchantments.NIGHT_BLADE);

        builder(ModEnchantments.EXCLUSIVE_RESERVED_CHAMBER)
            .addOptional(Enchantments.QUICK_CHARGE)
            .addOptional(Enchantments.MULTISHOT)
            .addOptional(ModEnchantments.RANGER_RELOAD)
            .addOptional(ModEnchantments.ANCIENT_SCROLL_RESERVED_CHAMBER);
    }
}
