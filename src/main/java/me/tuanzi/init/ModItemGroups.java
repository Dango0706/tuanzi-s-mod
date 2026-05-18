package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ModItemGroups {
    public static final CreativeModeTab TUANZIS_MOD_TAB = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "tuanzis_mod_tab"),
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.tuanzis_mod.tuanzis_mod_tab"))
            .icon(() -> new ItemStack(Items.ENCHANTED_BOOK)) 
            .displayItems((displayContext, entries) -> {
                var enchantmentRegistry = displayContext.holders().lookupOrThrow(Registries.ENCHANTMENT);
                var soulbound = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);
                var experience = enchantmentRegistry.getOrThrow(ModEnchantments.EXPERIENCE);
                
                ItemStack soulboundBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(soulboundBook, mutable -> mutable.set(soulbound, 1));
                entries.accept(soulboundBook);

                ItemStack experienceBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(experienceBook, mutable -> mutable.set(experience, 4));
                entries.accept(experienceBook);

                // 加入彩虹海绵
                entries.accept(ModItems.RAINBOW_SPONGE);
                // 加入尤里的复仇
                entries.accept(ModItems.YURIS_REVENGE);
                // 加入不朽圣符
                entries.accept(ModItems.IMMORTAL_TALISMAN);

                // 加入飞行药水及其变体
                addPotionVariants(entries, ModPotions.FLIGHT_POTION);
                addPotionVariants(entries, ModPotions.LONG_FLIGHT_POTION);

                // 加入不死药水及其变体
                addPotionVariants(entries, ModPotions.UNDYING_POTION);
                addPotionVariants(entries, ModPotions.LONG_UNDYING_POTION);
            })
            .build()
    );

    private static void addPotionVariants(CreativeModeTab.Output entries, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.SPLASH_POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.LINGERING_POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.TIPPED_ARROW, potion));
    }

    public static void initialize() {
    }
}
