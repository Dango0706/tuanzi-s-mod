package me.tuanzi.gametest;

import me.tuanzi.block.ElevatorBlock;
import me.tuanzi.block.entity.ElevatorBlockEntity;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.world.elevator.ElevatorManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;

public class ElevatorGameTest {

    @GameTest
    public void testElevatorCraftingAndAllDyeRecipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 1. 8个铁块 + 1个末影珍珠 -> 1个白色电梯
        CraftingInput baseInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.IRON_BLOCK),
                new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.IRON_BLOCK),
                new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.IRON_BLOCK)
        ));
        Optional<RecipeHolder<CraftingRecipe>> baseMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, baseInput, level);
        if (baseMatch.isEmpty()) {
            helper.fail(Component.literal("Expected white_elevator crafting recipe to match"));
            return;
        }
        ItemStack baseResult = baseMatch.get().value().assemble(baseInput);
        if (!baseResult.is(ModBlocks.WHITE_ELEVATOR.asItem()) || baseResult.getCount() != 1) {
            helper.fail(Component.literal("Expected 1x white_elevator, got " + baseResult));
            return;
        }

        // 2. 16种染料染色互染测试
        for (DyeColor targetColor : DyeColor.values()) {
            DyeColor sourceColor = (targetColor == DyeColor.WHITE) ? DyeColor.BLACK : DyeColor.WHITE;
            ItemStack sourceElevator = new ItemStack(ModBlocks.ELEVATORS_BY_COLOR.get(sourceColor).asItem());
            ItemStack dye = new ItemStack(Items.DYE.pick(targetColor));
            CraftingInput dyeInput = CraftingInput.of(2, 1, List.of(sourceElevator, dye));
            Optional<RecipeHolder<CraftingRecipe>> dyeMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, dyeInput, level);
            if (dyeMatch.isEmpty()) {
                helper.fail(Component.literal("Expected dye recipe for elevator color " + targetColor.getName()));
                return;
            }
            ItemStack dyeResult = dyeMatch.get().value().assemble(dyeInput);
            Block expectedBlock = ModBlocks.ELEVATORS_BY_COLOR.get(targetColor);
            if (!dyeResult.is(expectedBlock.asItem()) || dyeResult.getCount() != 1) {
                helper.fail(Component.literal("Expected 1x " + targetColor.getName() + "_elevator, got " + dyeResult));
                return;
            }
        }

        helper.succeed();
    }

    @GameTest
    public void testElevatorDisguiseMatchesPlacementAndCollision(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        BlockPos relElevator = new BlockPos(1, 1, 1);
        BlockPos absElevator = helper.absolutePos(relElevator);
        helper.setBlock(relElevator, ModBlocks.WHITE_ELEVATOR.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(absElevator.getX() + 0.5, absElevator.getY() + 1.0, absElevator.getZ() + 2.5);
        player.setYRot(180.0f); // 朝北看
        player.setXRot(30.0f);

        // 1. 下半砖测试（点击方块下半侧侧面）
        ItemStack slabStack = new ItemStack(Items.STONE_SLAB, 2);
        BlockHitResult bottomSideHit = new BlockHitResult(
                new Vec3(absElevator.getX() + 0.5, absElevator.getY() + 0.25, absElevator.getZ() + 1.0),
                Direction.SOUTH,
                absElevator,
                false
        );
        BlockState slabDisguise = ElevatorManager.computeDisguiseState(player, helper.getLevel(), absElevator, InteractionHand.MAIN_HAND, slabStack, bottomSideHit);
        if (slabDisguise == null || !slabDisguise.is(Blocks.STONE_SLAB) || slabDisguise.getValue(SlabBlock.TYPE) != SlabType.BOTTOM) {
            helper.fail(Component.literal("Expected bottom stone slab disguise, got " + slabDisguise));
            return;
        }
        ElevatorBlockEntity be = helper.getBlockEntity(relElevator, ElevatorBlockEntity.class);
        be.setDisguiseState(slabDisguise);
        helper.assertBlockProperty(relElevator, ElevatorBlock.DISGUISED, true);
        VoxelShape shape = helper.getLevel().getBlockState(absElevator).getCollisionShape(helper.getLevel(), absElevator);
        if (Math.abs(shape.max(Direction.Axis.Y) - 0.5) > 1e-4) {
            helper.fail(Component.literal("Expected bottom slab collision height 0.5, got " + shape.max(Direction.Axis.Y)));
            return;
        }

        // 2. 楼梯朝向测试
        ItemStack stairsStack = new ItemStack(Items.OAK_STAIRS);
        BlockState stairsDisguise = ElevatorManager.computeDisguiseState(player, helper.getLevel(), absElevator, InteractionHand.MAIN_HAND, stairsStack, bottomSideHit);
        if (stairsDisguise == null || !stairsDisguise.is(Blocks.OAK_STAIRS) || stairsDisguise.getValue(StairBlock.FACING) != Direction.NORTH) {
            helper.fail(Component.literal("Expected north-facing oak stairs disguise, got " + stairsDisguise));
            return;
        }

        // 3. 原木轴向测试（点击东面 -> X 轴）
        ItemStack logStack = new ItemStack(Items.OAK_LOG);
        BlockHitResult eastHit = new BlockHitResult(Vec3.atCenterOf(absElevator), Direction.EAST, absElevator, false);
        BlockState logDisguise = ElevatorManager.computeDisguiseState(player, helper.getLevel(), absElevator, InteractionHand.MAIN_HAND, logStack, eastHit);
        if (logDisguise == null || !logDisguise.is(Blocks.OAK_LOG) || logDisguise.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.X) {
            helper.fail(Component.literal("Expected X-axis oak log disguise, got " + logDisguise));
            return;
        }

        // 4. 熔炉朝向测试（朝向玩家 = 南）
        ItemStack furnaceStack = new ItemStack(Items.FURNACE);
        BlockState furnaceDisguise = ElevatorManager.computeDisguiseState(player, helper.getLevel(), absElevator, InteractionHand.MAIN_HAND, furnaceStack, bottomSideHit);
        if (furnaceDisguise == null || !furnaceDisguise.is(Blocks.FURNACE) || furnaceDisguise.getValue(FurnaceBlock.FACING) != Direction.SOUTH) {
            helper.fail(Component.literal("Expected south-facing furnace disguise, got " + furnaceDisguise));
            return;
        }

        // 5. 清除伪装恢复原貌
        be.clearDisguise();
        helper.assertBlockProperty(relElevator, ElevatorBlock.DISGUISED, false);
        VoxelShape fullShape = helper.getLevel().getBlockState(absElevator).getCollisionShape(helper.getLevel(), absElevator);
        if (Math.abs(fullShape.max(Direction.Axis.Y) - 1.0) > 1e-4) {
            helper.fail(Component.literal("Expected restored full block collision height 1.0, got " + fullShape.max(Direction.Axis.Y)));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testElevatorPickaxeTagAndSearchSkipInWorld(GameTestHelper helper) {
        Block white = ModBlocks.WHITE_ELEVATOR;
        Block blue = ModBlocks.ELEVATORS_BY_COLOR.get(DyeColor.BLUE);

        if (!white.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            helper.fail(Component.literal("Expected white_elevator in MINEABLE_WITH_PICKAXE tag"));
            return;
        }

        for (int y = 0; y <= 10; y++) {
            helper.setBlock(1, y, 1, Blocks.AIR);
        }
        helper.setBlock(1, 1, 1, white.defaultBlockState());
        helper.setBlock(1, 3, 1, blue.defaultBlockState());  // 异色
        helper.setBlock(1, 5, 1, white.defaultBlockState());
        helper.setBlock(1, 6, 1, Blocks.GLASS.defaultBlockState()); // 阻挡 y=5 上方
        helper.setBlock(1, 8, 1, white.defaultBlockState()); // 畅通目标

        BlockPos absOrigin = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos absExpected = helper.absolutePos(new BlockPos(1, 8, 1));
        BlockPos found = ElevatorManager.findTargetElevator(
                helper.getLevel(),
                absOrigin,
                DyeColor.WHITE,
                1,
                helper.getLevel().getMinY(),
                helper.getLevel().getMaxY(),
                CollisionContext.empty()
        );
        if (!absExpected.equals(found)) {
            helper.fail(Component.literal("Expected target elevator at " + absExpected + ", got " + found));
            return;
        }

        helper.succeed();
    }
}
