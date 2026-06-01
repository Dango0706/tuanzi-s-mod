package me.tuanzi.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class WardenExperienceMixin {

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$boostWardenExperience(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Warden warden) {
            boolean playerKilled = ((LivingEntityAccessor) this).getTuanziLastHurtByPlayerMemoryTime() > 0;
            if (playerKilled) {
                cir.setReturnValue(275);
            }
        }
    }
}
