package me.tuanzi.gametest;

import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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
                helper.fail(Component.literal("Cannon still running, progress=" + cannon.dataAccess.get(1) + "/" + cannon.dataAccess.get(2)));
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
                helper.fail(Component.literal("Cannon still running, progress=" + cannon.dataAccess.get(1) + "/" + cannon.dataAccess.get(2)));
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
}
