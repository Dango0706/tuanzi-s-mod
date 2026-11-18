package me.tuanzi.gametest;

import me.tuanzi.init.ModItems;
import me.tuanzi.item.WorldSculptorsPenItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlueprintAndPenGameTest {

    private static BlockHitResult hitAt(BlockPos absPos) {
        return new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
    }

    @GameTest
    public void testWorldSculptorsPenCreativeNoCheckOrConsume(GameTestHelper helper) {
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }
        helper.setBlock(0, 1, 0, Blocks.DIAMOND_BLOCK);

        Player creativePlayer = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack penStack = new ItemStack(ModItems.WORLD_SCULPTORS_PEN);
        penStack.setDamageValue(penStack.getMaxDamage() - 1); // 仅剩 1 点耐久
        creativePlayer.setItemInHand(InteractionHand.MAIN_HAND, penStack);

        BlockPos p010 = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos p111 = helper.absolutePos(new BlockPos(1, 1, 1));

        // 1. Shift+右键选定钻石块
        creativePlayer.setShiftKeyDown(true);
        penStack.useOn(new UseOnContext(creativePlayer, InteractionHand.MAIN_HAND, hitAt(p010)));

        // 2. 非潜行两点填充 (需要填充 3 个空气格，背包无钻石块且耐久仅剩 1)
        creativePlayer.setShiftKeyDown(false);
        penStack.useOn(new UseOnContext(creativePlayer, InteractionHand.MAIN_HAND, hitAt(p010)));
        penStack.useOn(new UseOnContext(creativePlayer, InteractionHand.MAIN_HAND, hitAt(p111)));

        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(0, 1, 1));
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(1, 1, 0));
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(1, 1, 1));

        if (penStack.getDamageValue() != penStack.getMaxDamage() - 1) {
            helper.fail(Component.literal("Creative fill must not consume pen durability, actual damage=" + penStack.getDamageValue()));
            return;
        }
        if (WorldSculptorsPenItem.getTargetItemCount(creativePlayer, Items.DIAMOND_BLOCK) != 0) {
            helper.fail(Component.literal("Creative fill must leave inventory diamond blocks at 0"));
            return;
        }

        // 3. 创造模式撤销：应还原方块且不返还方块与耐久
        creativePlayer.setShiftKeyDown(true);
        penStack.use(helper.getLevel(), creativePlayer, InteractionHand.MAIN_HAND);

        helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 1, 1));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 0));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));

        if (penStack.getDamageValue() != penStack.getMaxDamage() - 1) {
            helper.fail(Component.literal("Undoing creative fill must not alter pen durability"));
            return;
        }
        if (WorldSculptorsPenItem.getTargetItemCount(creativePlayer, Items.DIAMOND_BLOCK) != 0) {
            helper.fail(Component.literal("Undoing creative fill must not grant diamond blocks"));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testWorldSculptorsPenSurvivalStillChecksAndConsumes(GameTestHelper helper) {
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }
        helper.setBlock(0, 1, 0, Blocks.GOLD_BLOCK);

        Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack penStack = new ItemStack(ModItems.WORLD_SCULPTORS_PEN);
        penStack.setDamageValue(penStack.getMaxDamage() - 1); // 仅剩 1 点耐久
        survivalPlayer.setItemInHand(InteractionHand.MAIN_HAND, penStack);

        BlockPos p010 = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos p111 = helper.absolutePos(new BlockPos(1, 1, 1));

        // 选定金块
        survivalPlayer.setShiftKeyDown(true);
        penStack.useOn(new UseOnContext(survivalPlayer, InteractionHand.MAIN_HAND, hitAt(p010)));

        // 1. 生存模式缺方块时应阻断
        survivalPlayer.setShiftKeyDown(false);
        penStack.useOn(new UseOnContext(survivalPlayer, InteractionHand.MAIN_HAND, hitAt(p010)));
        penStack.useOn(new UseOnContext(survivalPlayer, InteractionHand.MAIN_HAND, hitAt(p111)));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));

        // 2. 给予 3 个金块但耐久仍不足 (仅剩 1 点，需要 3 点)，应阻断
        survivalPlayer.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 3));
        penStack.useOn(new UseOnContext(survivalPlayer, InteractionHand.MAIN_HAND, hitAt(p111)));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 1, 1));

        // 3. 恢复足够耐久后再次点击第二个点完成填充
        int initialDamage = penStack.getMaxDamage() - 12;
        penStack.setDamageValue(initialDamage);
        penStack.useOn(new UseOnContext(survivalPlayer, InteractionHand.MAIN_HAND, hitAt(p111)));

        helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(0, 1, 1));
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(1, 1, 0));
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(1, 1, 1));

        if (penStack.getDamageValue() != initialDamage + 3) {
            helper.fail(Component.literal("Survival fill should consume 3 durability"));
            return;
        }
        if (WorldSculptorsPenItem.getTargetItemCount(survivalPlayer, Items.GOLD_BLOCK) != 0) {
            helper.fail(Component.literal("Survival fill should consume 3 gold blocks"));
            return;
        }

        // 4. 生存模式撤销应退还方块与耐久
        survivalPlayer.setShiftKeyDown(true);
        penStack.use(helper.getLevel(), survivalPlayer, InteractionHand.MAIN_HAND);

        helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 1, 1));
        if (penStack.getDamageValue() != initialDamage) {
            helper.fail(Component.literal("Survival undo should restore 3 durability"));
            return;
        }
        if (WorldSculptorsPenItem.getTargetItemCount(survivalPlayer, Items.GOLD_BLOCK) != 3) {
            helper.fail(Component.literal("Survival undo should return 3 gold blocks"));
            return;
        }

        helper.succeed();
    }
}
