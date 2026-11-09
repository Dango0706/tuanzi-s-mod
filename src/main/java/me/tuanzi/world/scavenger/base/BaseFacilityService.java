package me.tuanzi.world.scavenger.base;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.CorridorSpawnManager;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridInventory;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 拾荒回廊玩家私人基地设施（基地整备台与独立仓库柜）权限与升级服务。
 * <p>
 * 规则：
 * <ul>
 *   <li>基地设施严格属于该出生基台的拥有者（基于 {@link CorridorSpawnManager} 分配坐标，OP 管理员豁免）；</li>
 *   <li>升级消耗材料时，自动横跨【随身快捷栏 0..8 + 已穿戴战术背包网格 + 基地独立仓库网格】统计总数并按顺序扣减；</li>
 *   <li>严禁将内部存有物品的非空背包/安全箱容器当作原料扣减。</li>
 * </ul>
 */
public final class BaseFacilityService {

    public static final int BASE_PROTECTION_RADIUS = 32;

    public enum UpgradeResult {
        SUCCESS,
        MAX_LEVEL_REACHED,
        INSUFFICIENT_MATERIALS,
        NO_COST_CONFIGURED
    }

    private BaseFacilityService() {
    }

    /**
     * 校验玩家是否为指定基地设施方块的合法所有者（或具备 OP 权限）。
     */
    public static boolean isAuthorizedOwner(ServerPlayer player, BlockPos facilityPos) {
        if (player.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))) {
            return true;
        }
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos spawnPos = CorridorSpawnManager.get(serverLevel).getOrCreateSpawnPos(player, serverLevel);
        int dx = Math.abs(facilityPos.getX() - spawnPos.getX());
        int dz = Math.abs(facilityPos.getZ() - spawnPos.getZ());
        return dx <= BASE_PROTECTION_RADIUS && dz <= BASE_PROTECTION_RADIUS;
    }

    /**
     * 统计指定材料在玩家【快捷栏 0..8 + 战术背包网格 + 基地独立仓库网格】中的可用总数。
     */
    public static int countAvailableMaterial(
            PlayerCorridorProfile profile,
            NonNullList<ItemStack> hotbar,
            Identifier targetItemId
    ) {
        int count = 0;

        // 1. 快捷栏 0..8
        if (hotbar != null) {
            for (int i = 0; i < Math.min(9, hotbar.size()); i++) {
                ItemStack stack = hotbar.get(i);
                if (isUsableMaterialStack(stack, targetItemId)) {
                    count += stack.getCount();
                }
            }
        }

        // 2. 战术背包网格
        if (profile != null && profile.hasBackpack()) {
            GridInventory bpGrid = profile.backpackGrid();
            if (bpGrid != null) {
                for (GridItemEntry entry : bpGrid.entries()) {
                    if (isUsableMaterialStack(entry.stack(), targetItemId)) {
                        count += entry.stack().getCount();
                    }
                }
            }
        }

        // 3. 基地独立仓库网格
        if (profile != null) {
            GridInventory stash = profile.baseStashGrid();
            for (GridItemEntry entry : stash.entries()) {
                if (isUsableMaterialStack(entry.stack(), targetItemId)) {
                    count += entry.stack().getCount();
                }
            }
        }

        return count;
    }

    /**
     * 尝试执行 8 级负重系统升级。
     */
    public static UpgradeResult tryExecuteWeightUpgrade(
            PlayerCorridorProfile profile,
            NonNullList<ItemStack> hotbar
    ) {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        int curLevel = profile.weightLevel();
        if (curLevel >= cfg.weightProgression().maxLevel()) {
            return UpgradeResult.MAX_LEVEL_REACHED;
        }

        int targetLevel = curLevel + 1;
        List<ScavengerProgressionConfig.UpgradeCostEntry> costs =
                cfg.weightProgression().getCostForNextLevel(targetLevel);

        if (!canAfford(profile, hotbar, costs)) {
            return UpgradeResult.INSUFFICIENT_MATERIALS;
        }

        // 校验通过，原子扣减
        deductCostsAcrossContainers(profile, hotbar, costs);
        profile.upgradeWeightLevel();

        ModLog.debug(String.format(
                "【基地整备升级】成功提升负重等级 Lv.%d -> Lv.%d (消耗材料项数=%d)",
                curLevel, profile.weightLevel(), costs.size()
        ));
        return UpgradeResult.SUCCESS;
    }

    /**
     * 尝试执行 10 级基地独立仓库升级。
     */
    public static UpgradeResult tryExecuteStashUpgrade(
            PlayerCorridorProfile profile,
            NonNullList<ItemStack> hotbar
    ) {
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        int curLevel = profile.stashLevel();
        if (curLevel >= cfg.stashProgression().maxLevel()) {
            return UpgradeResult.MAX_LEVEL_REACHED;
        }

        int targetLevel = curLevel + 1;
        List<ScavengerProgressionConfig.UpgradeCostEntry> costs =
                cfg.stashProgression().getCostForNextLevel(targetLevel);

        if (!canAfford(profile, hotbar, costs)) {
            return UpgradeResult.INSUFFICIENT_MATERIALS;
        }

        // 校验通过，原子扣减
        deductCostsAcrossContainers(profile, hotbar, costs);
        profile.upgradeStashLevel();

        ModLog.debug(String.format(
                "【基地整备升级】成功提升基地仓库等级 Lv.%d -> Lv.%d (当前行数=%d, 消耗材料项数=%d)",
                curLevel, profile.stashLevel(), profile.baseStashGrid().rows(), costs.size()
        ));
        return UpgradeResult.SUCCESS;
    }

    public static boolean canAfford(
            PlayerCorridorProfile profile,
            NonNullList<ItemStack> hotbar,
            List<ScavengerProgressionConfig.UpgradeCostEntry> costs
    ) {
        if (costs == null || costs.isEmpty()) {
            return true;
        }
        for (ScavengerProgressionConfig.UpgradeCostEntry cost : costs) {
            int available = countAvailableMaterial(profile, hotbar, cost.itemId());
            if (available < cost.count()) {
                ModLog.debug(String.format(
                        "【基地升级材料检查】材料不足 -> 需求 %s x%d, 跨容器可用=%d",
                        cost.itemId(), cost.count(), available
                ));
                return false;
            }
        }
        return true;
    }

    private static void deductCostsAcrossContainers(
            PlayerCorridorProfile profile,
            NonNullList<ItemStack> hotbar,
            List<ScavengerProgressionConfig.UpgradeCostEntry> costs
    ) {
        if (costs == null || costs.isEmpty()) {
            return;
        }
        for (ScavengerProgressionConfig.UpgradeCostEntry cost : costs) {
            deductMaterial(profile, hotbar, cost.itemId(), cost.count());
        }
    }

    private static void deductMaterial(
            PlayerCorridorProfile profile,
            NonNullList<ItemStack> hotbar,
            Identifier targetItemId,
            int requiredAmount
    ) {
        int remaining = requiredAmount;

        // 1. 优先从快捷栏扣除
        if (hotbar != null) {
            for (int i = 0; i < Math.min(9, hotbar.size()) && remaining > 0; i++) {
                ItemStack stack = hotbar.get(i);
                if (isUsableMaterialStack(stack, targetItemId)) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                    if (stack.isEmpty()) {
                        hotbar.set(i, ItemStack.EMPTY);
                    }
                }
            }
        }

        // 2. 从战术背包网格扣除
        if (remaining > 0 && profile != null && profile.hasBackpack()) {
            GridInventory bpGrid = profile.backpackGrid();
            if (bpGrid != null) {
                List<Integer> toRemoveIds = new ArrayList<>();
                for (GridItemEntry entry : bpGrid.entries()) {
                    if (remaining <= 0) break;
                    if (isUsableMaterialStack(entry.stack(), targetItemId)) {
                        toRemoveIds.add(entry.entryId());
                        remaining--; // 拾荒网格内每件物品数量恒为 1
                    }
                }
                for (int id : toRemoveIds) {
                    bpGrid.removeEntry(id);
                }
            }
        }

        // 3. 从基地独立仓库网格扣除
        if (remaining > 0 && profile != null) {
            GridInventory stash = profile.baseStashGrid();
            List<Integer> toRemoveIds = new ArrayList<>();
            for (GridItemEntry entry : stash.entries()) {
                if (remaining <= 0) break;
                if (isUsableMaterialStack(entry.stack(), targetItemId)) {
                    toRemoveIds.add(entry.entryId());
                    remaining--;
                }
            }
            for (int id : toRemoveIds) {
                stash.removeEntry(id);
            }
        }

        if (remaining > 0) {
            ModLog.warn("【基地材料扣除异常】扣除后仍有剩余需求未满足: " + targetItemId + " 剩余=" + remaining);
        }
    }

    private static boolean isUsableMaterialStack(ItemStack stack, Identifier targetItemId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        // 排除装有物品的非空背包/安全箱容器道具，防止玩家误把存货连箱带物当原料吞掉
        if (GridInventory.hasNestedGridItems(stack)) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId.equals(targetItemId);
    }
}
