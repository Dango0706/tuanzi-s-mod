package me.tuanzi.world.scavenger.loot.grid;

import org.jspecify.annotations.Nullable;

/**
 * 二维矩形网格放置、平移、旋转或快速互换操作的原子结果描述。
 */
public record GridPlacementResult(
        Status status,
        @Nullable GridItemEntry placedEntry,
        @Nullable GridItemEntry displacedEntry
) {

    public static GridPlacementResult placed(GridItemEntry placedEntry) {
        return new GridPlacementResult(Status.PLACED, placedEntry, null);
    }

    public static GridPlacementResult swapped(GridItemEntry placedEntry, GridItemEntry displacedEntry) {
        return new GridPlacementResult(Status.SWAPPED, placedEntry, displacedEntry);
    }

    public static GridPlacementResult rejected(Status rejectStatus) {
        if (rejectStatus == Status.PLACED || rejectStatus == Status.SWAPPED) {
            throw new IllegalArgumentException("不能将成功状态作为拒绝结果返回: " + rejectStatus);
        }
        return new GridPlacementResult(rejectStatus, null, null);
    }

    public boolean isSuccess() {
        return this.status == Status.PLACED || this.status == Status.SWAPPED;
    }

    public enum Status {
        /**
         * 成功放入空位，或在同网格内无冲突完成平移/旋转
         */
        PLACED,
        /**
         * 单目标快速互换成功：新物品已落位，被覆盖的唯一原物品已通过 displacedEntry 安全弹出
         */
        SWAPPED,
        /**
         * 拒绝：试图放入空 ItemStack
         */
        REJECTED_EMPTY_STACK,
        /**
         * 拒绝：违反容器准入策略（如标签不符，或试图套娃放入非空背包/安全箱）
         */
        REJECTED_POLICY,
        /**
         * 拒绝：锚点坐标或矩形右下角超出网格宽高边界
         */
        REJECTED_OUT_OF_BOUNDS,
        /**
         * 拒绝：目标区域存在碰撞（不允许互换，或同时碰撞了 >= 2 件物品）
         */
        REJECTED_COLLISION,
        /**
         * 拒绝：First-Fit 扫描全部坐标与正反朝向后仍无可用空位
         */
        REJECTED_FULL,
        /**
         * 拒绝：指定的 entryId 在网格中不存在
         */
        REJECTED_NOT_FOUND
    }
}
