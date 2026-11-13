package me.tuanzi.mixin;

import me.tuanzi.util.DamageCalculator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerActuallyHurtMixin {

    @ModifyVariable(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            argsOnly = true
    )
    private float tuanzis_mod$applyStandingFirmProtection(float dmg, ServerLevel level, DamageSource source) {
        Player player = (Player) (Object) this;
        return DamageCalculator.applyStandingFirmProtection(dmg, source, player);
    }
}
