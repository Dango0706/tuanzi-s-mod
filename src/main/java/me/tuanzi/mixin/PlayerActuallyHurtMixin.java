package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.DesperateStandDamageSource;
import me.tuanzi.util.DesperateStandHelper;
import me.tuanzi.util.DesperateStandTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerActuallyHurtMixin implements DesperateStandTracker {

    @Unique
    private float tuanzis_mod$desperateStandTrauma = 0.0f;

    @Unique
    private int tuanzis_mod$desperateStandSettleTicks = 0;

    @Override
    public float tuanzis_mod$getDesperateStandTrauma() {
        return this.tuanzis_mod$desperateStandTrauma;
    }

    @Override
    public void tuanzis_mod$setDesperateStandTrauma(float trauma) {
        this.tuanzis_mod$desperateStandTrauma = Math.max(0.0f, trauma);
    }

    @Override
    public int tuanzis_mod$getDesperateStandSettleTicks() {
        return this.tuanzis_mod$desperateStandSettleTicks;
    }

    @Override
    public void tuanzis_mod$setDesperateStandSettleTicks(int ticks) {
        this.tuanzis_mod$desperateStandSettleTicks = Math.max(0, ticks);
    }

    /**
     * 背水一战最终结算真实伤害：跳过护甲、抗性、伤害吸收与屹立不倒，直接扣除生命值并保留不死图腾/不死药水判定。
     */
    @Inject(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$handleDesperateStandSettlement(ServerLevel level, DamageSource source, float dmg, CallbackInfo ci) {
        if (source instanceof DesperateStandDamageSource) {
            Player player = (Player) (Object) this;
            if (!player.isInvulnerableTo(level, source) && dmg > 0.0F) {
                player.causeFoodExhaustion(source.getFoodExhaustion());
                player.getCombatTracker().recordDamage(source, dmg);
                player.setHealth(player.getHealth() - dmg);
                if (dmg < 3.4028235E37F) {
                    player.awardStat(Stats.DAMAGE_TAKEN, Math.round(dmg * 10.0F));
                }
                player.gameEvent(GameEvent.ENTITY_DAMAGE);
            }
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            argsOnly = true
    )
    private float tuanzis_mod$applyStandingFirmProtection(float dmg, ServerLevel level, DamageSource source) {
        Player player = (Player) (Object) this;
        if (!(source instanceof DesperateStandDamageSource)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && player.hasEffect(ModStatusEffects.DESPERATE_STAND)
                && dmg > 0.0F) {
            DesperateStandHelper.addTrauma(player, this, dmg, source.getMsgId());
            return 0.0F;
        }
        return DamageCalculator.applyStandingFirmProtection(dmg, source, player);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tuanzis_mod$tickDesperateStandSettlement(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.tuanzis_mod$desperateStandSettleTicks > 0) {
            if (player.hasEffect(ModStatusEffects.DESPERATE_STAND)) {
                this.tuanzis_mod$desperateStandSettleTicks = 0;
                return;
            }
            this.tuanzis_mod$desperateStandSettleTicks--;
            if (this.tuanzis_mod$desperateStandSettleTicks == 0) {
                DesperateStandHelper.settleRemainingTrauma(player, serverLevel, this);
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void tuanzis_mod$clearTraumaOnDie(DamageSource source, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide()) {
            this.tuanzis_mod$desperateStandTrauma = 0.0f;
            this.tuanzis_mod$desperateStandSettleTicks = 0;
            DesperateStandHelper.syncToClient(player, 0.0f);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$writeDesperateStandData(ValueOutput output, CallbackInfo ci) {
        output.putFloat("tuanzis_mod$desperate_stand_trauma", this.tuanzis_mod$desperateStandTrauma);
        output.putInt("tuanzis_mod$desperate_stand_settle_ticks", this.tuanzis_mod$desperateStandSettleTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$readDesperateStandData(ValueInput input, CallbackInfo ci) {
        this.tuanzis_mod$desperateStandTrauma = Math.max(0.0f, input.getFloatOr("tuanzis_mod$desperate_stand_trauma", 0.0f));
        this.tuanzis_mod$desperateStandSettleTicks = Math.max(0, input.getIntOr("tuanzis_mod$desperate_stand_settle_ticks", 0));
    }
}
