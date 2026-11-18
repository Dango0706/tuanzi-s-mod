package me.tuanzi.gametest;

import me.tuanzi.block.cannon.BlueprintDirectPlacer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlueprintDirectPlaceGameTest {

    private static CompoundTag buildBlueprintTag(
            int width,
            int height,
            int length,
            Map<BlockPos, BlockState> localBlocks,
            ListTag blockEntities,
            ListTag entities
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
        tag.put("BlockEntities", blockEntities);
        tag.put("Entities", entities);
        return tag;
    }

    @GameTest
    public void testDirectPlaceFullOverwriteVoidTileEntityAndEntities(GameTestHelper helper) {
        // 清理并搭建测试平台 (x: 0..4, z: 0..4)
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(x, 0, z, Blocks.BEDROCK);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        // 在蓝图放置目标区预置障碍：
        // 1) (1, 1, 1) 放置一个装满物品的旧箱子，蓝图此处为 AIR -> 应被清空且不掉落物品
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        ChestBlockEntity oldChest = helper.getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        oldChest.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        // 2) (3, 1, 1) 放置一个绿宝石块，蓝图此处为 STRUCTURE_VOID -> 应保留原绿宝石块
        helper.setBlock(3, 1, 1, Blocks.EMERALD_BLOCK);
        // 3) (2, 1, 1) 放置泥土，蓝图此处为新箱子 -> 应被覆盖且箱子内含蓝图指定钻石
        helper.setBlock(2, 1, 1, Blocks.DIRT);

        // 构造 3x2x2 蓝图：
        // x=0,y=0,z=0 -> AIR (覆盖 (1,1,1) 的旧箱子)
        // x=1,y=0,z=0 -> CHEST (覆盖 (2,1,1) 的泥土，含 5 颗钻石)
        // x=2,y=0,z=0 -> STRUCTURE_VOID (保留 (3,1,1) 的绿宝石块)
        // x=0,y=0,z=1 -> STONE, x=0,y=1,z=1 -> WALL_TORCH (SOUTH),
        // x=1,y=0,z=1 & x=1,y=1,z=1 -> OAK_DOOR (LOWER / UPPER)
        Map<BlockPos, BlockState> localBlocks = new HashMap<>();
        localBlocks.put(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState());
        localBlocks.put(new BlockPos(1, 0, 0), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        localBlocks.put(new BlockPos(2, 0, 0), Blocks.STRUCTURE_VOID.defaultBlockState());
        localBlocks.put(new BlockPos(0, 0, 1), Blocks.STONE.defaultBlockState());
        localBlocks.put(new BlockPos(0, 1, 1), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
        localBlocks.put(new BlockPos(1, 0, 1), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        localBlocks.put(new BlockPos(1, 1, 1), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // 蓝图箱子 NBT
        ListTag blockEntities = new ListTag();
        CompoundTag chestRecord = new CompoundTag();
        chestRecord.putIntArray("pos", new int[]{1, 0, 0});
        CompoundTag chestNbt = new CompoundTag();
        chestNbt.putString("id", "minecraft:chest");
        ListTag itemsList = new ListTag();
        CompoundTag diamondSlot = new CompoundTag();
        diamondSlot.putByte("Slot", (byte) 0);
        diamondSlot.putString("id", "minecraft:diamond");
        diamondSlot.putInt("count", 5);
        itemsList.add(diamondSlot);
        chestNbt.put("Items", itemsList);
        chestRecord.put("nbt", chestNbt);
        blockEntities.add(chestRecord);

        // 蓝图实体：盔甲架位于局部 (2.5, 1.0, 1.5)
        ListTag entities = new ListTag();
        CompoundTag armorStand = new CompoundTag();
        armorStand.putString("id", "minecraft:armor_stand");
        ListTag posList = new ListTag();
        posList.add(DoubleTag.valueOf(2.5));
        posList.add(DoubleTag.valueOf(1.0));
        posList.add(DoubleTag.valueOf(1.5));
        armorStand.put("Pos", posList);
        ListTag rotList = new ListTag();
        rotList.add(FloatTag.valueOf(90.0f));
        rotList.add(FloatTag.valueOf(0.0f));
        armorStand.put("Rotation", rotList);
        entities.add(armorStand);

        CompoundTag blueprintTag = buildBlueprintTag(3, 2, 2, localBlocks, blockEntities, entities);
        BlockPos basePos = helper.absolutePos(new BlockPos(1, 1, 1));

        BlueprintDirectPlacer.PlaceResult result = BlueprintDirectPlacer.placeBlueprint(
                helper.getLevel(), blueprintTag, basePos, BlockPos.ZERO, 0, false
        );

        if (result.clearedAirBlocks() < 1 || result.spawnedEntities() != 1) {
            helper.fail(Component.literal("Unexpected direct place result: " + result));
            return;
        }

        // 1. 原旧箱子处已被蓝图 AIR 清空
        helper.assertBlockPresent(Blocks.AIR, 1, 1, 1);
        // 2. STRUCTURE_VOID 处保留原绿宝石块
        helper.assertBlockPresent(Blocks.EMERALD_BLOCK, 3, 1, 1);
        // 3. 新箱子已放置且包含 5 颗钻石
        helper.assertBlockPresent(Blocks.CHEST, 2, 1, 1);
        ChestBlockEntity placedChest = helper.getBlockEntity(new BlockPos(2, 1, 1), ChestBlockEntity.class);
        ItemStack chestSlot0 = placedChest.getItem(0);
        if (!chestSlot0.is(Items.DIAMOND) || chestSlot0.getCount() != 5) {
            helper.fail(Component.literal("Expected 5 diamonds in placed chest, got: " + chestSlot0));
            return;
        }
        // 4. 石头、墙火把、完整木门完好
        helper.assertBlockPresent(Blocks.STONE, 1, 1, 2);
        helper.assertBlockPresent(Blocks.WALL_TORCH, 1, 2, 2);
        helper.assertBlockPresent(Blocks.OAK_DOOR, 2, 1, 2);
        helper.assertBlockPresent(Blocks.OAK_DOOR, 2, 2, 2);

        // 5. 无任何掉落物（旧箱子物品、火把、门均未掉落）
        BlockPos minAbs = helper.absolutePos(new BlockPos(0, 0, 0));
        BlockPos maxAbs = helper.absolutePos(new BlockPos(4, 4, 4));
        AABB box = new AABB(minAbs.getX(), minAbs.getY(), minAbs.getZ(), maxAbs.getX() + 1, maxAbs.getY() + 1, maxAbs.getZ() + 1);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box);
        if (!drops.isEmpty()) {
            helper.fail(Component.literal("Expected zero dropped items, found: " + drops.getFirst().getItem()));
            return;
        }

        // 6. 盔甲架正确生成且再次放置不会重复生成
        List<ArmorStand> stands = helper.getLevel().getEntitiesOfClass(ArmorStand.class, box);
        if (stands.size() != 1) {
            helper.fail(Component.literal("Expected 1 ArmorStand, found: " + stands.size()));
            return;
        }
        BlueprintDirectPlacer.placeBlueprint(helper.getLevel(), blueprintTag, basePos, BlockPos.ZERO, 0, false);
        List<ArmorStand> standsAfterSecondPlace = helper.getLevel().getEntitiesOfClass(ArmorStand.class, box);
        if (standsAfterSecondPlace.size() != 1) {
            helper.fail(Component.literal("Duplicate ArmorStand spawned on second direct place: " + standsAfterSecondPlace.size()));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testDirectPlaceRotationMirrorAndPermissionGate(GameTestHelper helper) {
        // 权限门控验证：生存模式玩家不能通过 canUseGameMasterBlocks()
        Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        if (survivalPlayer.canUseGameMasterBlocks()) {
            helper.fail(Component.literal("Survival player must not pass canUseGameMasterBlocks()"));
            return;
        }

        // 无 OP 权限的创造模式 MockPlayer 也不能通过 canUseGameMasterBlocks()
        Player nonOpCreative = helper.makeMockPlayer(GameType.CREATIVE);
        if (nonOpCreative.canUseGameMasterBlocks()) {
            helper.fail(Component.literal("Non-admin creative player must not pass canUseGameMasterBlocks()"));
            return;
        }

        // 旋转 90° + 偏移验证
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 1, z, Blocks.AIR);
            }
        }
        Map<BlockPos, BlockState> localBlocks = new HashMap<>();
        localBlocks.put(new BlockPos(0, 0, 0), Blocks.GOLD_BLOCK.defaultBlockState());
        localBlocks.put(new BlockPos(1, 0, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());

        CompoundTag blueprintTag = buildBlueprintTag(2, 1, 1, localBlocks, new ListTag(), new ListTag());
        BlockPos basePos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos offset = new BlockPos(1, 0, 1);

        // 2x1x1 顺时针旋转 90°: (0,0,0) -> (0,0,0), (1,0,0) -> (0,0,1)
        // 加上 base(1,1,1) + offset(1,0,1) => (2,1,2) 金块, (2,1,3) 钻石块
        BlueprintDirectPlacer.placeBlueprint(helper.getLevel(), blueprintTag, basePos, offset, 1, false);
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, 2, 1, 2);
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, 2, 1, 3);

        helper.succeed();
    }
}
