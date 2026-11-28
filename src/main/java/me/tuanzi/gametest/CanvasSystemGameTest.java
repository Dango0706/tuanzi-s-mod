package me.tuanzi.gametest;

import me.tuanzi.block.PrintedModelBlock;
import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
import me.tuanzi.block.entity.BlueprintTableBlockEntity;
import me.tuanzi.block.entity.CopierBlockEntity;
import me.tuanzi.block.entity.EaselBlockEntity;
import me.tuanzi.block.entity.PaperShredderBlockEntity;
import me.tuanzi.block.entity.Printer3DBlockEntity;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModItems;
import me.tuanzi.network.canvas.CopierActionC2SPacket;
import me.tuanzi.network.canvas.EaselActionC2SPacket;
import me.tuanzi.network.canvas.Printer3DActionC2SPacket;
import me.tuanzi.world.canvas.CanvasBlockRules;
import me.tuanzi.world.canvas.CanvasDimensionManager;
import me.tuanzi.world.canvas.CanvasFileStorage;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasPaletteEntry;
import me.tuanzi.world.canvas.CanvasPermission;
import me.tuanzi.world.canvas.CanvasSlotSavedData;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.canvas.CanvasVoxelData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CanvasSystemGameTest {

    @GameTest
    public void testAllCanvasSystemRecipesAndUpgradePreservesData(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();

        // 1. 画板配方
        CraftingInput easelIn = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.PAPER), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.OAK_PLANKS)
        ));
        Optional<RecipeHolder<CraftingRecipe>> easelMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, easelIn, level);
        if (easelMatch.isEmpty() || !easelMatch.get().value().assemble(easelIn).is(ModBlocks.EASEL.asItem())) {
            helper.fail(Component.literal("Expected easel crafting recipe to match"));
            return;
        }

        // 2. 基础画布配方
        CraftingInput canvasIn = CraftingInput.of(3, 1, List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.DYE.white()), new ItemStack(Items.STRING)
        ));
        Optional<RecipeHolder<CraftingRecipe>> canvasMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, canvasIn, level);
        if (canvasMatch.isEmpty() || !canvasMatch.get().value().assemble(canvasIn).is(ModItems.CANVAS)) {
            helper.fail(Component.literal("Expected canvas crafting recipe to match"));
            return;
        }

        // 3. 打印原料配方
        CraftingInput matIn = CraftingInput.of(3, 2, List.of(
                new ItemStack(Items.CLAY_BALL), new ItemStack(Items.RESIN_CLUMP), new ItemStack(Items.DYE.red()),
                new ItemStack(Items.DYE.green()), new ItemStack(Items.DYE.blue()), new ItemStack(Items.INK_SAC)
        ));
        Optional<RecipeHolder<CraftingRecipe>> matMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, matIn, level);
        if (matMatch.isEmpty() || !matMatch.get().value().assemble(matIn).is(ModItems.PRINT_MATERIAL)) {
            helper.fail(Component.literal("Expected print_material crafting recipe to match"));
            return;
        }

        // 4. 3D打印机配方
        CraftingInput printerIn = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.PISTON), new ItemStack(Items.IRON_BLOCK),
                new ItemStack(Items.DYE.red()), new ItemStack(Items.DYE.green()), new ItemStack(Items.DYE.blue()),
                new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_BLOCK)
        ));
        Optional<RecipeHolder<CraftingRecipe>> printerMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, printerIn, level);
        if (printerMatch.isEmpty() || !printerMatch.get().value().assemble(printerIn).is(ModBlocks.PRINTER_3D.asItem())) {
            helper.fail(Component.literal("Expected printer_3d crafting recipe to match"));
            return;
        }

        // 5. 复印机配方
        CraftingInput copierIn = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.GLASS),
                new ItemStack(Items.REDSTONE), new ItemStack(ModBlocks.PRINTER_3D), new ItemStack(Items.REDSTONE),
                new ItemStack(Items.SLIME_BALL), new ItemStack(Items.PAPER), new ItemStack(Items.SLIME_BALL)
        ));
        Optional<RecipeHolder<CraftingRecipe>> copierMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, copierIn, level);
        if (copierMatch.isEmpty() || !copierMatch.get().value().assemble(copierIn).is(ModBlocks.COPIER.asItem())) {
            helper.fail(Component.literal("Expected copier crafting recipe to match"));
            return;
        }

        // 6. 画布升级配方：16 -> 32 居中保留；3D 128 拒绝升级
        UUID srcUuid = UUID.randomUUID();
        CanvasVoxelData srcData = CanvasVoxelData.createEmpty(16, CanvasType.VOXEL_3D);
        srcData.setVoxel(4, 2, 4, new CanvasPaletteEntry("minecraft:stone", -1, -1));
        CanvasFileStorage.writeSaved(srcUuid, srcData);

        ItemStack smallCanvas = new ItemStack(ModItems.CANVAS);
        CanvasMetadata.write(smallCanvas, new CanvasMetadata(
                srcUuid, 1, 16, CanvasType.VOXEL_3D, null, "Tester",
                CanvasPermission.PRIVATE, 1, 1L, "h", false, 1, null, 0
        ));
        CraftingInput upgradeIn = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), smallCanvas, new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER)
        ));
        Optional<RecipeHolder<CraftingRecipe>> upgradeMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, upgradeIn, level);
        if (upgradeMatch.isEmpty()) {
            helper.fail(Component.literal("Expected canvas upgrade recipe to match for 16x16 3D canvas"));
            return;
        }
        ItemStack upgraded = upgradeMatch.get().value().assemble(upgradeIn);
        CanvasMetadata upMeta = CanvasFileStorage.ensureMaterialized(upgraded);
        if (upMeta.size() != 32 || upMeta.nonEmptyVoxels() != 1) {
            helper.fail(Component.literal("Expected upgraded canvas size=32 and nonEmpty=1, got " + upMeta));
            return;
        }

        ItemStack max3dCanvas = new ItemStack(ModItems.CANVAS);
        CanvasMetadata.write(max3dCanvas, new CanvasMetadata(
                UUID.randomUUID(), 1, 128, CanvasType.VOXEL_3D, null, "Tester",
                CanvasPermission.PRIVATE, 0, 1L, "", false, 1, null, 0
        ));
        CraftingInput blockedUpgrade = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), max3dCanvas, new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER)
        ));
        if (level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, blockedUpgrade, level).isPresent()) {
            helper.fail(Component.literal("3D canvas at size 128 must NOT upgrade to 256"));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testPrinterCopierAndMultiCellPrintedModelLifecycle(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
                helper.setBlock(x, 2, z, Blocks.AIR);
                helper.setBlock(x, 3, z, Blocks.AIR);
            }
        }

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();

        // 准备 32³ 3D 画布数据（含 300 个体素 -> 消耗 ceil(300/256)=2 个，2x 倍率消耗 4 个）
        UUID canvasUuid = UUID.randomUUID();
        CanvasVoxelData voxelData = CanvasVoxelData.createEmpty(32, CanvasType.VOXEL_3D);
        CanvasPaletteEntry colorEntry = new CanvasPaletteEntry("tuanzis_mod:color_block", 0xFF4422, -1);
        int count = 0;
        for (int y = 0; y < 16 && count < 300; y++) {
            for (int z = 0; z < 16 && count < 300; z++) {
                for (int x = 0; x < 16 && count < 300; x++) {
                    voxelData.setVoxel(x, y, z, colorEntry);
                    count++;
                }
            }
        }
        CanvasFileStorage.writeSaved(canvasUuid, voxelData);

        ItemStack canvas = new ItemStack(ModItems.CANVAS);
        CanvasMetadata.write(canvas, new CanvasMetadata(
                canvasUuid, 1, 32, CanvasType.VOXEL_3D, owner.getUUID(), "Owner",
                CanvasPermission.PRIVATE, 300, System.currentTimeMillis(), voxelData.computeSha256Hex(),
                false, 1, null, 0
        ));

        // 1. 放置 3D 打印机，测试原料不足阻止打印 -> 放入 4 个原料并选择 2x 打印
        BlockPos printerRel = new BlockPos(0, 1, 0);
        helper.setBlock(printerRel, ModBlocks.PRINTER_3D.defaultBlockState());
        Printer3DBlockEntity printer = (Printer3DBlockEntity) level.getBlockEntity(helper.absolutePos(printerRel));
        printer.setItem(Printer3DBlockEntity.SLOT_CANVAS, canvas);
        printer.setItem(Printer3DBlockEntity.SLOT_MATERIAL, new ItemStack(ModItems.PRINT_MATERIAL, 3));
        printer.handleAction(owner, Printer3DActionC2SPacket.ACTION_SELECT_SCALE, 2);
        printer.handleAction(owner, Printer3DActionC2SPacket.ACTION_PRINT, 2);
        if (!printer.getItem(Printer3DBlockEntity.SLOT_OUTPUT).isEmpty()) {
            helper.fail(Component.literal("Printer should refuse printing with 3 materials when cost is 4"));
            return;
        }

        printer.setItem(Printer3DBlockEntity.SLOT_MATERIAL, new ItemStack(ModItems.PRINT_MATERIAL, 4));
        printer.handleAction(owner, Printer3DActionC2SPacket.ACTION_PRINT, 2);
        ItemStack printed = printer.getItem(Printer3DBlockEntity.SLOT_OUTPUT);
        if (!printed.is(ModBlocks.PRINTED_MODEL.asItem()) || !printer.getItem(Printer3DBlockEntity.SLOT_MATERIAL).isEmpty()) {
            helper.fail(Component.literal("Expected printed model output and all 4 materials consumed"));
            return;
        }

        // 2. 放置复印机测试私有权限与复制
        BlockPos copierRel = new BlockPos(0, 1, 2);
        helper.setBlock(copierRel, ModBlocks.COPIER.defaultBlockState());
        CopierBlockEntity copier = (CopierBlockEntity) level.getBlockEntity(helper.absolutePos(copierRel));
        copier.setItem(CopierBlockEntity.SLOT_MODEL, printed.copy());
        copier.setItem(CopierBlockEntity.SLOT_MATERIAL, new ItemStack(ModItems.PRINT_MATERIAL, 10));

        copier.handleAction(stranger, CopierActionC2SPacket.ACTION_COPY);
        if (!copier.getItem(CopierBlockEntity.SLOT_OUTPUT).isEmpty()) {
            helper.fail(Component.literal("Stranger should not copy a PRIVATE printed model"));
            return;
        }
        copier.handleAction(owner, CopierActionC2SPacket.ACTION_COPY);
        ItemStack copied = copier.getItem(CopierBlockEntity.SLOT_OUTPUT);
        if (!copied.is(ModBlocks.PRINTED_MODEL.asItem()) || copier.getItem(CopierBlockEntity.SLOT_MATERIAL).getCount() != 6) {
            helper.fail(Component.literal("Owner copy should succeed and consume 4 materials"));
            return;
        }

        // 3. 多格 (2x2x2) 放置与碰撞箱验证及破坏不返还画布验证
        BlockPos modelRel = new BlockPos(2, 1, 2);
        BlockPos modelAbs = helper.absolutePos(modelRel);
        owner.setItemInHand(InteractionHand.MAIN_HAND, printed.copy());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(modelAbs.below()), Direction.UP, modelAbs.below(), false);
        BlockPlaceContext placeCtx = new BlockPlaceContext(owner, InteractionHand.MAIN_HAND, printed.copy(), hit);
        printed.getItem().useOn(placeCtx);

        if (!level.getBlockState(modelAbs).is(ModBlocks.PRINTED_MODEL)
                || !level.getBlockState(modelAbs.offset(1, 1, 1)).is(ModBlocks.PRINTED_MODEL_PART)) {
            helper.fail(Component.literal("Expected 2x2x2 PrintedModel + PrintedModelPart blocks"));
            return;
        }
        VoxelShape shape = level.getBlockState(modelAbs).getCollisionShape(level, modelAbs, CollisionContext.empty());
        if (shape.isEmpty()) {
            helper.fail(Component.literal("Expected non-empty merged collision shape on PrintedModelBlock"));
            return;
        }

        // 4. 画布空间黑名单与施工工具规则校验
        if (CanvasBlockRules.isAllowedInCanvasSpace(Blocks.CHEST.defaultBlockState())
                || CanvasBlockRules.isAllowedInCanvasSpace(Blocks.HOPPER.defaultBlockState())
                || CanvasBlockRules.isAllowedInCanvasSpace(ModBlocks.PAPER_SHREDDER.defaultBlockState())
                || !CanvasBlockRules.isAllowedInCanvasSpace(Blocks.WATER.defaultBlockState())
                || !CanvasBlockRules.isAllowedInCanvasSpace(ModBlocks.COLOR_BLOCK.defaultBlockState())
                || !CanvasBlockRules.isCanvasUtilityBlock(Blocks.SHULKER_BOX.defaultBlockState())
                || !CanvasBlockRules.isCanvasUtilityBlock(Blocks.DYED_SHULKER_BOX.red().defaultBlockState())
                || !CanvasBlockRules.isCanvasUtilityBlock(ModBlocks.BLUEPRINT_CANNON.defaultBlockState())
                || !CanvasBlockRules.isCanvasUtilityBlock(ModBlocks.BLUEPRINT_TABLE.defaultBlockState())
                || !CanvasBlockRules.isAllowedInCanvasSpace(Blocks.SHULKER_BOX.defaultBlockState())
                || !CanvasBlockRules.isAllowedInCanvasSpace(Blocks.DYED_SHULKER_BOX.red().defaultBlockState())
                || !CanvasBlockRules.isAllowedInCanvasSpace(ModBlocks.BLUEPRINT_CANNON.defaultBlockState())
                || !CanvasBlockRules.isAllowedInCanvasSpace(ModBlocks.BLUEPRINT_TABLE.defaultBlockState())
                || CanvasBlockRules.isAllowedAsCanvasVoxel(Blocks.SHULKER_BOX.defaultBlockState())
                || CanvasBlockRules.isAllowedAsCanvasVoxel(ModBlocks.BLUEPRINT_CANNON.defaultBlockState())
                || CanvasBlockRules.isAllowedAsCanvasVoxel(ModBlocks.BLUEPRINT_TABLE.defaultBlockState())) {
            helper.fail(Component.literal("CanvasBlockRules check failed"));
            return;
        }

        // 5. 画板定型测试
        BlockPos easelRel = new BlockPos(0, 1, 4);
        helper.setBlock(easelRel, ModBlocks.EASEL.defaultBlockState());
        EaselBlockEntity easel = (EaselBlockEntity) level.getBlockEntity(helper.absolutePos(easelRel));
        ItemStack freshCanvas = new ItemStack(ModItems.CANVAS);
        easel.setItem(0, freshCanvas);
        easel.handleAction(owner, EaselActionC2SPacket.ACTION_SET_3D);
        if (CanvasMetadata.read(easel.getCanvasStack()).type() != CanvasType.VOXEL_3D) {
            helper.fail(Component.literal("Expected Easel to lock canvas type to VOXEL_3D"));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testPaperShredderRecipePermissionConfirmAndRefund(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        // 1. 碎纸机合成配方校验
        CraftingInput shredderIn = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.SHEARS), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.HOPPER), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.REDSTONE), new ItemStack(Items.STONE_BRICKS)
        ));
        Optional<RecipeHolder<CraftingRecipe>> shredderMatch = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, shredderIn, level);
        if (shredderMatch.isEmpty() || !shredderMatch.get().value().assemble(shredderIn).is(ModBlocks.PAPER_SHREDDER.asItem())) {
            helper.fail(Component.literal("Expected paper_shredder crafting recipe to match"));
            return;
        }

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();

        UUID canvasUuid = UUID.randomUUID();
        CanvasVoxelData data = CanvasVoxelData.createEmpty(32, CanvasType.FLAT_2D);
        CanvasFileStorage.writeSaved(canvasUuid, data);
        CanvasSlotSavedData.get(level).incrementOwnerCount(owner.getUUID());
        int countBefore = CanvasSlotSavedData.get(level).getOwnerCount(owner.getUUID());

        ItemStack canvas = new ItemStack(ModItems.CANVAS);
        CanvasMetadata.write(canvas, new CanvasMetadata(
                canvasUuid, 1, 32, CanvasType.FLAT_2D, owner.getUUID(), "Owner",
                CanvasPermission.PRIVATE, 0, 1L, "h", false, 1, null, 0
        ));

        BlockPos shredderRel = new BlockPos(1, 1, 1);
        helper.setBlock(shredderRel, ModBlocks.PAPER_SHREDDER.defaultBlockState());
        PaperShredderBlockEntity shredder = (PaperShredderBlockEntity) level.getBlockEntity(helper.absolutePos(shredderRel));
        shredder.setItem(0, canvas);

        // 2. 他人无权损毁；未二次确认不能损毁
        if (shredder.handleShred(stranger, true) || shredder.getCanvasStack().isEmpty()) {
            helper.fail(Component.literal("Stranger must NOT shred owner's canvas"));
            return;
        }
        if (shredder.handleShred(owner, false) || shredder.getCanvasStack().isEmpty()) {
            helper.fail(Component.literal("Shredding without confirmation must NOT succeed"));
            return;
        }

        // 3. 本人二次确认后损毁成功，返还 9 纸 + 1 白染料 + 1 线，并归档 .bak 且配额 -1
        if (!shredder.handleShred(owner, true) || !shredder.getCanvasStack().isEmpty()) {
            helper.fail(Component.literal("Owner confirmed shred should succeed and empty slot"));
            return;
        }
        if (Files.exists(CanvasFileStorage.getSavedFile(canvasUuid))
                || !Files.isRegularFile(CanvasFileStorage.getPaintedDir().resolve(canvasUuid + ".painted.bak"))) {
            helper.fail(Component.literal("Expected .painted file to be archived as .bak"));
            return;
        }
        if (CanvasSlotSavedData.get(level).getOwnerCount(owner.getUUID()) != Math.max(0, countBefore - 1)) {
            helper.fail(Component.literal("Expected owner canvas quota count to decrement by 1"));
            return;
        }
        int papers = owner.getInventory().countItem(Items.PAPER);
        int whiteDyes = owner.getInventory().countItem(Items.DYE.white());
        int strings = owner.getInventory().countItem(Items.STRING);
        if (papers < 9 || whiteDyes < 1 || strings < 1) {
            helper.fail(Component.literal("Expected refunded materials (9 paper, 1 white dye, 1 string), got "
                    + papers + "/" + whiteDyes + "/" + strings));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testCanvasItemImmuneAndVoidRescue(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Vec3 ownerPos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        owner.teleportTo(ownerPos.x, ownerPos.y, ownerPos.z);

        ItemStack canvas = new ItemStack(ModItems.CANVAS);
        CanvasMetadata.write(canvas, CanvasMetadata.createDefaultBlank().withTypeAndOwner(
                CanvasType.FLAT_2D, owner.getUUID(), "Owner"
        ));

        Vec3 spawnPos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        ItemEntity itemEntity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, canvas);
        level.addFreshEntity(itemEntity);

        if (!itemEntity.fireImmune()
                || itemEntity.hurtServer(level, level.damageSources().inFire(), 10.0F)
                || itemEntity.hurtServer(level, level.damageSources().lava(), 10.0F)
                || itemEntity.hurtServer(level, level.damageSources().cactus(), 10.0F)) {
            helper.fail(Component.literal("Canvas ItemEntity must be immune to fire, lava, and cactus"));
            return;
        }

        // 模拟落入虚空并触发 checkBelowWorld
        itemEntity.setPos(spawnPos.x, level.getMinY() - 80.0, spawnPos.z);
        itemEntity.checkBelowWorld();
        if (itemEntity.isRemoved() || itemEntity.position().distanceTo(owner.position()) > 3.0) {
            helper.fail(Component.literal("Canvas ItemEntity should be rescued from void back to owner"));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testIndependentCanvasDimensionsAndZeroOrigin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();

        ServerLevel levelA = CanvasDimensionManager.getOrCreateCanvasLevel(level.getServer(), uuidA);
        ServerLevel levelB = CanvasDimensionManager.getOrCreateCanvasLevel(level.getServer(), uuidB);

        if (levelA == null || levelB == null || levelA == levelB || levelA.dimension().equals(levelB.dimension())) {
            helper.fail(Component.literal("Expected two distinct dynamic ServerLevels for distinct canvases"));
            return;
        }
        if (!ModDimensions.isCanvasSpaceLevel(levelA) || !ModDimensions.isCanvasSpaceLevel(levelB)) {
            helper.fail(Component.literal("Expected dynamic levels to match ModDimensions.isCanvasSpaceLevel"));
            return;
        }
        if (!CanvasSpaceManager.STANDARD_ORIGIN.equals(BlockPos.ZERO)) {
            helper.fail(Component.literal("Expected standard canvas origin to be (0, 0, 0)"));
            return;
        }

        CanvasDimensionManager.scheduleUnload(levelA.dimension());
        CanvasDimensionManager.scheduleUnload(levelB.dimension());
        CanvasDimensionManager.processPendingUnloads(level.getServer());
        helper.succeed();
    }

    @GameTest
    public void testCanvasUtilityBlocksPlacementExitAndCannonBounds(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        ServerPlayer editor = helper.makeMockServerPlayerInLevel();
        Vec3 returnPos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        editor.teleportTo(returnPos.x, returnPos.y, returnPos.z);

        BlockPos easelRel = new BlockPos(0, 1, 0);
        BlockPos easelAbs = helper.absolutePos(easelRel);
        helper.setBlock(easelRel, ModBlocks.EASEL.defaultBlockState());
        EaselBlockEntity easel = (EaselBlockEntity) overworld.getBlockEntity(easelAbs);
        ItemStack canvas = new ItemStack(ModItems.CANVAS);
        easel.setItem(0, canvas);
        easel.handleAction(editor, EaselActionC2SPacket.ACTION_SET_3D);

        if (!CanvasSpaceManager.enterCanvasSpace(editor, overworld, easelAbs, easel)) {
            helper.fail(Component.literal("Expected editor to enter Canvas Space"));
            return;
        }

        CanvasSpaceManager.CanvasEditSession session = CanvasSpaceManager.getSessionForPlayer(editor.getUUID());
        ServerLevel canvasLevel = overworld.getServer().getLevel(session.canvasDimension);
        if (canvasLevel == null || !ModDimensions.isCanvasSpaceLevel(canvasLevel)) {
            helper.fail(Component.literal("Expected active canvas dimension level"));
            return;
        }

        // 1. 外围 8 格缓冲内允许放置与破坏潜影盒、蓝图大炮、蓝图桌；普通石头仅限编辑区内部
        BlockPos bufferShulkerPos = new BlockPos(-2, -1, 0);
        BlockPos bufferCannonPos = new BlockPos(-2, 0, 1);
        BlockPos bufferTablePos = new BlockPos(-1, 0, 2);
        BlockPos outside9Pos = new BlockPos(-9, 0, 0);
        if (!CanvasSpaceManager.canPlaceAt(editor, canvasLevel, bufferShulkerPos, Blocks.DYED_SHULKER_BOX.red().defaultBlockState())
                || !CanvasSpaceManager.canPlaceAt(editor, canvasLevel, bufferCannonPos, ModBlocks.BLUEPRINT_CANNON.defaultBlockState())
                || !CanvasSpaceManager.canPlaceAt(editor, canvasLevel, bufferTablePos, ModBlocks.BLUEPRINT_TABLE.defaultBlockState())
                || !CanvasSpaceManager.canPlaceAt(editor, canvasLevel, new BlockPos(1, 0, 1), Blocks.SHULKER_BOX.defaultBlockState())
                || CanvasSpaceManager.canPlaceAt(editor, canvasLevel, outside9Pos, ModBlocks.BLUEPRINT_CANNON.defaultBlockState())
                || CanvasSpaceManager.canPlaceAt(editor, canvasLevel, bufferCannonPos, Blocks.STONE.defaultBlockState())
                || !CanvasSpaceManager.canBreakAt(editor, canvasLevel, bufferTablePos, ModBlocks.BLUEPRINT_TABLE.defaultBlockState())) {
            helper.fail(Component.literal("Utility block buffer placement/break rule check failed"));
            return;
        }

        // 2. 放置常规建筑方块 (1个金块) 与带物品的潜影盒、蓝图大炮、蓝图桌，验证保存体素时忽略施工工具
        canvasLevel.setBlock(new BlockPos(2, 0, 2), Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

        canvasLevel.setBlock(bufferShulkerPos, Blocks.DYED_SHULKER_BOX.red().defaultBlockState(), Block.UPDATE_ALL);
        ShulkerBoxBlockEntity shulkerBe = (ShulkerBoxBlockEntity) canvasLevel.getBlockEntity(bufferShulkerPos);
        shulkerBe.setItem(0, new ItemStack(Items.STONE, 16));
        shulkerBe.setItem(1, new ItemStack(Items.DIAMOND, 3));

        // 构造含界内(3,0,3石头)、界外(17,0,3石头)、禁用方块(4,0,3箱子)与实体(armor_stand)的蓝图
        Map<BlockPos, BlockState> bpBlocks = new HashMap<>();
        bpBlocks.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());   // -> (3, 0, 3) 界内
        bpBlocks.put(new BlockPos(1, 0, 0), Blocks.CHEST.defaultBlockState());   // -> (4, 0, 3) 禁用
        bpBlocks.put(new BlockPos(14, 0, 0), Blocks.STONE.defaultBlockState());  // -> (17, 0, 3) 越界
        ItemStack testBlueprint = buildTestBlueprintStack(new BlockPos(3, 0, 3), 15, 1, 1, bpBlocks, true);

        canvasLevel.setBlock(bufferCannonPos, ModBlocks.BLUEPRINT_CANNON.defaultBlockState(), Block.UPDATE_ALL);
        BlueprintCannonBlockEntity cannonBe = (BlueprintCannonBlockEntity) canvasLevel.getBlockEntity(bufferCannonPos);
        cannonBe.setItem(0, testBlueprint.copy());
        cannonBe.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 2));

        canvasLevel.setBlock(bufferTablePos, ModBlocks.BLUEPRINT_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        BlueprintTableBlockEntity tableBe = (BlueprintTableBlockEntity) canvasLevel.getBlockEntity(bufferTablePos);
        tableBe.setItem(1, new ItemStack(ModItems.BLANK_BLUEPRINT, 1));

        // 同时验证编辑区内的金块仍计入体素，而潜影盒/大炮/蓝图桌完全排除（总共应只有 1 个非空体素）
        if (!CanvasSpaceManager.commitSaveFromEasel(editor, easel)) {
            helper.fail(Component.literal("Expected commitSaveFromEasel to succeed"));
            return;
        }
        CanvasMetadata savedMeta = CanvasMetadata.read(easel.getCanvasStack());
        if (savedMeta.nonEmptyVoxels() != 1) {
            helper.fail(Component.literal("Expected only 1 non-empty voxel (utility blocks excluded), got " + savedMeta.nonEmptyVoxels()));
            return;
        }

        // 3. 在画布内运行蓝图大炮（开启破坏模式）：界内石头应建造，越界与箱子/实体应跳过，Y=-1白色底面与工具不受破坏
        cannonBe.handleAction("START", true, false);
        for (int t = 0; t < 80; t++) {
            BlueprintCannonBlockEntity.serverTick(canvasLevel, bufferCannonPos, canvasLevel.getBlockState(bufferCannonPos), cannonBe);
        }
        if (!canvasLevel.getBlockState(new BlockPos(3, 0, 3)).is(Blocks.STONE)
                || !canvasLevel.getBlockState(new BlockPos(17, 0, 3)).isAir()
                || canvasLevel.getBlockState(new BlockPos(4, 0, 3)).is(Blocks.CHEST)
                || !canvasLevel.getBlockState(new BlockPos(3, -1, 3)).is(ModBlocks.CANVAS_BASE_BLOCK)
                || !canvasLevel.getBlockState(bufferShulkerPos).is(Blocks.DYED_SHULKER_BOX.red())
                || !canvasLevel.getBlockState(bufferTablePos).is(ModBlocks.BLUEPRINT_TABLE)) {
            helper.fail(Component.literal("BlueprintCannon inside Canvas Space violated bounds, block rules, or base protection"));
            return;
        }

        // 4. 主动退出因仍有施工工具必须被拦截
        if (CanvasSpaceManager.exitCanvasSpace(editor, CanvasSpaceManager.ExitReason.VOLUNTARY)
                || !ModDimensions.isCanvasSpaceLevel(editor.level())) {
            helper.fail(Component.literal("Voluntary exit must be blocked while utility blocks remain"));
            return;
        }

        // 5. 强制退出（拆画板场景）应自动打包回收红潜影盒(含钻石)、大炮、蓝图桌与空白蓝图至玩家背包并退出成功
        if (!CanvasSpaceManager.exitCanvasSpace(editor, CanvasSpaceManager.ExitReason.EASEL_REMOVED)
                || ModDimensions.isCanvasSpaceLevel(editor.level())) {
            helper.fail(Component.literal("Forced exit should automatically recover utility blocks and return player"));
            return;
        }

        boolean foundRedShulkerWithDiamond = false;
        for (int i = 0; i < editor.getInventory().getContainerSize(); i++) {
            ItemStack st = editor.getInventory().getItem(i);
            if (st.is(Blocks.DYED_SHULKER_BOX.red().asItem())) {
                ItemContainerContents contents = st.get(DataComponents.CONTAINER);
                if (contents != null) {
                    for (ItemStackTemplate tpl : contents.nonEmptyItems()) {
                        ItemStack inner = tpl.create();
                        if (inner.is(Items.DIAMOND) && inner.getCount() == 3) {
                            foundRedShulkerWithDiamond = true;
                        }
                    }
                }
            }
        }
        if (!foundRedShulkerWithDiamond
                || editor.getInventory().countItem(ModBlocks.BLUEPRINT_CANNON.asItem()) < 1
                || editor.getInventory().countItem(ModBlocks.BLUEPRINT_TABLE.asItem()) < 1
                || editor.getInventory().countItem(ModItems.BLANK_BLUEPRINT) < 1) {
            helper.fail(Component.literal("Expected forced exit to recover Red Shulker Box (with contents), Cannon, Table, and Blank Blueprint"));
            return;
        }

        CanvasDimensionManager.processPendingUnloads(overworld.getServer());
        helper.succeed();
    }

    private static ItemStack buildTestBlueprintStack(
            BlockPos ghostPos,
            int width,
            int height,
            int length,
            Map<BlockPos, BlockState> localBlocks,
            boolean withEntity
    ) {
        List<BlockState> palette = new ArrayList<>();
        palette.add(Blocks.AIR.defaultBlockState());
        int[] blocks = new int[width * height * length];
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = localBlocks.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    int idx = palette.indexOf(state);
                    if (idx < 0) {
                        idx = palette.size();
                        palette.add(state);
                    }
                    blocks[y * width * length + z * width + x] = idx;
                }
            }
        }
        ListTag paletteTag = new ListTag();
        for (BlockState s : palette) {
            paletteTag.add(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, s).getOrThrow());
        }
        ListTag entities = new ListTag();
        if (withEntity) {
            CompoundTag e = new CompoundTag();
            e.putString("id", "minecraft:armor_stand");
            ListTag p = new ListTag();
            p.add(net.minecraft.nbt.DoubleTag.valueOf(0.5));
            p.add(net.minecraft.nbt.DoubleTag.valueOf(0.0));
            p.add(net.minecraft.nbt.DoubleTag.valueOf(0.5));
            e.put("Pos", p);
            entities.add(e);
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putInt("Length", length);
        tag.put("Palette", paletteTag);
        tag.putIntArray("Blocks", blocks);
        tag.put("BlockEntities", new ListTag());
        tag.put("Entities", entities);
        tag.putBoolean("GhostPlaced", true);
        tag.putInt("GhostX", ghostPos.getX());
        tag.putInt("GhostY", ghostPos.getY());
        tag.putInt("GhostZ", ghostPos.getZ());
        tag.putInt("OffsetX", 0);
        tag.putInt("OffsetY", 0);
        tag.putInt("OffsetZ", 0);
        tag.putInt("Rotation", 0);
        tag.putBoolean("Mirrored", false);
        ItemStack stack = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
