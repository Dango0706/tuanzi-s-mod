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
    public static final ResourceKey<MenuType<?>> BLUEPRINT_CANNON_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_cannon"));
    public static final ResourceKey<MenuType<?>> BLUEPRINT_TABLE_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_table"));
    public static final ResourceKey<MenuType<?>> PLAYER_SIMULATOR_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "player_simulator"));
    public static final ResourceKey<MenuType<?>> GOLDEN_POCKET_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "golden_pocket"));
    public static final ResourceKey<MenuType<?>> CORRIDOR_TACTICAL_LOADOUT_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "corridor_tactical_loadout"));
    public static final ResourceKey<MenuType<?>> SCAVENGER_TACTICAL_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_tactical"));
    public static final ResourceKey<MenuType<?>> BASE_STASH_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "base_stash"));
    public static final ResourceKey<MenuType<?>> BASE_UPGRADE_STATION_KEY = ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "base_upgrade_station"));

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

    public static final MenuType<me.tuanzi.world.inventory.BlueprintCannonMenu> BLUEPRINT_CANNON = Registry.register(
            BuiltInRegistries.MENU,
            BLUEPRINT_CANNON_KEY,
            new MenuType<>(me.tuanzi.world.inventory.BlueprintCannonMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.BlueprintTableMenu> BLUEPRINT_TABLE = Registry.register(
            BuiltInRegistries.MENU,
            BLUEPRINT_TABLE_KEY,
            new MenuType<>(me.tuanzi.world.inventory.BlueprintTableMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.PlayerSimulatorMenu> PLAYER_SIMULATOR = Registry.register(
            BuiltInRegistries.MENU,
            PLAYER_SIMULATOR_KEY,
            new MenuType<>(me.tuanzi.world.inventory.PlayerSimulatorMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.GoldenPocketMenu> GOLDEN_POCKET = Registry.register(
            BuiltInRegistries.MENU,
            GOLDEN_POCKET_KEY,
            new MenuType<>(me.tuanzi.world.inventory.GoldenPocketMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.scavenger.CorridorTacticalLoadoutMenu> CORRIDOR_TACTICAL_LOADOUT = Registry.register(
            BuiltInRegistries.MENU,
            CORRIDOR_TACTICAL_LOADOUT_KEY,
            new MenuType<>(me.tuanzi.world.inventory.scavenger.CorridorTacticalLoadoutMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.scavenger.ScavengerTacticalMenu> SCAVENGER_TACTICAL = Registry.register(
            BuiltInRegistries.MENU,
            SCAVENGER_TACTICAL_KEY,
            new MenuType<>(me.tuanzi.world.inventory.scavenger.ScavengerTacticalMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.scavenger.BaseStashMenu> BASE_STASH = Registry.register(
            BuiltInRegistries.MENU,
            BASE_STASH_KEY,
            new MenuType<>(me.tuanzi.world.inventory.scavenger.BaseStashMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final MenuType<me.tuanzi.world.inventory.scavenger.BaseUpgradeStationMenu> BASE_UPGRADE_STATION = Registry.register(
            BuiltInRegistries.MENU,
            BASE_UPGRADE_STATION_KEY,
            new MenuType<>(me.tuanzi.world.inventory.scavenger.BaseUpgradeStationMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static void initialize() {
        // 用于类加载注册
    }
}
