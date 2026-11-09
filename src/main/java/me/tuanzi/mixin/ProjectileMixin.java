package me.tuanzi.mixin;

import me.tuanzi.util.ReservedChamberProjectileAccessor;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Projectile.class)
public abstract class ProjectileMixin implements ReservedChamberProjectileAccessor {

    @Unique
    private float tuanzis_mod$reservedChamberDamagePenalty = 0.0f;
    @Unique
    private int tuanzis_mod$reservedChamberLevel = 0;

    @Override
    public void tuanzis_mod$setReservedChamberDamagePenalty(float penalty) {
        this.tuanzis_mod$reservedChamberDamagePenalty = penalty;
    }

    @Override
    public float tuanzis_mod$getReservedChamberDamagePenalty() {
        return this.tuanzis_mod$reservedChamberDamagePenalty;
    }

    @Override
    public void tuanzis_mod$setReservedChamberLevel(int level) {
        this.tuanzis_mod$reservedChamberLevel = level;
    }

    @Override
    public int tuanzis_mod$getReservedChamberLevel() {
        return this.tuanzis_mod$reservedChamberLevel;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "canHitEntity", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void tuanzis_mod$ignoreTeammateHitbox(net.minecraft.world.entity.Entity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        Projectile self = (Projectile) (Object) this;
        net.minecraft.world.entity.Entity owner = self.getOwner();
        if (owner != null && me.tuanzi.world.scavenger.team.ScavengerTeamCollisionHelper.areTeammates(owner, entity)) {
            // 如果是风弹
            if (self instanceof net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge) {
                if (owner instanceof net.minecraft.server.level.ServerPlayer ownerPlayer) {
                    me.tuanzi.world.scavenger.team.ScavengerTeam team = me.tuanzi.world.scavenger.team.ScavengerTeamManager.getTeamByPlayer(ownerPlayer);
                    if (team != null && team.isWindChargeAffectsTeammates()) {
                        return; // 允许击中队友触发击飞
                    }
                }
            }
            // 如果是钓鱼竿
            if (self instanceof net.minecraft.world.entity.projectile.FishingHook) {
                if (owner instanceof net.minecraft.server.level.ServerPlayer ownerPlayer) {
                    me.tuanzi.world.scavenger.team.ScavengerTeam team = me.tuanzi.world.scavenger.team.ScavengerTeamManager.getTeamByPlayer(ownerPlayer);
                    if (team != null && team.isFishingHookAffectsTeammates()) {
                        return; // 允许钩中队友进行拉扯
                    }
                }
            }
            // 其他所有弹射物（弓箭、三叉戟、药水、雪球等）直接穿透队友，不被队友碰撞箱阻挡
            cir.setReturnValue(false);
        }
    }
}
