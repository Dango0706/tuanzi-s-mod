package me.tuanzi.init;

import me.tuanzi.item.EchoBreakerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.item.BerserkCharmItem;
import me.tuanzi.item.ImmortalTalismanItem;
import me.tuanzi.item.YurisRevengeItem;
import me.tuanzi.item.WolfCommandItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public class ModItems {
    public static final Item SCARECROW = register("scarecrow", (properties) -> new me.tuanzi.item.ScarecrowItem(properties));
    public static final Item TRIAL_DUMMY = register("trial_dummy", (properties) -> new me.tuanzi.item.TrialDummyItem(properties.stacksTo(1).durability(32)));
    public static final Item DECOY_TOTEM = register("decoy_totem", (properties) -> new me.tuanzi.item.DecoyTotemItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final Item RAINBOW_SPONGE = register("rainbow_sponge", (properties) -> new Item(properties.stacksTo(1)));
    public static final Item YURIS_REVENGE = register("yuris_revenge", (properties) -> new YurisRevengeItem(properties.stacksTo(1).durability(5).rarity(Rarity.EPIC)));
    public static final Item IMMORTAL_TALISMAN = register("immortal_talisman", ImmortalTalismanItem::new);
    public static final Item BERSERK_CHARM = register("berserk_charm", BerserkCharmItem::new);
    public static final Item WOLF_COMMAND = register("wolf_command", WolfCommandItem::new);
    public static final Item WARDEN_HEART = register("warden_heart", (properties) -> new Item(properties.rarity(Rarity.UNCOMMON)));
    public static final Item ECHO_BREAKER = register("echo_breaker", (properties) -> {
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
        return new EchoBreakerItem(properties
            .durability(1550)
            .rarity(Rarity.RARE)
            .component(DataComponents.WEAPON, new Weapon(1))
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(ModItems.WARDEN_HEART.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item VILLAGER_CAGE = register("villager_cage", (properties) -> new me.tuanzi.item.VillagerCageItem(properties.stacksTo(1).durability(1).rarity(Rarity.UNCOMMON)));
    public static final Item TRAVELERS_NOTEBOOK = register("travelers_notebook", (properties) -> new me.tuanzi.item.TravelersNotebookItem(properties.stacksTo(1).rarity(Rarity.RARE)));
    public static final Item TELEPORTATION_PAPER = register("teleportation_paper", (properties) -> new me.tuanzi.item.TeleportationPaperItem(properties.stacksTo(64).rarity(net.minecraft.world.item.Rarity.COMMON)));
    public static final Item SIGNPOST_RUNE = register("signpost_rune", (properties) -> new me.tuanzi.item.SignpostRuneItem(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    private static Item register(String path, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Item item = itemFactory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // 加载类
    }
}
