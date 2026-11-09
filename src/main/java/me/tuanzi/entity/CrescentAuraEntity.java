package me.tuanzi.entity;

import me.tuanzi.init.ModEntities;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CrescentAuraEntity extends Entity {
    private LivingEntity owner;
    private float baseWeaponDamage = 7.0f;
    private Vec3 startPos;
    private final Set<UUID> hitEntities = new HashSet<>();

    public CrescentAuraEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public CrescentAuraEntity(Level level, LivingEntity owner, Vec3 direction, float baseWeaponDamage) {
        super(ModEntities.CRESCENT_AURA, level);
        this.owner = owner;
        this.baseWeaponDamage = baseWeaponDamage;
        this.noPhysics = true;
        this.setPos(owner.getEyePosition().add(direction.normalize().scale(0.8)));
        this.startPos = this.position();
        this.setDeltaMovement(direction.normalize().scale(1.0)); // 20 blocks/s = 1.0 block/tick
    }

    @Override
    public void tick() {
        super.tick();

        if (this.startPos == null) {
            this.startPos = this.position();
        }

        // 沿飞行方向推进 1.0 格 (20 格/秒)
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.position().add(motion));

        // 粒子特效渲染（前后端均生成视觉效果）
        Vec3 side = new Vec3(-motion.z, 0, motion.x).normalize();
        if (this.level().isClientSide()) {
            for (double offset = -1.2; offset <= 1.2; offset += 0.3) {
                Vec3 p = this.position().add(side.scale(offset)).add(0, 0.3, 0);
                this.level().addParticle(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, motion.x * 0.1, 0.0, motion.z * 0.1);
                this.level().addParticle(ParticleTypes.CRIT, p.x, p.y, p.z, 0.0, 0.05, 0.0);
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // 1. 斩断沿途方块（蜘蛛网、竹子、草叶、树叶）
            BlockPos minPos = BlockPos.containing(this.getBoundingBox().minX, this.getBoundingBox().minY, this.getBoundingBox().minZ);
            BlockPos maxPos = BlockPos.containing(this.getBoundingBox().maxX, this.getBoundingBox().maxY, this.getBoundingBox().maxZ);

            for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
                BlockState state = serverLevel.getBlockState(pos);
                if (state.is(Blocks.COBWEB) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)
                        || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                        || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                        || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)) {
                    serverLevel.destroyBlock(pos, true, this.owner);
                }
            }

            // 2. 贯穿碰撞检测（宽 2.5 格）
            AABB hitBox = this.getBoundingBox().inflate(0.2, 0.2, 0.2);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e.isAlive() && e != this.owner && !this.hitEntities.contains(e.getUUID()));

            for (LivingEntity target : targets) {
                this.hitEntities.add(target.getUUID());

                DamageSource source = this.owner instanceof Player player
                        ? serverLevel.damageSources().playerAttack(player)
                        : serverLevel.damageSources().generic();

                float rawDamage = Math.max(1.0f, this.baseWeaponDamage * 0.20f);
                float finalDamage = DamageCalculator.calculateDamage(rawDamage, source, target);

                target.hurtServer(serverLevel, source, finalDamage);
                target.knockback(0.45, -motion.x, -motion.z, source, finalDamage);

                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.3f);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);

                ModLog.debug(this.owner, target, "【月牙气刃】贯穿命中目标！造成武器 20% 纯物理伤害 (原始: "
                        + String.format("%.2f", rawDamage) + " 点) -> 结算最终伤害: " + String.format("%.2f", finalDamage) + " 点。");
            }

            // 3. 最大贯穿距离 4 格（飞行速度 20 格/秒 = 1.0 格/tick，4 ticks 即达到 4 格）
            if (this.position().distanceTo(this.startPos) >= 4.0 || this.tickCount >= 4) {
                this.discard();
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
