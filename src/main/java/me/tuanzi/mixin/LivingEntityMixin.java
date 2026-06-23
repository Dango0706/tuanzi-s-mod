package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements me.tuanzi.util.RhythmTracker {

    @Shadow
    protected abstract void onEffectsRemoved(Collection<MobEffectInstance> effects);

    @org.spongepowered.asm.mixin.Unique
    private java.util.UUID tuanzis_mod$lastRhythmTargetUuid = null;

    @Override
    public java.util.UUID tuanzis_mod$getLastRhythmTarget() {
        return this.tuanzis_mod$lastRhythmTargetUuid;
    }

    @Override
    public void tuanzis_mod$setLastRhythmTarget(java.util.UUID uuid) {
        this.tuanzis_mod$lastRhythmTargetUuid = uuid;
    }

    @org.spongepowered.asm.mixin.Unique
    private int tuanzis_mod$tearingTicks = 0;
    @org.spongepowered.asm.mixin.Unique
    private double tuanzis_mod$tearingDistance = 0.0;
    @org.spongepowered.asm.mixin.Unique
    private double tuanzis_mod$persistentX = 0.0;
    @org.spongepowered.asm.mixin.Unique
    private double tuanzis_mod$persistentY = 0.0;
    @org.spongepowered.asm.mixin.Unique
    private double tuanzis_mod$persistentZ = 0.0;
    @org.spongepowered.asm.mixin.Unique
    private boolean tuanzis_mod$hasPersistentPos = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickTearingDamage(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return;

        if (entity.hasEffect(me.tuanzi.init.ModStatusEffects.TEARING)) {
            tuanzis_mod$tearingTicks++;
            double dx = 0.0;
            double dy = 0.0;
            double dz = 0.0;
            if (tuanzis_mod$hasPersistentPos) {
                dx = entity.getX() - tuanzis_mod$persistentX;
                dy = entity.getY() - tuanzis_mod$persistentY;
                dz = entity.getZ() - tuanzis_mod$persistentZ;
            } else {
                dx = entity.getX() - entity.xo;
                dy = entity.getY() - entity.yo;
                dz = entity.getZ() - entity.zo;
            }
            tuanzis_mod$tearingDistance += Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (tuanzis_mod$tearingTicks >= 10) {
                if (tuanzis_mod$tearingDistance > 0.05) {
                    var effectInstance = entity.getEffect(me.tuanzi.init.ModStatusEffects.TEARING);
                    if (effectInstance != null) {
                        int level = effectInstance.getAmplifier() + 1;
                        float damageAmount = 0.5f * level;
                        entity.hurt(entity.damageSources().magic(), damageAmount);
                    }
                }
                tuanzis_mod$tearingTicks = 0;
                tuanzis_mod$tearingDistance = 0.0;
            }
        } else {
            tuanzis_mod$tearingTicks = 0;
            tuanzis_mod$tearingDistance = 0.0;
        }

        // 手里剑嵌入状态在身上产生小型手里剑（十字星）粒子符号
        if (entity.hasEffect(me.tuanzi.init.ModStatusEffects.SHURIKEN_STUCK)) {
            if (entity.tickCount % 4 == 0) {
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, 
                        entity.getRandomX(0.5), entity.getRandomY(), entity.getRandomZ(0.5), 
                        1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        // 无论是何种状态，在每一 tick 最终结算时，强制将当前最新坐标锁定为跨 tick 持久化对比坐标
        tuanzis_mod$persistentX = entity.getX();
        tuanzis_mod$persistentY = entity.getY();
        tuanzis_mod$persistentZ = entity.getZ();
        tuanzis_mod$hasPersistentPos = true;

        // 潮汐织靴水面足迹粒子效果
        if (!entity.level().isClientSide() && entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(me.tuanzi.init.ModItems.TIDAL_WEAVE_BOOTS)) {
            if (entity.level().getFluidState(entity.blockPosition()).is(net.minecraft.tags.FluidTags.WATER) 
                    || entity.level().getFluidState(entity.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER)) {
                double horizontalSpeed = entity.getDeltaMovement().horizontalDistanceSqr();
                if (horizontalSpeed > 0.001 && entity.tickCount % 3 == 0) {
                    if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, 
                            entity.getX(), entity.getY(), entity.getZ(), 
                            2, 0.1, 0.0, 0.1, 0.02);
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, 
                            entity.getX(), entity.getY(), entity.getZ(), 
                            3, 0.1, 0.0, 0.1, 0.01);
                    }
                }
            }
        }
    }

    /**
     * 当实体移除状态效果时（不管是到期自动移除，还是被其他逻辑清除），
     * 如果“肾上腺素”Buff被移除，且“肾上腺素透支”效果依然存在，就会在这里拦截并触发负面效果。
     */
    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void tuanzis_mod$onEffectsRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return;

        for (MobEffectInstance effect : effects) {
            if (effect.getEffect().equals(ModStatusEffects.SHURIKEN_STUCK)) {
                // 如果手里剑效果被彻底移除（不是被更新，更新时 entity.getEffect 还会存在新的实例）
                if (entity.getEffect(ModStatusEffects.SHURIKEN_STUCK) == null) {
                    int count = effect.getAmplifier() + 1;
                    if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            serverLevel, entity.getX(), entity.getY(), entity.getZ(), 
                            new net.minecraft.world.item.ItemStack(me.tuanzi.init.ModItems.SHURIKEN, count)
                        );
                        itemEntity.setDefaultPickUpDelay();
                        serverLevel.addFreshEntity(itemEntity);
                    }
                }
            }

            if (effect.getEffect().equals(ModStatusEffects.ADRENALINE)) {
                // 肾上腺素药水效果结束时
                MobEffectInstance overdraw = entity.getEffect(ModStatusEffects.ADRENALINE_OVERDRAW);
                if (overdraw != null) {
                    // 根据是否仍处于透支状态触发副作用！
                    // “每级叠加30秒虚弱I + 挖掘疲劳I，最高叠加至IV级”
                    // overdraw.getAmplifier() 范围为 0 至 3 (即 I 至 IV 级)
                    int level = overdraw.getAmplifier() + 1;
                    int sideEffectTicks = level * 30 * 20; // 30秒/级

                    // 虚弱 I 的 amplifier 是 0，挖掘疲劳 I 的 amplifier 是 0
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, sideEffectTicks, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, sideEffectTicks, 0));

                    // 播放虚脱副作用音效
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                        net.minecraft.sounds.SoundEvents.WITHER_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);

                    me.tuanzi.util.ModLog.debug(entity, null, "肾上腺素药水效果结束：透支副作用触发！已获得 " + (level * 30) + " 秒的虚弱 I 与挖掘疲劳 I（透支等级: " + level + " 级）。");

                    // 移除肾上腺素透支标识，以防止重复触发
                    entity.removeEffect(ModStatusEffects.ADRENALINE_OVERDRAW);
                }
            }
        }
    }

    /**
     * 拦截 removeAllEffects 中的 Map.clear() 逻辑。
     * 当喝牛奶清除所有状态效果时，主动把“肾上腺素透支”效果强行保留，不被清空。
     */
    @Redirect(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void tuanzis_mod$clearActiveEffects(Map<Holder<MobEffect>, MobEffectInstance> map) {
        MobEffectInstance overdrawInstance = map.get(ModStatusEffects.ADRENALINE_OVERDRAW);
        map.clear();
        if (overdrawInstance != null) {
            map.put(ModStatusEffects.ADRENALINE_OVERDRAW, overdrawInstance);
        }
    }

    /**
     * 拦截 removeAllEffects 中的 onEffectsRemoved 调用。
     * 把保留在 activeEffects 中的“肾上腺素透支”效果从已删除效果的 copy.values() 列表中过滤掉，
     * 这样系统就不会向客户端网络发送“该效果被移除”的包，在世界中完美维持它。
     */
    @Redirect(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onEffectsRemoved(Ljava/util/Collection;)V"))
    private void tuanzis_mod$onEffectsRemovedRedirect(LivingEntity entity, Collection<MobEffectInstance> effects) {
        List<MobEffectInstance> list = new ArrayList<>(effects);
        list.removeIf(instance -> instance.getEffect().equals(ModStatusEffects.ADRENALINE_OVERDRAW));
        this.onEffectsRemoved(list);
    }

    @org.spongepowered.asm.mixin.Unique
    private void tuanzis_mod$shakeOffShuriken() {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return;
        
        MobEffectInstance currentEffect = entity.getEffect(me.tuanzi.init.ModStatusEffects.SHURIKEN_STUCK);
        if (currentEffect != null) {
            // 1. 在脚下生成 1 枚手里剑物品
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                    serverLevel, entity.getX(), entity.getY(), entity.getZ(), 
                    new net.minecraft.world.item.ItemStack(me.tuanzi.init.ModItems.SHURIKEN, 1)
                );
                itemEntity.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(itemEntity);
            }
            
            // 2. 扣除 1 级效果或者直接移除
            if (currentEffect.getAmplifier() > 0) {
                int newAmplifier = currentEffect.getAmplifier() - 1;
                int remainingDuration = currentEffect.getDuration();
                entity.addEffect(new MobEffectInstance(me.tuanzi.init.ModStatusEffects.SHURIKEN_STUCK, remainingDuration, newAmplifier, false, false, true));
            } else {
                entity.removeEffect(me.tuanzi.init.ModStatusEffects.SHURIKEN_STUCK);
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @org.spongepowered.asm.mixin.injection.At("HEAD")
    )
    private void tuanzis_mod$onHurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof net.minecraft.world.entity.player.Player && !entity.level().isClientSide()) {
            if (entity.hasEffect(me.tuanzi.init.ModStatusEffects.SHURIKEN_STUCK)) {
                if (entity.getRandom().nextFloat() < 0.15f) {
                    tuanzis_mod$shakeOffShuriken();
                }
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void tuanzis_mod$onSwing(net.minecraft.world.InteractionHand hand, boolean updateAnim, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof net.minecraft.world.entity.player.Player && !entity.level().isClientSide()) {
            if (entity.hasEffect(me.tuanzi.init.ModStatusEffects.SHURIKEN_STUCK)) {
                if (entity.getRandom().nextFloat() < 0.10f) {
                    tuanzis_mod$shakeOffShuriken();
                }
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "canStandOnFluid(Lnet/minecraft/world/level/material/FluidState;)Z",
        at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$canStandOnWater(net.minecraft.world.level.material.FluidState fluid, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (fluid.is(net.minecraft.tags.FluidTags.WATER)) {
            if (entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(me.tuanzi.init.ModItems.TIDAL_WEAVE_BOOTS)) {
                // 只有当没有按住潜行，且玩家的眼睛不在水中（即头部在水面上方）时，才允许站在水面上。
                // 这保证了在水面正常行走、疾跑或跳跃时绝对不会意外沉没，而在下潜至水底时能正常游动且恢复水的阻力。
                if (!entity.isDescending() && !entity.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "getLiquidCollisionShape()Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$getLiquidCollisionShape(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.phys.shapes.VoxelShape> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(me.tuanzi.init.ModItems.TIDAL_WEAVE_BOOTS)) {
            if (!entity.isDescending() && !entity.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
                cir.setReturnValue(net.minecraft.world.level.block.Block.column(16.0, 0.0, 8.0));
            }
        }
    }


    @org.spongepowered.asm.mixin.injection.Inject(
        method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z",
        at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$onCauseFallDamage(double fallDistance, float damageModifier, net.minecraft.world.damagesource.DamageSource damageSource, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(me.tuanzi.init.ModItems.TIDAL_WEAVE_BOOTS)) {
            if (entity.level().getFluidState(entity.blockPosition()).is(net.minecraft.tags.FluidTags.WATER)
                    || entity.level().getFluidState(entity.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER)) {
                
                if (fallDistance > 0.5) {
                    double bounceSpeed = Math.min(0.45, Math.max(0.18, Math.sqrt(fallDistance) * 0.12));
                    entity.setDeltaMovement(entity.getDeltaMovement().x, bounceSpeed, entity.getDeltaMovement().z);
                    
                    if (!entity.level().isClientSide() && entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, 
                            entity.getX(), entity.getY(), entity.getZ(), 
                            18, 0.35, 0.1, 0.35, 0.1);
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, 
                            entity.getX(), entity.getY(), entity.getZ(), 
                            12, 0.25, 0.1, 0.25, 0.05);
                        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            net.minecraft.sounds.SoundEvents.PLAYER_SPLASH, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
                    }
                    
                    me.tuanzi.util.ModLog.debug(null, entity, "【潮汐织靴】落水判定！已从高度 " + String.format("%.2f", fallDistance) + " 弹起（弹起初速度: " + String.format("%.2f", bounceSpeed) + "）。");
                }
                cir.setReturnValue(false);
            }
        }
    }
}
