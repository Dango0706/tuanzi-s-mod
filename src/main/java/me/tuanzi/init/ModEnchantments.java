package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> SOULBOUND = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "soulbound"));
    public static final ResourceKey<Enchantment> EXPERIENCE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "experience"));

    public static void initialize() {
        // 仅用于加载此类
    }
}
