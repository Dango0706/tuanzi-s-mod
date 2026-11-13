package me.tuanzi.world.scavenger.profile;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridInventory;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.meta.ScavengerCategory;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 玩家在“拾荒回廊”维度内的专属战术装备与成长档案（服务端权威状态）。
 * <p>
 * 负责统一管理：
 * <ul>
 *   <li>战术背包槽（{@code equippedBackpack}）及其内部动态二维网格（死亡拆解掉落，非空严禁卸下）；</li>
 *   <li>黑匣安全箱槽（{@code equippedSafebox}）及其内部动态二维网格（死亡保留，首次进入发放 1x2 初始箱，非空严禁卸下）；</li>
 *   <li>基地独立仓库二维网格（{@code baseStashGrid}，固定 9 列，Lv.1 起始 10 行，满级 Lv.10 达 55 行）；</li>
 *   <li>8 级负重成长等级（{@code weightLevel: 1..8}）与 10 级仓库成长等级（{@code stashLevel: 1..10}）。</li>
 * </ul>
 */
public class PlayerCorridorProfile {

    /**
     * 首次进入拾荒回廊自动发放的初始默认 1x2 安全箱物品注册 ID
     */
    public static final Identifier DEFAULT_STARTER_SAFEBOX_ID =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "proto_lockbox_1x2");
    private final GridInventory baseStashGrid;
    private ItemStack equippedBackpack = ItemStack.EMPTY;
    private @Nullable GridInventory backpackGrid = null;
    private ItemStack equippedSafebox = ItemStack.EMPTY;
    private @Nullable GridInventory safeboxGrid = null;
    private int weightLevel = 1;
    private int stashLevel = 1;
    private boolean starterSafeboxGranted = false;
    private boolean dirty = true;

    public PlayerCorridorProfile() {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        this.baseStashGrid = new GridInventory(cfg.getStashColumns(), cfg.getStashRows(1));
    }

    // ==================== 基础状态访问 ====================

    /**
     * 判断指定物品是否为合法的战术背包装备道具。
     */
    public static Optional<EquipmentGridSpec> resolveEquipmentSpec(
            ItemStack stack,
            EquipmentGridSpec.SlotType expectedType
    ) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(stack);
        if (meta.equipmentGrid().isPresent() && meta.equipmentGrid().get().slotType() == expectedType) {
            return meta.equipmentGrid();
        }
        return Optional.empty();
    }

    public ItemStack equippedBackpack() {
        return this.equippedBackpack;
    }

    public boolean hasBackpack() {
        return !this.equippedBackpack.isEmpty() && this.backpackGrid != null;
    }

    public @Nullable GridInventory backpackGrid() {
        return this.backpackGrid;
    }

    public ItemStack equippedSafebox() {
        return this.equippedSafebox;
    }

    public boolean hasSafebox() {
        return !this.equippedSafebox.isEmpty() && this.safeboxGrid != null;
    }

    public @Nullable GridInventory safeboxGrid() {
        return this.safeboxGrid;
    }

    public GridInventory baseStashGrid() {
        return this.baseStashGrid;
    }

    public int weightLevel() {
        return this.weightLevel;
    }

    public int stashLevel() {
        return this.stashLevel;
    }

    public boolean isStarterSafeboxGranted() {
        return this.starterSafeboxGranted;
    }

    public void markDirty() {
        this.dirty = true;
    }

    // ==================== 战术背包穿卸逻辑 ====================

    public boolean consumeDirty() {
        if (this.dirty) {
            this.dirty = false;
            return true;
        }
        return false;
    }

    /**
     * 在当前战术背包槽为空时穿戴新战术背包，并根据其 {@link EquipmentGridSpec} 初始化内部网格。
     */
    public boolean equipBackpack(ItemStack candidate, HolderLookup.@Nullable Provider registries) {
        if (!this.equippedBackpack.isEmpty()) {
            ModLog.debug("【拾荒装备档案】战术背包槽已被占用，拒绝直接覆盖穿戴。");
            return false;
        }
        Optional<EquipmentGridSpec> specOpt = resolveEquipmentSpec(candidate, EquipmentGridSpec.SlotType.BACKPACK);
        if (specOpt.isEmpty()) {
            ModLog.debug("【拾荒装备档案】物品并非合法的战术背包规格，拒绝穿戴: " + (candidate == null ? "null" : candidate.getItem()));
            return false;
        }

        EquipmentGridSpec spec = specOpt.get();
        ItemStack copy = candidate.copyWithCount(1);
        GridInventory loadedGrid = registries != null
                ? GridInventory.readFromContainerStack(copy, spec.columns(), spec.rows(), registries)
                : new GridInventory(spec.columns(), spec.rows());
        // 运行态由 backpackGrid 权威托管，清理外层副本冗余键防止状态分叉
        if (registries != null) {
            GridInventory.writeToContainerStack(copy, null, registries);
        }

        this.equippedBackpack = copy;
        this.backpackGrid = loadedGrid;
        markDirty();

        ModLog.debug(String.format(
                "【拾荒装备档案】成功装备战术背包 -> 物品=%s, 内部网格=%dx%d, 已含物品=%d件",
                ScavengerDataRegistry.resolveItemMeta(copy).itemId(),
                spec.columns(), spec.rows(), loadedGrid.entryCount()
        ));
        return true;
    }

    /**
     * 卸下当前装备的战术背包（严格执行“非空禁卸”规则：若内部网格存在任何物品则拒绝卸下）。
     */
    public Optional<ItemStack> unequipBackpack(HolderLookup.@Nullable Provider registries) {
        if (this.equippedBackpack.isEmpty()) {
            return Optional.empty();
        }
        if (this.backpackGrid != null && !this.backpackGrid.isEmpty()) {
            ModLog.debug("【拾荒装备档案】拒绝卸下非空战术背包，当前内部剩余物品数=" + this.backpackGrid.entryCount());
            return Optional.empty();
        }

        ItemStack out = this.equippedBackpack.copyWithCount(1);
        if (registries != null) {
            GridInventory.writeToContainerStack(out, null, registries);
        }
        this.equippedBackpack = ItemStack.EMPTY;
        this.backpackGrid = null;
        markDirty();

        ModLog.debug("【拾荒装备档案】已成功卸下空战术背包: " + ScavengerDataRegistry.resolveItemMeta(out).itemId());
        return Optional.of(out);
    }

    // ==================== 安全箱穿卸与初始发放逻辑 ====================

    /**
     * 在当前安全箱槽为空时穿戴安全箱，并根据其 {@link EquipmentGridSpec} 初始化内部网格。
     */
    public boolean equipSafebox(ItemStack candidate, HolderLookup.@Nullable Provider registries) {
        if (!this.equippedSafebox.isEmpty()) {
            ModLog.debug("【拾荒装备档案】安全箱槽已被占用，拒绝直接覆盖穿戴。");
            return false;
        }
        Optional<EquipmentGridSpec> specOpt = resolveEquipmentSpec(candidate, EquipmentGridSpec.SlotType.SAFEBOX);
        if (specOpt.isEmpty()) {
            ModLog.debug("【拾荒装备档案】物品并非合法的安全箱规格，拒绝穿戴: " + (candidate == null ? "null" : candidate.getItem()));
            return false;
        }

        EquipmentGridSpec spec = specOpt.get();
        ItemStack copy = candidate.copyWithCount(1);
        GridInventory loadedGrid = registries != null
                ? GridInventory.readFromContainerStack(copy, spec.columns(), spec.rows(), registries)
                : new GridInventory(spec.columns(), spec.rows());
        if (registries != null) {
            GridInventory.writeToContainerStack(copy, null, registries);
        }
        // 安全箱本身同样禁止塞入其他安全箱或背包容器（若未来加额外限制可扩展 extraDepositPolicy）
        loadedGrid.setExtraDepositPolicy(stack -> {
            ScavengerItemMeta m = ScavengerDataRegistry.resolveItemMeta(stack);
            return m.category() != ScavengerCategory.BACKPACK
                    && m.category() != ScavengerCategory.SAFEBOX
                    && m.equipmentGrid().isEmpty();
        });

        this.equippedSafebox = copy;
        this.safeboxGrid = loadedGrid;
        markDirty();

        ModLog.debug(String.format(
                "【拾荒装备档案】成功装备安全箱 -> 物品=%s, 内部网格=%dx%d",
                ScavengerDataRegistry.resolveItemMeta(copy).itemId(),
                spec.columns(), spec.rows()
        ));
        return true;
    }

    /**
     * 卸下当前装备的安全箱（严格执行“非空禁卸”规则）。
     */
    public Optional<ItemStack> unequipSafebox(HolderLookup.@Nullable Provider registries) {
        if (this.equippedSafebox.isEmpty()) {
            return Optional.empty();
        }
        if (this.safeboxGrid != null && !this.safeboxGrid.isEmpty()) {
            ModLog.debug("【拾荒装备档案】拒绝卸下非空安全箱，当前内部剩余物品数=" + this.safeboxGrid.entryCount());
            return Optional.empty();
        }

        ItemStack out = this.equippedSafebox.copyWithCount(1);
        if (registries != null) {
            GridInventory.writeToContainerStack(out, null, registries);
        }
        this.equippedSafebox = ItemStack.EMPTY;
        this.safeboxGrid = null;
        markDirty();

        ModLog.debug("【拾荒装备档案】已成功卸下空安全箱: " + ScavengerDataRegistry.resolveItemMeta(out).itemId());
        return Optional.of(out);
    }

    /**
     * 首次进入拾荒回廊时尝试自动发放默认 1x2 初始安全箱（{@code proto_lockbox_1x2}）。
     * 若对应物品已在注册表中就绪则立即装备并标记已发放。
     */
    public boolean tryGrantStarterSafeboxIfEligible(HolderLookup.@Nullable Provider registries) {
        if (this.starterSafeboxGranted) {
            return false;
        }
        if (!BuiltInRegistries.ITEM.containsKey(DEFAULT_STARTER_SAFEBOX_ID)) {
            ModLog.debug("【拾荒装备档案】默认初始安全箱物品尚未注册 (" + DEFAULT_STARTER_SAFEBOX_ID + ")，留待注册后自动补发。");
            return false;
        }
        Item starterItem = BuiltInRegistries.ITEM.getValue(DEFAULT_STARTER_SAFEBOX_ID);
        ItemStack starterStack = new ItemStack(starterItem);
        return grantStarterSafeboxWithStack(starterStack, registries);
    }

    /**
     * 使用指定的初始安全箱物品执行首次发放（若安全箱槽当前为空则直接装备，即使数据包未配置也保底提供 1x2 网格）。
     */
    public boolean grantStarterSafeboxWithStack(ItemStack starterStack, HolderLookup.@Nullable Provider registries) {
        if (this.starterSafeboxGranted || starterStack == null || starterStack.isEmpty()) {
            return false;
        }
        if (this.equippedSafebox.isEmpty()) {
            if (!equipSafebox(starterStack, registries)) {
                // 兜底：若数据包尚未写入该初始箱的 equipment_grid，强制按设计规范创建 1x2 安全箱网格
                this.equippedSafebox = starterStack.copyWithCount(1);
                this.safeboxGrid = new GridInventory(1, 2, stack -> {
                    ScavengerItemMeta m = ScavengerDataRegistry.resolveItemMeta(stack);
                    return m.category() != ScavengerCategory.BACKPACK
                            && m.category() != ScavengerCategory.SAFEBOX
                            && m.equipmentGrid().isEmpty();
                });
            }
            this.starterSafeboxGranted = true;
            markDirty();
            ModLog.debug("【拾荒装备档案】已为初入回廊玩家发放并穿戴初始 1x2 安全箱: "
                    + BuiltInRegistries.ITEM.getKey(starterStack.getItem()));
            return true;
        }
        this.starterSafeboxGranted = true;
        return false;
    }

    // ==================== 8级负重与 10级基地仓库成长 ====================

    public boolean upgradeWeightLevel() {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        int maxLv = cfg.weightProgression().maxLevel();
        if (this.weightLevel >= maxLv) {
            ModLog.debug("【拾荒成长】负重等级已达上限 Lv." + maxLv + "，无法继续升级。");
            return false;
        }
        int oldLv = this.weightLevel;
        this.weightLevel = cfg.clampWeightLevel(oldLv + 1);
        markDirty();
        ModLog.debug(String.format(
                "【拾荒成长】负重等级提升 Lv.%d -> Lv.%d (超重线=%.1fkg, 超负荷线=%.1fkg)",
                oldLv, this.weightLevel,
                cfg.getOverweightThresholdKg(this.weightLevel),
                cfg.getOverloadedThresholdKg(this.weightLevel)
        ));
        return true;
    }

    public void setWeightLevel(int targetLevel) {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        this.weightLevel = cfg.clampWeightLevel(targetLevel);
        markDirty();
    }

    public boolean upgradeStashLevel() {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        int maxLv = cfg.stashProgression().maxLevel();
        if (this.stashLevel >= maxLv) {
            ModLog.debug("【拾荒成长】基地独立仓库等级已达上限 Lv." + maxLv + "，无法继续升级。");
            return false;
        }
        int nextLv = cfg.clampStashLevel(this.stashLevel + 1);
        int targetCols = cfg.getStashColumns();
        int targetRows = cfg.getStashRows(nextLv);
        if (!this.baseStashGrid.resizePreservingItems(targetCols, targetRows)) {
            ModLog.warn("【拾荒成长】基地仓库尝试扩容至 " + targetCols + "x" + targetRows + " 失败，已中止升级。");
            return false;
        }
        int oldLv = this.stashLevel;
        this.stashLevel = nextLv;
        markDirty();
        ModLog.debug(String.format(
                "【拾荒成长】基地独立仓库升级 Lv.%d -> Lv.%d，当前容量=%d列x%d行 (%d格)",
                oldLv, this.stashLevel, targetCols, targetRows, targetCols * targetRows
        ));
        return true;
    }

    public void setStashLevel(int targetLevel) {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        int clamped = cfg.clampStashLevel(targetLevel);
        int cols = cfg.getStashColumns();
        int rows = cfg.getStashRows(clamped);
        if (this.baseStashGrid.resizePreservingItems(cols, rows)) {
            this.stashLevel = clamped;
            markDirty();
        }
    }

    /**
     * 在数据包 /reload 或存档恢复后，将基地仓库行列尺寸对齐至当前等级配置（若配置调小但已有物品占满，则保留原尺寸绝不吞物品）。
     */
    public void syncEquipmentAndStashDimensionsWithConfig() {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        this.weightLevel = cfg.clampWeightLevel(this.weightLevel);
        this.stashLevel = cfg.clampStashLevel(this.stashLevel);
        int expectedCols = cfg.getStashColumns();
        int expectedRows = cfg.getStashRows(this.stashLevel);
        if (this.baseStashGrid.columns() != expectedCols || this.baseStashGrid.rows() != expectedRows) {
            this.baseStashGrid.resizePreservingItems(expectedCols, expectedRows);
        }

        if (!this.equippedBackpack.isEmpty() && this.backpackGrid != null) {
            resolveEquipmentSpec(this.equippedBackpack, EquipmentGridSpec.SlotType.BACKPACK).ifPresent(spec -> {
                if (this.backpackGrid.columns() != spec.columns() || this.backpackGrid.rows() != spec.rows()) {
                    this.backpackGrid.resizePreservingItems(spec.columns(), spec.rows());
                }
            });
        }
        if (!this.equippedSafebox.isEmpty() && this.safeboxGrid != null) {
            resolveEquipmentSpec(this.equippedSafebox, EquipmentGridSpec.SlotType.SAFEBOX).ifPresent(spec -> {
                if (this.safeboxGrid.columns() != spec.columns() || this.safeboxGrid.rows() != spec.rows()) {
                    this.safeboxGrid.resizePreservingItems(spec.columns(), spec.rows());
                }
            });
        }
        markDirty();
    }

    // ==================== 回廊死亡拆解掉落（保留安全箱与基地仓库） ====================

    /**
     * 在拾荒回廊死亡时拆解战术背包：
     * 提取背包内全部战利品单件 + 清空内部数据后的空战术背包本体作为地面爆落物返回，并清空背包槽；
     * 安全箱槽及其内部全部战利品、基地仓库、负重/仓库等级均 100% 保留。
     */
    public List<ItemStack> unpackAndClearBackpackForDeath(HolderLookup.@Nullable Provider registries) {
        List<ItemStack> drops = new ArrayList<>();
        if (this.equippedBackpack.isEmpty()) {
            return drops;
        }

        int unpackedCount = 0;
        if (this.backpackGrid != null) {
            for (GridItemEntry entry : this.backpackGrid.clearAll()) {
                ItemStack stack = entry.copyStack();
                if (!stack.isEmpty()) {
                    drops.add(stack);
                    unpackedCount++;
                }
            }
        }

        ItemStack emptyBackpackDrop = this.equippedBackpack.copyWithCount(1);
        if (registries != null) {
            GridInventory.writeToContainerStack(emptyBackpackDrop, null, registries);
        }
        drops.add(emptyBackpackDrop);

        this.equippedBackpack = ItemStack.EMPTY;
        this.backpackGrid = null;
        markDirty();

        int keptInSafebox = this.safeboxGrid != null ? this.safeboxGrid.entryCount() : 0;
        ModLog.debug(String.format(
                "【拾荒死亡结算】战术背包已拆解爆落 -> 散落内部战利品=%d件 + 空背包本体=1件；安全箱无损保留物品=%d件",
                unpackedCount, keptInSafebox
        ));
        return drops;
    }

    // ==================== NBT 序列化与重生深拷贝 ====================

    public CompoundTag saveToTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("WeightLevel", this.weightLevel);
        tag.putInt("StashLevel", this.stashLevel);
        tag.putBoolean("StarterSafeboxGranted", this.starterSafeboxGranted);

        if (!this.equippedBackpack.isEmpty()) {
            tag.put("EquippedBackpack", ItemStackNbtHelper.write(registries, this.equippedBackpack));
            if (this.backpackGrid != null) {
                tag.put("BackpackGrid", this.backpackGrid.saveToTag(registries));
            }
        }

        if (!this.equippedSafebox.isEmpty()) {
            tag.put("EquippedSafebox", ItemStackNbtHelper.write(registries, this.equippedSafebox));
            if (this.safeboxGrid != null) {
                tag.put("SafeboxGrid", this.safeboxGrid.saveToTag(registries));
            }
        }

        tag.put("BaseStashGrid", this.baseStashGrid.saveToTag(registries));
        return tag;
    }

    public void loadFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        this.equippedBackpack = ItemStack.EMPTY;
        this.backpackGrid = null;
        this.equippedSafebox = ItemStack.EMPTY;
        this.safeboxGrid = null;

        if (tag == null || tag.isEmpty()) {
            ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
            this.weightLevel = 1;
            this.stashLevel = 1;
            this.starterSafeboxGranted = false;
            this.baseStashGrid.clearAll();
            this.baseStashGrid.resizePreservingItems(cfg.getStashColumns(), cfg.getStashRows(1));
            markDirty();
            return;
        }

        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        this.weightLevel = cfg.clampWeightLevel(tag.getIntOr("WeightLevel", 1));
        this.stashLevel = cfg.clampStashLevel(tag.getIntOr("StashLevel", 1));
        this.starterSafeboxGranted = tag.getBooleanOr("StarterSafeboxGranted", false);

        if (tag.contains("EquippedBackpack")) {
            ItemStack bpStack = ItemStackNbtHelper.read(registries, tag.getCompoundOrEmpty("EquippedBackpack"));
            if (!bpStack.isEmpty()) {
                this.equippedBackpack = bpStack.copyWithCount(1);
                CompoundTag bpGridTag = tag.getCompoundOrEmpty("BackpackGrid");
                Optional<EquipmentGridSpec> spec = resolveEquipmentSpec(this.equippedBackpack, EquipmentGridSpec.SlotType.BACKPACK);
                int cols = spec.map(EquipmentGridSpec::columns).orElseGet(() -> bpGridTag.getIntOr("columns", 4));
                int rows = spec.map(EquipmentGridSpec::rows).orElseGet(() -> bpGridTag.getIntOr("rows", 4));
                this.backpackGrid = new GridInventory(Math.max(1, cols), Math.max(1, rows));
                this.backpackGrid.loadFromTag(registries, bpGridTag, spec.isEmpty());
            }
        }

        if (tag.contains("EquippedSafebox")) {
            ItemStack sbStack = ItemStackNbtHelper.read(registries, tag.getCompoundOrEmpty("EquippedSafebox"));
            if (!sbStack.isEmpty()) {
                this.equippedSafebox = sbStack.copyWithCount(1);
                CompoundTag sbGridTag = tag.getCompoundOrEmpty("SafeboxGrid");
                Optional<EquipmentGridSpec> spec = resolveEquipmentSpec(this.equippedSafebox, EquipmentGridSpec.SlotType.SAFEBOX);
                int cols = spec.map(EquipmentGridSpec::columns).orElseGet(() -> sbGridTag.getIntOr("columns", 1));
                int rows = spec.map(EquipmentGridSpec::rows).orElseGet(() -> sbGridTag.getIntOr("rows", 2));
                this.safeboxGrid = new GridInventory(Math.max(1, cols), Math.max(1, rows), stack -> {
                    ScavengerItemMeta m = ScavengerDataRegistry.resolveItemMeta(stack);
                    return m.category() != ScavengerCategory.BACKPACK
                            && m.category() != ScavengerCategory.SAFEBOX
                            && m.equipmentGrid().isEmpty();
                });
                this.safeboxGrid.loadFromTag(registries, sbGridTag, spec.isEmpty());
            }
        }

        if (tag.contains("BaseStashGrid")) {
            this.baseStashGrid.loadFromTag(registries, tag.getCompoundOrEmpty("BaseStashGrid"), true);
        } else {
            this.baseStashGrid.clearAll();
        }
        syncEquipmentAndStashDimensionsWithConfig();
    }

    public void copyFrom(PlayerCorridorProfile other, HolderLookup.Provider registries) {
        CompoundTag serialized = other.saveToTag(registries);
        this.loadFromTag(registries, serialized);
    }
}
