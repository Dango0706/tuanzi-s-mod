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
                
                ItemStack soulboundBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(soulboundBook, mutable -> mutable.set(soulbound, 1));
                entries.accept(soulboundBook);
            })
            .build()
    );

    public static void initialize() {
    }
}
