package me.tuanzi.world.scavenger.profile;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.ScavengerManager;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridInventory;
import me.tuanzi.world.scavenger.loot.grid.GridPlacementResult;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * 拾荒回廊槽位封锁与地面拾取路由规则引擎：
 * <ul>
 *   <li>彻底封锁原版隐藏主物品栏（{@code slots 9..35}）；</li>
 *   <li>快捷栏（{@code slots 0..8}）仅允许放置近战武器/工具，或尺寸为 {@code 1x1} 的非容器道具，且每格最多容纳 1 件；</li>
 *   <li>{@code > 1x1} 战利品（或快捷栏已满时的 {@code 1x1} 战利品）自动 First-Fit 塞入已穿戴的战术背包网格；</li>
 *   <li>若未穿戴战术背包，拾取空战术背包时自动穿戴至背包槽；拾取大尺寸战利品则拒绝拾取保留在地面。</li>
 * </ul>
 */
public final class CorridorInventoryRules {

    public static final int FIRST_BLOCKED_MAIN_SLOT = 9;
    public static final int LAST_BLOCKED_MAIN_SLOT = 35;

    private CorridorInventoryRules() {
    }

    public static boolean isBlockedVanillaMainSlot(int slot) {
        return slot >= FIRST_BLOCKED_MAIN_SLOT && slot <= LAST_BLOCKED_MAIN_SLOT;
    }

    public static boolean shouldInterceptInventory(Player player) {
        if (player == null || player.level() == null) {
            return false;
        }
        if (ScavengerManager.isRestoringSnapshot()) {
            return false;
        }
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return false;
        }
        return !player.isCreative() && !player.isSpectator();
    }

    /**
     * 执行拾荒回廊物品添加路由（纯抽象接口形式，支持真实 {@link Inventory} 与无实体 JUnit 测试调用）。
     *
     * @param targetSlot      目标槽位（{@code -1} 表示自动拾取/自动分配；{@code 0..8} 表示指定快捷栏；{@code 9..35} 恒定拒绝）
     * @param incomingStack   待拾取/添加的物品栈（成功容纳的部分会原地 {@code shrink} 扣减）
     * @param hotbarGetter    读取快捷栏 {@code 0..8} 物品的访问器
     * @param hotbarSetter    写入快捷栏 {@code 0..8} 物品的修改器
     * @param profile         玩家的回廊战术装备档案
     * @param registries      注册表上下文（可为 null）
     * @param random          用于固化新战利品搜索时长的随机源
     * @return 若至少成功接收了 1 件物品则返回 {@code true}，否则原封不动返回 {@code false}
     */
    public static boolean routeItemAddition(
            int targetSlot,
            ItemStack incomingStack,
            IntFunction<ItemStack> hotbarGetter,
            HotkeySlotSetter hotbarSetter,
            @Nullable PlayerCorridorProfile profile,
            HolderLookup.@Nullable Provider registries,
            RandomSource random
    ) {
        if (incomingStack == null || incomingStack.isEmpty()) {
            return false;
        }
        if (isBlockedVanillaMainSlot(targetSlot)) {
            ModLog.debug("【拾荒槽位拦截】拒绝向原版隐藏背包槽位 (" + targetSlot + ") 放入物品: " + incomingStack.getItem());
            return false;
        }

        int initialCount = incomingStack.getCount();
        ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(incomingStack);

        // 情况 A：显式指定了快捷栏槽位 0..8
        if (targetSlot >= 0 && targetSlot < Inventory.SELECTION_SIZE) {
            if (!meta.isAllowedInHotbar(incomingStack)) {
                ModLog.debug("【拾荒槽位拦截】物品尺寸或类别不符合快捷栏准入规则，拒绝放入槽位 " + targetSlot + ": " + meta.itemId());
                return false;
            }
            if (hotbarGetter.apply(targetSlot).isEmpty()) {
                ItemStack single = incomingStack.copyWithCount(1);
                single.setPopTime(Inventory.POP_TIME_DURATION);
                hotbarSetter.set(targetSlot, single);
                incomingStack.shrink(1);
                if (profile != null) {
                    profile.markDirty();
                }
                return true;
            }
            return false;
        }

        // 情况 B：targetSlot == -1（地面自动拾取、指令发放等自动分配）
        if (targetSlot != -1) {
            return false;
        }

        while (!incomingStack.isEmpty()) {
            boolean placedOne = false;

            // 1. 若拾取的是空战术背包且当前未穿戴战术背包 -> 自动穿戴至战术背包槽！
            if (profile != null
                    && !profile.hasBackpack()
                    && !GridInventory.hasNestedGridItems(incomingStack)
                    && PlayerCorridorProfile.resolveEquipmentSpec(incomingStack, EquipmentGridSpec.SlotType.BACKPACK).isPresent()) {
                if (profile.equipBackpack(incomingStack, registries)) {
                    incomingStack.shrink(1);
                    placedOne = true;
                }
            }

            // 2. 若符合快捷栏准入规则（近战武器/工具 或 1x1 非容器物品）-> 优先放入快捷栏 0..8 空位（每格最多 1 件）
            if (!placedOne && meta.isAllowedInHotbar(incomingStack)) {
                for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
                    if (hotbarGetter.apply(slot).isEmpty()) {
                        ItemStack single = incomingStack.copyWithCount(1);
                        single.setPopTime(Inventory.POP_TIME_DURATION);
                        hotbarSetter.set(slot, single);
                        incomingStack.shrink(1);
                        if (profile != null) {
                            profile.markDirty();
                        }
                        placedOne = true;
                        ModLog.debug("【拾荒拾取路由】物品符合快捷栏规则，已放入快捷栏槽位 " + slot + " -> " + meta.itemId());
                        break;
                    }
                }
            }

            // 3. 尝试放入已装备的战术背包二维网格（>1x1 物品唯一自动去向，或快捷栏已满时的 1x1 物品后备去向）
            if (!placedOne && profile != null && profile.hasBackpack()) {
                GridInventory bpGrid = profile.backpackGrid();
                if (bpGrid != null) {
                    int solidifiedTicks = ScavengerDataRegistry.getProgression().computeSolidifiedSearchTicks(meta, random);
                    GridPlacementResult res = bpGrid.quickInsertFirstFit(incomingStack.copyWithCount(1), solidifiedTicks);
                    if (res.isSuccess()) {
                        incomingStack.shrink(1);
                        profile.markDirty();
                        placedOne = true;
                        ModLog.debug("【拾荒拾取路由】物品已自动装入战术背包网格 -> " + meta.itemId());
                    }
                }
            }

            if (!placedOne) {
                ModLog.debug(String.format(
                        "【拾荒拾取路由】无可用快捷栏或战术背包空间(已穿背包=%b)，剩余 %d 件 %s 留在地面。",
                        profile != null && profile.hasBackpack(),
                        incomingStack.getCount(),
                        meta.itemId()
                ));
                break;
            }
        }

        return incomingStack.getCount() < initialCount;
    }

    @FunctionalInterface
    public interface HotkeySlotSetter {
        void set(int slot, ItemStack stack);
    }

    /**
     * 服务端安全巡检：若有外部逻辑绕过 {@link Inventory#add} 直接往 {@code 9..35} 塞入了物品，
     * 自动将其转入快捷栏/战术背包网格，实在塞不下则安全弹出到玩家脚下地面，确保隐藏 27 格永远为空。
     */
    public static void sanitizeHiddenMainInventory(ServerPlayer player, IntConsumer onChangedCallback) {
        if (!shouldInterceptInventory(player)) {
            return;
        }
        Inventory inv = player.getInventory();
        PlayerCorridorProfile profile = (player instanceof ScavengerPlayerTracker tracker)
                ? tracker.tuanzis_mod$getCorridorProfile()
                : null;
        boolean changed = false;

        for (int slot = FIRST_BLOCKED_MAIN_SLOT; slot <= LAST_BLOCKED_MAIN_SLOT; slot++) {
            ItemStack leaked = inv.getItem(slot);
            if (leaked.isEmpty()) {
                continue;
            }
            inv.setItem(slot, ItemStack.EMPTY);
            changed = true;

            routeItemAddition(
                    -1,
                    leaked,
                    inv::getItem,
                    inv::setItem,
                    profile,
                    player.registryAccess(),
                    player.getRandom()
            );

            while (!leaked.isEmpty()) {
                ItemStack singleDrop = leaked.copyWithCount(1);
                leaked.shrink(1);
                ItemEntity dropEntity = player.createItemStackToDrop(singleDrop, false, true);
                if (dropEntity != null) {
                    dropEntity.setDefaultPickUpDelay();
                    player.level().addFreshEntity(dropEntity);
                }
            }
        }

        if (changed && onChangedCallback != null) {
            onChangedCallback.accept(1);
        }
    }
}
