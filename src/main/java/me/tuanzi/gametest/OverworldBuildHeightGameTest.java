package me.tuanzi.gametest;

import me.tuanzi.config.ModConfig;
import me.tuanzi.config.OverworldBuildHeightControl;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 主世界建筑高度限制解除与热加载 GameTest 验证。
 */
public class OverworldBuildHeightGameTest {

    @GameTest
    public void testOverworldPhysicalBoundsAndHotReloadLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        if (!Level.OVERWORLD.equals(level.dimension())) {
            helper.fail(Component.literal("Expected overworld dimension for GameTest, got: " + level.dimension()));
            return;
        }

        int originalHeight = ModConfig.getOverworldMaxBuildHeight();
        try {
            // 1. 验证主世界底层物理容量已扩展到 MAX_Y = 2031
            if (level.getMinY() != -64 || level.getMaxY() != 2031 || level.getHeight() != 2096) {
                helper.fail(Component.literal("Overworld physical bounds mismatch: minY=" + level.getMinY()
                        + ", maxY=" + level.getMaxY() + ", height=" + level.getHeight()));
                return;
            }

            // 2. 热加载到最大 2032，验证高空可真实放置与读取方块，且 Y=2032 不可放置
            ModConfig.setOverworldMaxBuildHeight(2032);
            BlockPos basePos = helper.absolutePos(new BlockPos(1, 0, 1));
            BlockPos y400 = new BlockPos(basePos.getX(), 400, basePos.getZ());
            BlockPos y2031 = new BlockPos(basePos.getX(), 2031, basePos.getZ());
            BlockPos y2032 = new BlockPos(basePos.getX(), 2032, basePos.getZ());

            if (!level.isInWorldBounds(y400) || !level.isInWorldBounds(y2031) || level.isInWorldBounds(y2032)) {
                helper.fail(Component.literal("isInWorldBounds mismatch at maxBuildHeight=2032"));
                return;
            }

            if (!level.setBlock(y400, Blocks.GOLD_BLOCK.defaultBlockState(), 3)
                    || !level.getBlockState(y400).is(Blocks.GOLD_BLOCK)) {
                helper.fail(Component.literal("Failed to place/read block at Y=400"));
                return;
            }

            if (!level.setBlock(y2031, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3)
                    || !level.getBlockState(y2031).is(Blocks.DIAMOND_BLOCK)) {
                helper.fail(Component.literal("Failed to place/read block at Y=2031"));
                return;
            }

            if (level.setBlock(y2032, Blocks.STONE.defaultBlockState(), 3)) {
                helper.fail(Component.literal("Must not allow placing block at Y=2032"));
                return;
            }

            // 清理高空测试方块
            level.setBlock(y400, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(y2031, Blocks.AIR.defaultBlockState(), 3);

            // 3. 热加载降至 320，验证实时生效 (maxY=319)
            ModConfig.setOverworldMaxBuildHeight(320);
            if (OverworldBuildHeightControl.getEffectiveMaxY(level) != 319) {
                helper.fail(Component.literal("Effective maxY should be 319 after hot reload to 320"));
                return;
            }
            if (!level.isInWorldBounds(new BlockPos(basePos.getX(), 319, basePos.getZ()))
                    || level.isInWorldBounds(new BlockPos(basePos.getX(), 320, basePos.getZ()))) {
                helper.fail(Component.literal("isInWorldBounds mismatch at maxBuildHeight=320"));
                return;
            }

            // 4. 热加载至 500，验证实时生效 (maxY=499)
            ModConfig.setOverworldMaxBuildHeight(500);
            if (OverworldBuildHeightControl.getEffectiveMaxY(level) != 499) {
                helper.fail(Component.literal("Effective maxY should be 499 after hot reload to 500"));
                return;
            }
            if (!level.isInWorldBounds(new BlockPos(basePos.getX(), 499, basePos.getZ()))
                    || level.isInWorldBounds(new BlockPos(basePos.getX(), 500, basePos.getZ()))) {
                helper.fail(Component.literal("isInWorldBounds mismatch at maxBuildHeight=500"));
                return;
            }

            helper.succeed();
        } finally {
            ModConfig.setOverworldMaxBuildHeight(originalHeight);
        }
    }
}
