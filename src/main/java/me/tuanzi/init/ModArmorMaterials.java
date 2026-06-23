package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class ModArmorMaterials {
    public static final TagKey<Item> REPAIRS_TIDAL_WEAVE_BOOTS = TagKey.create(
        Registries.ITEM, 
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "repairs_tidal_weave_boots")
    );

    public static final ResourceKey<EquipmentAsset> TIDAL_WEAVE_ASSET = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "tidal_weave")
    );

    public static final ArmorMaterial TIDAL_WEAVE = new ArmorMaterial(
        33, // durability multiplier. boots durability = 13 * 33 = 429
        ArmorMaterials.makeDefense(3, 6, 8, 3, 11), // same defense as diamond
        10, // enchantment value (same as diamond)
        SoundEvents.ARMOR_EQUIP_DIAMOND, // equip sound
        2.0F, // toughness (same as diamond)
        0.0F, // knockback resistance (same as diamond)
        REPAIRS_TIDAL_WEAVE_BOOTS, // repairs tag
        TIDAL_WEAVE_ASSET // asset id
    );
}
