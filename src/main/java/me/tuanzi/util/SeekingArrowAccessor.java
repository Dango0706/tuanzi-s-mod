package me.tuanzi.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface SeekingArrowAccessor {
    void tuanzis_mod$setSeekingTargetEntity(Entity entity);
    Entity tuanzis_mod$getSeekingTargetEntity();
    
    void tuanzis_mod$setSeekingTargetPos(Vec3 pos);
    Vec3 tuanzis_mod$getSeekingTargetPos();
}
