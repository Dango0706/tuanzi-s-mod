package me.tuanzi.integration.voxy;

/**
 * Duck 接口，由 {@code VoxyStateEntryMixin} 实现于 {@code me.cortex.voxy.common.world.other.Mapper$StateEntry}。
 */
public interface VoxyStateEntryAccess {
    int tuanzis_mod$getCustomColor();

    int tuanzis_mod$getCustomTopColor();

    void tuanzis_mod$setCustomColors(int color, int topColor);
}
