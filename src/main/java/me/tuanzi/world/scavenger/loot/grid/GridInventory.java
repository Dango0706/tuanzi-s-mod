package me.tuanzi.world.scavenger.loot.grid;

import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerSortOrder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * 服务端权威二维矩形占地网格引擎（支持 {@code W*H} 碰撞检测、90° 旋转、单目标快速互换、
 * First-Fit 快速塞入、两阶段无损自动整理原子回滚、动态扩容保序以及防非空容器套娃）。
 */
public class GridInventory {

    /**
     * 存储在背包/安全箱物品 {@link DataComponents#CUSTOM_DATA} 内的嵌套网格 NBT 根键名
     */
    public static final String NESTED_GRID_TAG_KEY = "ScavengerNestedGrid";

    public static final int EMPTY_CELL = -1;
    private final LinkedHashMap<Integer, GridItemEntry> entriesById = new LinkedHashMap<>();
    private final List<ItemStack> loadOverflowBuffer = new ArrayList<>();
    private int columns;
    private int rows;
    private int[] occupancy;
    private int nextEntryId = 1;
    private Predicate<ItemStack> extraDepositPolicy;

    public GridInventory(int columns, int rows) {
        this(columns, rows, stack -> true);
    }

    public GridInventory(int columns, int rows, Predicate<ItemStack> extraDepositPolicy) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("网格列数与行数必须 >= 1，当前传入: " + columns + "x" + rows);
        }
        this.columns = columns;
        this.rows = rows;
        this.occupancy = new int[columns * rows];
        Arrays.fill(this.occupancy, EMPTY_CELL);
        this.extraDepositPolicy = extraDepositPolicy != null ? extraDepositPolicy : stack -> true;
    }

    // ==================== 嵌套容器防套娃与工具方法 ====================

    /**
     * 检查指定物品栈（如战术背包或安全箱）内部是否存有未清空的网格物品。
     * 凡内部非空的容器物品严禁放入其他任何网格或从装备槽卸下。
     */
    public static boolean hasNestedGridItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return false;
        }
        CompoundTag root = customData.copyTag();
        if (!root.contains(NESTED_GRID_TAG_KEY)) {
            return false;
        }
        CompoundTag gridTag = root.getCompoundOrEmpty(NESTED_GRID_TAG_KEY);
        ListTag entries = gridTag.getListOrEmpty("entries");
        return !entries.isEmpty();
    }

    /**
     * 将内部网格序列化写入战术背包/安全箱物品栈的 {@link DataComponents#CUSTOM_DATA}。
     * 若内部网格为空，则自动清理对应键以保持干净组件状态。
     */
    public static void writeToContainerStack(ItemStack containerStack, GridInventory grid, HolderLookup.Provider registries) {
        if (containerStack == null || containerStack.isEmpty()) {
            return;
        }
        if (grid == null || grid.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, containerStack, tag -> tag.remove(NESTED_GRID_TAG_KEY));
            return;
        }
        CompoundTag savedGrid = grid.saveToTag(registries);
        CustomData.update(DataComponents.CUSTOM_DATA, containerStack, tag -> tag.put(NESTED_GRID_TAG_KEY, savedGrid));
    }

    /**
     * 从战术背包/安全箱物品栈的 {@link DataComponents#CUSTOM_DATA} 恢复内部网格。
     */
    public static GridInventory readFromContainerStack(
            ItemStack containerStack,
            int columns,
            int rows,
            HolderLookup.Provider registries
    ) {
        GridInventory grid = new GridInventory(columns, rows);
        if (containerStack == null || containerStack.isEmpty()) {
            return grid;
        }
        CustomData customData = containerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return grid;
        }
        CompoundTag root = customData.copyTag();
        if (root.contains(NESTED_GRID_TAG_KEY)) {
            grid.loadFromTag(registries, root.getCompoundOrEmpty(NESTED_GRID_TAG_KEY));
        }
        return grid;
    }

    // ==================== 基础属性与只读查询 ====================

    public int columns() {
        return this.columns;
    }

    public int rows() {
        return this.rows;
    }

    public int totalCells() {
        return this.columns * this.rows;
    }

    public int entryCount() {
        return this.entriesById.size();
    }

    public boolean isEmpty() {
        return this.entriesById.isEmpty();
    }

    public Collection<GridItemEntry> entries() {
        return Collections.unmodifiableCollection(this.entriesById.values());
    }

    public Optional<GridItemEntry> getEntry(int entryId) {
        return Optional.ofNullable(this.entriesById.get(entryId));
    }

    public void setExtraDepositPolicy(Predicate<ItemStack> extraDepositPolicy) {
        this.extraDepositPolicy = Objects.requireNonNullElse(extraDepositPolicy, stack -> true);
    }

    /**
     * 测试某物品栈是否符合该网格的准入规则（始终强制拦截内部非空的嵌套背包/安全箱）。
     */
    public boolean acceptsItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (hasNestedGridItems(stack)) {
            ModLog.debug("【拾荒网格校验】拒绝放入内部装有物品的容器道具: " + stack.getItem());
            return false;
        }
        return this.extraDepositPolicy.test(stack);
    }

    public int occupiedCellCount() {
        int occupied = 0;
        for (int cell : this.occupancy) {
            if (cell != EMPTY_CELL) {
                occupied++;
            }
        }
        return occupied;
    }

    public int freeCellCount() {
        return totalCells() - occupiedCellCount();
    }

    /**
     * 查询指定坐标 {@code (x, y)} 所在的物品条目（若越界或为空格则返回 {@link Optional#empty()}）。
     */
    public Optional<GridItemEntry> getEntryAt(int x, int y) {
        if (x < 0 || x >= this.columns || y < 0 || y >= this.rows) {
            return Optional.empty();
        }
        int id = this.occupancy[cellIndex(x, y)];
        if (id == EMPTY_CELL) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.entriesById.get(id));
    }

    public int getOccupantIdAt(int x, int y) {
        if (x < 0 || x >= this.columns || y < 0 || y >= this.rows) {
            return EMPTY_CELL;
        }
        return this.occupancy[cellIndex(x, y)];
    }

    public boolean isInBounds(int x, int y, int width, int height) {
        return x >= 0 && y >= 0
                && width >= 1 && height >= 1
                && x + width <= this.columns
                && y + height <= this.rows;
    }

    /**
     * 扫描指定合法矩形区域内与 {@code ignoreEntryId} 不同的全部已占用条目 ID（保持扫描发现顺序）。
     */
    public Set<Integer> findCollidingIds(int x, int y, int width, int height, int ignoreEntryId) {
        if (!isInBounds(x, y, width, height)) {
            return Set.of();
        }
        Set<Integer> collisions = new LinkedHashSet<>();
        for (int cy = y; cy < y + height; cy++) {
            int rowOffset = cy * this.columns;
            for (int cx = x; cx < x + width; cx++) {
                int occupant = this.occupancy[rowOffset + cx];
                if (occupant != EMPTY_CELL && occupant != ignoreEntryId) {
                    collisions.add(occupant);
                }
            }
        }
        return collisions;
    }

    public boolean canPlace(ScavengerItemMeta meta, int x, int y, boolean rotated, int ignoreEntryId) {
        if (meta == null) {
            return false;
        }
        int w = meta.effectiveWidth(rotated);
        int h = meta.effectiveHeight(rotated);
        if (!isInBounds(x, y, w, h)) {
            return false;
        }
        return findCollidingIds(x, y, w, h, ignoreEntryId).isEmpty();
    }

    // ==================== 核心修改操作（原子性与日志） ====================

    /**
     * 在指定锚点 {@code (x, y)} 尝试放置一件外部物品。
     *
     * @param stack                 待放入物品（自动复制并归一为数量 1）
     * @param x                     目标左上角列号
     * @param y                     目标左上角行号
     * @param rotated               是否旋转 90 度
     * @param solidifiedSearchTicks 固化的搜索耗时（单位：tick）
     * @param allowSingleSwap       若为 {@code true} 且目标矩形仅与 1 件现有物品重叠，则原子换出该物品
     */
    public GridPlacementResult placeAt(
            ItemStack stack,
            int x,
            int y,
            boolean rotated,
            int solidifiedSearchTicks,
            boolean allowSingleSwap
    ) {
        if (stack == null || stack.isEmpty()) {
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_EMPTY_STACK);
        }
        if (!acceptsItem(stack)) {
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_POLICY);
        }

        ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(stack);
        boolean normRotated = rotated && meta.canRotate();
        int w = meta.effectiveWidth(normRotated);
        int h = meta.effectiveHeight(normRotated);

        if (!isInBounds(x, y, w, h)) {
            ModLog.debug(String.format(
                    "【拾荒网格放置】越界拒绝 -> 物品=%s(%dx%d, rot=%b), 目标=(%d,%d), 网格尺寸=%dx%d",
                    meta.itemId(), w, h, normRotated, x, y, this.columns, this.rows
            ));
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_OUT_OF_BOUNDS);
        }

        Set<Integer> collisions = findCollidingIds(x, y, w, h, EMPTY_CELL);
        if (collisions.isEmpty()) {
            int assignedId = allocateEntryId();
            GridItemEntry placed = new GridItemEntry(assignedId, x, y, normRotated, stack, solidifiedSearchTicks);
            putEntryInternal(placed);
            ModLog.debug(String.format(
                    "【拾荒网格放置】成功落位 -> id=%d, 物品=%s(%dx%d, rot=%b), 锚点=(%d,%d)",
                    assignedId, meta.itemId(), w, h, normRotated, x, y
            ));
            return GridPlacementResult.placed(placed);
        }

        if (allowSingleSwap && collisions.size() == 1) {
            int victimId = collisions.iterator().next();
            GridItemEntry displaced = removeEntryInternal(victimId);
            int assignedId = allocateEntryId();
            GridItemEntry placed = new GridItemEntry(assignedId, x, y, normRotated, stack, solidifiedSearchTicks);
            putEntryInternal(placed);
            ModLog.debug(String.format(
                    "【拾荒网格互换】单目标快速互换成功 -> 放入 id=%d(%s, %dx%d @ %d,%d), 换出 id=%d(%s)",
                    assignedId, meta.itemId(), w, h, x, y,
                    displaced.entryId(), displaced.resolveMeta().itemId()
            ));
            return GridPlacementResult.swapped(placed, displaced);
        }

        ModLog.debug(String.format(
                "【拾荒网格放置】碰撞拒绝 -> 物品=%s(%dx%d @ %d,%d), 冲突条目数=%d, 允许单换=%b",
                meta.itemId(), w, h, x, y, collisions.size(), allowSingleSwap
        ));
        return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_COLLISION);
    }

    /**
     * 在当前网格内部将已有条目 {@code entryId} 平移或旋转至 {@code (targetX, targetY, targetRotated)}。
     * 若失败，原条目严格保持原坐标与原旋转朝向不变。
     */
    public GridPlacementResult moveOrRotate(
            int entryId,
            int targetX,
            int targetY,
            boolean targetRotated,
            boolean allowSingleSwap
    ) {
        GridItemEntry existing = this.entriesById.get(entryId);
        if (existing == null) {
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_NOT_FOUND);
        }

        ScavengerItemMeta meta = existing.resolveMeta();
        boolean normRotated = targetRotated && meta.canRotate();
        int w = meta.effectiveWidth(normRotated);
        int h = meta.effectiveHeight(normRotated);

        if (!isInBounds(targetX, targetY, w, h)) {
            ModLog.debug(String.format(
                    "【拾荒网格移动】越界拒绝(原位保留) -> id=%d(%s), 原=(%d,%d,rot=%b), 目标=(%d,%d,%dx%d)",
                    entryId, meta.itemId(), existing.anchorX(), existing.anchorY(), existing.rotated(),
                    targetX, targetY, w, h
            ));
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_OUT_OF_BOUNDS);
        }

        Set<Integer> collisions = findCollidingIds(targetX, targetY, w, h, entryId);
        if (collisions.isEmpty()) {
            removeEntryInternal(entryId);
            GridItemEntry updated = existing.withPosition(targetX, targetY, normRotated);
            putEntryInternal(updated);
            ModLog.debug(String.format(
                    "【拾荒网格移动】平移/旋转成功 -> id=%d(%s), (%d,%d,rot=%b) -> (%d,%d,rot=%b)",
                    entryId, meta.itemId(),
                    existing.anchorX(), existing.anchorY(), existing.rotated(),
                    targetX, targetY, normRotated
            ));
            return GridPlacementResult.placed(updated);
        }

        ModLog.debug(String.format(
                "【拾荒网格移动】碰撞拒绝(原位保留) -> id=%d(%s), 目标=(%d,%d,rot=%b), 冲突条目数=%d",
                entryId, meta.itemId(), targetX, targetY, normRotated, collisions.size()
        ));
        return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_COLLISION);
    }

    /**
     * 按行优先（{@code y: 0..rows-1, x: 0..columns-1}）扫描首个可容纳空位（优先原朝向，放不下时自动尝试 90° 旋转）。
     */
    public GridPlacementResult quickInsertFirstFit(ItemStack stack, int solidifiedSearchTicks) {
        if (stack == null || stack.isEmpty()) {
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_EMPTY_STACK);
        }
        if (!acceptsItem(stack)) {
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_POLICY);
        }

        ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(stack);
        if (meta.cellCount() > freeCellCount()) {
            ModLog.debug(String.format(
                    "【拾荒快速塞入】剩余格数不足快速拒绝 -> 物品=%s(需%d格), 剩余=%d格",
                    meta.itemId(), meta.cellCount(), freeCellCount()
            ));
            return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_FULL);
        }

        for (int y = 0; y < this.rows; y++) {
            for (int x = 0; x < this.columns; x++) {
                if (canPlace(meta, x, y, false, EMPTY_CELL)) {
                    return placeAt(stack, x, y, false, solidifiedSearchTicks, false);
                }
                if (meta.canRotate() && canPlace(meta, x, y, true, EMPTY_CELL)) {
                    return placeAt(stack, x, y, true, solidifiedSearchTicks, false);
                }
            }
        }

        ModLog.debug(String.format(
                "【拾荒快速塞入】空间碎片化无法容纳 -> 物品=%s(%dx%d), 剩余散格=%d",
                meta.itemId(), meta.width(), meta.height(), freeCellCount()
        ));
        return GridPlacementResult.rejected(GridPlacementResult.Status.REJECTED_FULL);
    }

    /**
     * 按 {@code entryId} 移除并返回条目。
     */
    public Optional<GridItemEntry> removeEntry(int entryId) {
        GridItemEntry removed = removeEntryInternal(entryId);
        if (removed != null) {
            ModLog.debug(String.format(
                    "【拾荒网格移除】已摘除条目 -> id=%d, 物品=%s, 原锚点=(%d,%d)",
                    removed.entryId(), removed.resolveMeta().itemId(), removed.anchorX(), removed.anchorY()
            ));
        }
        return Optional.ofNullable(removed);
    }

    /**
     * 清空网格并返回被移除的全部条目列表。
     */
    public List<GridItemEntry> clearAll() {
        List<GridItemEntry> snapshot = new ArrayList<>(this.entriesById.values());
        this.entriesById.clear();
        Arrays.fill(this.occupancy, EMPTY_CELL);
        return snapshot;
    }

    // ==================== 两阶段无损自动整理与原子回滚 ====================

    /**
     * 使用指定排序规则执行无损自动整理：
     * <ul>
     *   <li>Phase A：严格按 {@code sortOrder} 比较器顺序在临时双缓冲网格中执行 First-Fit 重排（保留原 {@code entryId} 与固化搜索时长）。</li>
     *   <li>Phase B：若 Phase A 因碎格导致后续大尺寸物品无法放入，则切换为“面积降序 + 长边降序 + {@code sortOrder}”重试装箱。</li>
     *   <li>原子回滚：若两阶段均无法 100% 装回所有物品（如极限满容风车互锁构型），原网格状态保持零修改并返回 {@code false}。</li>
     * </ul>
     */
    public boolean autoSort(ScavengerSortOrder sortOrder) {
        Objects.requireNonNull(sortOrder, "sortOrder 不能为空");
        if (this.entriesById.isEmpty()) {
            return true;
        }

        List<GridItemEntry> originalEntries = new ArrayList<>(this.entriesById.values());
        Comparator<ScavengerItemMeta> metaComparator = sortOrder.comparator();

        // Phase A：纯用户排序规则
        Comparator<GridItemEntry> phaseAComparator = (a, b) -> {
            int cmp = metaComparator.compare(a.resolveMeta(), b.resolveMeta());
            return cmp != 0 ? cmp : Integer.compare(a.entryId(), b.entryId());
        };
        List<GridItemEntry> phaseAOrder = new ArrayList<>(originalEntries);
        phaseAOrder.sort(phaseAComparator);

        GridInventory packedA = tryPackAllEntries(phaseAOrder);
        if (packedA != null) {
            adoptStateFrom(packedA);
            ModLog.debug(String.format(
                    "【拾荒自动整理】Phase A 整理成功 -> 规则=%s, 物品数=%d, 网格=%dx%d",
                    sortOrder.getSerializedName(), this.entriesById.size(), this.columns, this.rows
            ));
            return true;
        }

        // Phase B：大面积优先 + 长边优先 + 用户排序规则（解决小物品卡死大物品问题）
        Comparator<GridItemEntry> phaseBComparator = (a, b) -> {
            ScavengerItemMeta ma = a.resolveMeta();
            ScavengerItemMeta mb = b.resolveMeta();
            int areaCmp = Integer.compare(mb.cellCount(), ma.cellCount());
            if (areaCmp != 0) {
                return areaCmp;
            }
            int maxDimA = Math.max(ma.width(), ma.height());
            int maxDimB = Math.max(mb.width(), mb.height());
            int dimCmp = Integer.compare(maxDimB, maxDimA);
            if (dimCmp != 0) {
                return dimCmp;
            }
            int ruleCmp = metaComparator.compare(ma, mb);
            return ruleCmp != 0 ? ruleCmp : Integer.compare(a.entryId(), b.entryId());
        };
        List<GridItemEntry> phaseBOrder = new ArrayList<>(originalEntries);
        phaseBOrder.sort(phaseBComparator);

        GridInventory packedB = tryPackAllEntries(phaseBOrder);
        if (packedB != null) {
            adoptStateFrom(packedB);
            ModLog.debug(String.format(
                    "【拾荒自动整理】Phase A 碎格受阻，已通过 Phase B(面积优先) 成功整理 -> 规则=%s, 物品数=%d",
                    sortOrder.getSerializedName(), this.entriesById.size()
            ));
            return true;
        }

        ModLog.debug(String.format(
                "【拾荒自动整理】满容几何锁死无法重排，已执行无损原子保留原布局 -> 规则=%s, 物品数=%d",
                sortOrder.getSerializedName(), this.entriesById.size()
        ));
        return false;
    }

    /**
     * 调整网格行列尺寸并无损保留已有物品（用于基地独立仓库升级扩容 {@code 10x9 -> ... -> 55x9}）。
     * 原坐标仍合法的物品严格保留原位置与原 {@code entryId}；若缩容导致部分物品越界则尝试 First-Fit 迁移，无法容纳时无损放弃并返回 {@code false}。
     */
    public boolean resizePreservingItems(int newColumns, int newRows) {
        if (newColumns < 1 || newRows < 1) {
            throw new IllegalArgumentException("新网格尺寸必须 >= 1，当前传入: " + newColumns + "x" + newRows);
        }
        if (newColumns == this.columns && newRows == this.rows) {
            return true;
        }

        GridInventory candidate = new GridInventory(newColumns, newRows, this.extraDepositPolicy);
        candidate.nextEntryId = this.nextEntryId;

        List<GridItemEntry> displaced = new ArrayList<>();
        for (GridItemEntry entry : this.entriesById.values()) {
            ScavengerItemMeta meta = entry.resolveMeta();
            boolean normRot = entry.effectiveRotated();
            if (candidate.canPlace(meta, entry.anchorX(), entry.anchorY(), normRot, EMPTY_CELL)) {
                candidate.putEntryInternal(entry.withPosition(entry.anchorX(), entry.anchorY(), normRot));
            } else {
                displaced.add(entry);
            }
        }

        for (GridItemEntry entry : displaced) {
            if (!candidate.placeFirstFitPreservingId(entry)) {
                ModLog.debug(String.format(
                        "【拾荒网格变维】目标尺寸 %dx%d 无法容纳全部原有物品(卡在 id=%d)，已放弃变维保留原状态",
                        newColumns, newRows, entry.entryId()
                ));
                return false;
            }
        }

        int oldCols = this.columns;
        int oldRows = this.rows;
        this.columns = candidate.columns;
        this.rows = candidate.rows;
        this.occupancy = candidate.occupancy;
        this.entriesById.clear();
        this.entriesById.putAll(candidate.entriesById);
        this.nextEntryId = Math.max(this.nextEntryId, candidate.nextEntryId);

        ModLog.debug(String.format(
                "【拾荒网格变维】成功由 %dx%d 调整为 %dx%d，无损保留 %d 件物品",
                oldCols, oldRows, newColumns, newRows, this.entriesById.size()
        ));
        return true;
    }

    // ==================== 统计计算与不变量自检 ====================

    /**
     * 计算当前网格内所有物品的总重量（kg）。
     */
    public float calculateTotalWeightKg() {
        double sum = 0.0;
        for (GridItemEntry entry : this.entriesById.values()) {
            sum += entry.resolveMeta().weightKg();
        }
        return (float) sum;
    }

    /**
     * 计算当前网格内所有物品的标定基础总价值之和（遵循规则：最终价值恒等于 baseValue）。
     */
    public long calculateTotalBaseValue() {
        long sum = 0L;
        for (GridItemEntry entry : this.entriesById.values()) {
            sum += entry.resolveMeta().baseValue();
        }
        return sum;
    }

    /**
     * 严格校验占用矩阵与条目映射表的一致性不变量（供测试断言与关键事务核验）。
     */
    public void verifyInvariantsOrThrow() {
        if (this.occupancy.length != this.columns * this.rows) {
            throw new IllegalStateException("占用数组长度异常: " + this.occupancy.length + " != " + (this.columns * this.rows));
        }
        int expectedOccupiedCells = 0;
        for (GridItemEntry entry : this.entriesById.values()) {
            if (entry.stack().isEmpty() || entry.stack().getCount() != 1) {
                throw new IllegalStateException("网格条目物品栈必须非空且数量恒为 1, entryId=" + entry.entryId());
            }
            int w = entry.effectiveWidth();
            int h = entry.effectiveHeight();
            if (!isInBounds(entry.anchorX(), entry.anchorY(), w, h)) {
                throw new IllegalStateException("网格条目越界: entryId=" + entry.entryId());
            }
            expectedOccupiedCells += w * h;
            for (int cy = entry.anchorY(); cy < entry.anchorY() + h; cy++) {
                for (int cx = entry.anchorX(); cx < entry.anchorX() + w; cx++) {
                    int actualId = this.occupancy[cellIndex(cx, cy)];
                    if (actualId != entry.entryId()) {
                        throw new IllegalStateException(String.format(
                                "占用矩阵不一致: 坐标(%d,%d) 期望 id=%d, 实际=%d",
                                cx, cy, entry.entryId(), actualId
                        ));
                    }
                }
            }
        }
        int actualOccupied = occupiedCellCount();
        if (actualOccupied != expectedOccupiedCells) {
            throw new IllegalStateException(String.format(
                    "存在幽灵占用格: 矩阵占用=%d, 条目面积总和=%d",
                    actualOccupied, expectedOccupiedCells
            ));
        }
    }

    // ==================== 持久化（ValueInput/ValueOutput & CompoundTag） ====================

    public void saveToValueOutput(ValueOutput output) {
        output.putInt("columns", this.columns);
        output.putInt("rows", this.rows);
        output.putInt("next_entry_id", this.nextEntryId);
        ValueOutput.TypedOutputList<GridItemEntry> list = output.list("entries", GridItemEntry.CODEC);
        for (GridItemEntry entry : this.entriesById.values()) {
            list.add(entry);
        }
    }

    public void loadFromValueInput(ValueInput input, boolean allowSavedDimensions) {
        int targetCols = allowSavedDimensions ? Math.max(1, input.getIntOr("columns", this.columns)) : this.columns;
        int targetRows = allowSavedDimensions ? Math.max(1, input.getIntOr("rows", this.rows)) : this.rows;
        int savedNextId = Math.max(1, input.getIntOr("next_entry_id", 1));

        List<GridItemEntry> decoded = new ArrayList<>();
        for (GridItemEntry entry : input.listOrEmpty("entries", GridItemEntry.CODEC)) {
            if (!entry.stack().isEmpty()) {
                decoded.add(entry);
            }
        }
        restoreEntriesSafely(targetCols, targetRows, savedNextId, decoded);
    }

    public CompoundTag saveToTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("columns", this.columns);
        tag.putInt("rows", this.rows);
        tag.putInt("next_entry_id", this.nextEntryId);
        ListTag entriesTag = new ListTag();
        for (GridItemEntry entry : this.entriesById.values()) {
            entriesTag.add(entry.toTag(registries));
        }
        tag.put("entries", entriesTag);
        return tag;
    }

    public void loadFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        loadFromTag(registries, tag, false);
    }

    public void loadFromTag(HolderLookup.Provider registries, CompoundTag tag, boolean allowSavedDimensions) {
        if (tag == null || tag.isEmpty()) {
            clearAll();
            return;
        }
        int targetCols = allowSavedDimensions ? Math.max(1, tag.getIntOr("columns", this.columns)) : this.columns;
        int targetRows = allowSavedDimensions ? Math.max(1, tag.getIntOr("rows", this.rows)) : this.rows;
        int savedNextId = Math.max(1, tag.getIntOr("next_entry_id", 1));

        List<GridItemEntry> decoded = new ArrayList<>();
        ListTag listTag = tag.getListOrEmpty("entries");
        for (Tag element : listTag) {
            if (element instanceof CompoundTag entryTag) {
                GridItemEntry.fromTag(registries, entryTag).ifPresent(decoded::add);
            }
        }
        restoreEntriesSafely(targetCols, targetRows, savedNextId, decoded);
    }

    /**
     * 取出并清空因数据包热修改物品尺寸导致存档原网格实在塞不下而溢出的物品列表（确保极端改配置下也绝不吞物品）。
     */
    public List<ItemStack> drainLoadOverflow() {
        if (this.loadOverflowBuffer.isEmpty()) {
            return List.of();
        }
        List<ItemStack> drained = new ArrayList<>(this.loadOverflowBuffer);
        this.loadOverflowBuffer.clear();
        return drained;
    }

    // ==================== 内部辅助方法 ====================

    private int cellIndex(int x, int y) {
        return y * this.columns + x;
    }

    private int allocateEntryId() {
        while (this.entriesById.containsKey(this.nextEntryId)) {
            this.nextEntryId++;
        }
        return this.nextEntryId++;
    }

    private void putEntryInternal(GridItemEntry entry) {
        int w = entry.effectiveWidth();
        int h = entry.effectiveHeight();
        for (int cy = entry.anchorY(); cy < entry.anchorY() + h; cy++) {
            int rowOffset = cy * this.columns;
            for (int cx = entry.anchorX(); cx < entry.anchorX() + w; cx++) {
                this.occupancy[rowOffset + cx] = entry.entryId();
            }
        }
        this.entriesById.put(entry.entryId(), entry);
        if (entry.entryId() >= this.nextEntryId) {
            this.nextEntryId = entry.entryId() + 1;
        }
    }

    private @Nullable GridItemEntry removeEntryInternal(int entryId) {
        GridItemEntry existing = this.entriesById.remove(entryId);
        if (existing == null) {
            return null;
        }
        int w = existing.effectiveWidth();
        int h = existing.effectiveHeight();
        for (int cy = existing.anchorY(); cy < existing.anchorY() + h; cy++) {
            int rowOffset = cy * this.columns;
            for (int cx = existing.anchorX(); cx < existing.anchorX() + w; cx++) {
                if (cy >= 0 && cy < this.rows && cx >= 0 && cx < this.columns
                        && this.occupancy[rowOffset + cx] == entryId) {
                    this.occupancy[rowOffset + cx] = EMPTY_CELL;
                }
            }
        }
        return existing;
    }

    private @Nullable GridInventory tryPackAllEntries(List<GridItemEntry> orderedEntries) {
        GridInventory temp = new GridInventory(this.columns, this.rows, this.extraDepositPolicy);
        temp.nextEntryId = this.nextEntryId;
        for (GridItemEntry entry : orderedEntries) {
            if (!temp.placeFirstFitPreservingId(entry)) {
                return null;
            }
        }
        return temp;
    }

    private boolean placeFirstFitPreservingId(GridItemEntry entry) {
        ScavengerItemMeta meta = entry.resolveMeta();
        if (meta.cellCount() > freeCellCount()) {
            return false;
        }
        for (int y = 0; y < this.rows; y++) {
            for (int x = 0; x < this.columns; x++) {
                if (canPlace(meta, x, y, false, EMPTY_CELL)) {
                    putEntryInternal(entry.withPosition(x, y, false));
                    return true;
                }
                if (meta.canRotate() && canPlace(meta, x, y, true, EMPTY_CELL)) {
                    putEntryInternal(entry.withPosition(x, y, true));
                    return true;
                }
            }
        }
        return false;
    }

    private void adoptStateFrom(GridInventory other) {
        this.columns = other.columns;
        this.rows = other.rows;
        this.occupancy = other.occupancy;
        this.entriesById.clear();
        this.entriesById.putAll(other.entriesById);
        this.nextEntryId = Math.max(this.nextEntryId, other.nextEntryId);
    }

    private void restoreEntriesSafely(int targetCols, int targetRows, int savedNextId, List<GridItemEntry> decoded) {
        this.columns = targetCols;
        this.rows = targetRows;
        this.occupancy = new int[targetCols * targetRows];
        Arrays.fill(this.occupancy, EMPTY_CELL);
        this.entriesById.clear();
        this.loadOverflowBuffer.clear();
        this.nextEntryId = Math.max(1, savedNextId);

        List<GridItemEntry> fallbackRelocate = new ArrayList<>();
        Set<Integer> usedIds = new LinkedHashSet<>();

        for (GridItemEntry raw : decoded) {
            int safeId = raw.entryId();
            if (usedIds.contains(safeId)) {
                safeId = allocateEntryId();
            }
            usedIds.add(safeId);
            GridItemEntry normalized = new GridItemEntry(
                    safeId,
                    raw.anchorX(),
                    raw.anchorY(),
                    raw.effectiveRotated(),
                    raw.stack(),
                    raw.solidifiedSearchTicks()
            );
            ScavengerItemMeta meta = normalized.resolveMeta();
            if (canPlace(meta, normalized.anchorX(), normalized.anchorY(), normalized.rotated(), EMPTY_CELL)) {
                putEntryInternal(normalized);
            } else {
                fallbackRelocate.add(normalized);
            }
        }

        for (GridItemEntry displaced : fallbackRelocate) {
            if (placeFirstFitPreservingId(displaced)) {
                ModLog.warn(String.format(
                        "【拾荒网格反序列化】条目 id=%d(%s) 因配置尺寸变更在原坐标(%d,%d)冲突/越界，已自动重排至安全空位",
                        displaced.entryId(), displaced.resolveMeta().itemId(), displaced.anchorX(), displaced.anchorY()
                ));
            } else {
                this.loadOverflowBuffer.add(displaced.copyStack());
                ModLog.warn(String.format(
                        "【拾荒网格反序列化】条目 id=%d(%s) 因配置尺寸增大无法塞回 %dx%d 网格，已转入溢出保护队列防止丢物品",
                        displaced.entryId(), displaced.resolveMeta().itemId(), this.columns, this.rows
                ));
            }
        }
    }
}
