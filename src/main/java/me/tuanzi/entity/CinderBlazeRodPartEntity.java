package me.tuanzi.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import me.tuanzi.init.ModEntities;
import me.tuanzi.init.ModSounds;
import me.tuanzi.util.ModLog;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 日冕核心 · 外环日冕烈焰棒独立部位实体 (Cinder Blaze Rod Part)
 * 具备独立 Blockbench 模型渲染与动画自转能力
 * 单根独立生命值：15.0 点
 * 物理拦截与偏折机制：反弹常规投射物，受穿透附魔穿透，损毁后通知 Boss 独立重铸
 */
public class CinderBlazeRodPartEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Float> ROD_HEALTH = SynchedEntityData.defineId(CinderBlazeRodPartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ROD_INDEX = SynchedEntityData.defineId(CinderBlazeRodPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARENT_BOSS_ID = SynchedEntityData.defineId(CinderBlazeRodPartEntity.class, EntityDataSerializers.INT);

    public static final float MAX_ROD_HEALTH = 15.0F;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.cinder_blaze_rod.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID parentBossUuid = null;
    private CinderSovereignEntity cachedBoss = null;

    public CinderBlazeRodPartEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CinderBlazeRodPartEntity(Level level, CinderSovereignEntity boss, int rodIndex) {
        this(ModEntities.CINDER_BLAZE_ROD_PART, level);
        this.cachedBoss = boss;
        this.parentBossUuid = boss.getUUID();
        this.entityData.set(ROD_INDEX, rodIndex);
        this.entityData.set(ROD_HEALTH, MAX_ROD_HEALTH);
        this.entityData.set(PARENT_BOSS_ID, boss.getId());
        this.setPos(boss.getX(), boss.getY(), boss.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROD_HEALTH, MAX_ROD_HEALTH);
        builder.define(ROD_INDEX, 0);
        builder.define(PARENT_BOSS_ID, -1);
    }

    public int getRodIndex() {
        return this.entityData.get(ROD_INDEX);
    }

    public float getRodHealth() {
        return this.entityData.get(ROD_HEALTH);
    }

    public boolean isRodAlive() {
        return this.getRodHealth() > 0.0F;
    }

    public void restoreRod() {
        this.entityData.set(ROD_HEALTH, MAX_ROD_HEALTH);
    }

    public CinderSovereignEntity getParentBoss() {
        if (this.cachedBoss != null && !this.cachedBoss.isRemoved()) {
            return this.cachedBoss;
        }
        int bossId = this.entityData.get(PARENT_BOSS_ID);
        if (bossId >= 0) {
            Entity entity = this.level().getEntity(bossId);
            if (entity instanceof CinderSovereignEntity sovereign) {
                this.cachedBoss = sovereign;
                return this.cachedBoss;
            }
        }
        if (this.parentBossUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.parentBossUuid);
            if (entity instanceof CinderSovereignEntity sovereign) {
                this.cachedBoss = sovereign;
                return this.cachedBoss;
            }
        }
        return null;
    }

    @Override
    public boolean isPickable() {
        return this.isRodAlive();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (!this.isRodAlive() || this.isInvulnerableToBase(damageSource)) {
            return false;
        }

        // 通知本体 Boss 外围烈焰棒受到攻击，打断脱战自愈
        CinderSovereignEntity boss = this.getParentBoss();
        if (boss != null && boss.isAlive()) {
            boss.notifyAttacked();
        }

        // 弹道拦截与物理偏折检测
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            boolean isPiercing = false;
            if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
                isPiercing = true;
                ModLog.debug(directEntity, this, "【日冕烈焰棒 " + this.getRodIndex() + "】遭受穿透箭矢贯穿攻击 (贯穿等级: " + arrow.getPierceLevel() + ")！");
            }

            // 常规未贯穿投射物被烈焰棒偏折反弹并点燃：
            // 物理偏折规则：水平线速度反转并施加 15° 向上仰角，速度重置为原初速度的 80%
            // 注意：若 Boss 处于【炽核坠碾·卡地虚弱窗口】(isCrucibleVulnerable)，烈焰棒熄火垂落，弹道偏折完全失效！
            if (!isPiercing && !(boss != null && boss.isCrucibleVulnerable())) {
                Vec3 motion = projectile.getDeltaMovement();
                double hSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                double newVx = -motion.x * 0.8;
                double newVz = -motion.z * 0.8;
                // tan(15°) ≈ 0.267949
                double newVy = Math.max(0.12, hSpeed * 0.8 * 0.267949);
                projectile.setDeltaMovement(newVx, newVy, newVz);
                projectile.igniteForSeconds(5.0F);

                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CINDER_SOVEREIGN_DEFLECT, SoundSource.HOSTILE, 1.0F, 1.8F + this.random.nextFloat() * 0.2F);
                level.sendParticles(ParticleTypes.FLAME, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.1, 0.1, 0.1, 0.05);

                ModLog.debug(directEntity, this, "【日冕烈焰棒 " + this.getRodIndex() + "】成功偏折弹道投射物！施加 15° 向上仰角反弹！");
            } else if (boss != null && boss.isCrucibleVulnerable()) {
                ModLog.debug(directEntity, this, "【日冕烈焰棒 " + this.getRodIndex() + "】处于卡地破绽易伤期，烈焰棒熄火垂落，弹道偏折失效！");
            }
        }

        // 扣除单根独立生命值
        float currentHp = this.getRodHealth();
        float newHp = Math.max(0.0F, currentHp - amount);
        this.entityData.set(ROD_HEALTH, newHp);

        // 受击音效与火花
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, 0.9F, 1.4F);
        level.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.1, 0.2, 0.1, 0.05);

        ModLog.debug(damageSource.getEntity(), this, "【日冕烈焰棒 " + this.getRodIndex() + "】受到 "
                + String.format("%.2f", amount) + " 点伤害！剩余生命值: " + String.format("%.2f", newHp));

        // 生命归零，损毁
        if (newHp <= 0.0F) {
            onRodBreak(level);
        }

        return true;
    }

    private void onRodBreak(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.4F, 1.4F);

        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.2, 0.3, 0.2, 0.1);
        level.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.5, this.getZ(), 8, 0.15, 0.2, 0.15, 0.05);

        ModLog.debug(null, this, "【日冕烈焰棒 " + this.getRodIndex() + "】彻底损毁！");

        CinderSovereignEntity boss = this.getParentBoss();
        if (boss != null) {
            boss.onBlazeRodDestroyed(this.getRodIndex());
        }
    }

    public void updatePositionAndRotation(double x, double y, double z, float yRot) {
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setOldPosAndRot();
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xOld = x;
        this.yOld = y;
        this.zOld = z;
    }

    @Override
    protected void lerpPositionAndRotationStep(
            int stepsToTarget, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot
    ) {
        // 服务端权威同步：客户端接收网络包后立即 0 延迟更新目标坐标与偏航角，杜绝原版插值滞后拉扯
        this.updatePositionAndRotation(targetX, targetY, targetZ, (float) targetYRot);
    }

    @Override
    public void tick() {
        super.tick();
        CinderSovereignEntity boss = this.getParentBoss();
        if (boss == null || boss.isRemoved() || boss.isDeadOrDying()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, event ->
                event.setAndContinue(ANIM_IDLE)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
