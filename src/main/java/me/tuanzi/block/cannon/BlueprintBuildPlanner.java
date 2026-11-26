package me.tuanzi.block.cannon;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import me.tuanzi.block.entity.ColorBlockBlockEntity;
import me.tuanzi.block.entity.ColorSlabBlockEntity;
import me.tuanzi.util.LitematicaParser;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 蓝图大炮建造规划器：
 * 1. 通用多方块耦合识别（基于 Minecraft 原生 BlockState#updateShape 双向依赖判定，无需硬编码特定方块）；
 * 2. 破坏优先阶段（自顶向下扫描并清空不匹配/多余方块）；
 * 3. 依附方块前置校验（基于 BlockState#canSurvive 与下落方块支撑检测，确保前置依附方块先落地）；
 * 4. 依附方块处理完后再整体放置多方块，最后放置依赖多方块的残余方块与实体。
 */
public class BlueprintBuildPlanner {

    public record BlueprintCell(BlockPos pos, BlockState state, @Nullable CompoundTag teData, int originalIndex) {
    }

    public record MultiBlockGroup(List<BlueprintCell> parts, Item requiredItem) {
    }

    public record MaterialStat(int itemCount, int voxelCount) {
    }

    /**
     * 蓝图材料唯一键：普通方块按 Block 聚合（color == null），
     * 彩色方块/楼梯/半砖按 (Block, 24-bit RGB) 独立聚合展示与消耗。
     */
    public record MaterialKey(Block block, @Nullable Integer color) {
        public MaterialKey {
            if (color != null) {
                color = color & 0xFFFFFF;
            }
        }

        public boolean isColored() {
            return color != null;
        }

        public String formatHex() {
            return color != null ? String.format("#%06X", color & 0xFFFFFF) : "";
        }
    }

    public static boolean isColorBlockSeries(Block block) {
        return block instanceof ColorBlock || block instanceof ColorStairs || block instanceof ColorSlab;
    }

    /**
     * 提取单个蓝图格所需的颜色列表（仅彩色方块/楼梯/半砖返回非空数组，普通方块返回空数组）。
     * 双层彩色半砖返回 2 个颜色 [bottomColor, topColor]；单半砖、彩色方块与彩色楼梯返回 1 个颜色。
     */
    public static int[] getRequiredColors(BlockState state, @Nullable CompoundTag teData) {
        Block block = state.getBlock();
        if (block instanceof ColorBlock || block instanceof ColorStairs) {
            int col = teData != null ? (teData.getIntOr("Color", 0xFFFFFF) & 0xFFFFFF) : 0xFFFFFF;
            return new int[]{col};
        }
        if (block instanceof ColorSlab) {
            SlabType type = state.hasProperty(BlockStateProperties.SLAB_TYPE)
                    ? state.getValue(BlockStateProperties.SLAB_TYPE)
                    : SlabType.BOTTOM;
            int bottom = teData != null ? (teData.getIntOr("BottomColor", 0xFFFFFF) & 0xFFFFFF) : 0xFFFFFF;
            int top = teData != null ? (teData.getIntOr("TopColor", 0xFFFFFF) & 0xFFFFFF) : 0xFFFFFF;
            if (type == SlabType.DOUBLE) {
                return new int[]{bottom, top};
            }
            if (type == SlabType.TOP) {
                return new int[]{top};
            }
            return new int[]{bottom};
        }
        return new int[0];
    }

    /**
     * 判断世界指定位置的方块及颜色（若为彩色方块系列）是否已与蓝图格完全匹配。
     */
    public static boolean isCellMatch(Level level, BlockPos pos, BlockState worldState, BlueprintCell cell, boolean exactMatch) {
        if (!isStateMatch(worldState, cell.state(), exactMatch)) {
            return false;
        }
        Block block = cell.state().getBlock();
        if (!isColorBlockSeries(block)) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (block instanceof ColorBlock || block instanceof ColorStairs) {
            int expected = getRequiredColors(cell.state(), cell.teData())[0];
            int actual = (be instanceof ColorBlockBlockEntity colorBe) ? (colorBe.getColor() & 0xFFFFFF) : 0xFFFFFF;
            return actual == expected;
        }
        if (block instanceof ColorSlab) {
            if (!(be instanceof ColorSlabBlockEntity slabBe)) {
                return false;
            }
            SlabType type = cell.state().hasProperty(BlockStateProperties.SLAB_TYPE)
                    ? cell.state().getValue(BlockStateProperties.SLAB_TYPE)
                    : SlabType.BOTTOM;
            int[] req = getRequiredColors(cell.state(), cell.teData());
            if (type == SlabType.DOUBLE) {
                return (slabBe.getBottomColor() & 0xFFFFFF) == req[0]
                        && (slabBe.getTopColor() & 0xFFFFFF) == req[1];
            }
            if (type == SlabType.TOP) {
                return (slabBe.getTopColor() & 0xFFFFFF) == req[0];
            }
            return (slabBe.getBottomColor() & 0xFFFFFF) == req[0];
        }
        return true;
    }

    public record ReplaceResult(
            boolean success,
            int replacedVoxels,
            int clearedFlowingVoxels,
            int removedBlockEntities,
            Component message
    ) {
    }

    private static final Set<String> IGNORED_NON_PART_PROPERTIES = Set.of(
            "facing", "axis", "waterlogged", "lit", "open", "powered",
            "joins", "back", "north", "south", "east", "west", "up", "down",
            "shape", "type", "age", "level", "power", "distance", "stage",
            "rotation", "frame", "layers", "candles", "pickles", "eggs",
            "flower_amount", "moisture", "charges", "bites", "delay", "hatch",
            "honey_level", "dusted", "note", "color", "variant", "style",
            "hinge", "attached", "triggered", "inverted", "extended", "occupied",
            "in_wall", "persistent", "unstable", "drag", "has_record", "has_book",
            "conditional", "enabled", "locked", "eye", "disarmed", "mode",
            "instrument", "signal_fire", "cracked", "ominous", "snowy"
    );

    public record ParsedBlueprint(
            int width,
            int height,
            int length,
            int totalVoxels,
            int entityCount,
            List<BlockPos> destroyCandidatesTopDown,
            Map<BlockPos, BlueprintCell> cellMap,
            List<BlueprintCell> singleCellsBottomUp,
            List<MultiBlockGroup> multiGroupsBottomUp
    ) {
    }

    public static final int MAX_CANNON_RANGE = 256;

    public static boolean isValidDimensionsAndBlocks(int width, int height, int length, int blocksLength) {
        if (width <= 0 || height <= 0 || length <= 0 || blocksLength <= 0) {
            return false;
        }
        long layerSize = (long) width * (long) length;
        return layerSize > 0 && layerSize <= Integer.MAX_VALUE;
    }

    public static boolean isWithinCannonRange(@Nullable BlockPos cannonPos, @Nullable BlockPos targetPos) {
        return cannonPos != null && targetPos != null && targetPos.closerThan(cannonPos, MAX_CANNON_RANGE);
    }

    public static boolean isValidWorldTarget(Level level, @Nullable BlockPos cannonPos, @Nullable BlockPos targetPos) {
        return targetPos != null && level.isInValidBounds(targetPos) && isWithinCannonRange(cannonPos, targetPos);
    }

    public static boolean isBlueprintAir(BlockState state) {
        return state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.BARRIER);
    }

    /**
     * 通用判定是否为纯流体方块（排除含水方块如 waterlogged 台阶/楼梯）。
     */
    public static boolean isPureFluidBlock(BlockState state) {
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty()) {
            return false;
        }
        return state.getBlock() instanceof LiquidBlock
                || fluidState.createLegacyBlock().getBlock() == state.getBlock();
    }

    /**
     * 通用判定是否为流动液体格（非源头流体格）。
     */
    public static boolean isFlowingFluid(BlockState state) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !fluidState.isSource();
    }

    public static boolean isBreakableBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;
        return state.getDestroySpeed(level, pos) >= 0;
    }

    public static boolean canOverwriteWorldState(BlockState state) {
        return state.isAir() || state.getBlock() instanceof LiquidBlock;
    }

    public static boolean isStateMatch(BlockState worldState, BlockState blueprintState, boolean exactMatch) {
        if (exactMatch) {
            return worldState == blueprintState;
        }
        return worldState.getBlock() == blueprintState.getBlock();
    }

    /**
     * 解析蓝图 NBT，完成旋转/镜像坐标变换、多方块耦合分组与阶段排序。
     */
    public static ParsedBlueprint parseBlueprint(CompoundTag blueprintTag, @Nullable LevelReader worldLevel) {
        blueprintTag = LitematicaParser.expandBlueprintTag(blueprintTag);
        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");

        if (!isValidDimensionsAndBlocks(width, height, length, blocks.length)) {
            ModLog.debug("BlueprintBuildPlanner.parseBlueprint: 非法蓝图尺寸或数组长度 width=" + width
                    + ", height=" + height + ", length=" + length + ", blocksLength=" + blocks.length);
            return new ParsedBlueprint(width, height, length, blocks.length, entitiesTag.size(),
                    List.of(), Map.of(), List.of(), List.of());
        }

        long layerSize = (long) width * (long) length;
        long maxVolume = layerSize * (long) height;
        int effectiveLength = (int) Math.min((long) blocks.length, maxVolume);

        BlockPos basePos = new BlockPos(
                blueprintTag.getIntOr("GhostX", 0),
                blueprintTag.getIntOr("GhostY", 0),
                blueprintTag.getIntOr("GhostZ", 0)
        );
        BlockPos offset = new BlockPos(
                blueprintTag.getIntOr("OffsetX", 0),
                blueprintTag.getIntOr("OffsetY", 0),
                blueprintTag.getIntOr("OffsetZ", 0)
        );
        int rotationIdx = blueprintTag.getIntOr("Rotation", 0);
        boolean mirrored = blueprintTag.getBooleanOr("Mirrored", false);
        Rotation rot = Rotation.values()[Math.floorMod(rotationIdx, 4)];
        Mirror mir = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;

        ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
        BlockState[] decodedPalette = new BlockState[paletteNbt.size()];
        for (int i = 0; i < paletteNbt.size(); i++) {
            Tag stateTag = paletteNbt.get(i);
            BlockState raw = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag)
                    .result()
                    .orElseGet(() -> LitematicaParser.parseBlockStateFromTag(stateTag));
            decodedPalette[i] = raw.rotate(rot).mirror(mir);
        }

        // 建立 (x,y,z) -> 方块实体 NBT 索引表
        Map<BlockPos, CompoundTag> teMap = new HashMap<>();
        ListTag tileEntities = blueprintTag.getListOrEmpty("BlockEntities");
        for (int i = 0; i < tileEntities.size(); i++) {
            CompoundTag te = tileEntities.getCompoundOrEmpty(i);
            int[] tpos = te.getIntArray("pos").orElse(new int[0]);
            if (tpos.length == 3) {
                teMap.put(new BlockPos(tpos[0], tpos[1], tpos[2]), te.getCompoundOrEmpty("nbt").copy());
            }
        }

        Map<BlockPos, BlueprintCell> cellMap = new LinkedHashMap<>();
        List<BlockPos> allWorldPositions = new ArrayList<>(effectiveLength);

        for (int idx = 0; idx < effectiveLength; idx++) {
            int y = (int) ((long) idx / layerSize);
            if (y >= height) {
                break;
            }
            int rem = (int) ((long) idx % layerSize);
            int z = rem / width;
            int x = rem % width;

            BlockPos targetPos = transformLocalPos(x, y, z, width, length, rot, mirrored, basePos, offset);
            allWorldPositions.add(targetPos);

            int paletteIdx = blocks[idx];
            if (paletteIdx < 0 || paletteIdx >= decodedPalette.length) {
                continue;
            }
            BlockState transformedState = decodedPalette[paletteIdx];
            if (isBlueprintAir(transformedState) || isFlowingFluid(transformedState)) {
                continue;
            }

            CompoundTag teData = teMap.get(new BlockPos(x, y, z));
            cellMap.put(targetPos, new BlueprintCell(targetPos, transformedState, teData != null ? teData.copy() : null, idx));
        }

        // 破坏阶段列表：严格自顶向下（y 降序），先拆上方/依附物，再拆下方支撑
        List<BlockPos> destroyCandidatesTopDown = new ArrayList<>(allWorldPositions);
        destroyCandidatesTopDown.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                .thenComparingInt(BlockPos::getZ)
                .thenComparingInt(BlockPos::getX));

        // 通用多方块分组（仅对蓝图中的非空气方块做邻接耦合连通分量）
        List<BlueprintCell> orderedCells = new ArrayList<>(cellMap.values());
        orderedCells.sort(Comparator
                .comparingInt((BlueprintCell c) -> c.pos().getY())
                .thenComparingInt(c -> c.pos().getZ())
                .thenComparingInt(c -> c.pos().getX()));

        List<BlueprintCell> singleCells = new ArrayList<>();
        List<MultiBlockGroup> multiGroups = new ArrayList<>();

        Map<BlockPos, BlockState> blueprintStateOverlay = new HashMap<>(cellMap.size());
        for (BlueprintCell c : orderedCells) {
            blueprintStateOverlay.put(c.pos(), c.state());
        }

        Set<BlockPos> visited = new HashSet<>();
        RandomSource random = RandomSource.create();

        for (BlueprintCell seed : orderedCells) {
            if (!visited.add(seed.pos())) {
                continue;
            }

            List<BlueprintCell> component = new ArrayList<>();
            Deque<BlueprintCell> queue = new ArrayDeque<>();
            queue.add(seed);
            boolean usedDeferredTick = false;

            while (!queue.isEmpty()) {
                BlueprintCell current = queue.poll();
                component.add(current);

                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = current.pos().relative(dir);
                    if (visited.contains(neighborPos)) {
                        continue;
                    }
                    BlueprintCell neighbor = cellMap.get(neighborPos);
                    if (neighbor == null) {
                        continue;
                    }
                    if (current.state().getBlock() != neighbor.state().getBlock()) {
                        continue;
                    }
                    if (areMutuallyCoupled(current, neighbor, dir, blueprintStateOverlay, worldLevel, random)) {
                        visited.add(neighborPos);
                        queue.add(neighbor);
                    } else if (areDeferredTickMultiblockCoupled(current, neighbor, dir, blueprintStateOverlay, worldLevel, random)) {
                        usedDeferredTick = true;
                        visited.add(neighborPos);
                        queue.add(neighbor);
                    }
                }
            }

            if (component.size() == 1) {
                singleCells.add(seed);
            } else {
                List<List<BlueprintCell>> splitInstances;
                if (usedDeferredTick) {
                    Property<?> partProp = findPartProperty(seed.state().getBlock());
                    splitInstances = splitMultiBlockInstances(component, partProp);
                } else {
                    splitInstances = List.of(component);
                }
                for (List<BlueprintCell> instance : splitInstances) {
                    if (instance.size() == 1) {
                        singleCells.add(instance.getFirst());
                        continue;
                    }
                    List<BlueprintCell> sortedInstance = new ArrayList<>(instance);
                    sortedInstance.sort(Comparator
                            .comparingInt((BlueprintCell c) -> c.pos().getY())
                            .thenComparingInt(c -> c.pos().getZ())
                            .thenComparingInt(c -> c.pos().getX()));
                    Item requiredItem = Items.AIR;
                    for (BlueprintCell part : sortedInstance) {
                        Item candidate = part.state().getBlock().asItem();
                        if (candidate != Items.AIR) {
                            requiredItem = candidate;
                            break;
                        }
                    }
                    if (requiredItem != Items.AIR) {
                        multiGroups.add(new MultiBlockGroup(List.copyOf(sortedInstance), requiredItem));
                    }
                }
            }
        }

        ModLog.debug("BlueprintBuildPlanner: 解析完成 single=" + singleCells.size()
                + ", multiGroups=" + multiGroups.size()
                + ", destroyCandidates=" + destroyCandidatesTopDown.size());

        return new ParsedBlueprint(width, height, length, blocks.length, entitiesTag.size(),
                destroyCandidatesTopDown, cellMap, singleCells, multiGroups);
    }

    public static BlockPos transformLocalPos(
            int x, int y, int z,
            int width, int length,
            Rotation rot, boolean mirrored,
            BlockPos basePos, BlockPos offset
    ) {
        int rx = x;
        int rz = z;
        int wRot = width;
        if (rot == Rotation.CLOCKWISE_90) {
            rx = length - 1 - z;
            rz = x;
            wRot = length;
        } else if (rot == Rotation.CLOCKWISE_180) {
            rx = width - 1 - x;
            rz = length - 1 - z;
            wRot = width;
        } else if (rot == Rotation.COUNTERCLOCKWISE_90) {
            rx = z;
            rz = width - 1 - x;
            wRot = length;
        }

        if (mirrored) {
            rx = wRot - 1 - rx;
        }

        return basePos.offset(offset).offset(rx, y, rz);
    }

    /**
     * 通用检测两个相邻同种方块是否属于同一个耦合多方块（如门上下半、床头尾、双格高植物等）：
     * 任一侧变为 AIR 时通过 updateShape 导致另一侧也变为 AIR，而在双方都在场时均存活。
     */
    public static boolean areMutuallyCoupled(
            BlueprintCell a,
            BlueprintCell b,
            Direction dirFromAToB,
            Map<BlockPos, BlockState> blueprintOverlay,
            @Nullable LevelReader worldLevel,
            RandomSource random
    ) {
        try {
            BlueprintLevelReader fullReader = new BlueprintLevelReader(worldLevel, blueprintOverlay);
            BlueprintLevelReader readerWithoutB = new BlueprintLevelReader(worldLevel, blueprintOverlay, b.pos(), Blocks.AIR.defaultBlockState());
            BlueprintLevelReader readerWithoutA = new BlueprintLevelReader(worldLevel, blueprintOverlay, a.pos(), Blocks.AIR.defaultBlockState());

            BlockState aWithoutB = a.state().updateShape(readerWithoutB, readerWithoutB, a.pos(), dirFromAToB, b.pos(), Blocks.AIR.defaultBlockState(), random);
            if (!aWithoutB.isAir()) return false;

            BlockState bWithoutA = b.state().updateShape(readerWithoutA, readerWithoutA, b.pos(), dirFromAToB.getOpposite(), a.pos(), Blocks.AIR.defaultBlockState(), random);
            if (!bWithoutA.isAir()) return false;

            BlockState aWithB = a.state().updateShape(fullReader, fullReader, a.pos(), dirFromAToB, b.pos(), b.state(), random);
            BlockState bWithA = b.state().updateShape(fullReader, fullReader, b.pos(), dirFromAToB.getOpposite(), a.pos(), a.state(), random);

            if (!aWithB.isAir() && !bWithA.isAir()) {
                return true;
            }
            // 若一方因外部地面未包含在蓝图中（如未附带草方块的向日葵下半部分）而在 fullReader 下无法 canSurvive，
            // 只要双方方块状态不同（如 LOWER/UPPER）且至少另一方在对方存在时存活，仍判定为耦合多方块。
            return !a.state().equals(b.state()) && (!aWithB.isAir() || !bWithA.isAir());
        } catch (Exception e) {
            ModLog.debug("BlueprintBuildPlanner: updateShape 耦合判定回退为 false: " + a.state() + " <-> " + b.state() + ", err=" + e.getMessage());
            return false;
        }
    }

    /**
     * 通用识别采用延迟 1-tick 自检（updateShape 中调用 scheduleTick 而非同步返回 AIR）的模组多方块结构：
     * 要求方块具有多部件索引属性（排除连接掩码/堆叠数量等通用属性）、相邻格朝向一致，且移除邻居时双方均调度了自检 tick。
     */
    public static boolean areDeferredTickMultiblockCoupled(
            BlueprintCell a,
            BlueprintCell b,
            Direction dirFromAToB,
            Map<BlockPos, BlockState> blueprintOverlay,
            @Nullable LevelReader worldLevel,
            RandomSource random
    ) {
        Block block = a.state().getBlock();
        Property<?> partProp = findPartProperty(block);
        if (partProp == null || !haveCompatibleFacing(a.state(), b.state())) {
            return false;
        }
        try {
            BlueprintLevelReader readerWithoutB = new BlueprintLevelReader(worldLevel, blueprintOverlay, b.pos(), Blocks.AIR.defaultBlockState());
            a.state().updateShape(readerWithoutB, readerWithoutB, a.pos(), dirFromAToB, b.pos(), Blocks.AIR.defaultBlockState(), random);
            if (!readerWithoutB.wasTickScheduled()) {
                return false;
            }

            BlueprintLevelReader readerWithoutA = new BlueprintLevelReader(worldLevel, blueprintOverlay, a.pos(), Blocks.AIR.defaultBlockState());
            b.state().updateShape(readerWithoutA, readerWithoutA, b.pos(), dirFromAToB.getOpposite(), a.pos(), Blocks.AIR.defaultBlockState(), random);
            return readerWithoutA.wasTickScheduled();
        } catch (Exception e) {
            ModLog.debug("BlueprintBuildPlanner: 延迟 tick 多方块耦合判定异常: " + a.state() + " <-> " + b.state() + ", err=" + e.getMessage());
            return false;
        }
    }

    public static @Nullable Property<?> findPartProperty(Block block) {
        Property<?> fallbackInt = null;
        for (Property<?> prop : block.getStateDefinition().getProperties()) {
            String name = prop.getName().toLowerCase(Locale.ROOT);
            if (IGNORED_NON_PART_PROPERTIES.contains(name)) {
                continue;
            }
            if (name.equals("part") || name.endsWith("_part")
                    || name.equals("half") || name.endsWith("_half")
                    || name.equals("section") || name.equals("segment")
                    || name.equals("piece") || name.contains("multiblock")
                    || name.contains("subblock")) {
                return prop;
            }
            if (fallbackInt == null && prop instanceof IntegerProperty intProp
                    && intProp.getPossibleValues().contains(0) && intProp.getPossibleValues().size() >= 2) {
                fallbackInt = prop;
            }
        }
        return fallbackInt;
    }

    private static boolean haveCompatibleFacing(BlockState a, BlockState b) {
        if (a.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && !a.getValue(BlockStateProperties.HORIZONTAL_FACING).equals(b.getValue(BlockStateProperties.HORIZONTAL_FACING))) {
            return false;
        }
        if (a.hasProperty(BlockStateProperties.FACING)
                && !a.getValue(BlockStateProperties.FACING).equals(b.getValue(BlockStateProperties.FACING))) {
            return false;
        }
        return true;
    }

    /**
     * 将紧贴摆放在一起的多台相同模组多方块实例按锚点与相对偏移一致性精确拆分为独立 MultiBlockGroup。
     */
    private static List<List<BlueprintCell>> splitMultiBlockInstances(
            List<BlueprintCell> component,
            @Nullable Property<?> partProp
    ) {
        if (component.size() <= 1 || partProp == null) {
            return List.of(component);
        }
        Block block = component.getFirst().state().getBlock();
        Comparable<?> anchorVal = block.defaultBlockState().getValue(partProp);

        List<BlueprintCell> anchors = new ArrayList<>();
        Map<Comparable<?>, Integer> partFreq = new HashMap<>();
        Map<BlockPos, Comparable<?>> posToPart = new HashMap<>(component.size());
        for (BlueprintCell cell : component) {
            Comparable<?> val = cell.state().getValue(partProp);
            partFreq.merge(val, 1, Integer::sum);
            posToPart.put(cell.pos(), val);
            if (val.equals(anchorVal)) {
                anchors.add(cell);
            }
        }

        int k = anchors.size();
        if (k <= 1) {
            return List.of(component);
        }

        anchors.sort(Comparator
                .comparingInt((BlueprintCell c) -> c.pos().getY())
                .thenComparingInt(c -> c.pos().getZ())
                .thenComparingInt(c -> c.pos().getX()));

        List<List<BlueprintCell>> groups = new ArrayList<>(k);
        List<Set<Comparable<?>>> usedUniqueParts = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            List<BlueprintCell> list = new ArrayList<>();
            list.add(anchors.get(i));
            groups.add(list);
            Set<Comparable<?>> used = new HashSet<>();
            used.add(anchorVal);
            usedUniqueParts.add(used);
        }

        List<BlueprintCell> nonAnchors = new ArrayList<>(component.size() - k);
        for (BlueprintCell cell : component) {
            if (!cell.state().getValue(partProp).equals(anchorVal)) {
                nonAnchors.add(cell);
            }
        }

        // 优先分配锚点相对偏移共识度最高的部件（使并排 3x3 电视、转盘等边界绝不互抢）
        Map<BlueprintCell, Integer> maxConsensusCache = new HashMap<>(nonAnchors.size());
        for (BlueprintCell cell : nonAnchors) {
            Comparable<?> partVal = cell.state().getValue(partProp);
            int maxConsensus = 0;
            for (BlueprintCell anchor : anchors) {
                BlockPos delta = cell.pos().subtract(anchor.pos());
                int consensus = countOffsetConsensus(anchors, posToPart, delta, partVal);
                if (consensus > maxConsensus) {
                    maxConsensus = consensus;
                }
            }
            maxConsensusCache.put(cell, maxConsensus);
        }

        nonAnchors.sort((c1, c2) -> {
            int cmpConsensus = Integer.compare(maxConsensusCache.get(c2), maxConsensusCache.get(c1));
            if (cmpConsensus != 0) return cmpConsensus;
            int d1 = minManhattanToAnchors(c1.pos(), anchors);
            int d2 = minManhattanToAnchors(c2.pos(), anchors);
            return Integer.compare(d1, d2);
        });

        for (BlueprintCell cell : nonAnchors) {
            Comparable<?> partVal = cell.state().getValue(partProp);
            boolean repeatable = partFreq.getOrDefault(partVal, 0) > k;

            int bestIdx = -1;
            int bestConsensus = -1;
            int bestDist = Integer.MAX_VALUE;

            for (int i = 0; i < k; i++) {
                if (!repeatable && usedUniqueParts.get(i).contains(partVal)) {
                    continue;
                }
                BlockPos delta = cell.pos().subtract(anchors.get(i).pos());
                int consensus = countOffsetConsensus(anchors, posToPart, delta, partVal);
                int dist = cell.pos().distManhattan(anchors.get(i).pos());
                if (consensus > bestConsensus || (consensus == bestConsensus && dist < bestDist)) {
                    bestConsensus = consensus;
                    bestDist = dist;
                    bestIdx = i;
                }
            }

            if (bestIdx < 0) {
                // 回退：若该部位已超出各组上限，按最近曼哈顿距离归属
                for (int i = 0; i < k; i++) {
                    int dist = cell.pos().distManhattan(anchors.get(i).pos());
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestIdx = i;
                    }
                }
            }

            if (bestIdx >= 0) {
                groups.get(bestIdx).add(cell);
                if (!repeatable) {
                    usedUniqueParts.get(bestIdx).add(partVal);
                }
            }
        }

        return groups;
    }

    private static int countOffsetConsensus(
            List<BlueprintCell> anchors,
            Map<BlockPos, Comparable<?>> posToPart,
            BlockPos delta,
            Comparable<?> expectedPart
    ) {
        int count = 0;
        for (BlueprintCell anchor : anchors) {
            Comparable<?> p = posToPart.get(anchor.pos().offset(delta));
            if (expectedPart.equals(p)) {
                count++;
            }
        }
        return count;
    }

    private static int minManhattanToAnchors(BlockPos pos, List<BlueprintCell> anchors) {
        int min = Integer.MAX_VALUE;
        for (BlueprintCell a : anchors) {
            int d = pos.distManhattan(a.pos());
            if (d < min) min = d;
        }
        return min;
    }

    /**
     * 通用计算单个方块格所需物品数量（如双半砖计 2、蜡烛/海泡菜/粉红花瓣/海龟蛋/雪层按堆叠属性计数）。
     */
    public static int getSingleBlockItemCount(BlockState state) {
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            return 2;
        }
        if (state.hasProperty(BlockStateProperties.CANDLES)) {
            return state.getValue(BlockStateProperties.CANDLES);
        }
        if (state.hasProperty(BlockStateProperties.PICKLES)) {
            return state.getValue(BlockStateProperties.PICKLES);
        }
        if (state.hasProperty(BlockStateProperties.FLOWER_AMOUNT)) {
            return state.getValue(BlockStateProperties.FLOWER_AMOUNT);
        }
        if (state.hasProperty(BlockStateProperties.EGGS)) {
            return state.getValue(BlockStateProperties.EGGS);
        }
        if (state.hasProperty(BlockStateProperties.LAYERS)) {
            return state.getValue(BlockStateProperties.LAYERS);
        }
        return 1;
    }

    /**
     * 按方块类型统计蓝图真实材料需求量与占用体素格数（多方块整组计 1，双半砖等按实际消耗计 N）。
     */
    public static Map<Block, MaterialStat> collectMaterialStats(CompoundTag blueprintTag, @Nullable LevelReader worldLevel) {
        ParsedBlueprint parsed = parseBlueprint(blueprintTag, worldLevel);
        Map<Block, Integer> voxelCounts = new LinkedHashMap<>();
        for (BlueprintCell cell : parsed.cellMap().values()) {
            voxelCounts.merge(cell.state().getBlock(), 1, Integer::sum);
        }

        Map<Block, Integer> itemCounts = new LinkedHashMap<>();
        for (BlueprintCell cell : parsed.singleCellsBottomUp()) {
            Block b = cell.state().getBlock();
            int needed = (b.asItem() == Items.AIR) ? 1 : getSingleBlockItemCount(cell.state());
            itemCounts.merge(b, needed, Integer::sum);
        }
        for (MultiBlockGroup group : parsed.multiGroupsBottomUp()) {
            if (!group.parts().isEmpty()) {
                Block b = group.parts().getFirst().state().getBlock();
                itemCounts.merge(b, 1, Integer::sum);
            }
        }

        Map<Block, MaterialStat> result = new LinkedHashMap<>();
        for (Map.Entry<Block, Integer> entry : voxelCounts.entrySet()) {
            Block b = entry.getKey();
            int voxels = entry.getValue();
            int items = itemCounts.getOrDefault(b, voxels);
            result.put(b, new MaterialStat(items, voxels));
        }
        ModLog.debug("BlueprintBuildPlanner.collectMaterialStats: 统计材料种类=" + result.size());
        return result;
    }

    /**
     * 按 MaterialKey（彩色方块系列按具体 RGB 颜色细分，其余方块按 Block 聚合）统计蓝图真实材料需求量与占用格数。
     */
    public static Map<MaterialKey, MaterialStat> collectColoredMaterialStats(CompoundTag blueprintTag, @Nullable LevelReader worldLevel) {
        ParsedBlueprint parsed = parseBlueprint(blueprintTag, worldLevel);
        Map<MaterialKey, Integer> voxelCounts = new LinkedHashMap<>();
        Map<MaterialKey, Integer> itemCounts = new LinkedHashMap<>();

        for (BlueprintCell cell : parsed.singleCellsBottomUp()) {
            Block b = cell.state().getBlock();
            int[] colors = getRequiredColors(cell.state(), cell.teData());
            if (colors.length > 0) {
                if (colors.length == 2 && colors[0] != colors[1]) {
                    MaterialKey k0 = new MaterialKey(b, colors[0]);
                    MaterialKey k1 = new MaterialKey(b, colors[1]);
                    itemCounts.merge(k0, 1, Integer::sum);
                    itemCounts.merge(k1, 1, Integer::sum);
                    voxelCounts.merge(k0, 1, Integer::sum);
                    voxelCounts.merge(k1, 1, Integer::sum);
                } else {
                    MaterialKey k = new MaterialKey(b, colors[0]);
                    itemCounts.merge(k, colors.length, Integer::sum);
                    voxelCounts.merge(k, 1, Integer::sum);
                }
            } else {
                MaterialKey k = new MaterialKey(b, null);
                int needed = (b.asItem() == Items.AIR) ? 1 : getSingleBlockItemCount(cell.state());
                itemCounts.merge(k, needed, Integer::sum);
                voxelCounts.merge(k, 1, Integer::sum);
            }
        }

        for (MultiBlockGroup group : parsed.multiGroupsBottomUp()) {
            if (!group.parts().isEmpty()) {
                Block b = group.parts().getFirst().state().getBlock();
                MaterialKey k = new MaterialKey(b, null);
                itemCounts.merge(k, 1, Integer::sum);
                voxelCounts.merge(k, group.parts().size(), Integer::sum);
            }
        }

        // 兜底统计仅出现在 cellMap 中（例如非源头流体等）而不在 single/multi 列表里的体素格
        for (BlueprintCell cell : parsed.cellMap().values()) {
            Block b = cell.state().getBlock();
            int[] colors = getRequiredColors(cell.state(), cell.teData());
            MaterialKey k = colors.length > 0 ? new MaterialKey(b, colors[0]) : new MaterialKey(b, null);
            if (!itemCounts.containsKey(k)) {
                voxelCounts.merge(k, 1, Integer::sum);
            }
        }

        Map<MaterialKey, MaterialStat> result = new LinkedHashMap<>();
        for (Map.Entry<MaterialKey, Integer> entry : itemCounts.entrySet()) {
            MaterialKey k = entry.getKey();
            int items = entry.getValue();
            int voxels = voxelCounts.getOrDefault(k, items);
            result.put(k, new MaterialStat(items, voxels));
        }
        for (Map.Entry<MaterialKey, Integer> entry : voxelCounts.entrySet()) {
            result.putIfAbsent(entry.getKey(), new MaterialStat(entry.getValue(), entry.getValue()));
        }
        ModLog.debug("BlueprintBuildPlanner.collectColoredMaterialStats: 细分材料条目数=" + result.size());
        return result;
    }

    /**
     * 校验将原方块替换为目标方块是否合法（禁止多格与单格互替或不同多方块体系互替；允许替换为空气清除）。
     */
    public static @Nullable Component checkReplacementCompatibility(Block sourceBlock, Block targetBlock) {
        if (sourceBlock == null || targetBlock == null) {
            return Component.translatable("message.tuanzis_mod.blueprint.replace_invalid");
        }
        if (sourceBlock == targetBlock) {
            return Component.translatable("message.tuanzis_mod.blueprint.replace_same_block");
        }
        if (targetBlock == Blocks.AIR) {
            return null;
        }
        Property<?> srcPart = findPartProperty(sourceBlock);
        Property<?> dstPart = findPartProperty(targetBlock);
        if (srcPart != null && dstPart == null) {
            return Component.translatable(
                    "message.tuanzis_mod.blueprint.replace_multiblock_to_single",
                    sourceBlock.getName(),
                    targetBlock.getName()
            );
        }
        if (srcPart == null && dstPart != null) {
            return Component.translatable(
                    "message.tuanzis_mod.blueprint.replace_single_to_multiblock",
                    sourceBlock.getName(),
                    targetBlock.getName()
            );
        }
        if (srcPart != null && dstPart != null) {
            if (!srcPart.getName().equals(dstPart.getName())
                    || !new HashSet<>(srcPart.getPossibleValues()).equals(new HashSet<>(dstPart.getPossibleValues()))) {
                return Component.translatable(
                        "message.tuanzis_mod.blueprint.replace_multiblock_mismatch",
                        sourceBlock.getName(),
                        targetBlock.getName()
                );
            }
        }
        return null;
    }

    /**
     * 通用保留原方块状态中与目标方块同名的兼容属性（如朝向、上下半、含水等）。
     * 若目标方块为纯流体则统一返回默认源头状态；若原方块为纯流体则不复制流体的 level 属性。
     */
    public static BlockState transferCompatibleProperties(BlockState oldState, Block targetBlock) {
        BlockState targetDefault = targetBlock.defaultBlockState();
        if (targetBlock == Blocks.AIR || isPureFluidBlock(targetDefault)) {
            return targetDefault;
        }
        boolean oldIsPureFluid = isPureFluidBlock(oldState);
        var targetDefinition = targetBlock.getStateDefinition();
        BlockState result = targetDefault;
        for (Property<?> oldProp : oldState.getProperties()) {
            String propName = oldProp.getName();
            if (oldIsPureFluid && "level".equals(propName)) {
                continue;
            }
            Property<?> targetProp = targetDefinition.getProperty(propName);
            if (targetProp == null) {
                continue;
            }
            result = copyNamedProperty(oldState, oldProp, result, targetProp);
        }
        return result;
    }

    private static <S extends Comparable<S>, T extends Comparable<T>> BlockState copyNamedProperty(
            BlockState oldState,
            Property<S> oldProp,
            BlockState targetState,
            Property<T> targetProp
    ) {
        S oldVal = oldState.getValue(oldProp);
        String valName = oldProp.getName(oldVal);
        Optional<T> parsed = targetProp.getValue(valName);
        return parsed.map(t -> targetState.setValue(targetProp, t)).orElse(targetState);
    }

    /**
     * 将蓝图 NBT 中指定的原方块全部替换为目标方块：
     * - 校验多格/单格兼容性；
     * - 保留兼容的方块状态属性；
     * - 若原方块为纯流体，仅将源头格替换为目标方块，流动格自动清理为空气；
     * - 自动清理已修改格上类型不再匹配的旧 BlockEntities NBT；
     * - 对 Palette 执行压缩去重并回写 Palette / Blocks / BlockEntities。
     */
    public static ReplaceResult replaceBlockInBlueprint(CompoundTag blueprintTag, Block sourceBlock, Block targetBlock) {
        if (blueprintTag == null) {
            return new ReplaceResult(false, 0, 0, 0, Component.translatable("message.tuanzis_mod.blueprint.replace_invalid"));
        }
        Component incompatError = checkReplacementCompatibility(sourceBlock, targetBlock);
        if (incompatError != null) {
            ModLog.debug("BlueprintBuildPlanner.replaceBlockInBlueprint: 兼容性校验拦截 "
                    + sourceBlock.getName().getString() + " -> " + targetBlock.getName().getString()
                    + ", reason=" + incompatError.getString());
            return new ReplaceResult(false, 0, 0, 0, incompatError);
        }

        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        ListTag oldPaletteNbt = blueprintTag.getListOrEmpty("Palette");
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        if (width <= 0 || height <= 0 || length <= 0 || oldPaletteNbt.isEmpty() || blocks.length == 0) {
            return new ReplaceResult(false, 0, 0, 0, Component.translatable("message.tuanzis_mod.blueprint.replace_invalid"));
        }

        BlockState[] oldStates = new BlockState[oldPaletteNbt.size()];
        BlockState[] newStates = new BlockState[oldPaletteNbt.size()];
        boolean[] replacedPaletteSlot = new boolean[oldPaletteNbt.size()];
        boolean[] clearedFlowingSlot = new boolean[oldPaletteNbt.size()];

        for (int i = 0; i < oldPaletteNbt.size(); i++) {
            BlockState st = LitematicaParser.parseBlockStateFromTag(oldPaletteNbt.get(i));
            oldStates[i] = st;
            if (st.getBlock() == sourceBlock) {
                if (isPureFluidBlock(st) && isFlowingFluid(st)) {
                    newStates[i] = Blocks.AIR.defaultBlockState();
                    clearedFlowingSlot[i] = true;
                } else {
                    newStates[i] = transferCompatibleProperties(st, targetBlock);
                    replacedPaletteSlot[i] = true;
                }
            } else {
                newStates[i] = st;
            }
        }

        // 统计受影响格数与重构去重后的调色板
        int replacedVoxels = 0;
        int clearedFlowingVoxels = 0;
        List<BlockState> compactPalette = new ArrayList<>();
        compactPalette.add(Blocks.AIR.defaultBlockState());
        Map<BlockState, Integer> stateToCompactIdx = new HashMap<>();
        stateToCompactIdx.put(Blocks.AIR.defaultBlockState(), 0);

        int[] oldIdxToNewIdx = new int[oldStates.length];
        for (int i = 0; i < newStates.length; i++) {
            BlockState ns = newStates[i];
            Integer mapped = stateToCompactIdx.get(ns);
            if (mapped == null) {
                mapped = compactPalette.size();
                compactPalette.add(ns);
                stateToCompactIdx.put(ns, mapped);
            }
            oldIdxToNewIdx[i] = mapped;
        }

        int[] updatedBlocks = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            int pIdx = blocks[i];
            if (pIdx < 0 || pIdx >= oldIdxToNewIdx.length) {
                updatedBlocks[i] = 0;
                continue;
            }
            if (replacedPaletteSlot[pIdx]) {
                replacedVoxels++;
            } else if (clearedFlowingSlot[pIdx]) {
                clearedFlowingVoxels++;
            }
            updatedBlocks[i] = oldIdxToNewIdx[pIdx];
        }

        ListTag encodedPalette = new ListTag();
        for (BlockState state : compactPalette) {
            encodedPalette.add(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state)
                    .result()
                    .orElseGet(() -> LitematicaParser.parseBlockStateFromTag(null).isAir()
                            ? net.minecraft.nbt.StringTag.valueOf("minecraft:air")
                            : net.minecraft.nbt.StringTag.valueOf("minecraft:stone")));
        }
        blueprintTag.put("Palette", encodedPalette);
        blueprintTag.putIntArray("Blocks", updatedBlocks);

        // 清理已替换格上类型不匹配的旧 BlockEntities NBT
        int removedBlockEntities = 0;
        if (blueprintTag.contains("BlockEntities")) {
            ListTag oldBes = blueprintTag.getListOrEmpty("BlockEntities");
            ListTag keptBes = new ListTag();
            for (int i = 0; i < oldBes.size(); i++) {
                CompoundTag entry = oldBes.getCompoundOrEmpty(i);
                int[] posArr = entry.getIntArray("pos").orElse(new int[0]);
                boolean keep = true;
                if (posArr.length >= 3) {
                    int bx = posArr[0];
                    int by = posArr[1];
                    int bz = posArr[2];
                    if (bx >= 0 && bx < width && by >= 0 && by < height && bz >= 0 && bz < length) {
                        long bIdxLong = (long) by * (long) width * (long) length + (long) bz * (long) width + (long) bx;
                        if (bIdxLong >= 0 && bIdxLong < blocks.length) {
                            int bIdx = (int) bIdxLong;
                            int oldPIdx = blocks[bIdx];
                            if (oldPIdx >= 0 && oldPIdx < oldStates.length
                                    && (replacedPaletteSlot[oldPIdx] || clearedFlowingSlot[oldPIdx])) {
                                BlockState oldState = oldStates[oldPIdx];
                                BlockState newState = newStates[oldPIdx];
                                if (!hasSameBlockEntityType(oldState, newState, new BlockPos(bx, by, bz))) {
                                    keep = false;
                                    removedBlockEntities++;
                                }
                            }
                        }
                    }
                }
                if (keep) {
                    keptBes.add(entry.copy());
                }
            }
            blueprintTag.put("BlockEntities", keptBes);
        }

        ModLog.debug("BlueprintBuildPlanner.replaceBlockInBlueprint 完成: "
                + sourceBlock.getName().getString() + " -> " + targetBlock.getName().getString()
                + ", replacedVoxels=" + replacedVoxels
                + ", clearedFlowingVoxels=" + clearedFlowingVoxels
                + ", removedBlockEntities=" + removedBlockEntities);

        Component msg = Component.translatable(
                "message.tuanzis_mod.blueprint.replace_success",
                sourceBlock.getName(),
                targetBlock.getName(),
                replacedVoxels
        );
        return new ReplaceResult(true, replacedVoxels, clearedFlowingVoxels, removedBlockEntities, msg);
    }

    private static boolean hasSameBlockEntityType(BlockState oldState, BlockState newState, BlockPos pos) {
        if (!(oldState.getBlock() instanceof EntityBlock oldEb) || !(newState.getBlock() instanceof EntityBlock newEb)) {
            return false;
        }
        try {
            BlockEntity oldBe = oldEb.newBlockEntity(pos, oldState);
            BlockEntity newBe = newEb.newBlockEntity(pos, newState);
            if (oldBe == null || newBe == null) {
                return false;
            }
            BlockEntityType<?> oldType = oldBe.getType();
            BlockEntityType<?> newType = newBe.getType();
            return oldType != null && oldType == newType && newType.isValid(newState);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查单个方块当前在真实世界中是否已具备放置/存活前置条件（通用依附检测）。
     */
    public static boolean isSingleBlockReady(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Map<BlockPos, BlueprintCell> cellMap
    ) {
        BlueprintLevelReader safeReader = level instanceof BlueprintLevelReader reader
                ? reader
                : new BlueprintLevelReader(level, Map.of());
        if (!state.canSurvive(safeReader, pos)) {
            return false;
        }
        // 下落方块（沙子、砂砾、铁砧、混凝土粉末等）：若下方在蓝图中存在待放置方块且当前世界仍为空，需等待下方先落地
        if (state.getBlock() instanceof FallingBlock) {
            BlockPos below = pos.below();
            if (FallingBlock.isFree(safeReader.getBlockState(below)) && cellMap.containsKey(below)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查多方块组合的外部前置依赖是否已就绪（内部互相依附部分通过局部叠加模拟满足）。
     */
    public static boolean isMultiBlockReady(
            LevelReader level,
            MultiBlockGroup group,
            Map<BlockPos, BlueprintCell> cellMap
    ) {
        Map<BlockPos, BlockState> groupOverlay = new HashMap<>(group.parts().size());
        for (BlueprintCell part : group.parts()) {
            groupOverlay.put(part.pos(), part.state());
        }
        BlueprintLevelReader overlayReader = new BlueprintLevelReader(level, groupOverlay);

        for (BlueprintCell part : group.parts()) {
            if (!part.state().canSurvive(overlayReader, part.pos())) {
                return false;
            }
            if (part.state().getBlock() instanceof FallingBlock) {
                BlockPos below = part.pos().below();
                if (!groupOverlay.containsKey(below)
                        && FallingBlock.isFree(overlayReader.getBlockState(below))
                        && cellMap.containsKey(below)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 轻量虚拟只读 LevelReader + 空 ScheduledTickAccess：
     * 用于在不修改真实世界的前提下安全调用 BlockState#updateShape 与 BlockState#canSurvive。
     */
    public static class BlueprintLevelReader implements LevelReader, ScheduledTickAccess {
        private final @Nullable LevelReader delegate;
        private final Map<BlockPos, BlockState> overlay;
        private final @Nullable BlockPos overridePos;
        private final @Nullable BlockState overrideState;
        private boolean scheduledTick = false;

        public BlueprintLevelReader(@Nullable LevelReader delegate, Map<BlockPos, BlockState> overlay) {
            this(delegate, overlay, null, null);
        }

        public BlueprintLevelReader(
                @Nullable LevelReader delegate,
                Map<BlockPos, BlockState> overlay,
                @Nullable BlockPos overridePos,
                @Nullable BlockState overrideState
        ) {
            this.delegate = delegate;
            this.overlay = overlay;
            this.overridePos = overridePos;
            this.overrideState = overrideState;
        }

        public boolean wasTickScheduled() {
            return this.scheduledTick;
        }

        private boolean isDelegatePosAccessible(BlockPos pos) {
            if (delegate == null) {
                return false;
            }
            if (delegate instanceof BlueprintLevelReader) {
                return true;
            }
            if (delegate instanceof Level level && !level.isInValidBounds(pos)) {
                return false;
            }
            return delegate.hasChunkAt(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (overridePos != null && overrideState != null && overridePos.equals(pos)) {
                return overrideState;
            }
            BlockState overlaid = overlay.get(pos);
            if (overlaid != null) {
                return overlaid;
            }
            return isDelegatePosAccessible(pos) ? delegate.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            BlockState state = getBlockState(pos);
            if (state != Blocks.AIR.defaultBlockState() || (overridePos != null && overridePos.equals(pos)) || overlay.containsKey(pos)) {
                return state.getFluidState();
            }
            return isDelegatePosAccessible(pos) ? delegate.getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return isDelegatePosAccessible(pos) ? delegate.getBlockEntity(pos) : null;
        }

        @Override
        public int getMinY() {
            return delegate != null ? delegate.getMinY() : -64;
        }

        @Override
        public int getHeight() {
            return delegate != null ? delegate.getHeight() : 384;
        }

        @Override
        public @Nullable ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus targetStatus, boolean loadOrGenerate) {
            return delegate != null ? delegate.getChunk(chunkX, chunkZ, targetStatus, loadOrGenerate) : null;
        }

        @Override
        public boolean hasChunk(int chunkX, int chunkZ) {
            return delegate != null && delegate.hasChunk(chunkX, chunkZ);
        }

        @Override
        public int getHeight(Heightmap.Types type, int x, int z) {
            return delegate != null ? delegate.getHeight(type, x, z) : 0;
        }

        @Override
        public int getSkyDarken() {
            return delegate != null ? delegate.getSkyDarken() : 0;
        }

        @Override
        public BiomeManager getBiomeManager() {
            return delegate != null ? delegate.getBiomeManager() : null;
        }

        @Override
        public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
            return delegate != null ? delegate.getUncachedNoiseBiome(quartX, quartY, quartZ) : null;
        }

        @Override
        public boolean isClientSide() {
            return delegate != null && delegate.isClientSide();
        }

        @Override
        public int getSeaLevel() {
            return delegate != null ? delegate.getSeaLevel() : 63;
        }

        @Override
        public DimensionType dimensionType() {
            return delegate != null ? delegate.dimensionType() : null;
        }

        @Override
        public RegistryAccess registryAccess() {
            return delegate != null ? delegate.registryAccess() : null;
        }

        @Override
        public FeatureFlagSet enabledFeatures() {
            return delegate != null ? delegate.enabledFeatures() : FeatureFlags.VANILLA_SET;
        }

        @Override
        public EnvironmentAttributeReader environmentAttributes() {
            return delegate != null ? delegate.environmentAttributes() : EnvironmentAttributeReader.EMPTY;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return delegate != null ? delegate.getLightEngine() : null;
        }

        @Override
        public WorldBorder getWorldBorder() {
            return delegate != null ? delegate.getWorldBorder() : new WorldBorder();
        }

        @Override
        public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
            return delegate != null ? delegate.getEntityCollisions(source, testArea) : List.of();
        }

        @Override
        public <T> ScheduledTick<T> createTick(BlockPos pos, T type, int tickDelay, TickPriority priority) {
            this.scheduledTick = true;
            return new ScheduledTick<>(type, pos, 0L, priority, 0L);
        }

        @Override
        public <T> ScheduledTick<T> createTick(BlockPos pos, T type, int tickDelay) {
            this.scheduledTick = true;
            return new ScheduledTick<>(type, pos, 0L, TickPriority.NORMAL, 0L);
        }

        @Override
        public LevelTickAccess<Block> getBlockTicks() {
            return BlackholeTickAccess.emptyLevelList();
        }

        @Override
        public LevelTickAccess<Fluid> getFluidTicks() {
            return BlackholeTickAccess.emptyLevelList();
        }
    }
}
