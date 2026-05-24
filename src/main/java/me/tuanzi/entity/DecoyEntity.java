package me.tuanzi.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DecoyEntity extends Avatar {
    public static EntityType.EntityFactory<DecoyEntity> constructor = DecoyEntity::new;

    protected static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE = SynchedEntityData.defineId(
            DecoyEntity.class, EntityDataSerializers.RESOLVABLE_PROFILE
    );

    public enum DecoyAction {
        IDLE, WALK, RUN
    }

    private DecoyAction currentAction = DecoyAction.IDLE;
    private int actionTicks = 0;
    private float moveAngle = 0.0f;

    // 头部自主随机运动状态，使假目标头部可以随意自然地转动
    private float relativeTargetHeadYaw = 0.0f;
    private float targetHeadPitch = 0.0f;
    private int lookTimer = 0;

    public DecoyEntity(EntityType<? extends Avatar> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    public static DecoyEntity create(EntityType<DecoyEntity> type, Level level) {
        return constructor.create(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PROFILE, ResolvableProfile.Static.EMPTY);
    }

    @Override
    public ResolvableProfile getProfile() {
        return this.entityData.get(DATA_PROFILE);
    }

    public void setProfile(ResolvableProfile profile) {
        this.entityData.set(DATA_PROFILE, profile);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ARMOR, 0.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // 15 秒存活计时 (300 ticks)
            if (this.tickCount >= 300) {
                // 播放陶罐碎裂音效
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.DECORATED_POT_SHATTER, SoundSource.NEUTRAL, 1.0F, 1.0F);

                // 发送烟雾和Poof粒子效果
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, 
                            this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 
                            this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                }

                // 激怒 10 格内所有怪物：获得速度 II 持续 10 秒
                AABB enrageBox = this.getBoundingBox().inflate(10.0);
                List<Mob> monsters = this.level().getEntitiesOfClass(Mob.class, enrageBox, mob -> mob.isAlive());
                for (Mob monster : monsters) {
                    monster.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
                }

                // 原地直接消失，避免受击变红与倒地动画
                this.discard();
                return;
            }

            // 每 5 ticks 嘲讽周围 10 格的怪物
            if (this.tickCount % 5 == 0) {
                AABB searchBox = this.getBoundingBox().inflate(10.0);
                List<Mob> monsters = this.level().getEntitiesOfClass(Mob.class, searchBox, mob -> mob.isAlive());
                for (Mob monster : monsters) {
                    if (monster.getTarget() != this) {
                        monster.setTarget(this);
                    }
                }
            }

            // 拟真动作状态机
            if (this.actionTicks <= 0) {
                double rand = this.random.nextDouble();
                if (rand < 0.4) {
                    this.currentAction = DecoyAction.IDLE;
                } else if (rand < 0.8) {
                    this.currentAction = DecoyAction.WALK;
                } else {
                    this.currentAction = DecoyAction.RUN;
                }
                this.actionTicks = 40 + this.random.nextInt(60);
                this.moveAngle = this.random.nextFloat() * (float) Math.PI * 2.0F;
            } else {
                this.actionTicks--;
            }

            // 根据状态控制物理移动和朝向
            if (this.currentAction != DecoyAction.IDLE) {
                float speed = this.currentAction == DecoyAction.RUN ? 0.18F : 0.10F;
                
                // 疾跑时跳跃增加移动速度
                if (this.currentAction == DecoyAction.RUN) {
                    if (this.onGround() && this.random.nextFloat() < 0.05F) {
                        this.setDeltaMovement(this.getDeltaMovement().x, 0.42, this.getDeltaMovement().z);
                    }
                    // 疾跑在空中时速度增加 20%
                    if (!this.onGround()) {
                        speed *= 1.2F;
                    }
                }

                double vx = -Math.sin(this.moveAngle) * speed;
                double vz = Math.cos(this.moveAngle) * speed;
                double vy = this.getDeltaMovement().y;
                this.setDeltaMovement(vx, vy, vz);

                float rotDegrees = (float) Math.toDegrees(this.moveAngle);
                this.setYRot(rotDegrees);
                this.setYBodyRot(rotDegrees);

                // 撞墙（水平碰撞）且在地面时自动跳跃越障
                if (this.horizontalCollision && this.onGround()) {
                    this.setDeltaMovement(this.getDeltaMovement().x, 0.42, this.getDeltaMovement().z);
                }
            } else {
                this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
            }

            // 视线随机转动计时器及自主头部随性运动
            this.lookTimer--;
            if (this.lookTimer <= 0) {
                this.lookTimer = 30 + this.random.nextInt(40); // 1.5 - 3.5秒随机改变一次视线
                if (this.currentAction == DecoyAction.IDLE) {
                    this.relativeTargetHeadYaw = (this.random.nextFloat() - 0.5F) * 100.0F; // 左右各50度范围
                    this.targetHeadPitch = (this.random.nextFloat() - 0.5F) * 50.0F;       // 上下各25度范围
                } else if (this.currentAction == DecoyAction.WALK) {
                    this.relativeTargetHeadYaw = (this.random.nextFloat() - 0.5F) * 60.0F;  // 左右各30度范围
                    this.targetHeadPitch = (this.random.nextFloat() - 0.5F) * 30.0F;       // 上下各15度范围
                } else {
                    // 疾跑时视线保持专注前方，摆动幅度极小
                    this.relativeTargetHeadYaw = (this.random.nextFloat() - 0.5F) * 20.0F;  // 左右各10度范围
                    this.targetHeadPitch = (this.random.nextFloat() - 0.5F) * 10.0F;       // 上下各5度范围
                }
            }

            // 头部平滑转向，统一平滑计算并限制旋转角速度，使得动作极其自然
            float currentHeadYaw = this.getYHeadRot();
            float targetHeadYaw = this.getYRot() + this.relativeTargetHeadYaw;
            float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetHeadYaw - currentHeadYaw);
            float maxYawSpeed = this.currentAction == DecoyAction.RUN ? 6.0F : 4.0F;
            float newHeadYaw = currentHeadYaw + net.minecraft.util.Mth.clamp(yawDiff, -maxYawSpeed, maxYawSpeed);
            this.setYHeadRot(newHeadYaw);

            float currentPitch = this.getXRot();
            float pitchDiff = this.targetHeadPitch - currentPitch;
            float maxPitchSpeed = this.currentAction == DecoyAction.RUN ? 4.0F : 2.5F;
            float newPitch = currentPitch + net.minecraft.util.Mth.clamp(pitchDiff, -maxPitchSpeed, maxPitchSpeed);
            this.setXRot(newPitch);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(level, source, amount);
        }

        boolean flag = super.hurtServer(level, source, amount);
        if (flag) {
            // 受击时强制切换到奔跑 (Run) 并设定更长的持续时间
            this.currentAction = DecoyAction.RUN;
            this.actionTicks = 60 + this.random.nextInt(40);

            // 远离伤害源逃跑
            Entity attacker = source.getEntity();
            if (attacker != null) {
                double dx = this.getX() - attacker.getX();
                double dz = this.getZ() - attacker.getZ();
                this.moveAngle = (float) Math.atan2(-dx, dz);
            } else {
                this.moveAngle = this.random.nextFloat() * (float) Math.PI * 2.0F;
            }

            // 顺势触发一次向前上方的大跳跃
            if (this.onGround()) {
                double escapeSpeed = 0.25;
                double vx = -Math.sin(this.moveAngle) * escapeSpeed;
                double vz = Math.cos(this.moveAngle) * escapeSpeed;
                this.setDeltaMovement(vx, 0.52, vz);
            }
        }
        return flag;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // 播放陶罐碎裂音效
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.DECORATED_POT_SHATTER, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // 发送烟雾和Poof粒子效果
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, 
                    this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 
                    this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);

            // 激怒 10 格内所有怪物：获得速度 II 持续 10 秒
            AABB enrageBox = this.getBoundingBox().inflate(10.0);
            List<Mob> monsters = this.level().getEntitiesOfClass(Mob.class, enrageBox, mob -> mob.isAlive());
            for (Mob monster : monsters) {
                monster.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        // 重写确保绝对不会掉落任何物品
    }
}
