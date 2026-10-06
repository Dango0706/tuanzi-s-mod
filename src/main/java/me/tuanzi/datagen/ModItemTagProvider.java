package me.tuanzi.datagen;

import me.tuanzi.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagsProvider<Item> {
    public ModItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.ITEM, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // 将回响破障者和蜂刺余响归入"剑 (swords)"这一类 Item 标签中
        builder(ItemTags.SWORDS)
            .add(ModItems.ECHO_BREAKER.builtInRegistryHolder().key())
            .add(ModItems.BEE_STING_ECHO.builtInRegistryHolder().key())
            .add(ModItems.RIFT_SCAR.builtInRegistryHolder().key())
            .add(ModItems.SCULLY_SHARD.builtInRegistryHolder().key())
            .add(ModItems.TIDE_CLEAVER.builtInRegistryHolder().key())
            .add(ModItems.STEEL_BARRIER.builtInRegistryHolder().key())
            .add(ModItems.PULSE_RESONANCE_SWORD.builtInRegistryHolder().key())
            .add(ModItems.WOODEN_NODACHI.builtInRegistryHolder().key())
            .add(ModItems.STONE_NODACHI.builtInRegistryHolder().key())
            .add(ModItems.IRON_NODACHI.builtInRegistryHolder().key())
            .add(ModItems.GOLDEN_NODACHI.builtInRegistryHolder().key())
            .add(ModItems.DIAMOND_NODACHI.builtInRegistryHolder().key())
            .add(ModItems.NETHERITE_NODACHI.builtInRegistryHolder().key());

        // 将潮汐织靴加入原版 boots 标签
        builder(ItemTags.FOOT_ARMOR)
            .add(ModItems.TIDAL_WEAVE_BOOTS.builtInRegistryHolder().key());

        // 注册潮汐织靴的修补材料标签
        builder(me.tuanzi.init.ModArmorMaterials.REPAIRS_TIDAL_WEAVE_BOOTS)
            .add(net.minecraft.world.item.Items.PHANTOM_MEMBRANE.builtInRegistryHolder().key())
            .add(net.minecraft.world.item.Items.PRISMARINE_CRYSTALS.builtInRegistryHolder().key());

        // 注册贯革适用的弓与弩标签
        builder(me.tuanzi.init.ModItemTags.BOW_AND_CROSSBOW_ENCHANTABLE)
            .add(net.minecraft.world.item.Items.BOW.builtInRegistryHolder().key())
            .add(net.minecraft.world.item.Items.CROSSBOW.builtInRegistryHolder().key())
            .addOptionalTag(ItemTags.BOW_ENCHANTABLE)
            .addOptionalTag(ItemTags.CROSSBOW_ENCHANTABLE);
    }
}
