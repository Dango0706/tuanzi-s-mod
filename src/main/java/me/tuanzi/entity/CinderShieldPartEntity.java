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
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * 日冕核心 · 悬浮黑石重盾独立部位实体 (Cinder Shield Part)
 * 具备独立 Blockbench 模型渲染与动画展开能力
 * 单面独立生命值：35.0 点，无护甲减免
 * 受击与碎裂特效联动，全毁触发 Boss 过载眩晕
 */
public class CinderShieldPartEntity extends Entity implements GeoEntity {
    public static final float MAX_SHIELD_HEALTH = 35.0F;
    private static final EntityDataAccessor<Float> SHIELD_HEALTH = SynchedEntityData.defineId(CinderShieldPartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SHIELD_INDEX = SynchedEntityData.defineId(CinderShieldPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARENT_BOSS_ID = SynchedEntityData.defineId(CinderShieldPartEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.cinder_shield.idle");
    private static final RawAnimation ANIM_PHASE2 = RawAnimation.begin().thenLoop("animation.cinder_shield.phase2_unfold");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID parentBossUuid = null;
    private CinderSovereignEntity cachedBoss = null;

    public CinderShieldPartEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CinderShieldPartEntity(Level level, CinderSovereignEntity boss, int shieldIndex) {
        this(ModEntities.CINDER_SHIELD_PART, level);
        this.cachedBoss = boss;
        this.parentBossUuid = boss.getUUID();
        this.entityData.set(SHIELD_INDEX, shieldIndex);
        this.entityData.set(SHIELD_HEALTH, MAX_SHIELD_HEALTH);
        this.entityData.set(PARENT_BOSS_ID, boss.getId());
        this.setPos(boss.getX(), boss.getY(), boss.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHIELD_HEALTH, MAX_SHIELD_HEALTH);
        builder.define(SHIELD_INDEX, 0);
        builder.define(PARENT_BOSS_ID, -1);
    }

    public int getShieldIndex() {
        return this.entityData.get(SHIELD_INDEX);
    }

    public float getShieldHealth() {
        return this.entityData.get(SHIELD_HEALTH);
    }

    public boolean isShieldAlive() {
        return this.getShieldHealth() > 0.0F;
    }

    public void restoreShield() {
        this.entityData.set(SHIELD_HEALTH, MAX_SHIELD_HEALTH);
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
        return this.isShieldAlive();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (!this.isShieldAlive() || this.isInvulnerableToBase(damageSource)) {
            return false;
        }

        // 通知本体 Boss 重盾受到攻击，打断脱战自愈
        CinderSovereignEntity boss = this.getParentBoss();
        if (boss != null && boss.isAlive()) {
            boss.notifyAttacked();
        }

        // 吸收伤害，扣除单面盾牌独立生命值
        float currentHp = this.getShieldHealth();
        float newHp = Math.max(0.0F, currentHp - amount);
        this.entityData.set(SHIELD_HEALTH, newHp);

        // 受击音效：厚重钝器撞击耐火合金钝响 (音量 1.4, 音调 0.65 ~ 0.8)
        if (newHp > 0.0F) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CINDER_SOVEREIGN_HURT_SHIELD, SoundSource.HOSTILE, 1.4F, 0.65F + this.random.nextFloat() * 0.15F);
        }

        ModLog.debug(damageSource.getEntity(), this, "【黑石重盾 " + this.getShieldIndex() + "】受到 "
                + String.format("%.2f", amount) + " 点直接伤害！剩余生命值: " + String.format("%.2f", newHp));

        // 生命值归零，碎裂毁坏
        if (newHp <= 0.0F) {
            onShieldBreak(level);
        }

        return true;
    }

    private void onShieldBreak(ServerLevel level) {
        // 方块破裂音效 (音量 2.0)
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GILDED_BLACKSTONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.9F);

        // 炸出黑曜石碎屑与黑石残片粒子
        BlockParticleOption cryingObsidian = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        BlockParticleOption gildedBlackstone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GILDED_BLACKSTONE.defaultBlockState());

        level.sendParticles(cryingObsidian, this.getX(), this.getY() + 0.9, this.getZ(), 28, 0.4, 0.7, 0.4, 0.15);
        level.sendParticles(gildedBlackstone, this.getX(), this.getY() + 0.9, this.getZ(), 24, 0.4, 0.7, 0.4, 0.15);

        ModLog.debug(null, this, "【黑石重盾 " + this.getShieldIndex() + "】彻底破碎碎裂！");

        CinderSovereignEntity boss = this.getParentBoss();
        if (boss != null) {
            boss.onShieldDestroyed(this.getShieldIndex());
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
        controllers.add(new AnimationController<>("controller", 5, event -> {
            CinderSovereignEntity boss = this.getParentBoss();
            boolean phase2 = (boss != null && boss.isPhase2());
            if (phase2) {
                return event.setAndContinue(ANIM_PHASE2);
            } else {
                return event.setAndContinue(ANIM_IDLE);
            }
        }));
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
