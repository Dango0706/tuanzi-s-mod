package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> SOULBOUND = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "soulbound"));
    public static final ResourceKey<Enchantment> EXPERIENCE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "experience"));
    public static final ResourceKey<Enchantment> SMELTING = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "smelting"));
    public static final ResourceKey<Enchantment> CHAIN_MINING = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "chain_mining"));
    public static final ResourceKey<Enchantment> BLOOD_RAGE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blood_rage"));
    public static final ResourceKey<Enchantment> BERSERKER = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "berserker"));
    public static final ResourceKey<Enchantment> EXECUTE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "execute"));
    public static final ResourceKey<Enchantment> CHAIN_PAIN = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "chain_pain"));
    public static final ResourceKey<Enchantment> SEEKING_ARROW = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "seeking_arrow"));
    public static final ResourceKey<Enchantment> BUZZING_RHYTHM = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "buzzing_rhythm"));
    public static final ResourceKey<Enchantment> RESONANCE_PULSE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "resonance_pulse"));
    public static final ResourceKey<Enchantment> ABYSSAL_RHYTHM = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "abyssal_rhythm"));

    public static final net.minecraft.tags.TagKey<Enchantment> EXCLUSIVE_NORMALIZATION = net.minecraft.tags.TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "exclusive_normalization"));

    public static void initialize() {
        // 仅用于加载此类
    }
}
