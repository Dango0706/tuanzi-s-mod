package me.tuanzi.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WardenExperienceMixin {
    @Shadow protected int lastHurtByPlayerMemoryTime;


    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$boostWardenExperience(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Warden) {
            boolean playerKilled = lastHurtByPlayerMemoryTime > 0;
            if (playerKilled) {
                cir.setReturnValue(275);
            }
        }
    }
}
