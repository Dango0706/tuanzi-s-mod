package me.tuanzi.init;

import me.tuanzi.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Function;

public class ModItems {
    public static final Item SCARECROW = register("scarecrow", ScarecrowItem::new);
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
            .sword(net.minecraft.world.item.ToolMaterial.NETHERITE, 8.0F, -2.8F)
            .durability(1550)
            .rarity(Rarity.RARE)
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(ModItems.WARDEN_HEART.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item BEE_STING_ECHO = register("bee_sting_echo", (properties) -> {
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.9, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "bee_sting_echo_reach"), -0.25, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
        return new BeeStingEchoItem(properties
            .sword(net.minecraft.world.item.ToolMaterial.NETHERITE, 6.0F, -1.9F)
            .durability(2031)
            .rarity(Rarity.EPIC)
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(ModItems.WARDEN_HEART.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item STEEL_BARRIER = register("steel_barrier", (properties) -> {
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.7, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
        return new SteelBarrierItem(properties
            .sword(net.minecraft.world.item.ToolMaterial.NETHERITE, 6.7F, -2.4F)
            .durability(2031)
            .rarity(Rarity.EPIC)
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(net.minecraft.world.item.Items.NETHERITE_INGOT.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item RIFT_SCAR = register("rift_scar", (properties) -> {
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.12, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
        return new RiftScarItem(properties
            .sword(net.minecraft.world.item.ToolMaterial.NETHERITE, 4.12F, -2.0F)
            .durability(2031)
            .rarity(Rarity.EPIC)
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(net.minecraft.world.item.Items.ENDER_EYE.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item SCULLY_SHARD = register("scully_shard", (properties) -> {
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.75, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
        return new ScullyShardItem(properties
            .sword(net.minecraft.world.item.ToolMaterial.NETHERITE, 9.0F, -2.75F)
            .durability(2031)
            .rarity(Rarity.EPIC)
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(ModItems.WARDEN_HEART.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item TIDE_CLEAVER = register("tide_cleaver", (properties) -> {
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.34, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
        return new TideCleaverItem(properties
            .sword(net.minecraft.world.item.ToolMaterial.NETHERITE, 6.3F, -2.34F)
            .durability(2031)
            .rarity(Rarity.EPIC)
            .component(DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(net.minecraft.world.item.Items.HEART_OF_THE_SEA.builtInRegistryHolder())))
            .attributes(modifiers));
    });

    public static final Item VILLAGER_CAGE = register("villager_cage", (properties) -> new me.tuanzi.item.VillagerCageItem(properties.stacksTo(1).durability(1).rarity(Rarity.UNCOMMON)));
    public static final Item TRAVELERS_NOTEBOOK = register("travelers_notebook", (properties) -> new me.tuanzi.item.TravelersNotebookItem(properties.stacksTo(1).rarity(Rarity.RARE)));
    public static final Item CRAFTSMAN_CHARM = register("craftsman_charm", (properties) -> new me.tuanzi.item.CraftsmanCharmItem(properties.stacksTo(1).rarity(Rarity.RARE)));
    public static final Item TELEPORTATION_PAPER = register("teleportation_paper", (properties) -> new me.tuanzi.item.TeleportationPaperItem(properties.stacksTo(64).rarity(net.minecraft.world.item.Rarity.COMMON)));
    public static final Item SIGNPOST_RUNE = register("signpost_rune", (properties) -> new me.tuanzi.item.SignpostRuneItem(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final Item SHURIKEN = register("shuriken", (properties) -> new me.tuanzi.item.ShurikenItem(properties));
    public static final Item NETHER_STEW = register("nether_stew", (properties) -> new me.tuanzi.item.NetherStewItem(properties
        .stacksTo(64)
        .food(new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(7)
            .saturationModifier(0.57142857f)
            .alwaysEdible()
            .build())
    ));

    public static final Item STAR_TRAVEL_CARD_PACK = register("star_travel_card_pack", (properties) -> new me.tuanzi.item.GachaItem(properties.stacksTo(64).rarity(Rarity.UNCOMMON), "normal", false));
    public static final Item STAR_TRAVEL_CARD_CHEST = register("star_travel_card_chest", (properties) -> new me.tuanzi.item.GachaItem(properties.stacksTo(64).rarity(Rarity.RARE), "normal", true));
    public static final Item SAKURA_FESTIVAL_CARD_PACK = register("sakura_festival_card_pack", (properties) -> new me.tuanzi.item.GachaItem(properties.stacksTo(64).rarity(Rarity.EPIC), "sakura_moon", false));
    public static final Item SAKURA_FESTIVAL_CARD_CHEST = register("sakura_festival_card_chest", (properties) -> new me.tuanzi.item.GachaItem(properties.stacksTo(64).rarity(Rarity.EPIC), "sakura_moon", true));
    public static final Item TIDAL_WEAVE_BOOTS = register("tidal_weave_boots", (properties) -> new Item(properties
        .humanoidArmor(ModArmorMaterials.TIDAL_WEAVE, ArmorType.BOOTS)
        .rarity(Rarity.EPIC)
    ));
    public static final Item WORLD_SCULPTORS_PEN = register("world_sculptors_pen", (properties) -> new me.tuanzi.item.WorldSculptorsPenItem(properties
        .stacksTo(1)
        .durability(8192)
        .rarity(Rarity.EPIC)
    ));
    public static final Item VOID_INK_INGOT = register("void_ink_ingot", (properties) -> new Item(properties
        .rarity(Rarity.EPIC)
    ) {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
            tooltipComponents.accept(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.void_ink_ingot.desc_line1").withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltipComponents.accept(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.void_ink_ingot.desc_line2").withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltipComponents.accept(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.void_ink_ingot.desc_line3").withStyle(net.minecraft.ChatFormatting.GRAY));
            super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        }
    });


    private static Item register(String path, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Item item = itemFactory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // 加载类
    }
}
