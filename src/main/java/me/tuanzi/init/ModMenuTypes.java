package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.inventory.TravelersNotebookMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;

public class ModMenuTypes {
    public static final ResourceKey<MenuType<?>> TRAVELERS_NOTEBOOK_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "travelers_notebook"));
    public static final ResourceKey<MenuType<?>> CRAFTSMAN_CHARM_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "craftsman_charm"));
    
    public static final MenuType<TravelersNotebookMenu> TRAVELERS_NOTEBOOK = Registry.register(
            BuiltInRegistries.MENU,
            TRAVELERS_NOTEBOOK_KEY,
            new MenuType<>((containerId, playerInventory) -> new TravelersNotebookMenu(containerId, playerInventory, net.minecraft.world.InteractionHand.MAIN_HAND), FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.CraftsmanCharmMenu> CRAFTSMAN_CHARM = Registry.register(
            BuiltInRegistries.MENU,
            CRAFTSMAN_CHARM_KEY,
            new MenuType<>((containerId, playerInventory) -> new me.tuanzi.world.inventory.CraftsmanCharmMenu(containerId, playerInventory, net.minecraft.world.InteractionHand.MAIN_HAND), FeatureFlags.VANILLA_SET)
    );

    public static void initialize() {
        // 用于类加载注册
    }
}
