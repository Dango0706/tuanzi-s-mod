package me.tuanzi.gametest;

import me.tuanzi.block.BlockBreakerBlock;
import me.tuanzi.block.entity.BlockBreakerBlockEntity;
import me.tuanzi.init.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * FIX-05 / FIX-06 活塞推动方块破坏器防复制与正常破坏掉落 GameTest。
 */
public class BlockBreakerPistonGameTest {

    @GameTest(maxTicks = 40)
    public void testPistonPushBlockBreakerNoDropOrDuplication(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        BlockPos breakerStart = new BlockPos(2, 1, 1);
        BlockPos breakerEnd = new BlockPos(3, 1, 1);
        helper.setBlock(breakerStart, ModBlocks.BLOCK_BREAKER.defaultBlockState().setValue(BlockBreakerBlock.FACING, Direction.NORTH));
        BlockBreakerBlockEntity be = helper.getBlockEntity(breakerStart, BlockBreakerBlockEntity.class);
        be.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        be.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        be.setItem(2, new ItemStack(Items.EMERALD_BLOCK, 16));

        helper.setBlock(1, 1, 1, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(0, 1, 1, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(ModBlocks.BLOCK_BREAKER, breakerEnd);
            BlockBreakerBlockEntity movedBe = helper.getBlockEntity(breakerEnd, BlockBreakerBlockEntity.class);
            if (!movedBe.getItem(0).is(Items.DIAMOND_PICKAXE) || movedBe.getItem(0).getCount() != 1) {
                helper.fail(Component.literal("Expected 1 diamond_pickaxe in slot 0, got " + movedBe.getItem(0)));
                return;
            }
            if (!movedBe.getItem(1).is(Items.COBBLESTONE) || movedBe.getItem(1).getCount() != 64) {
                helper.fail(Component.literal("Expected 64 cobblestone in slot 1, got " + movedBe.getItem(1)));
                return;
            }
            if (!movedBe.getItem(2).is(Items.EMERALD_BLOCK) || movedBe.getItem(2).getCount() != 16) {
                helper.fail(Component.literal("Expected 16 emerald_block in slot 2, got " + movedBe.getItem(2)));
                return;
            }

            BlockPos absMin = helper.absolutePos(new BlockPos(0, 0, 0));
            BlockPos absMax = helper.absolutePos(new BlockPos(5, 3, 3));
            List<ItemEntity> drops = helper.getLevel().getEntities(
                    EntityTypes.ITEM,
                    new AABB(absMin.getX(), absMin.getY(), absMin.getZ(), absMax.getX(), absMax.getY(), absMax.getZ()),
                    e -> true
            );
            if (!drops.isEmpty()) {
                helper.fail(Component.literal("Expected 0 dropped item entities after piston push, found " + drops.size() + ": " + drops.stream().map(e -> e.getItem().toString()).toList()));
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void testStickyPistonPushAndPullBlockBreaker(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        BlockPos posA = new BlockPos(2, 1, 1);
        BlockPos posB = new BlockPos(3, 1, 1);
        helper.setBlock(posA, ModBlocks.BLOCK_BREAKER.defaultBlockState().setValue(BlockBreakerBlock.FACING, Direction.SOUTH));
        BlockBreakerBlockEntity be = helper.getBlockEntity(posA, BlockBreakerBlockEntity.class);
        be.setItem(0, new ItemStack(Items.NETHERITE_PICKAXE));
        be.setItem(1, new ItemStack(Items.DIAMOND, 32));

        helper.setBlock(1, 1, 1, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(0, 1, 1, Blocks.REDSTONE_BLOCK);

        // 推到 posB 后收回红石块触发粘性活塞拉回 posA
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(ModBlocks.BLOCK_BREAKER, posB);
            helper.setBlock(0, 1, 1, Blocks.AIR);

            helper.runAfterDelay(10, () -> {
                helper.assertBlockPresent(ModBlocks.BLOCK_BREAKER, posA);
                BlockBreakerBlockEntity pulledBe = helper.getBlockEntity(posA, BlockBreakerBlockEntity.class);
                if (!pulledBe.getItem(0).is(Items.NETHERITE_PICKAXE) || pulledBe.getItem(1).getCount() != 32) {
                    helper.fail(Component.literal("Items lost after sticky piston push+pull: slot0=" + pulledBe.getItem(0) + ", slot1=" + pulledBe.getItem(1)));
                    return;
                }

                BlockPos absMin = helper.absolutePos(new BlockPos(0, 0, 0));
                BlockPos absMax = helper.absolutePos(new BlockPos(5, 3, 3));
                List<ItemEntity> drops = helper.getLevel().getEntities(
                        EntityTypes.ITEM,
                        new AABB(absMin.getX(), absMin.getY(), absMin.getZ(), absMax.getX(), absMax.getY(), absMax.getZ()),
                        e -> true
                );
                if (!drops.isEmpty()) {
                    helper.fail(Component.literal("Expected 0 dropped items after sticky push+pull, found " + drops.size()));
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(maxTicks = 40)
    public void testDestroyBlockBreakerStillDropsAndResetsComparator(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        BlockPos breakerPos = new BlockPos(1, 1, 1);
        BlockPos compPos = new BlockPos(1, 1, 2);
        helper.setBlock(breakerPos, ModBlocks.BLOCK_BREAKER.defaultBlockState().setValue(BlockBreakerBlock.FACING, Direction.NORTH));
        helper.setBlock(compPos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));

        BlockBreakerBlockEntity be = helper.getBlockEntity(breakerPos, BlockBreakerBlockEntity.class);
        be.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        be.setItem(1, new ItemStack(Items.STONE, 10));

        helper.runAfterDelay(4, () -> {
            if (!helper.getBlockState(compPos).getValue(ComparatorBlock.POWERED)) {
                helper.fail(Component.literal("Expected comparator to be powered before destroying block breaker"));
                return;
            }

            helper.getLevel().destroyBlock(helper.absolutePos(breakerPos), true);

            helper.runAfterDelay(4, () -> {
                if (helper.getBlockState(compPos).getValue(ComparatorBlock.POWERED)) {
                    helper.fail(Component.literal("Expected comparator to turn off after destroying block breaker"));
                    return;
                }

                BlockPos absMin = helper.absolutePos(new BlockPos(0, 0, 0));
                BlockPos absMax = helper.absolutePos(new BlockPos(3, 3, 4));
                List<ItemEntity> drops = helper.getLevel().getEntities(
                        EntityTypes.ITEM,
                        new AABB(absMin.getX(), absMin.getY(), absMin.getZ(), absMax.getX(), absMax.getY(), absMax.getZ()),
                        e -> true
                );
                boolean hasPickaxe = drops.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND_PICKAXE));
                boolean hasStone = drops.stream().anyMatch(e -> e.getItem().is(Items.STONE) && e.getItem().getCount() == 10);
                if (!hasPickaxe || !hasStone) {
                    helper.fail(Component.literal("Expected normal destroy to drop pickaxe and 10 stone, got " + drops.stream().map(e -> e.getItem().toString()).toList()));
                    return;
                }
                helper.succeed();
            });
        });
    }

    @GameTest(maxTicks = 60)
    public void testContinuousPushAndShortPulseFinalTickNoDropOrDuplication(GameTestHelper helper) {
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        // 先测 1-tick 短脉冲（在移动未完成时断电，触发 PistonMovingBlockEntity.finalTick() 落位）
        BlockPos posA = new BlockPos(2, 1, 1);
        BlockPos posB = new BlockPos(3, 1, 1);
        BlockPos posC = new BlockPos(4, 1, 1);
        helper.setBlock(posA, ModBlocks.BLOCK_BREAKER.defaultBlockState().setValue(BlockBreakerBlock.FACING, Direction.NORTH));
        BlockBreakerBlockEntity be = helper.getBlockEntity(posA, BlockBreakerBlockEntity.class);
        be.setItem(0, new ItemStack(Items.IRON_PICKAXE));
        be.setItem(1, new ItemStack(Items.GOLD_BLOCK, 12));

        helper.setBlock(1, 1, 1, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(0, 1, 1, Blocks.REDSTONE_BLOCK);

        // 1 tick 后立刻撤掉红石块，再用第二只侧向活塞将 posB 继续推至 posC（连推 A -> B -> C）
        helper.runAfterDelay(1, () -> {
            helper.setBlock(0, 1, 1, Blocks.AIR);

            helper.runAfterDelay(10, () -> {
                helper.assertBlockPresent(ModBlocks.BLOCK_BREAKER, posB);
                helper.setBlock(2, 1, 1, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
                helper.setBlock(1, 1, 1, Blocks.REDSTONE_BLOCK);

                helper.runAfterDelay(10, () -> {
                    helper.assertBlockPresent(ModBlocks.BLOCK_BREAKER, posC);
                    BlockBreakerBlockEntity finalBe = helper.getBlockEntity(posC, BlockBreakerBlockEntity.class);
                    if (!finalBe.getItem(0).is(Items.IRON_PICKAXE) || finalBe.getItem(1).getCount() != 12) {
                        helper.fail(Component.literal("Expected 1 iron_pickaxe and 12 gold_block at posC, got " + finalBe.getItem(0) + ", " + finalBe.getItem(1)));
                        return;
                    }

                    BlockPos absMin = helper.absolutePos(new BlockPos(0, 0, 0));
                    BlockPos absMax = helper.absolutePos(new BlockPos(6, 3, 4));
                    List<ItemEntity> drops = helper.getLevel().getEntities(
                            EntityTypes.ITEM,
                            new AABB(absMin.getX(), absMin.getY(), absMin.getZ(), absMax.getX(), absMax.getY(), absMax.getZ()),
                            e -> true
                    );
                    if (!drops.isEmpty()) {
                        helper.fail(Component.literal("Expected 0 dropped items after continuous push, found " + drops.size()));
                        return;
                    }
                    helper.succeed();
                });
            });
        });
    }
}
