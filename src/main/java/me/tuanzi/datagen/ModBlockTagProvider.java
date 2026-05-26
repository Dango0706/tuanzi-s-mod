package me.tuanzi.datagen;

import me.tuanzi.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider<Block> {
    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BLOCK, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // 将灵笼贸易站添加到镐子可加速挖掘的标签中
        builder(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(ModBlocks.SOUL_MERCHANT_STATION.builtInRegistryHolder().key());

        // 鉴于贸易站是高级黑科技设备且有防爆性能，设定其需要铁镐及以上工具挖掘
        builder(BlockTags.NEEDS_IRON_TOOL)
            .add(ModBlocks.SOUL_MERCHANT_STATION.builtInRegistryHolder().key());
    }
}
