package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
    public static final Item COLOR_SPONGE = register("color_sponge", Item::new);

    private static Item register(String path, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Item item = itemFactory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // 加载类
    }
}
