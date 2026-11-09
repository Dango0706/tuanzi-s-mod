package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrow {

    protected ThrownTridentMixin(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract ItemStack getWeaponItem();

    @Unique
    private boolean tuanzis_mod$waterSpeedBoosted = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tuanzis_mod$onTickWaterSpeed(CallbackInfo ci) {
        if (this.isInGround()) {
            this.tuanzis_mod$waterSpeedBoosted = false;
            return;
        }

        ItemStack weapon = this.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            var enchRegistry = this.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var waveHolder = enchRegistry.getOrThrow(ModEnchantments.WAVE_LISTENER);
            int level = EnchantmentHelper.getItemEnchantmentLevel(waveHolder, weapon);
            if (level > 0) {
                float boostFactor = 1.0f + 0.15f * level; // 15%, 30%, 45%
                if (this.isInWater()) {
                    if (!this.tuanzis_mod$waterSpeedBoosted) {
                        this.tuanzis_mod$waterSpeedBoosted = true;
                        Vec3 currentMovement = this.getDeltaMovement();
                        this.setDeltaMovement(currentMovement.scale(boostFactor));
                        ModLog.debug(this, "【听涛】三叉戟入水/在水中飞行速度提升 %.0f%% (等级: %d)", 0.15f * level * 100f, level);
                    }
                } else {
                    if (this.tuanzis_mod$waterSpeedBoosted) {
                        this.tuanzis_mod$waterSpeedBoosted = false;
                        Vec3 currentMovement = this.getDeltaMovement();
                        this.setDeltaMovement(currentMovement.scale(1.0f / boostFactor));
                    }
                }
            }
        }
    }
}
