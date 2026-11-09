package me.tuanzi.world.scavenger.loot.container;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * 拾荒回廊通用二维网格搜刮容器接口（方块实体或实体载体均可实现）
 */
public interface IScavengerLootContainer {

    /**
     * 该容器引用的数据驱动容器模板 ID（例如 tuanzis_mod:blackbox_core_case）
     */
    Identifier scavengerContainerDefId();

    /**
     * 该容器实例持有的权威状态管理器
     */
    ScavengerLootContainerState scavengerLootState();

    /**
     * 该容器在世界中的方块位置
     */
    BlockPos containerWorldPos();

    /**
     * 标记容器数据脏标记以触发存档与区块变更
     */
    void markContainerChanged();

    /**
     * 检查容器是否已被破坏或移除
     */
    default boolean isContainerRemoved() {
        return false;
    }
}
