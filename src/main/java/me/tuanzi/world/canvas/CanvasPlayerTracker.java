package me.tuanzi.world.canvas;

import net.minecraft.nbt.CompoundTag;

/**
 * 挂接在 ServerPlayer 上的画布空间会话跟踪接口。
 */
public interface CanvasPlayerTracker {
    CompoundTag tuanzis_mod$getCanvasSessionData();

    void tuanzis_mod$setCanvasSessionData(CompoundTag tag);
}
