package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 使主世界的 Level#isInWorldBounds 遵守可热加载的配置建筑高度上限。
 */
@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "isInWorldBounds", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$checkOverworldBuildHeight(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (!OverworldBuildHeightControl.isWithinBuildLimit(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
