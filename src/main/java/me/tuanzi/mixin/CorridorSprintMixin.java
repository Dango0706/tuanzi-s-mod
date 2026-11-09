package me.tuanzi.mixin;

import me.tuanzi.world.scavenger.profile.CorridorWeightController;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 拾荒回廊超负荷禁疾跑拦截器：
 * 当玩家处于拾荒回廊且全身负重超过当前等级超负荷线（{@code > W2}）时，强制将 {@code setSprinting(true)} 修正为 {@code false}。
 */
@Mixin(LivingEntity.class)
public abstract class CorridorSprintMixin {

    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean tuanzis_mod$preventSprintWhenOverloaded(boolean isSprinting) {
        if (isSprinting) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (CorridorWeightController.shouldBlockSprinting(self)) {
                return false;
            }
        }
        return isSprinting;
    }
}
