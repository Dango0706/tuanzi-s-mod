package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.block.SoulMerchantStationBlock;
import me.tuanzi.block.BlueprintCannonBlock;
import me.tuanzi.block.BlueprintTableBlock;
import me.tuanzi.block.entity.SoulMerchantStationBlockEntity;
import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
import me.tuanzi.block.entity.BlueprintTableBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final Block SOUL_MERCHANT_STATION = registerBlock("soul_merchant_station", 
        properties -> new SoulMerchantStationBlock(properties.strength(3.5f, 1200.0f).noOcclusion())); // 免疫爆炸

    public static final Block BLUEPRINT_CANNON = registerBlock("blueprint_cannon",
        properties -> new BlueprintCannonBlock(properties.strength(3.0f, 6.0f).noOcclusion()));

    public static final Block BLUEPRINT_TABLE = registerBlock("blueprint_table",
        properties -> new BlueprintTableBlock(properties.strength(2.5f, 5.0f).noOcclusion()));

    public static final Block PLAYER_SIMULATOR = registerBlock("player_simulator", 
        properties -> new me.tuanzi.block.PlayerSimulatorBlock(properties.strength(3.5f, 6.0f).noOcclusion().isRedstoneConductor((state, level, pos) -> false)));

    public static final Block COLOR_BLOCK = registerBlock("color_block",
        properties -> new me.tuanzi.block.  ColorBlock(properties.strength(0.5f, 3.0f)));

    public static final Block COLOR_SLAB = registerBlock("color_slab",
        properties -> new me.tuanzi.block.ColorSlab(properties.strength(0.5f, 3.0f)));

    public static final Block COLOR_STAIRS = registerBlock("color_stairs",
        properties -> new me.tuanzi.block.ColorStairs(COLOR_BLOCK.defaultBlockState(), properties.strength(0.5f, 3.0f)));

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

    private static Block registerBlock(String path, java.util.function.Function<BlockBehaviour.Properties, Block> blockFactory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        Block block = blockFactory.apply(BlockBehaviour.Properties.of().setId(key));
        
        // 注册方块
        Registry.register(BuiltInRegistries.BLOCK, key, block);

        // 注册对应的 BlockItem
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path));
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }

    public static void initialize() {
        // 用于类加载和静态字段初始化
    }
}
