package me.tuanzi.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockAttachedEntity.class)
public interface BlockAttachedEntityAccessor {
    @Accessor("pos")
    void setPos(BlockPos pos);

    @Invoker("recalculateBoundingBox")
    void invokeRecalculateBoundingBox();
}
