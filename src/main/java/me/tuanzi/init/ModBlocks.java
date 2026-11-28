package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.block.BlueprintCannonBlock;
import me.tuanzi.block.BlueprintTableBlock;
import me.tuanzi.block.SoulMerchantStationBlock;
import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
import me.tuanzi.block.entity.BlueprintTableBlockEntity;
import me.tuanzi.block.entity.SoulMerchantStationBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {
    public static final Block SOUL_MERCHANT_STATION = registerBlock("soul_merchant_station",
            properties -> new SoulMerchantStationBlock(properties.strength(3.5f, 1200.0f).noOcclusion())); // 免疫爆炸

    public static final Block BLUEPRINT_CANNON = registerBlock("blueprint_cannon",
            properties -> new BlueprintCannonBlock(properties.strength(3.0f, 6.0f).noOcclusion()));

    public static final Block BLUEPRINT_TABLE = registerBlock("blueprint_table",
            properties -> new BlueprintTableBlock(properties.strength(2.5f, 5.0f).noOcclusion()));

    public static final Block PLAYER_SIMULATOR = registerBlock("player_simulator",
            properties -> new me.tuanzi.block.PlayerSimulatorBlock(properties.strength(3.5f, 6.0f).noOcclusion().isRedstoneConductor((state, level, pos) -> false)));

    public static final Block BLOCK_BREAKER = registerBlock("block_breaker",
            properties -> new me.tuanzi.block.BlockBreakerBlock(properties.strength(3.5f, 3.5f).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.PUSH_PULL)));

    public static final Block BLOCK_PLACER = registerBlock("block_placer",
            properties -> new me.tuanzi.block.BlockPlacerBlock(properties.strength(3.5f, 3.5f).requiresCorrectToolForDrops().noOcclusion()));

    public static final Block COLOR_BLOCK = registerBlock("color_block",
            properties -> new me.tuanzi.block.ColorBlock(properties.strength(0.5f, 3.0f)));

    public static final Block COLOR_SLAB = registerBlock("color_slab",
            properties -> new me.tuanzi.block.ColorSlab(properties.strength(0.5f, 3.0f)));

    public static final Block COLOR_STAIRS = registerBlock("color_stairs",
            properties -> new me.tuanzi.block.ColorStairs(COLOR_BLOCK.defaultBlockState(), properties.strength(0.5f, 3.0f)));

    public static final Block BLACKBOX_CORE_CASE = registerBlock("blackbox_core_case",
            properties -> new me.tuanzi.block.BlackboxCoreCaseBlock(properties.strength(3.5f, 600.0f).noOcclusion()));

    public static final Block BASE_STASH_CABINET = registerBlock("base_stash_cabinet",
            properties -> new me.tuanzi.block.BaseStashCabinetBlock(properties.strength(3.5f, 600.0f).noOcclusion()));

    public static final Block BASE_UPGRADE_STATION = registerBlock("base_upgrade_station",
            properties -> new me.tuanzi.block.BaseUpgradeStationBlock(properties.strength(3.5f, 600.0f).noOcclusion()));

    // 画板与可打印画布系统方块
    public static final Block EASEL = registerBlock("easel",
            properties -> new me.tuanzi.block.EaselBlock(properties.strength(2.0f, 3.0f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));

    public static final Block PRINTER_3D = registerBlock("printer_3d",
            properties -> new me.tuanzi.block.Printer3DBlock(properties.strength(3.5f, 6.0f).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));

    public static final Block COPIER = registerBlock("copier",
            properties -> new me.tuanzi.block.CopierBlock(properties.strength(3.5f, 6.0f).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));

    public static final Block PAPER_SHREDDER = registerBlock("paper_shredder",
            properties -> new me.tuanzi.block.PaperShredderBlock(properties.strength(3.5f, 6.0f).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));

    public static final Block PRINTED_MODEL = registerBlockWithCustomItem("printed_model",
            properties -> new me.tuanzi.block.PrintedModelBlock(properties.strength(1.5f, 3.0f).noOcclusion().dynamicShape()),
            (block, itemProps) -> new me.tuanzi.item.PrintedModelBlockItem(block, itemProps));

    public static final Block PRINTED_MODEL_PART = registerBlockNoItem("printed_model_part",
            properties -> new me.tuanzi.block.PrintedModelPartBlock(properties.strength(1.5f, 3.0f).noOcclusion().dynamicShape()));

    public static final Block CANVAS_BASE_BLOCK = registerBlockNoItem("canvas_base_block",
            properties -> new me.tuanzi.block.CanvasBaseBlock(properties.strength(0.4f, 1.0f).sound(net.minecraft.world.level.block.SoundType.STONE)));

    // 棋盘系统方块（五子棋、中国象棋、国际象棋）
    public static final Block GOMOKU = registerBlock("gomoku",
            properties -> new me.tuanzi.block.board.GomokuBlock(properties));

    public static final Block CCHESS = registerBlock("cchess",
            properties -> new me.tuanzi.block.board.CChessBlock(properties));

    public static final Block WCHESS = registerBlock("wchess",
            properties -> new me.tuanzi.block.board.WChessBlock(properties));

    // 16 种原版染料颜色电梯方块（硬度 4，需镐子挖掘）
    public static final java.util.Map<net.minecraft.world.item.DyeColor, Block> ELEVATORS_BY_COLOR = registerElevators();
    public static final java.util.List<Block> ELEVATORS = java.util.List.copyOf(ELEVATORS_BY_COLOR.values());
    public static final Block WHITE_ELEVATOR = ELEVATORS_BY_COLOR.get(net.minecraft.world.item.DyeColor.WHITE);

    private static java.util.Map<net.minecraft.world.item.DyeColor, Block> registerElevators() {
        java.util.EnumMap<net.minecraft.world.item.DyeColor, Block> map = new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);
        for (net.minecraft.world.item.DyeColor dyeColor : net.minecraft.world.item.DyeColor.values()) {
            Block elevator = registerBlock(dyeColor.getName() + "_elevator",
                    properties -> new me.tuanzi.block.ElevatorBlock(
                            dyeColor,
                            properties.strength(4.0f, 6.0f)
                                    .requiresCorrectToolForDrops()
                                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                                    .mapColor(dyeColor.getMapColor())
                                    .noOcclusion()
                                    .dynamicShape()
                    ));
            map.put(dyeColor, elevator);
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    // 装饰性筹码系列（按面额升序，支持地面堆叠最多 8 枚）
    public static final Block CHIP_50 = registerChipBlock("chip_50");
    public static final Block CHIP_100 = registerChipBlock("chip_100");
    public static final Block CHIP_500 = registerChipBlock("chip_500");
    public static final Block CHIP_1000 = registerChipBlock("chip_1000");
    public static final Block CHIP_5000 = registerChipBlock("chip_5000");
    public static final Block CHIP_10000 = registerChipBlock("chip_10000");
    public static final Block CHIP_50000 = registerChipBlock("chip_50000");
    public static final Block CHIP_100000 = registerChipBlock("chip_100000");

    public static final java.util.List<Block> CHIPS = java.util.List.of(
            CHIP_50,
            CHIP_100,
            CHIP_500,
            CHIP_1000,
            CHIP_5000,
            CHIP_10000,
            CHIP_50000,
            CHIP_100000
    );

    private static Block registerChipBlock(String path) {
        return registerBlock(path, properties -> new me.tuanzi.block.ChipBlock(
                properties.strength(0.2f, 0.2f)
                        .noOcclusion()
                        .sound(net.minecraft.world.level.block.SoundType.STONE)
                        .pushReaction(PushReaction.POPPED)
        ));
    }

    public static final BlockEntityType<SoulMerchantStationBlockEntity> SOUL_MERCHANT_STATION_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "soul_merchant_station_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            SoulMerchantStationBlockEntity::new, SOUL_MERCHANT_STATION
                    ).build()
            );

    public static final BlockEntityType<BlueprintCannonBlockEntity> BLUEPRINT_CANNON_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_cannon_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            BlueprintCannonBlockEntity::new, BLUEPRINT_CANNON
                    ).build()
            );

    public static final BlockEntityType<BlueprintTableBlockEntity> BLUEPRINT_TABLE_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blueprint_table_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            BlueprintTableBlockEntity::new, BLUEPRINT_TABLE
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.PlayerSimulatorBlockEntity> PLAYER_SIMULATOR_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "player_simulator_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.PlayerSimulatorBlockEntity::new, PLAYER_SIMULATOR
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.BlockBreakerBlockEntity> BLOCK_BREAKER_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block_breaker_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.BlockBreakerBlockEntity::new, BLOCK_BREAKER
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.BlockPlacerBlockEntity> BLOCK_PLACER_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block_placer_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.BlockPlacerBlockEntity::new, BLOCK_PLACER
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.ColorBlockBlockEntity> COLOR_BLOCK_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "color_block_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.ColorBlockBlockEntity::new, COLOR_BLOCK, COLOR_STAIRS
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.ColorSlabBlockEntity> COLOR_SLAB_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "color_slab_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.ColorSlabBlockEntity::new, COLOR_SLAB
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.BlackboxCoreCaseBlockEntity> BLACKBOX_CORE_CASE_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blackbox_core_case_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.BlackboxCoreCaseBlockEntity::new, BLACKBOX_CORE_CASE
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.BaseStashCabinetBlockEntity> BASE_STASH_CABINET_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "base_stash_cabinet_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.BaseStashCabinetBlockEntity::new, BASE_STASH_CABINET
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.BaseUpgradeStationBlockEntity> BASE_UPGRADE_STATION_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "base_upgrade_station_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.BaseUpgradeStationBlockEntity::new, BASE_UPGRADE_STATION
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.ElevatorBlockEntity> ELEVATOR_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "elevator_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.ElevatorBlockEntity::new, ELEVATORS.toArray(Block[]::new)
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.EaselBlockEntity> EASEL_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "easel_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.EaselBlockEntity::new, EASEL
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.Printer3DBlockEntity> PRINTER_3D_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "printer_3d_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.Printer3DBlockEntity::new, PRINTER_3D
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.CopierBlockEntity> COPIER_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "copier_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.CopierBlockEntity::new, COPIER
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.PaperShredderBlockEntity> PAPER_SHREDDER_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "paper_shredder_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.PaperShredderBlockEntity::new, PAPER_SHREDDER
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.PrintedModelBlockEntity> PRINTED_MODEL_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "printed_model_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.PrintedModelBlockEntity::new, PRINTED_MODEL
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.entity.PrintedModelPartBlockEntity> PRINTED_MODEL_PART_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "printed_model_part_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.entity.PrintedModelPartBlockEntity::new, PRINTED_MODEL_PART
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.board.GomokuBlockEntity> GOMOKU_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "gomoku_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.board.GomokuBlockEntity::new, GOMOKU
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.board.CChessBlockEntity> CCHESS_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cchess_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.board.CChessBlockEntity::new, CCHESS
                    ).build()
            );

    public static final BlockEntityType<me.tuanzi.block.board.WChessBlockEntity> WCHESS_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "wchess_be"),
                    net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                            me.tuanzi.block.board.WChessBlockEntity::new, WCHESS
                    ).build()
            );

    private static Block registerBlock(String path, java.util.function.Function<BlockBehaviour.Properties, Block> blockFactory) {
        return registerBlockWithCustomItem(path, blockFactory, (b, p) -> new BlockItem(b, p));
    }

    private static Block registerBlockWithCustomItem(
            String path,
            java.util.function.Function<BlockBehaviour.Properties, Block> blockFactory,
            java.util.function.BiFunction<Block, Item.Properties, BlockItem> itemFactory
    ) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Block block = blockFactory.apply(BlockBehaviour.Properties.of().setId(key));

        Registry.register(BuiltInRegistries.BLOCK, key, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        BlockItem blockItem = itemFactory.apply(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }

    private static Block registerBlockNoItem(String path, java.util.function.Function<BlockBehaviour.Properties, Block> blockFactory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Block block = blockFactory.apply(BlockBehaviour.Properties.of().setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static void initialize() {
        // 用于类加载和静态字段初始化
    }
}
