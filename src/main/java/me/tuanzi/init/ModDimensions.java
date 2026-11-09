package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.timeline.Timeline;

import java.util.List;
import java.util.Optional;

public class ModDimensions {
    public static final ResourceKey<Level> SCAVENGER_CORRIDOR_LEVEL = ResourceKey.create(
        Registries.DIMENSION,
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_corridor")
    );

    public static final ResourceKey<DimensionType> SCAVENGER_CORRIDOR_TYPE = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_corridor_type")
    );

    public static final ResourceKey<LevelStem> SCAVENGER_CORRIDOR_STEM = ResourceKey.create(
        Registries.LEVEL_STEM,
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_corridor")
    );

    public static void bootstrapDimensionType(BootstrapContext<DimensionType> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        HolderGetter<Timeline> timelines = context.lookup(Registries.TIMELINE);
        HolderGetter<WorldClock> clocks = context.lookup(Registries.WORLD_CLOCK);

        EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
            .set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(-15199464)) // 深暗迷雾
            .set(EnvironmentAttributes.SKY_LIGHT_COLOR, ARGB.vector3fFromRGB24(-5480243))
            .set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(-16777216))
            .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.0F)
            .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(-12630209))
            .set(EnvironmentAttributes.BED_RULE, new BedRule(
                BedRule.Rule.NEVER,
                BedRule.Rule.NEVER,
                false,
                false,
                Optional.of(Component.translatable("message.tuanzis_mod.scavenger_corridor.no_sleep"))
            ))
            .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
            .build();

        context.register(
            SCAVENGER_CORRIDOR_TYPE,
            new DimensionType(
                true,
                false,
                false,
                false,
                1.0,
                -64,
                384,
                384,
                blocks.getOrThrow(BlockTags.INFINIBURN_END),
                0.1F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.END,
                CardinalLighting.Type.DEFAULT,
                attributes,
                timelines.getOrThrow(TimelineTags.IN_END),
                Optional.of(clocks.getOrThrow(WorldClocks.THE_END))
            )
        );
    }

    public static void bootstrapLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        Holder<DimensionType> type = dimensionTypes.getOrThrow(SCAVENGER_CORRIDOR_TYPE);
        Holder<Biome> voidBiome = biomes.getOrThrow(Biomes.THE_VOID);

        FlatLevelGeneratorSettings flatSettings = new FlatLevelGeneratorSettings(
            Optional.empty(),
            voidBiome,
            List.of()
        ).withBiomeAndLayers(
            List.of(new FlatLayerInfo(1, Blocks.AIR)),
            Optional.of(HolderSet.direct()),
            voidBiome
        );

        context.register(
            SCAVENGER_CORRIDOR_STEM,
            new LevelStem(type, new FlatLevelSource(flatSettings))
        );
    }

    /**
     * 确保在回廊中心 (0, 64, 0) 生成 5x5 深板岩/黑曜石防爆安全基台及末影箱
     */
    public static void ensureBasePlatform(ServerLevel level) {
        ensurePlatform(level, new BlockPos(0, 64, 0));
    }

    /**
     * 确保在指定中心生成 5x5 深板岩/黑曜石防爆安全基台及专属末影箱
     */
    public static void ensurePlatform(ServerLevel level, BlockPos centerPos) {
        // 如果中心已经有非空气方块，则说明基台已生成
        if (!level.getBlockState(centerPos).isAir()) {
            return;
        }

        int cx = centerPos.getX();
        int cy = centerPos.getY();
        int cz = centerPos.getZ();

        // 构建 5x5 基台
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = new BlockPos(cx + x, cy, cz + z);
                boolean isEdge = Math.abs(x) == 2 || Math.abs(z) == 2;
                BlockState state = isEdge ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                level.setBlock(pos, state, Block.UPDATE_ALL);
                // 清理上方空气
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // 放置四角灵魂火把，防止漆黑
        level.setBlock(new BlockPos(cx - 2, cy + 1, cz - 2), Blocks.SOUL_TORCH.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 2, cy + 1, cz - 2), Blocks.SOUL_TORCH.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx - 2, cy + 1, cz + 2), Blocks.SOUL_TORCH.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(new BlockPos(cx + 2, cy + 1, cz + 2), Blocks.SOUL_TORCH.defaultBlockState(), Block.UPDATE_ALL);

        // 在边缘放置专属末影箱，面朝玩家出生点（南）
        BlockPos chestPos = new BlockPos(cx, cy + 1, cz - 2);
        level.setBlock(chestPos, Blocks.ENDER_CHEST.defaultBlockState().setValue(EnderChestBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        
        me.tuanzi.util.ModLog.debug("【拾荒回廊】已在 (" + cx + ", " + cy + ", " + cz + ") 成功构建 5x5 安全防爆基台并部署专属末影箱。");
    }
}
