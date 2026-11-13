package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags {
    public static final TagKey<Item> BOW_AND_CROSSBOW_ENCHANTABLE = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "enchantable/bow_and_crossbow")
    );
    public static final TagKey<Item> NODACHI_ENCHANTABLE = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "enchantable/nodachi")
    );
    public static final TagKey<Item> AXE_PICKAXE_HOE_ENCHANTABLE = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "enchantable/axe_pickaxe_hoe")
    );
    public static final TagKey<Item> ELYTRA_ENCHANTABLE = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "enchantable/elytra")
    );
    public static final TagKey<Item> BLOCK_BREAKER_TOOLS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block_breaker_tools")
    );
}
