package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 限制水/熔岩桶在主世界配置建筑高度上限之外倒出流体。
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$checkOverworldBuildLimit(
            @Nullable LivingEntity user,
            Level level,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!OverworldBuildHeightControl.isWithinBuildLimit(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
