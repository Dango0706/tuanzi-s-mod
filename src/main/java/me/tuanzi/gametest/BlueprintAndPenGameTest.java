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

    @GameTest
    public void testBlueprintsForbiddenInShulkerBoxAndGoldenPocket(GameTestHelper helper) {
        // 1. 放置潜影盒与朝下指向潜影盒的漏斗，验证漏斗无法输入蓝图/空白蓝图但可正常输入钻石
        BlockPos shulkerPos = new BlockPos(1, 1, 1);
        BlockPos hopperPos = new BlockPos(1, 2, 1);
        helper.setBlock(1, 0, 1, Blocks.BEDROCK);
        helper.setBlock(shulkerPos, Blocks.SHULKER_BOX);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));

        net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity shulkerBe =
                helper.getBlockEntity(shulkerPos, net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.class);
        net.minecraft.world.level.block.entity.HopperBlockEntity hopperBe =
                helper.getBlockEntity(hopperPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);

        ItemStack blankBp = new ItemStack(ModItems.BLANK_BLUEPRINT);
        ItemStack structBp = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        // 直接容器接口校验
        if (shulkerBe.canPlaceItemThroughFace(0, blankBp, Direction.UP)
                || shulkerBe.canPlaceItemThroughFace(0, structBp, Direction.UP)
                || shulkerBe.canPlaceItem(0, blankBp)
                || shulkerBe.canPlaceItem(0, structBp)) {
            helper.fail(Component.literal("ShulkerBoxBlockEntity must reject Blank/Structure Blueprints"));
            return;
        }
        if (!shulkerBe.canPlaceItemThroughFace(0, diamond, Direction.UP) || !shulkerBe.canPlaceItem(0, diamond)) {
            helper.fail(Component.literal("ShulkerBoxBlockEntity must accept normal items like diamond"));
            return;
        }

        // 漏斗放入空白蓝图、结构蓝图与钻石
        hopperBe.setItem(0, blankBp.copy());
        hopperBe.setItem(1, structBp.copy());
        hopperBe.setItem(2, diamond.copy());

        // 2. 校验 ShulkerBoxMenu 与 GoldenPocketMenu 槽位放置及 Shift 快捷移动
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModItems.GOLDEN_POCKET));
        player.getInventory().setItem(1, blankBp.copy());
        player.getInventory().setItem(2, structBp.copy());
        player.getInventory().setItem(3, new ItemStack(Items.EMERALD));

        net.minecraft.world.inventory.ShulkerBoxMenu shulkerMenu =
                new net.minecraft.world.inventory.ShulkerBoxMenu(1, player.getInventory(), new net.minecraft.world.SimpleContainer(27));
        if (shulkerMenu.getSlot(0).mayPlace(blankBp) || shulkerMenu.getSlot(0).mayPlace(structBp)) {
            helper.fail(Component.literal("ShulkerBoxMenu slot must reject Blank/Structure Blueprints"));
            return;
        }
        // 快捷栏槽位在 ShulkerBoxMenu 中位于 27 + 27 = 54 起（索引 1 -> slot 55, 索引 2 -> slot 56, 索引 3 -> slot 57）
        shulkerMenu.quickMoveStack(player, 55);
        shulkerMenu.quickMoveStack(player, 56);
        shulkerMenu.quickMoveStack(player, 57);
        if (!player.getInventory().getItem(1).is(ModItems.BLANK_BLUEPRINT)
                || !player.getInventory().getItem(2).is(ModItems.STRUCTURE_BLUEPRINT)) {
            helper.fail(Component.literal("ShulkerBoxMenu quickMoveStack must not move blueprints into shulker box"));
            return;
        }
        if (!player.getInventory().getItem(3).isEmpty()) {
            helper.fail(Component.literal("ShulkerBoxMenu quickMoveStack should move emerald into shulker box"));
            return;
        }

        // 黄金口袋校验
        player.getInventory().setItem(3, new ItemStack(Items.GOLD_INGOT));
        net.minecraft.world.SimpleContainer pocketContainer =
                new net.minecraft.world.SimpleContainer(me.tuanzi.world.inventory.GoldenPocketMenu.CONTAINER_SIZE);
        me.tuanzi.world.inventory.GoldenPocketMenu pocketMenu = new me.tuanzi.world.inventory.GoldenPocketMenu(
                2, player.getInventory(), pocketContainer, player.getInventory().getItem(0), 0);
        if (pocketMenu.getSlot(0).mayPlace(blankBp) || pocketMenu.getSlot(0).mayPlace(structBp)) {
            helper.fail(Component.literal("GoldenPocketMenu slot must reject Blank/Structure Blueprints"));
            return;
        }
        // 快捷栏在 GoldenPocketMenu 中位于 81..89（索引 1 -> slot 82, 索引 2 -> slot 83, 索引 3 -> slot 84）
        pocketMenu.quickMoveStack(player, 82);
        pocketMenu.quickMoveStack(player, 83);
        pocketMenu.quickMoveStack(player, 84);
        if (!player.getInventory().getItem(1).is(ModItems.BLANK_BLUEPRINT)
                || !player.getInventory().getItem(2).is(ModItems.STRUCTURE_BLUEPRINT)) {
            helper.fail(Component.literal("GoldenPocketMenu quickMoveStack must not move blueprints into golden pocket"));
            return;
        }
        if (!pocketContainer.getItem(0).is(Items.GOLD_INGOT)) {
            helper.fail(Component.literal("GoldenPocketMenu quickMoveStack should move gold ingot into golden pocket"));
            return;
        }

        // 3. 等待 15 ticks 确认漏斗只把钻石转移到潜影盒，而两个蓝图仍留在漏斗内
        helper.runAfterDelay(15, () -> {
            if (!shulkerBe.getItem(0).is(Items.DIAMOND)) {
                helper.fail(Component.literal("Hopper should have pushed diamond into shulker box, slot0=" + shulkerBe.getItem(0)));
                return;
            }
            for (int i = 0; i < shulkerBe.getContainerSize(); i++) {
                ItemStack st = shulkerBe.getItem(i);
                if (st.is(ModItems.BLANK_BLUEPRINT) || st.is(ModItems.STRUCTURE_BLUEPRINT)) {
                    helper.fail(Component.literal("Hopper must not push blueprints into shulker box: " + st));
                    return;
                }
            }
            if (!hopperBe.getItem(0).is(ModItems.BLANK_BLUEPRINT) || !hopperBe.getItem(1).is(ModItems.STRUCTURE_BLUEPRINT)) {
                helper.fail(Component.literal("Blueprints should remain in hopper slots 0 and 1"));
                return;
            }
            helper.succeed();
        });
    }
}
