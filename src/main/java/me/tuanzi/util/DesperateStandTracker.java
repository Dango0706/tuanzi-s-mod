package me.tuanzi.util;

/**
 * 玩家「背水一战」创伤值与延迟结算状态访问接口（由 Player Mixin 实现）。
 */
public interface DesperateStandTracker {
    float tuanzis_mod$getDesperateStandTrauma();

    void tuanzis_mod$setDesperateStandTrauma(float trauma);

    int tuanzis_mod$getDesperateStandSettleTicks();

    void tuanzis_mod$setDesperateStandSettleTicks(int ticks);
}
