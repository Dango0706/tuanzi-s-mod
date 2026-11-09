package me.tuanzi.mixin;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.team.ScavengerTeam;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityExplosionMixin {

    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$ignoreTeammateWindChargeExplosion(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer targetPlayer) {
            if (!targetPlayer.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
                return;
            }

            Entity directSource = explosion.getDirectSourceEntity();
            LivingEntity indirectSource = explosion.getIndirectSourceEntity();

            Entity attackerEntity = null;
            if (directSource instanceof AbstractWindCharge windCharge) {
                attackerEntity = windCharge.getOwner();
            } else if (indirectSource instanceof ServerPlayer) {
                attackerEntity = indirectSource;
            }

            if (attackerEntity instanceof ServerPlayer attackerPlayer) {
                if (ScavengerTeamManager.areTeammates(attackerPlayer, targetPlayer)) {
                    ScavengerTeam team = ScavengerTeamManager.getTeamByPlayer(targetPlayer);
                    if (team != null) {
                        // 若是风弹爆炸且队伍配置关闭了风弹影响
                        if (directSource instanceof AbstractWindCharge && !team.isWindChargeAffectsTeammates()) {
                            ModLog.debug("【队伍配置】拦截队友风弹爆炸击退: 释放者=" + attackerPlayer.getName().getString() + ", 目标=" + targetPlayer.getName().getString());
                            cir.setReturnValue(true);
                        }
                    }
                }
            }
        }
    }
}
