package me.tuanzi.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import me.tuanzi.init.ModEntities;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.ModLog;
import me.tuanzi.util.TempBlockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 地狱熔岩流星 (Cataclysmic Meteor)
 * 垂直向下砸落 (1.2 格/tick)。触地引发 20 点爆炸伤害，在 mobGriefing 下炸出 3x3 玄武岩坑与 6 秒临时岩浆。
 */
public class CinderMeteorEntity extends Projectile implements GeoEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.cinder_meteor.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private double targetGroundY = Double.NEGATIVE_INFINITY;

    public CinderMeteorEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CinderMeteorEntity(Level level, LivingEntity owner, double x, double startY, double z, double targetGroundY) {
        this(ModEntities.CINDER_METEOR, level);
        this.setOwner(owner);
        this.snapTo(x, startY, z, 0.0F, 0.0F);
        this.targetGroundY = targetGroundY;
        this.setDeltaMovement(0, -0.85, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize() * 4.0;
        if (Double.isNaN(size)) {
            size = 4.0;
        }
        size *= 64.0;
        return distance < size * size;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 movement = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            return;
        }

        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        // 到达目标地表高度时触地爆炸
        if (!this.level().isClientSide() && this.getY() <= this.targetGroundY + 0.5) {
            explode();
            return;
        }

        // 下落拖尾与地面预警粒子
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 4, 0.2, 0.3, 0.2, 0.05);
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);

            if (this.targetGroundY > Double.NEGATIVE_INFINITY) {
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.targetGroundY + 0.1, this.getZ(), 3, 0.6, 0.05, 0.6, 0.02);
            }
        }

        // 超时清除 (15 秒防卡死)
        if (!this.level().isClientSide() && this.tickCount > 300) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == this.getOwner() || entity instanceof CinderShieldPartEntity || entity instanceof CinderBlazeRodPartEntity) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        explode();
    }

    private void explode() {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos centerPos = this.blockPosition();

            // 范围爆炸伤害 (4 格半径)
            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0));
            DamageSource explosionSource = this.damageSources().explosion(this, this.getOwner());

            for (LivingEntity v : victims) {
                if (v == this.getOwner()) continue;
                float dist = (float) this.distanceTo(v);
                float falloff = Math.max(0.2F, 1.0F - (dist / 4.0F));
                float rawDmg = 20.0F * falloff;
                float finalDmg = DamageCalculator.calculateDamage(rawDmg, explosionSource, v);
                v.hurtServer(serverLevel, explosionSource, finalDmg);
                v.igniteForSeconds(6.0F);

                ModLog.debug(this.getOwner(), v, "【地狱熔岩陨石】爆炸波及目标！距离: " + String.format("%.2f", dist)
                        + " 格，造成 " + String.format("%.2f", finalDmg) + " 点爆炸伤害！");
            }

            // 爆炸视听反馈
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.8, 0.5, 0.8, 0.2);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);

            // 地形破坏与弹坑逻辑 (遵循 mobGriefing)
            if (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                // 寻找地表接触点
                BlockPos groundPos = centerPos;
                while (groundPos.getY() > serverLevel.getMinY() && serverLevel.isEmptyBlock(groundPos)) {
                    groundPos = groundPos.below();
                }

                // 3x3 玄武岩弹坑
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos p = groundPos.offset(dx, 0, dz);
                        if (!serverLevel.getBlockState(p).isAir() && serverLevel.getBlockState(p).getDestroySpeed(serverLevel, p) >= 0) {
                            serverLevel.setBlockAndUpdate(p, Blocks.BASALT.defaultBlockState());
                        }
                    }
                }

                // 中心生成临时岩浆，并在 6 秒 (120 ticks) 后固化为黑曜石
                BlockPos craterCenter = groundPos.above();
                if (serverLevel.isEmptyBlock(craterCenter)) {
                    serverLevel.setBlockAndUpdate(craterCenter, Blocks.LAVA.defaultBlockState());
                    TempBlockManager.schedule(serverLevel, craterCenter, 120, Blocks.OBSIDIAN.defaultBlockState());
                }
            }

            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("meteor_controller", 0, state ->
                state.setAndContinue(IDLE_ANIM)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
