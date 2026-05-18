package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.item.ImmortalTalismanItem;
import me.tuanzi.item.YurisRevengeItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public class ModItems {
    public static final Item RAINBOW_SPONGE = register("rainbow_sponge", (properties) -> new Item(properties.stacksTo(1)));
    public static final Item YURIS_REVENGE = register("yuris_revenge", (properties) -> new YurisRevengeItem(properties.stacksTo(1).durability(5).rarity(Rarity.EPIC)));
    public static final Item IMMORTAL_TALISMAN = register("immortal_talisman", ImmortalTalismanItem::new);

    private static Item register(String path, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Item item = itemFactory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // 加载类
    }
}
