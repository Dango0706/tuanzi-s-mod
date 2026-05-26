package me.tuanzi.mixin;

import me.tuanzi.world.teleport.TeleportManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void tuanzis_mod$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount > 0.0f) {
            TeleportManager.onPlayerDamage((ServerPlayer) (Object) this);
        }
    }
}
