package me.tuanzi.gametest;

import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
import me.tuanzi.block.entity.BlueprintTableBlockEntity;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlueprintCannonGameTest {

    private static ItemStack createBlueprintItem(
            BlockPos ghostAbsPos,
            int width,
            int height,
            int length,
            Map<BlockPos, BlockState> localBlocks
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
            Tag encoded = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, s).getOrThrow();
            paletteTag.add(encoded);
        }

        CompoundTag tag = new CompoundTag();
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putInt("Length", length);
        tag.put("Palette", paletteTag);
        tag.putIntArray("Blocks", blocks);
        tag.put("BlockEntities", new ListTag());
        tag.put("Entities", new ListTag());
        tag.putBoolean("GhostPlaced", true);
        tag.putInt("GhostX", ghostAbsPos.getX());
        tag.putInt("GhostY", ghostAbsPos.getY());
        tag.putInt("GhostZ", ghostAbsPos.getZ());
        tag.putInt("OffsetX", 0);
        tag.putInt("OffsetY", 0);
        tag.putInt("OffsetZ", 0);
        tag.putInt("Rotation", 0);
        tag.putBoolean("Mirrored", false);

        ItemStack stack = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @GameTest(maxTicks = 400)
    public void testDestroyFirstThenPlace(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        // 预置需要拆除的障碍（含普通泥土、圆石以及含水石砖半砖）
        helper.setBlock(3, 1, 1, Blocks.DIRT);
        helper.setBlock(4, 1, 1, Blocks.DIRT);
        helper.setBlock(3, 2, 1, Blocks.STONE_BRICK_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(SlabBlock.WATERLOGGED, true));
        helper.setBlock(4, 2, 1, Blocks.COBBLESTONE);

        // 放置大炮与补给箱
        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);

        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.STONE, 4));

        Map<BlockPos, BlockState> blueprintMap = new HashMap<>();
        blueprintMap.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());
        blueprintMap.put(new BlockPos(0, 1, 0), Blocks.STONE.defaultBlockState());
        // (1,0,0) 与 (1,1,0) 为 AIR，用于验证拆除多余障碍方块

        BlueprintCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        cannon.setItem(0, createBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 1)), 2, 2, 1, blueprintMap));
        cannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 4));
        cannon.handleAction("START", true, false);

        final boolean[] sawNewBlockWhileOldRemained = {false};
        helper.succeedWhen(() -> {
            // 在建造完成前监控：不得在旧障碍仍存在时提前放置新石头
            boolean anyOldLeft = !helper.getBlockState(new BlockPos(3, 1, 1)).isAir()
                    && helper.getBlockState(new BlockPos(3, 1, 1)).is(Blocks.DIRT)
                    || helper.getBlockState(new BlockPos(4, 1, 1)).is(Blocks.DIRT)
                    || helper.getBlockState(new BlockPos(3, 2, 1)).is(Blocks.STONE_BRICK_SLAB)
                    || helper.getBlockState(new BlockPos(4, 2, 1)).is(Blocks.COBBLESTONE);
            boolean anyNewPlaced = helper.getBlockState(new BlockPos(3, 1, 1)).is(Blocks.STONE)
                    || helper.getBlockState(new BlockPos(3, 2, 1)).is(Blocks.STONE);
            if (anyOldLeft && anyNewPlaced) {
                sawNewBlockWhileOldRemained[0] = true;
            }
            if (sawNewBlockWhileOldRemained[0]) {
                helper.fail(Component.literal("Must finish destroying obstructing blocks before placing new blocks"));
                return;
            }

            if (cannon.dataAccess.get(0) != 0) {
                helper.fail(Component.literal("Cannon still running, progress="
                        + me.tuanzi.world.inventory.BlueprintCannonMenu.reconstructInt(cannon.dataAccess.get(1), cannon.dataAccess.get(2))
                        + "/"
                        + me.tuanzi.world.inventory.BlueprintCannonMenu.reconstructInt(cannon.dataAccess.get(3), cannon.dataAccess.get(4))));
                return;
            }

            helper.assertBlockPresent(Blocks.STONE, 3, 1, 1);
            helper.assertBlockPresent(Blocks.STONE, 3, 2, 1);
            helper.assertBlockPresent(Blocks.AIR, 4, 1, 1);
            helper.assertBlockPresent(Blocks.AIR, 4, 2, 1);

            if (chest.getItem(0).getCount() != 2) {
                helper.fail(Component.literal("Expected 2 stone remaining in chest, got " + chest.getItem(0).getCount()));
            }
        });
    }

    @GameTest(maxTicks = 600)
    public void testDependentAndMultiBlockOrderAndPlacement(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 5; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);

        // 结构布局 (起点 3,1,1 尺寸 4x3x2):
        // y=0(世界y=1): 石头(0,0,0), 橡木板(1,0,0), 橡木板(2,0,0), 橡木板(2,0,1), 草方块(3,0,0)
        // y=1(世界y=2): 沙子(0,1,0), 墙火把朝南(0,1,1)依附(0,1,0)? 沙子不稳改放石头支撑墙火把; 悬挂灯笼(1,1,1)依附上方(1,2,1); 橡木门下半(1,1,0)? 错开x避免干扰:
        // 清晰布局 (5x3x2):
        // x=0: (0,0,0)=STONE, (0,1,0)=SAND (检验下落等待), (0,2,0)=AIR
        // x=1: (1,2,1)=OAK_PLANKS(天花板), (1,1,1)=LANTERN[hanging=true](依附上方天花板), (1,0,0)=OAK_PLANKS(地面), (1,1,0)=WALL_TORCH[SOUTH]? -> 放到(1,0,1)墙上
        // 更直观的 5x3x2:
        // x=0: (0,0,0)=STONE, (0,1,0)=SAND, (0,0,1)=STONE, (0,1,1)=WALL_TORCH[SOUTH] 依附 (0,1,0)? No, (0,1,0) is SAND (not sturdy? actually sand is sturdy, let's use STONE at (0,2,0) & OAK_PLANKS at (0,0,1))
        Map<BlockPos, BlockState> bp = new HashMap<>();
        // 支撑与单方块
        bp.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());
        bp.put(new BlockPos(0, 1, 0), Blocks.SAND.defaultBlockState());
        bp.put(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
        bp.put(new BlockPos(1, 2, 1), Blocks.OAK_PLANKS.defaultBlockState());
        bp.put(new BlockPos(1, 1, 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        bp.put(new BlockPos(0, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
        bp.put(new BlockPos(0, 1, 1), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        bp.put(new BlockPos(2, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
        bp.put(new BlockPos(3, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
        bp.put(new BlockPos(3, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
        bp.put(new BlockPos(4, 0, 0), Blocks.GRASS_BLOCK.defaultBlockState());

        // 多方块 1: 橡木门 (位于 (2,1,0) & (2,2,0)，依赖下方 (2,0,0) 橡木板)
        bp.put(new BlockPos(2, 1, 0), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.FACING, Direction.NORTH));
        bp.put(new BlockPos(2, 2, 0), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(DoorBlock.FACING, Direction.NORTH));

        // 多方块 2: 红床 (位于 (3,1,0) FOOT & (3,1,1) HEAD) + 床上地毯 (3,2,0)
        bp.put(new BlockPos(3, 1, 0), Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
        bp.put(new BlockPos(3, 1, 1), Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        bp.put(new BlockPos(3, 2, 0), Blocks.CARPET.white().defaultBlockState());

        // 多方块 3: 向日葵 (位于 (4,1,0) LOWER & (4,2,0) UPPER，依赖下方 (4,0,0) 草方块)
        bp.put(new BlockPos(4, 1, 0), Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        bp.put(new BlockPos(4, 2, 0), Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));

        // 精确放入 1 份材料（门1、床1、向日葵1、灯笼1、火把1、地毯1、石头1、沙子1、草方块1、橡木板6）
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.STONE, 1));
        chest.setItem(1, new ItemStack(Items.SAND, 1));
        chest.setItem(2, new ItemStack(Items.OAK_PLANKS, 6));
        chest.setItem(3, new ItemStack(Items.LANTERN, 1));
        chest.setItem(4, new ItemStack(Items.TORCH, 1));
        chest.setItem(5, new ItemStack(Items.GRASS_BLOCK, 1));
        chest.setItem(6, new ItemStack(Items.OAK_DOOR, 1));
        chest.setItem(7, new ItemStack(Blocks.BED.red().asItem(), 1));
        chest.setItem(8, new ItemStack(Blocks.CARPET.white().asItem(), 1));
        chest.setItem(9, new ItemStack(Items.SUNFLOWER, 1));

        BlueprintCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        cannon.setItem(0, createBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 1)), 5, 3, 2, bp));
        cannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 4));
        cannon.setItem(2, new ItemStack(Items.WRITABLE_BOOK));

        // 校验材料清单统计：门/床/向日葵各自应当只统计为 1 个
        if (!cannon.writeMaterialBook(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 1)))) {
            helper.fail(Component.literal("writeMaterialBook failed"));
            return;
        }
        ItemStack writtenBook = cannon.getItem(2);
        var bookContent = writtenBook.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (bookContent == null) {
            helper.fail(Component.literal("Expected WRITTEN_BOOK_CONTENT"));
            return;
        }
        StringBuilder allBookText = new StringBuilder();
        for (var page : bookContent.pages()) {
            allBookText.append(page.raw().getString()).append("\n");
        }
        if (allBookText.toString().contains("✗")) {
            helper.fail(Component.literal("Material book should show all materials fulfilled (✔), got:\n" + allBookText));
            return;
        }

        cannon.handleAction("START", true, false);

        final boolean[] lanternPlacedBeforeDoor = {false};
        final boolean[] sawDoorBeforeLantern = {false};

        helper.succeedWhen(() -> {
            boolean lanternPresent = helper.getBlockState(new BlockPos(4, 2, 2)).is(Blocks.LANTERN);
            boolean doorPresent = helper.getBlockState(new BlockPos(5, 2, 1)).is(Blocks.OAK_DOOR);
            if (lanternPresent && !doorPresent) {
                lanternPlacedBeforeDoor[0] = true;
            }
            if (doorPresent && !lanternPresent && !lanternPlacedBeforeDoor[0]) {
                sawDoorBeforeLantern[0] = true;
            }
            if (sawDoorBeforeLantern[0]) {
                helper.fail(Component.literal("Dependent block (Lantern) must be handled before multi-blocks (Door)"));
                return;
            }

            if (cannon.dataAccess.get(0) != 0) {
                helper.fail(Component.literal("Cannon still running, progress="
                        + me.tuanzi.world.inventory.BlueprintCannonMenu.reconstructInt(cannon.dataAccess.get(1), cannon.dataAccess.get(2))
                        + "/"
                        + me.tuanzi.world.inventory.BlueprintCannonMenu.reconstructInt(cannon.dataAccess.get(3), cannon.dataAccess.get(4))));
                return;
            }

            // 校验所有方块均完好放置
            helper.assertBlockPresent(Blocks.STONE, 3, 1, 1);
            helper.assertBlockPresent(Blocks.SAND, 3, 2, 1);
            helper.assertBlockPresent(Blocks.WALL_TORCH, 3, 2, 2);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, 4, 3, 2);
            helper.assertBlockPresent(Blocks.LANTERN, 4, 2, 2);
            helper.assertBlockPresent(Blocks.OAK_DOOR, 5, 2, 1);
            helper.assertBlockPresent(Blocks.OAK_DOOR, 5, 3, 1);
            helper.assertBlockPresent(Blocks.BED.red(), 6, 2, 1);
            helper.assertBlockPresent(Blocks.BED.red(), 6, 2, 2);
            helper.assertBlockPresent(Blocks.CARPET.white(), 6, 3, 1);
            helper.assertBlockPresent(Blocks.SUNFLOWER, 7, 2, 1);
            helper.assertBlockPresent(Blocks.SUNFLOWER, 7, 3, 1);

            // 确认无掉落损坏物
            BlockPos minAbs = helper.absolutePos(new BlockPos(0, 0, 0));
            BlockPos maxAbs = helper.absolutePos(new BlockPos(8, 5, 4));
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(minAbs.getX(), minAbs.getY(), minAbs.getZ(), maxAbs.getX() + 1, maxAbs.getY() + 1, maxAbs.getZ() + 1)
            );
            if (!drops.isEmpty()) {
                helper.fail(Component.literal("Expected zero dropped items, found: " + drops.getFirst().getItem()));
                return;
            }

            // 确认箱内材料全部恰好消耗完毕
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (!chest.getItem(i).isEmpty()) {
                    helper.fail(Component.literal("Expected chest slot " + i + " empty, found " + chest.getItem(i)));
                    return;
                }
            }
        });
    }

    @GameTest(maxTicks = 100)
    public void testCannonRejectsOutOfBoundsAndDistantTargetsAndMalformedDimensions(GameTestHelper helper) {
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }

        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.STONE, 10));

        BlueprintCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        cannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 4));

        // 1. 构造超出建筑高度上限(Y=500)以及超远未加载区块(X+10000)的目标蓝图，验证不消耗材料、不消耗燃料且不强制加载远端区块
        BlockPos absCannonPos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos farUnloadedPos = new BlockPos(absCannonPos.getX() + 10000, 64, absCannonPos.getZ() + 10000);
        if (helper.getLevel().isLoaded(farUnloadedPos)) {
            helper.fail(Component.literal("Precondition failed: farUnloadedPos should not be loaded"));
            return;
        }

        Map<BlockPos, BlockState> outOfBoundsMap = new HashMap<>();
        outOfBoundsMap.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());
        cannon.setItem(0, createBlueprintItem(new BlockPos(absCannonPos.getX(), 500, absCannonPos.getZ()), 1, 1, 1, outOfBoundsMap));
        cannon.handleAction("START", true, false);

        for (int i = 0; i < 10; i++) {
            BlueprintCannonBlockEntity.serverTick(helper.getLevel(), absCannonPos, cannon.getBlockState(), cannon);
        }
        if (chest.getItem(0).getCount() != 10) {
            helper.fail(Component.literal("Out-of-height target must not consume materials, remaining=" + chest.getItem(0).getCount()));
            return;
        }

        // 2. 远端目标（超射程且未加载）：不能强制加载区块且不能消耗材料
        cannon.setItem(0, createBlueprintItem(farUnloadedPos, 1, 1, 1, outOfBoundsMap));
        cannon.handleAction("START", true, false);
        for (int i = 0; i < 10; i++) {
            BlueprintCannonBlockEntity.serverTick(helper.getLevel(), absCannonPos, cannon.getBlockState(), cannon);
        }
        if (helper.getLevel().isLoaded(farUnloadedPos)) {
            helper.fail(Component.literal("Cannon must not force-load distant chunks"));
            return;
        }
        if (chest.getItem(0).getCount() != 10) {
            helper.fail(Component.literal("Distant target must not consume materials, remaining=" + chest.getItem(0).getCount()));
            return;
        }

        // 3. 篡改 Width=Length=65536 的恶意蓝图不得崩溃，且大炮安全重置为空闲状态
        CompoundTag badTag = new CompoundTag();
        badTag.putInt("Width", 65536);
        badTag.putInt("Height", 1);
        badTag.putInt("Length", 65536);
        ListTag paletteTag = new ListTag();
        paletteTag.add(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, Blocks.STONE.defaultBlockState()).getOrThrow());
        badTag.put("Palette", paletteTag);
        badTag.putIntArray("Blocks", new int[]{0});
        ItemStack badBlueprint = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
        badBlueprint.set(DataComponents.CUSTOM_DATA, CustomData.of(badTag));
        cannon.setItem(0, badBlueprint);
        cannon.handleAction("START", false, false);
        BlueprintCannonBlockEntity.serverTick(helper.getLevel(), absCannonPos, cannon.getBlockState(), cannon);
        if (cannon.dataAccess.get(0) != 0) {
            helper.fail(Component.literal("Malformed dimension blueprint should reset cannon to IDLE"));
            return;
        }

        // 4. LaunchedItem 直接落地在越界坐标上必须安全跳过
        me.tuanzi.block.cannon.LaunchedItem.ForBlockState outBoundsDrop =
                new me.tuanzi.block.cannon.LaunchedItem.ForBlockState(absCannonPos, new BlockPos(absCannonPos.getX(), 500, absCannonPos.getZ()), new ItemStack(Items.STONE), Blocks.STONE.defaultBlockState(), null, false);
        outBoundsDrop.place(helper.getLevel());

        helper.succeed();
    }

    @GameTest(maxTicks = 400)
    public void testCreativeCoreCustomFiringSpeed(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);

        // 仅放入配置为「每 2 ticks 发射 3 次」的创造核心（不放石头与浆料，由创造核心和附近/自身浆料提供）
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        ItemStack creativeCore = new ItemStack(ModItems.CREATIVE_CORE);
        me.tuanzi.item.CreativeCoreItem.setCannonSpeed(creativeCore, 2, 3);
        chest.setItem(0, creativeCore);

        Map<BlockPos, BlockState> bp = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            bp.put(new BlockPos(i % 3, i / 3, 0), Blocks.STONE.defaultBlockState());
        }

        BlockPos absCannonPos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlueprintCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        cannon.setItem(0, createBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 1)), 3, 2, 1, bp));
        cannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 4));
        cannon.handleAction("START", false, false);

        // 第 1 tick：一次性发射 3 个方块，然后重置 ticksToNextPlace = 2
        BlueprintCannonBlockEntity.serverTick(helper.getLevel(), absCannonPos, cannon.getBlockState(), cannon);
        if (cannon.flyingBlocks.size() != 3) {
            helper.fail(Component.literal("Expected 3 flying blocks after first firing tick, got " + cannon.flyingBlocks.size()));
            return;
        }

        // 第 2 tick：ticksToNextPlace 从 2 减到 1，不应发射新方块
        BlueprintCannonBlockEntity.serverTick(helper.getLevel(), absCannonPos, cannon.getBlockState(), cannon);
        if (cannon.flyingBlocks.size() != 3) {
            helper.fail(Component.literal("Expected still 3 flying blocks during 1-tick cooldown, got " + cannon.flyingBlocks.size()));
            return;
        }

        // 第 3 tick：ticksToNextPlace 从 1 减到 0，一次性再发射剩余 3 个方块（共 6 个在途）
        BlueprintCannonBlockEntity.serverTick(helper.getLevel(), absCannonPos, cannon.getBlockState(), cannon);
        if (cannon.flyingBlocks.size() != 6) {
            helper.fail(Component.literal("Expected 6 flying blocks after second firing tick, got " + cannon.flyingBlocks.size()));
            return;
        }

        helper.succeedWhen(() -> {
            if (cannon.dataAccess.get(0) != 0) {
                helper.fail(Component.literal("Cannon still running, progress="
                        + me.tuanzi.world.inventory.BlueprintCannonMenu.reconstructInt(cannon.dataAccess.get(1), cannon.dataAccess.get(2))
                        + "/"
                        + me.tuanzi.world.inventory.BlueprintCannonMenu.reconstructInt(cannon.dataAccess.get(3), cannon.dataAccess.get(4))));
                return;
            }
            for (int i = 0; i < 6; i++) {
                helper.assertBlockPresent(Blocks.STONE, 3 + (i % 3), 1 + (i / 3), 1);
            }
            if (chest.getItem(0).isEmpty() || !chest.getItem(0).is(ModItems.CREATIVE_CORE)) {
                helper.fail(Component.literal("Creative core should remain intact in chest"));
                return;
            }
            ItemStack remainingSlurry = cannon.getItem(1);
            if (remainingSlurry.isEmpty() || !remainingSlurry.is(ModItems.COMPRESSED_BUILD_SLURRY) || remainingSlurry.getCount() != 4) {
                helper.fail(Component.literal("Creative core should NOT consume slurry, expected 4, got "
                        + (remainingSlurry.isEmpty() ? 0 : remainingSlurry.getCount())));
                return;
            }
            if (cannon.dataAccess.get(7) != 0) {
                helper.fail(Component.literal("Creative core should NOT consume slurry durability, expected fuel=0, got "
                        + cannon.dataAccess.get(7)));
            }
        });
    }

    private static ItemStack createColoredBlueprintItem(
            BlockPos ghostAbsPos,
            int width,
            int height,
            int length,
            Map<BlockPos, BlockState> localBlocks,
            Map<BlockPos, Integer> localColors
    ) {
        ItemStack stack = createBlueprintItem(ghostAbsPos, width, height, length, localBlocks);
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        ListTag bes = new ListTag();
        for (Map.Entry<BlockPos, Integer> e : localColors.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putIntArray("pos", new int[]{e.getKey().getX(), e.getKey().getY(), e.getKey().getZ()});
            CompoundTag nbt = new CompoundTag();
            nbt.putString("id", "tuanzis_mod:color_block_be");
            nbt.putInt("Color", e.getValue() & 0xFFFFFF);
            entry.put("nbt", nbt);
            bes.add(entry);
        }
        tag.put("BlockEntities", bes);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @GameTest(maxTicks = 400)
    public void testColorBlockExactAndPaintBucketPrinting(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);

        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        // 放入 1 个精确红色彩色方块 + 2 个白色彩色方块 + 1 个仅剩 2 点耐久的油漆桶
        chest.setItem(0, me.tuanzi.block.cannon.BlueprintColorMaterialHelper.createColoredDisplayStack(ModBlocks.COLOR_BLOCK.asItem(), 0xFF0000));
        ItemStack whites = me.tuanzi.block.cannon.BlueprintColorMaterialHelper.createColoredDisplayStack(ModBlocks.COLOR_BLOCK.asItem(), 0xFFFFFF);
        whites.setCount(2);
        chest.setItem(1, whites);
        ItemStack bucket = new ItemStack(ModItems.PAINT_BUCKET);
        bucket.setDamageValue(bucket.getMaxDamage() - 2);
        chest.setItem(2, bucket);

        Map<BlockPos, BlockState> bp = new HashMap<>();
        bp.put(new BlockPos(0, 0, 0), ModBlocks.COLOR_BLOCK.defaultBlockState());
        bp.put(new BlockPos(1, 0, 0), ModBlocks.COLOR_BLOCK.defaultBlockState());
        bp.put(new BlockPos(2, 0, 0), ModBlocks.COLOR_BLOCK.defaultBlockState());
        Map<BlockPos, Integer> colors = Map.of(
                new BlockPos(0, 0, 0), 0xFF0000,
                new BlockPos(1, 0, 0), 0x00FF00,
                new BlockPos(2, 0, 0), 0x0000FF
        );

        BlueprintCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        cannon.setItem(0, createColoredBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 1)), 3, 1, 1, bp, colors));
        cannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 4));
        cannon.handleAction("START", false, false);

        helper.succeedWhen(() -> {
            if (cannon.dataAccess.get(0) != 0) {
                helper.fail(Component.literal("Cannon still running, state=" + cannon.dataAccess.get(0)));
                return;
            }
            helper.assertBlockPresent(ModBlocks.COLOR_BLOCK, 3, 1, 1);
            helper.assertBlockPresent(ModBlocks.COLOR_BLOCK, 4, 1, 1);
            helper.assertBlockPresent(ModBlocks.COLOR_BLOCK, 5, 1, 1);

            var be0 = helper.getBlockEntity(new BlockPos(3, 1, 1), me.tuanzi.block.entity.ColorBlockBlockEntity.class);
            var be1 = helper.getBlockEntity(new BlockPos(4, 1, 1), me.tuanzi.block.entity.ColorBlockBlockEntity.class);
            var be2 = helper.getBlockEntity(new BlockPos(5, 1, 1), me.tuanzi.block.entity.ColorBlockBlockEntity.class);
            if (be0.getColor() != 0xFF0000 || be1.getColor() != 0x00FF00 || be2.getColor() != 0x0000FF) {
                helper.fail(Component.literal("Color mismatch: "
                        + String.format("#%06X, #%06X, #%06X", be0.getColor(), be1.getColor(), be2.getColor())));
                return;
            }
            if (!chest.getItem(0).isEmpty() || !chest.getItem(1).isEmpty()) {
                helper.fail(Component.literal("Expected all 3 color blocks consumed"));
                return;
            }
            if (!chest.getItem(2).is(Items.BUCKET)) {
                helper.fail(Component.literal("Expected paint bucket depleted into empty BUCKET, got " + chest.getItem(2)));
            }
        });
    }

    @GameTest(maxTicks = 60)
    public void testLegacyUncompressedBlueprintAutoCompactedOnEntityAndCodec(GameTestHelper helper) {
        int width = 32;
        int height = 16;
        int length = 32;
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("Width", width);
        legacyTag.putInt("Height", height);
        legacyTag.putInt("Length", length);
        ListTag palette = new ListTag();
        palette.add(net.minecraft.nbt.StringTag.valueOf("minecraft:air"));
        palette.add(net.minecraft.nbt.StringTag.valueOf("tuanzis_mod:color_block"));
        legacyTag.put("Palette", palette);
        legacyTag.putIntArray("Blocks", new int[width * height * length]);

        ListTag blockEntities = new ListTag();
        for (int i = 0; i < 15_000; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putIntArray("pos", new int[]{i & 31, (i >> 5) & 15, (i >> 9) & 31});
            CompoundTag inner = new CompoundTag();
            inner.putString("id", "tuanzis_mod:color_block");
            inner.putInt("Color", 0xFF000000 | (i & 0xFFFFFF));
            entry.put("nbt", inner);
            blockEntities.add(entry);
        }
        legacyTag.put("BlockEntities", blockEntities);

        // 1. 确认未压缩数据确实超出原版 2MB NbtAccounter 限制
        net.minecraft.network.FriendlyByteBuf rawCheckBuf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            net.minecraft.network.FriendlyByteBuf.writeNbt(rawCheckBuf, legacyTag);
            boolean threwQuota = false;
            try {
                net.minecraft.network.FriendlyByteBuf.readNbt(rawCheckBuf, net.minecraft.nbt.NbtAccounter.defaultQuota());
            } catch (net.minecraft.nbt.NbtAccounterException expected) {
                threwQuota = true;
            }
            if (!threwQuota) {
                helper.fail(Component.literal("Expected raw 15,000 ColorBlock tag to exceed 2MB NbtAccounter quota"));
                return;
            }
        } finally {
            rawCheckBuf.release();
        }

        ItemStack legacyStack = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
        legacyStack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

        // 2. 通过 ItemStack.CODEC 编解码（模拟旧存档反序列化）应自动压缩
        var encodedNbt = ItemStack.CODEC.encodeStart(
                helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                legacyStack.copy()
        ).getOrThrow();
        ItemStack decodedFromSave = ItemStack.CODEC.parse(
                helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                encodedNbt
        ).getOrThrow();
        CompoundTag saveCompactTag = decodedFromSave.get(DataComponents.CUSTOM_DATA).copyTag();
        if (!saveCompactTag.contains(me.tuanzi.util.LitematicaParser.KEY_COMPRESSED_PAYLOAD)
                || saveCompactTag.contains("BlockEntities")) {
            helper.fail(Component.literal("Expected ItemStack.CODEC deserialization to auto-compact legacy blueprint"));
            return;
        }

        // 3. ItemEntity#setItem 应自动压缩，且 ClientboundSetEntityDataPacket 可正常编解码
        BlockPos dropPos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemEntity itemEntity = new ItemEntity(helper.getLevel(), dropPos.getX() + 0.5, dropPos.getY(), dropPos.getZ() + 0.5, legacyStack.copy());
        CompoundTag itemEntityTag = itemEntity.getItem().get(DataComponents.CUSTOM_DATA).copyTag();
        if (!itemEntityTag.contains(me.tuanzi.util.LitematicaParser.KEY_COMPRESSED_PAYLOAD)
                || itemEntityTag.contains("BlockEntities")) {
            helper.fail(Component.literal("Expected ItemEntity#setItem to auto-compact blueprint"));
            return;
        }

        var nonDefaultData = itemEntity.getEntityData().getNonDefaultValues();
        if (nonDefaultData == null || nonDefaultData.isEmpty()) {
            helper.fail(Component.literal("Expected non-empty SynchedEntityData on ItemEntity"));
            return;
        }
        net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket packet =
                new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(itemEntity.getId(), nonDefaultData);
        net.minecraft.network.RegistryFriendlyByteBuf wireBuf =
                new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(wireBuf, packet);
            net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket decodedPacket =
                    net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(wireBuf);
            if (decodedPacket.packedItems().isEmpty()) {
                helper.fail(Component.literal("Expected decoded set_entity_data packet items"));
                return;
            }
        } finally {
            wireBuf.release();
        }

        // 4. ItemFrame#setItem 应自动压缩
        net.minecraft.world.entity.decoration.ItemFrame frame =
                new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 1)), Direction.NORTH);
        frame.setItem(legacyStack.copy(), false);
        CompoundTag frameTag = frame.getItem().get(DataComponents.CUSTOM_DATA).copyTag();
        if (!frameTag.contains(me.tuanzi.util.LitematicaParser.KEY_COMPRESSED_PAYLOAD)
                || frameTag.contains("BlockEntities")) {
            helper.fail(Component.literal("Expected ItemFrame#setItem to auto-compact blueprint"));
            return;
        }

        helper.succeed();
    }

    @GameTest(maxTicks = 120)
    public void testColorBlockMissingWithoutPaintBucketPauses(GameTestHelper helper) {
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);

        // 仅放入 1 个白色彩色方块、不放油漆桶，而蓝图需要红色 #FF0000
        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        chest.setItem(0, me.tuanzi.block.cannon.BlueprintColorMaterialHelper.createColoredDisplayStack(ModBlocks.COLOR_BLOCK.asItem(), 0xFFFFFF));

        Map<BlockPos, BlockState> bp = Map.of(new BlockPos(0, 0, 0), ModBlocks.COLOR_BLOCK.defaultBlockState());
        Map<BlockPos, Integer> colors = Map.of(new BlockPos(0, 0, 0), 0xFF0000);

        BlueprintCannonBlockEntity cannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        cannon.setItem(0, createColoredBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 1)), 1, 1, 1, bp, colors));
        cannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 2));
        cannon.handleAction("START", false, false);

        helper.succeedWhen(() -> {
            if (cannon.dataAccess.get(0) != 2 || cannon.dataAccess.get(13) != 2) {
                helper.fail(Component.literal("Expected cannon paused for missing material (state=2, reason=2), got state="
                        + cannon.dataAccess.get(0) + ", reason=" + cannon.dataAccess.get(13)));
                return;
            }
            helper.assertBlockPresent(Blocks.AIR, 3, 1, 1);
            if (chest.getItem(0).getCount() != 1) {
                helper.fail(Component.literal("White color block should NOT be consumed without paint bucket"));
            }
        });
    }

    @GameTest(maxTicks = 240)
    public void testCreativeCoreNoSlurryConsumption(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        // 1. 第一台大炮：无任何压缩建造浆料，仅有创造核心，开启拆除模式清理泥土并放置 4 个石头
        helper.setBlock(0, 1, 1, ModBlocks.BLUEPRINT_CANNON);
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        helper.setBlock(3, 1, 1, Blocks.DIRT);

        ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(ModItems.CREATIVE_CORE));

        Map<BlockPos, BlockState> bp1 = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            bp1.put(new BlockPos(i % 2, i / 2, 0), Blocks.STONE.defaultBlockState());
        }

        BlueprintCannonBlockEntity emptySlurryCannon = helper.getBlockEntity(new BlockPos(0, 1, 1), BlueprintCannonBlockEntity.class);
        emptySlurryCannon.setItem(0, createBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 1)), 2, 2, 1, bp1));
        emptySlurryCannon.handleAction("START", true, false);

        // 2. 第二台大炮：Slot 1 放入 4 个压缩建造浆料，验证有创造核心时完全不消耗已有浆料
        helper.setBlock(0, 1, 3, ModBlocks.BLUEPRINT_CANNON);
        Map<BlockPos, BlockState> bp2 = new HashMap<>();
        bp2.put(new BlockPos(0, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
        bp2.put(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());

        BlueprintCannonBlockEntity filledSlurryCannon = helper.getBlockEntity(new BlockPos(0, 1, 3), BlueprintCannonBlockEntity.class);
        filledSlurryCannon.setItem(0, createBlueprintItem(helper.absolutePos(new BlockPos(3, 1, 3)), 2, 1, 1, bp2));
        filledSlurryCannon.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY, 4));
        filledSlurryCannon.handleAction("START", false, false);

        helper.succeedWhen(() -> {
            if (emptySlurryCannon.dataAccess.get(0) != 0) {
                helper.fail(Component.literal("Empty-slurry creative-core cannon still running/paused, state="
                        + emptySlurryCannon.dataAccess.get(0) + ", reason=" + emptySlurryCannon.dataAccess.get(13)));
                return;
            }
            if (filledSlurryCannon.dataAccess.get(0) != 0) {
                helper.fail(Component.literal("Filled-slurry creative-core cannon still running/paused, state="
                        + filledSlurryCannon.dataAccess.get(0) + ", reason=" + filledSlurryCannon.dataAccess.get(13)));
                return;
            }

            for (int i = 0; i < 4; i++) {
                helper.assertBlockPresent(Blocks.STONE, 3 + (i % 2), 1 + (i / 2), 1);
            }
            helper.assertBlockPresent(Blocks.OAK_PLANKS, 3, 1, 3);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, 4, 1, 3);

            ItemStack slurryLeft = filledSlurryCannon.getItem(1);
            if (slurryLeft.isEmpty() || !slurryLeft.is(ModItems.COMPRESSED_BUILD_SLURRY) || slurryLeft.getCount() != 4) {
                helper.fail(Component.literal("Expected all 4 slurry items preserved under Creative Core, got "
                        + (slurryLeft.isEmpty() ? 0 : slurryLeft.getCount())));
                return;
            }
            if (filledSlurryCannon.dataAccess.get(7) != 0) {
                helper.fail(Component.literal("Expected fuel durability untouched (0) under Creative Core, got "
                        + filledSlurryCannon.dataAccess.get(7)));
            }
        });
    }

    @GameTest
    public void testBlueprintTableAndCannonMiningTagsSpeedAndDrops(GameTestHelper helper) {
        BlockState tableState = ModBlocks.BLUEPRINT_TABLE.defaultBlockState();
        BlockState cannonState = ModBlocks.BLUEPRINT_CANNON.defaultBlockState();

        // 1. 标签校验：蓝图桌用斧、无需铁工具；蓝图大炮用镐
        if (!tableState.is(BlockTags.MINEABLE_WITH_AXE)
                || tableState.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || tableState.is(BlockTags.NEEDS_IRON_TOOL)) {
            helper.fail(Component.literal("BlueprintTable tags mismatch: axe=" + tableState.is(BlockTags.MINEABLE_WITH_AXE)
                    + ", pickaxe=" + tableState.is(BlockTags.MINEABLE_WITH_PICKAXE)
                    + ", needsIron=" + tableState.is(BlockTags.NEEDS_IRON_TOOL)));
            return;
        }
        if (!cannonState.is(BlockTags.MINEABLE_WITH_PICKAXE) || cannonState.is(BlockTags.MINEABLE_WITH_AXE)) {
            helper.fail(Component.literal("BlueprintCannon tags mismatch: pickaxe=" + cannonState.is(BlockTags.MINEABLE_WITH_PICKAXE)
                    + ", axe=" + cannonState.is(BlockTags.MINEABLE_WITH_AXE)));
            return;
        }

        // 2. 挖掘进度速度对比（斧 vs 镐 vs 空手）
        Player speedPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack ironAxe = new ItemStack(Items.IRON_AXE);
        ItemStack ironPickaxe = new ItemStack(Items.IRON_PICKAXE);

        BlockPos tablePos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos cannonPos = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.setBlock(1, 1, 1, ModBlocks.BLUEPRINT_TABLE);
        helper.setBlock(3, 1, 1, ModBlocks.BLUEPRINT_CANNON);

        speedPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        float tableHandProgress = tableState.getDestroyProgress(speedPlayer, helper.getLevel(), tablePos);
        float cannonHandProgress = cannonState.getDestroyProgress(speedPlayer, helper.getLevel(), cannonPos);

        speedPlayer.setItemInHand(InteractionHand.MAIN_HAND, ironAxe);
        float tableAxeProgress = tableState.getDestroyProgress(speedPlayer, helper.getLevel(), tablePos);
        float cannonAxeProgress = cannonState.getDestroyProgress(speedPlayer, helper.getLevel(), cannonPos);

        speedPlayer.setItemInHand(InteractionHand.MAIN_HAND, ironPickaxe);
        float tablePickProgress = tableState.getDestroyProgress(speedPlayer, helper.getLevel(), tablePos);
        float cannonPickProgress = cannonState.getDestroyProgress(speedPlayer, helper.getLevel(), cannonPos);

        if (!(tableAxeProgress > tablePickProgress && tableAxeProgress > tableHandProgress)) {
            helper.fail(Component.literal("Expected iron axe faster than pickaxe/hand on BlueprintTable: axe="
                    + tableAxeProgress + ", pick=" + tablePickProgress + ", hand=" + tableHandProgress));
            return;
        }
        if (!(cannonPickProgress > cannonAxeProgress && cannonPickProgress > cannonHandProgress)) {
            helper.fail(Component.literal("Expected iron pickaxe faster than axe/hand on BlueprintCannon: pick="
                    + cannonPickProgress + ", axe=" + cannonAxeProgress + ", hand=" + cannonHandProgress));
            return;
        }

        // 3. 放入容器物品并通过生存模式方块破坏链路验证自身与内容物掉落
        BlueprintTableBlockEntity tableBe = helper.getBlockEntity(new BlockPos(1, 1, 1), BlueprintTableBlockEntity.class);
        tableBe.setItem(1, new ItemStack(ModItems.BLANK_BLUEPRINT));
        BlueprintCannonBlockEntity cannonBe = helper.getBlockEntity(new BlockPos(3, 1, 1), BlueprintCannonBlockEntity.class);
        cannonBe.setItem(1, new ItemStack(ModItems.COMPRESSED_BUILD_SLURRY));

        // 触发真实方块移除 + 战利品掉落（含 Container 内容物 preRemoveSideEffects 与 Block.getDrops）
        net.minecraft.world.level.block.Block.dropResources(tableState, helper.getLevel(), tablePos, tableBe, speedPlayer, ironAxe);
        helper.getLevel().removeBlock(tablePos, false);

        net.minecraft.world.level.block.Block.dropResources(cannonState, helper.getLevel(), cannonPos, cannonBe, speedPlayer, ironPickaxe);
        helper.getLevel().removeBlock(cannonPos, false);

        helper.assertBlockPresent(Blocks.AIR, 1, 1, 1);
        helper.assertBlockPresent(Blocks.AIR, 3, 1, 1);

        List<ItemEntity> droppedAtTable = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, new AABB(tablePos).inflate(1.5));
        boolean gotTable = droppedAtTable.stream().anyMatch(e -> e.getItem().is(ModBlocks.BLUEPRINT_TABLE.asItem()));
        boolean gotBlankBp = droppedAtTable.stream().anyMatch(e -> e.getItem().is(ModItems.BLANK_BLUEPRINT));
        if (!gotTable || !gotBlankBp) {
            helper.fail(Component.literal("Expected BlueprintTable and BlankBlueprint drops, gotTable="
                    + gotTable + ", gotBlankBp=" + gotBlankBp + ", count=" + droppedAtTable.size()));
            return;
        }

        List<ItemEntity> droppedAtCannon = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, new AABB(cannonPos).inflate(1.5));
        boolean gotCannon = droppedAtCannon.stream().anyMatch(e -> e.getItem().is(ModBlocks.BLUEPRINT_CANNON.asItem()));
        boolean gotSlurry = droppedAtCannon.stream().anyMatch(e -> e.getItem().is(ModItems.COMPRESSED_BUILD_SLURRY));
        if (!gotCannon || !gotSlurry) {
            helper.fail(Component.literal("Expected BlueprintCannon and Slurry drops, gotCannon="
                    + gotCannon + ", gotSlurry=" + gotSlurry + ", count=" + droppedAtCannon.size()));
            return;
        }

        helper.succeed();
    }
}
