package me.tuanzi.block.cannon;

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
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    public static boolean isBlueprintAir(BlockState state) {
        return state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.BARRIER);
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
        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");

        if (width <= 0 || height <= 0 || length <= 0 || blocks.length == 0) {
            return new ParsedBlueprint(width, height, length, blocks.length, entitiesTag.size(),
                    List.of(), Map.of(), List.of(), List.of());
        }

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
        List<BlockPos> allWorldPositions = new ArrayList<>(blocks.length);

        for (int idx = 0; idx < blocks.length; idx++) {
            int y = idx / (width * length);
            int rem = idx % (width * length);
            int z = rem / width;
            int x = rem % width;

            BlockPos targetPos = transformLocalPos(x, y, z, width, length, rot, mirrored, basePos, offset);
            allWorldPositions.add(targetPos);

            int paletteIdx = blocks[idx];
            if (paletteIdx < 0 || paletteIdx >= decodedPalette.length) {
                continue;
            }
            BlockState transformedState = decodedPalette[paletteIdx];
            if (isBlueprintAir(transformedState)) {
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
                    }
                }
            }

            if (component.size() == 1) {
                singleCells.add(seed);
            } else {
                component.sort(Comparator
                        .comparingInt((BlueprintCell c) -> c.pos().getY())
                        .thenComparingInt(c -> c.pos().getZ())
                        .thenComparingInt(c -> c.pos().getX()));
                Item requiredItem = Items.AIR;
                for (BlueprintCell part : component) {
                    Item candidate = part.state().getBlock().asItem();
                    if (candidate != Items.AIR) {
                        requiredItem = candidate;
                        break;
                    }
                }
                if (requiredItem != Items.AIR) {
                    multiGroups.add(new MultiBlockGroup(List.copyOf(component), requiredItem));
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
     * 检查单个方块当前在真实世界中是否已具备放置/存活前置条件（通用依附检测）。
     */
    public static boolean isSingleBlockReady(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Map<BlockPos, BlueprintCell> cellMap
    ) {
        if (!state.canSurvive(level, pos)) {
            return false;
        }
        // 下落方块（沙子、砂砾、铁砧、混凝土粉末等）：若下方在蓝图中存在待放置方块且当前世界仍为空，需等待下方先落地
        if (state.getBlock() instanceof FallingBlock) {
            BlockPos below = pos.below();
            if (FallingBlock.isFree(level.getBlockState(below)) && cellMap.containsKey(below)) {
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
                        && FallingBlock.isFree(level.getBlockState(below))
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

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (overridePos != null && overrideState != null && overridePos.equals(pos)) {
                return overrideState;
            }
            BlockState overlaid = overlay.get(pos);
            if (overlaid != null) {
                return overlaid;
            }
            return delegate != null ? delegate.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            BlockState state = getBlockState(pos);
            if (state != Blocks.AIR.defaultBlockState() || (overridePos != null && overridePos.equals(pos)) || overlay.containsKey(pos)) {
                return state.getFluidState();
            }
            return delegate != null ? delegate.getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return delegate != null ? delegate.getBlockEntity(pos) : null;
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
            return new ScheduledTick<>(type, pos, 0L, priority, 0L);
        }

        @Override
        public <T> ScheduledTick<T> createTick(BlockPos pos, T type, int tickDelay) {
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
