package me.tuanzi.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class WardenExperienceMixin {

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$boostWardenExperience(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Warden warden) {
            DamageSource lastDamageSource = warden.getLastDamageSource();
            
            if (lastDamageSource != null) {
                var attacker = lastDamageSource.getEntity();
                // 检查是否是被玩家或驯服的狼杀死
                boolean killedByPlayer = attacker instanceof Player;
                boolean killedByTamedWolf = attacker instanceof Wolf wolf && wolf.isTame();
                
                if (killedByPlayer || killedByTamedWolf) {
                    cir.setReturnValue(275);
                }
            }
        }
    }
}
