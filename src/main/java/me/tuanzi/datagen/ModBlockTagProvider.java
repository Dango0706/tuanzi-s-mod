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
                .add(ModBlocks.SOUL_MERCHANT_STATION.builtInRegistryHolder().key())
                .add(ModBlocks.BLUEPRINT_CANNON.builtInRegistryHolder().key())
                .add(ModBlocks.BLUEPRINT_TABLE.builtInRegistryHolder().key())
                .add(ModBlocks.PLAYER_SIMULATOR.builtInRegistryHolder().key())
                .add(ModBlocks.BLACKBOX_CORE_CASE.builtInRegistryHolder().key())
                .add(ModBlocks.BASE_STASH_CABINET.builtInRegistryHolder().key())
                .add(ModBlocks.BASE_UPGRADE_STATION.builtInRegistryHolder().key())
                .add(ModBlocks.BLOCK_BREAKER.builtInRegistryHolder().key())
                .add(ModBlocks.BLOCK_PLACER.builtInRegistryHolder().key());

        // 方块破坏器黑名单
        builder(me.tuanzi.init.ModBlockTags.BLOCK_BREAKER_BLACKLIST)
                .add(net.minecraft.world.level.block.Blocks.BEDROCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.BARRIER.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.COMMAND_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.CHAIN_COMMAND_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.REPEATING_COMMAND_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.JIGSAW.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.END_PORTAL.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.END_GATEWAY.builtInRegistryHolder().key());

        // 方块放置器黑名单
        builder(me.tuanzi.init.ModBlockTags.BLOCK_PLACER_BLACKLIST)
                .add(net.minecraft.world.level.block.Blocks.BEDROCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.BARRIER.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.COMMAND_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.CHAIN_COMMAND_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.REPEATING_COMMAND_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.JIGSAW.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.SPAWNER.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.TRIAL_SPAWNER.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.VAULT.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.END_PORTAL.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.END_GATEWAY.builtInRegistryHolder().key())
                .add(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE.builtInRegistryHolder().key());

        // 鉴于贸易站是高级黑科技设备且有防爆性能，设定其需要铁镐及以上工具挖掘
        builder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.SOUL_MERCHANT_STATION.builtInRegistryHolder().key())
                .add(ModBlocks.BLUEPRINT_CANNON.builtInRegistryHolder().key())
                .add(ModBlocks.BLUEPRINT_TABLE.builtInRegistryHolder().key())
                .add(ModBlocks.BLACKBOX_CORE_CASE.builtInRegistryHolder().key())
                .add(ModBlocks.BASE_STASH_CABINET.builtInRegistryHolder().key())
                .add(ModBlocks.BASE_UPGRADE_STATION.builtInRegistryHolder().key());

        builder(me.tuanzi.init.ModBlockTags.SNOW_STRIDER_BLOCKS)
                .addOptionalTag(BlockTags.SNOW)
                .addOptionalTag(BlockTags.ICE);
    }
}
