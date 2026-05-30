package me.tuanzi.entity;

import me.tuanzi.init.ModEntities;
import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModStatusEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ShurikenEntity extends ThrowableItemProjectile {

    public ShurikenEntity(EntityType<? extends ShurikenEntity> type, Level level) {
        super(type, level);
    }

    public ShurikenEntity(Level level, LivingEntity owner, ItemStack itemStack) {
        super(ModEntities.SHURIKEN, owner, level, itemStack);
    }

    public ShurikenEntity(Level level, double x, double y, double z, ItemStack itemStack) {
        super(ModEntities.SHURIKEN, x, y, z, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SHURIKEN;
    }

    @Override
    protected double getDefaultGravity() {
        // 无重力下坠
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        // 在服务端运行 5 秒（100 ticks）后自动清除，防实体堆积
        if (!this.level().isClientSide() && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity entity = hitResult.getEntity();
        if (entity instanceof LivingEntity target && !this.level().isClientSide()) {
            float baseDamage = 2.0f;
            DamageSource source = this.damageSources().thrown(this, this.getOwner());
            float finalDamage = me.tuanzi.util.DamageCalculator.calculateDamage(baseDamage, source, target);
            
            // 造成伤害
            target.hurt(source, finalDamage);
            
            MobEffectInstance currentEffect = target.getEffect(ModStatusEffects.SHURIKEN_STUCK);
            int currentCount = currentEffect != null ? currentEffect.getAmplifier() + 1 : 0;
            
            // 20% 概率附加“手里剑嵌入”效果
            if (target.level().getRandom().nextFloat() < 0.20f) {
                int nextCount = Math.min(3, currentCount + 1);
                int nextAmplifier = nextCount - 1;
                target.addEffect(new MobEffectInstance(ModStatusEffects.SHURIKEN_STUCK, 160, nextAmplifier, false, false, true));
            } else if (currentEffect != null) {
                // 再次命中刷新所有嵌入手里剑的持续时间至8秒
                target.addEffect(new MobEffectInstance(ModStatusEffects.SHURIKEN_STUCK, 160, currentEffect.getAmplifier(), false, false, true));
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            // 播放清脆的金属碰撞音效
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.METAL_HIT, SoundSource.NEUTRAL, 0.6F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            
            // 产生十字星碎片粒子
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, 
                    this.getX(), this.getY(), this.getZ(), 8, 0.1, 0.1, 0.1, 0.15);
            }
            
            this.discard();
        }
    }
}
