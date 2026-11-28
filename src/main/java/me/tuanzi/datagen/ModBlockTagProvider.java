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
                .add(ModBlocks.BLOCK_PLACER.builtInRegistryHolder().key())
                .add(ModBlocks.PRINTER_3D.builtInRegistryHolder().key())
                .add(ModBlocks.COPIER.builtInRegistryHolder().key())
                .add(ModBlocks.PAPER_SHREDDER.builtInRegistryHolder().key())
                .add(ModBlocks.PRINTED_MODEL.builtInRegistryHolder().key())
                .add(ModBlocks.PRINTED_MODEL_PART.builtInRegistryHolder().key())
                .addAll(ModBlocks.ELEVATORS.stream().map(b -> b.builtInRegistryHolder().key()).toList())
                .add(casinodecoKey("glass_coffee_table"))
                .add(casinodecoKey("casino_chandelier"))
                .add(casinodecoKey("velvet_rope_post"))
                .add(casinodecoKey("glass_table"))
                .add(casinodecoKey("ivory_black_checkers"))
                .add(casinodecoKey("jade_inlay"))
                .add(casinodecoKey("ivory_marble"))
                .add(casinodecoKey("obsidian_marble"))
                .add(casinodecoKey("gold_veined_black_marble"))
                .add(casinodecoKey("ivory_marble_bar"))
                .add(casinodecoKey("black_marble_bar"))
                .add(casinodecoKey("chandelier_wall_sconce"))
                .add(casinodecoKey("reception_plaque"))
                .add(casinodecoKey("vip_only_plaque"))
                .add(casinodecoKey("serving_tray"))
                .add(casinodecoKey("ivory_black_checkers_stairs"))
                .add(casinodecoKey("ivory_black_checkers_slab"))
                .add(casinodecoKey("ivory_marble_stairs"))
                .add(casinodecoKey("ivory_marble_slab"))
                .add(casinodecoKey("obsidian_marble_stairs"))
                .add(casinodecoKey("obsidian_marble_slab"))
                .add(casinodecoKey("gold_veined_black_marble_stairs"))
                .add(casinodecoKey("gold_veined_black_marble_slab"));

        builder(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.EASEL.builtInRegistryHolder().key())
                .add(casinodecoKey("casino_sofa"))
                .add(casinodecoKey("casino_sign"))
                .add(casinodecoKey("small_casino_sign"))
                .add(casinodecoKey("wood_coffee_table"))
                .add(casinodecoKey("wood_table"))
                .add(casinodecoKey("dark_oak_paneling"))
                .add(casinodecoKey("gilded_dark_oak_paneling"))
                .add(casinodecoKey("dark_oak_baseboard"))
                .add(casinodecoKey("dark_oak_cornice"))
                .add(casinodecoKey("dark_oak_dado_rail"))
                .add(casinodecoKey("dark_oak_pilaster"))
                .add(casinodecoKey("dark_oak_diamond_panel"))
                .add(casinodecoKey("classic_gilded_dark_oak_paneling"))
                .add(casinodecoKey("classic_dark_oak_baseboard"))
                .add(casinodecoKey("classic_dark_oak_cornice"))
                .add(casinodecoKey("classic_dark_oak_dado_rail"))
                .add(casinodecoKey("classic_dark_oak_pilaster"))
                .add(casinodecoKey("classic_dark_oak_diamond_panel"))
                .add(casinodecoKey("walnut_bar"))
                .add(casinodecoKey("chip_case"))
                .add(casinodecoKey("cash_register"))
                .add(casinodecoKey("bottle_display_shelf"))
                .add(casinodecoKey("glass_display_shelf"));

        builder(BlockTags.SLABS)
                .add(casinodecoKey("ruby_velvet_slab"))
                .add(casinodecoKey("emerald_velvet_slab"))
                .add(casinodecoKey("sapphire_velvet_slab"))
                .add(casinodecoKey("ivory_black_checkers_slab"))
                .add(casinodecoKey("ivory_marble_slab"))
                .add(casinodecoKey("obsidian_marble_slab"))
                .add(casinodecoKey("gold_veined_black_marble_slab"));

        builder(BlockTags.STAIRS)
                .add(casinodecoKey("ruby_velvet_stairs"))
                .add(casinodecoKey("emerald_velvet_stairs"))
                .add(casinodecoKey("sapphire_velvet_stairs"))
                .add(casinodecoKey("ivory_black_checkers_stairs"))
                .add(casinodecoKey("ivory_marble_stairs"))
                .add(casinodecoKey("obsidian_marble_stairs"))
                .add(casinodecoKey("gold_veined_black_marble_stairs"));

        builder(BlockTags.WOOL)
                .add(casinodecoKey("ruby_trellis"))
                .add(casinodecoKey("ruby_stars"))
                .add(casinodecoKey("ruby_velvet"))
                .add(casinodecoKey("emerald_trellis"))
                .add(casinodecoKey("emerald_stars"))
                .add(casinodecoKey("emerald_velvet"))
                .add(casinodecoKey("sapphire_trellis"))
                .add(casinodecoKey("sapphire_stars"))
                .add(casinodecoKey("sapphire_velvet"));

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

        builder(me.tuanzi.init.ModBlockTags.ELEVATORS)
                .addAll(ModBlocks.ELEVATORS.stream().map(b -> b.builtInRegistryHolder().key()).toList());
    }

    private static net.minecraft.resources.ResourceKey<Block> casinodecoKey(String path) {
        return net.minecraft.resources.ResourceKey.create(
                Registries.BLOCK,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("casinodeco", path)
        );
    }
}
