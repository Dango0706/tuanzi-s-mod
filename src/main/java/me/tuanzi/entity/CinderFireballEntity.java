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
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 微型日冕火球 (Corona Fireball)
 * 直线高速飞行 (1.6 格/tick)，无重力。
 * 命中造成 7 点伤害并点燃 4 秒；若被举盾格挡，扣除盾牌 18 点耐久并施加硬直破防与穿透伤害。
 */
public class CinderFireballEntity extends Projectile implements GeoEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.cinder_fireball.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CinderFireballEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CinderFireballEntity(Level level, LivingEntity owner, Vec3 direction) {
        this(ModEntities.CINDER_FIREBALL, level);
        this.setOwner(owner);
        Vec3 spawnPos = owner.position().add(0, 1.6, 0).add(direction.normalize().scale(1.2));
        this.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, owner.getYRot(), owner.getXRot());
        this.setDeltaMovement(direction.normalize().scale(1.25));
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

        // 直线飞行位移
        Vec3 movement = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        // 飞行粒子拖尾
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                    -movement.x * 0.1, -movement.y * 0.1, -movement.z * 0.1);
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(),
                    0, 0.02, 0);
        }

        // 超时自动销毁 (5 秒 / 100 ticks)
        if (!this.level().isClientSide() && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // 不打本体和同属部件，且前 5 ticks 不打任何 CinderSovereign 实体（出膛保护）
        if (entity == this.getOwner() || entity instanceof CinderShieldPartEntity || entity instanceof CinderBlazeRodPartEntity) {
            return false;
        }
        if (entity instanceof CinderSovereignEntity && this.tickCount < 5) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity target = hitResult.getEntity();
            Entity owner = this.getOwner();
            DamageSource source = (owner == null)
                    ? this.damageSources().source(net.minecraft.world.damagesource.DamageTypes.UNATTRIBUTED_FIREBALL, this)
                    : this.damageSources().source(net.minecraft.world.damagesource.DamageTypes.FIREBALL, this, owner);

            if (target instanceof LivingEntity living) {
                if (living.isBlocking()) {
                    // 盾牌格挡判定：扣除 18 耐久 + 破盾硬直 + 3 点穿透真实伤害
                    ItemStack shield = living.getItemBlockingWith();
                    if (shield != null) {
                        EquipmentSlot slot = (living instanceof Player player && player.getUsedItemHand() == InteractionHand.MAIN_HAND)
                                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        shield.hurtAndBreak(18, living, slot);

                        BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
                        if (blocksAttacks != null && living instanceof Player player) {
                            blocksAttacks.disable(serverLevel, player, 0.3F, shield);
                        } else if (living instanceof Player player) {
                            player.getCooldowns().addCooldown(shield, 6);
                        }
                        living.stopUsingItem();
                    }

                    // 穿透伤害结算
                    float piercedDmg = DamageCalculator.calculateDamage(3.0F, source, living);
                    living.hurtServer(serverLevel, source, piercedDmg);
                    living.igniteForSeconds(2.0F);

                    serverLevel.playSound(null, living.getX(), living.getY(), living.getZ(),
                            SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
                    ModLog.debug(owner, living, "【微型日冕火球】命中举盾目标！扣除 18 耐久，施加 0.3s 破防硬直并造成 "
                            + String.format("%.2f", piercedDmg) + " 点穿透伤害！");
                } else {
                    // 正常命中：7 点伤害 + 4 秒点燃
                    float baseDmg = 7.0F;
                    float finalDmg = DamageCalculator.calculateDamage(baseDmg, source, living);
                    living.hurtServer(serverLevel, source, finalDmg);
                    living.igniteForSeconds(4.0F);

                    ModLog.debug(owner, living, "【微型日冕火球】直接命中目标！造成 "
                            + String.format("%.2f", finalDmg) + " 点伤害，点燃 4 秒！");
                }

                // 爆裂粒子
                serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
            }

            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                BlockPos firePos = hitResult.getBlockPos().relative(hitResult.getDirection());
                if (serverLevel.isEmptyBlock(firePos)) {
                    serverLevel.setBlockAndUpdate(firePos, BaseFireBlock.getState(serverLevel, firePos));
                }
            }

            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6F, 1.4F);
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
        controllers.add(new AnimationController<>("fireball_controller", 0, state ->
                state.setAndContinue(IDLE_ANIM)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
