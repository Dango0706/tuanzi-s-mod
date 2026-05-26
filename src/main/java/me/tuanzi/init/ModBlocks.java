package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.block.SoulMerchantStationBlock;
import me.tuanzi.block.entity.SoulMerchantStationBlockEntity;
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


    public static final BlockEntityType<SoulMerchantStationBlockEntity> SOUL_MERCHANT_STATION_BLOCK_ENTITY = 
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "soul_merchant_station_be"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                SoulMerchantStationBlockEntity::new, SOUL_MERCHANT_STATION
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
