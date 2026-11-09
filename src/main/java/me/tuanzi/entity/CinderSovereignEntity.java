package me.tuanzi.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import me.tuanzi.init.ModEntities;
import me.tuanzi.init.ModSounds;
import me.tuanzi.network.CinderHeatSyncPacket;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 日冕核心 · 烈焰统领 (The Cinder Sovereign)
 * 复合多部位结构：4 面黑石重盾 (中环) 与 12 根日冕烈焰棒 (外环)
 * 包含完整 AI 状态机、防作弊机制、五大主动技能、二阶段切削利刃与日冕过热环境领域
 */
public class CinderSovereignEntity extends Monster implements GeoEntity {
    // 同步数据与状态定义
    private static final EntityDataAccessor<Integer> SHIELD_BITMASK = SynchedEntityData.defineId(CinderSovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROD_BITMASK = SynchedEntityData.defineId(CinderSovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOSS_STATE = SynchedEntityData.defineId(CinderSovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_PHASE_2 = SynchedEntityData.defineId(CinderSovereignEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int STATE_IDLE = 0;
    public static final int STATE_BARRAGE = 1;
    public static final int STATE_BEAM_CHARGE = 2;
    public static final int STATE_BEAM_FIRE = 3;
    public static final int STATE_METEOR = 4;
    public static final int STATE_STUNNED = 5;
    public static final int STATE_SHOCKWAVE = 6;
    public static final int STATE_CRUCIBLE_PILEDRIVER = 7;

    // GeckoLib 动画定义
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.cinder_sovereign.idle");
    private static final RawAnimation ANIM_PHASE2_IDLE = RawAnimation.begin().thenLoop("animation.cinder_sovereign.phase2_idle");
    private static final RawAnimation ANIM_BARRAGE = RawAnimation.begin().thenPlay("animation.cinder_sovereign.barrage_shoot");
    private static final RawAnimation ANIM_BEAM_CHARGE = RawAnimation.begin().thenPlay("animation.cinder_sovereign.beam_charge");
    private static final RawAnimation ANIM_BEAM_FIRE = RawAnimation.begin().thenPlay("animation.cinder_sovereign.beam_fire");
    private static final RawAnimation ANIM_METEOR = RawAnimation.begin().thenPlay("animation.cinder_sovereign.meteor_charge");
    private static final RawAnimation ANIM_STUNNED = RawAnimation.begin().thenPlay("animation.cinder_sovereign.stunned");
    private static final RawAnimation ANIM_SHOCKWAVE = RawAnimation.begin().thenPlay("animation.cinder_sovereign.shockwave");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true);

    // 复合子部位实体
    private final CinderShieldPartEntity[] shields = new CinderShieldPartEntity[4];
    private final CinderBlazeRodPartEntity[] blazeRods = new CinderBlazeRodPartEntity[12];
    private final int[] rodRebuildTimers = new int[12]; // 单根烈焰棒 9.0s (180 ticks) 独立重铸倒计时

    // 旋转与公转物理角度
    private float orbitAngle = 0.0F;

    // 技能冷却与计时器
    private int barrageCooldown = 100;
    private int beamCooldown = 240;
    private int meteorCooldown = 360;
    private int flameWaveCooldown = 280; // 二阶段贴地烈焰跳绳 CD: 14s
    private int piledriverCooldown = 280; // 近战惩戒技能：日冕破城杵 · 炽核坠碾 CD: 14.0s (280 ticks)
    private int skillIntervalCooldown = 0; // 技能间公共冷却间隔 (2.0s = 40 ticks)，禁止无缝连续释放技能
    private int closeProximityTicks = 0; // 目标玩家贴脸近身持续刻数 (连续 1.2s = 24 ticks)
    private int piledriverPhase = 0; // 炽核坠碾四阶段状态标记 (1: 蓄势, 2: 砸地, 3: 卡地, 4: 复位)
    private double piledriverTargetGroundY = 0.0; // 目标下砸触地 Y 坐标
    private int piledriverStunExtraTicks = 0; // Mace 破韧打断后额外延长的眩晕时间 (30 ticks = 1.5s)
    private int stateTicks = 0; // 当前状态持续刻数

    // 全损应急脉冲冷却 (25s) 与 10s 全损窗口监测
    private int emergencyPulseCooldown = 0;
    private int allRodsBrokenWindowTimer = 0;

    // 48 格领域内冰块融化计时映射表 (BlockPos -> 剩余 ticks)
    private final Map<BlockPos, Integer> meltingIceTimers = new HashMap<>();

    // 二阶段利刃切削命中冷却映射表 (每实体 12 ticks)
    private final Map<UUID, Long> bladeHitCooldowns = new HashMap<>();

    // 玩家日冕过热度映射表 (0 ~ 100)
    private final Map<UUID, Integer> playerHeatMap = new HashMap<>();

    // 环境反应计时器
    private int waterDamageTickTimer = 0;

    // 脱战计时器 (ticks)：记录自上次本体、盾牌或烈焰棒受到攻击以来经过的时间
    private int outOfCombatTimer = 0;

    // 音效触发状态标志与独立冷却
    private boolean spawnSoundPlayed = false;
    private boolean hadTarget = false;
    private int dashSoundCooldown = 0;
    private int altitudeSoundCooldown = 0;

    /**
     * 受到攻击通知（本体、4面悬浮黑石重盾或外环12根日冕烈焰棒被攻击时触发）
     */
    public void notifyAttacked() {
        if (this.outOfCombatTimer >= 200) {
            ModLog.debug(null, this, "【日冕核心·烈焰统领】受到攻击（本体或复合子部位受损），脱战疾速自愈被打断！脱战计时器重置为 0。");
        }
        this.outOfCombatTimer = 0;
    }

    /**
     * 技能结束回调：设置技能间公共间隔（2.0 秒 / 40 ticks）
     * 保证每个技能结束后必须留出 2 秒空档期巡航游走，严禁连续无缝倾泻技能。
     */
    private void onSkillFinished() {
        this.skillIntervalCooldown = 40; // 2.0s (40 ticks)
        ModLog.debug(null, this, "【日冕核心·烈焰统领】技能释放完毕，进入 2.0 秒公共技能间隔期（巡航游走，禁止连续释放下一个技能）。");
    }

    public int getSkillIntervalCooldown() {
        return this.skillIntervalCooldown;
    }

    public CinderSovereignEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = false;
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setPersistenceRequired();
        this.xpReward = 275;
    }

    public static void reducePlayerHeat(ServerPlayer player, int amount) {
        if (player.level() instanceof ServerLevel serverLevel) {
            List<CinderSovereignEntity> bosses = serverLevel.getEntitiesOfClass(
                    CinderSovereignEntity.class,
                    player.getBoundingBox().inflate(64.0),
                    LivingEntity::isAlive
            );
            for (CinderSovereignEntity boss : bosses) {
                Integer currentHeat = boss.playerHeatMap.get(player.getUUID());
                if (currentHeat != null && currentHeat > 0) {
                    int newHeat = Math.max(0, currentHeat - amount);
                    boss.playerHeatMap.put(player.getUUID(), newHeat);
                    boolean inArena = player.distanceTo(boss) <= 48.0;
                    ServerPlayNetworking.send(player, new CinderHeatSyncPacket(newHeat, inArena));
                    ModLog.debug(boss, player, "【日冕过热缓解】玩家饮用水/药水！降低过热度 " + amount + "，剩余: " + newHeat);
                }
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        float halfWidth = 0.8F; // 宽度 1.6F (半宽 0.8F)，贴身包裹八面体核心
        double bottomOffset = 0.5; // 底部向上收起 0.5 格
        double topHeight = 2.4; // 顶部高度
        return new AABB(
                position.x - halfWidth, position.y + bottomOffset, position.z - halfWidth,
                position.x + halfWidth, position.y + topHeight, position.z + halfWidth
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 24.0) // 基础 12.0 + 4 面黑石重盾各 +3.0 = 24.0
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
                .add(Attributes.FLYING_SPEED, 0.256) // 巡航速度全时期降至 80% (0.32 -> 0.256)
                .add(Attributes.MOVEMENT_SPEED, 0.256)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHIELD_BITMASK, 0b1111); // 默认 4 面盾牌全存活
        builder.define(ROD_BITMASK, 0b111111111111); // 默认 12 根烈焰棒全存活
        builder.define(BOSS_STATE, STATE_IDLE);
        builder.define(IS_PHASE_2, false);
    }

    public int getShieldBitmask() {
        return this.entityData.get(SHIELD_BITMASK);
    }

    public int getRodBitmask() {
        return this.entityData.get(ROD_BITMASK);
    }

    public int getBossState() {
        return this.entityData.get(BOSS_STATE);
    }

    public boolean isPhase2() {
        return this.entityData.get(IS_PHASE_2);
    }

    public int getAliveShieldCount() {
        return Integer.bitCount(this.getShieldBitmask() & 0b1111);
    }

    public int getAliveRodCount() {
        return Integer.bitCount(this.getRodBitmask() & 0b111111111111);
    }

    public void setBossState(int state) {
        this.entityData.set(BOSS_STATE, state);
        this.stateTicks = 0;
        ModLog.debug(null, this, "【日冕核心·烈焰统领】状态切换为: " + stateName(state));
    }

    private String stateName(int state) {
        return switch (state) {
            case STATE_IDLE -> "巡航游走 (IDLE)";
            case STATE_BARRAGE -> "日冕弹幕 (BARRAGE)";
            case STATE_BEAM_CHARGE -> "聚变射线蓄力 (BEAM_CHARGE)";
            case STATE_BEAM_FIRE -> "聚变射线扫射 (BEAM_FIRE)";
            case STATE_METEOR -> "熔岩流星雨 (METEOR)";
            case STATE_STUNNED -> "过载眩晕瘫痪 (STUNNED)";
            case STATE_SHOCKWAVE -> "聚变冲击波爆发 (SHOCKWAVE)";
            case STATE_CRUCIBLE_PILEDRIVER -> "日冕破城杵 · 炽核坠碾 (CRUCIBLE_PILEDRIVER)";
            default -> "未知状态 (" + state + ")";
        };
    }

    /**
     * 判断是否处于“炽核坠碾”技能的卡地虚弱破绽窗口（阶段三：3.0 秒）
     * 期间核心本体落地暴露，全额弹道偏折与减伤被动失效，受到近战攻击必定暴击且享受 +30% 额外弱点增伤！
     */
    public boolean isCrucibleVulnerable() {
        return this.getBossState() == STATE_CRUCIBLE_PILEDRIVER && this.piledriverPhase == 3;
    }

    /**
     * 判断是否处于“炽核坠碾”技能全流程中（阶段 1 ~ 4）
     */
    public boolean isCruciblePiledriverActive() {
        return this.getBossState() == STATE_CRUCIBLE_PILEDRIVER;
    }

    /**
     * 判断指定实体是否为当前有效目标（存活、未移除、在索敌范围内且为非创造/旁观模式）
     */
    public boolean isValidTarget(@Nullable LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) return false;
        if (this.distanceTo(entity) > this.getAttributeValue(Attributes.FOLLOW_RANGE)) return false;
        if (entity instanceof Player player) {
            return !player.isSpectator() && !player.isCreative();
        }
        return true;
    }

    /**
     * 在索敌范围（Attributes.FOLLOW_RANGE，64格）内查找最近的有效玩家（存活且处于生存/冒险模式）
     */
    public @Nullable Player findNearestValidPlayer() {
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        Predicate<Entity> predicate = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        return this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), followRange, predicate);
    }

    /**
     * 判定 Boss 索敌范围内是否存在有效玩家。
     * 若存在则视为处于战斗中；仅当范围内无任何有效玩家时才视为脱战。
     */
    public boolean hasPlayerInFollowRange() {
        return findNearestValidPlayer() != null;
    }

    /**
     * 判断 Boss 是否处于脱战状态：
     * 1. 索敌范围内无任何有效玩家（不在索敌范围内才视为脱战）
     * 2. 脱战未受损时长达到 10.0 秒（200 ticks）以上
     */
    public boolean isOutOfCombat() {
        return !this.hasPlayerInFollowRange() && this.outOfCombatTimer >= 200;
    }

    @Override
    public void tick() {
        super.tick();

        // 维持悬浮巡航浮动
        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            // 索敌与脱战状态检测：需要玩家不在 Boss 索敌范围内，不在索敌范围内才视为脱战
            boolean hasPlayerInRange = this.hasPlayerInFollowRange();
            if (hasPlayerInRange) {
                if (this.outOfCombatTimer >= 200) {
                    ModLog.debug(null, this, "【日冕核心·烈焰统领】检测到玩家进入索敌范围（" + (int) this.getAttributeValue(Attributes.FOLLOW_RANGE) + "格），打断脱战状态，疾速自愈立即失效！");
                }
                this.outOfCombatTimer = 0;
            } else {
                this.outOfCombatTimer++;
            }

            // 登场音效：生成完成瞬间全场广播 (音量 3.5, 音调 0.75)
            if (!this.spawnSoundPlayed) {
                this.spawnSoundPlayed = true;
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CINDER_SOVEREIGN_SPAWN, SoundSource.HOSTILE, 3.5F, 0.75F);
                ModLog.debug(null, this, "【日冕核心·烈焰统领】登场降临！播放唤醒登场音效 (音量 3.5, 音调 0.75)");
            }

            // 核心脉动音效：每 1.5 秒 (30 ticks) 触发一次 (音量 1.2, 音调 0.85 ~ 0.95)
            if (this.tickCount % 30 == 0 && this.isAlive()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CINDER_SOVEREIGN_PULSE, SoundSource.HOSTILE, 1.2F, 0.85F + this.random.nextFloat() * 0.1F);
            }

            // 自然自愈：每秒 (20 ticks) 自动恢复生命值 (常态 1.0 点；脱战且索敌范围内无玩家 >= 10s (200 ticks) 恢复 5.0 点)
            if (this.tickCount % 20 == 0 && this.isAlive() && this.getHealth() < this.getMaxHealth()) {
                float oldHp = this.getHealth();
                boolean outOfCombat = isOutOfCombat();
                float regenAmount = outOfCombat ? 5.0F : 1.0F;
                this.heal(regenAmount);
                ModLog.debug(null, this, "【日冕核心·烈焰统领】" + (outOfCombat ? "脱战疾速自愈 (+5.0)" : "自然光合自愈 (+1.0)")
                        + "：生命值从 " + String.format("%.1f", oldHp) + " 恢复至 " + String.format("%.1f", this.getHealth())
                        + " (索敌范围内玩家: " + (hasPlayerInRange ? "存在 (未脱战)" : "无 (已脱战)")
                        + "，脱战时长: " + String.format("%.1f", this.outOfCombatTimer / 20.0F) + "s)");
            }

            // 阶段判定：
            // 1. 一阶段血量 <= 40% (HP <= 160.0F) 触发进入第二阶段
            // 2. 第二阶段血量 > 50% (HP > 200.0F) 恢复为第一阶段
            if (!this.isPhase2() && this.getHealth() <= this.getMaxHealth() * 0.4F) {
                triggerPhase2();
            } else if (this.isPhase2() && this.getHealth() > this.getMaxHealth() * 0.5F) {
                revertToPhase1();
            }

            // 更新复合子部位实体位置与生命周期
            updatePartEntities();

            // 更新外环烈焰棒 9.0s 独立重铸与全损应急倒计时
            updateRodReconstruction();

            // 执行 AI 状态机与技能逻辑
            tickBossStateMachine();

            // 执行日冕过热环境扫描与惩罚
            tickArenaHeatGauge();

            // 冷却缩减
            if (skillIntervalCooldown > 0) skillIntervalCooldown--;
            if (barrageCooldown > 0) barrageCooldown--;
            if (beamCooldown > 0) beamCooldown--;
            if (meteorCooldown > 0) meteorCooldown--;
            if (piledriverCooldown > 0) piledriverCooldown--;
            if (emergencyPulseCooldown > 0) emergencyPulseCooldown--;
            if (isPhase2() && flameWaveCooldown > 0) flameWaveCooldown--;
            if (dashSoundCooldown > 0) dashSoundCooldown--;
            if (altitudeSoundCooldown > 0) altitudeSoundCooldown--;
        }
    }

    public void updateArmorAttributes() {
        if (this.getAttribute(Attributes.ARMOR) != null) {
            double armor = 12.0;
            if (!this.isPhase2()) {
                armor += getAliveShieldCount() * 3.0;
            }
            this.getAttribute(Attributes.ARMOR).setBaseValue(armor);
        }
    }

    private void triggerPhase2() {
        this.entityData.set(IS_PHASE_2, true);

        // 进入第二阶段：恢复完全的四面盾牌 (满血重组，重置位掩码)
        this.entityData.set(SHIELD_BITMASK, 0b1111);
        for (CinderShieldPartEntity shield : shields) {
            if (shield != null) {
                shield.restoreShield();
            }
        }
        updateArmorAttributes();
        if (getBossState() == STATE_STUNNED) {
            setBossState(STATE_IDLE);
        }
        onSkillFinished();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CINDER_SOVEREIGN_PHASE2_OVERLOAD, SoundSource.HOSTILE, 4.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.6, this.getZ(), 2, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 1.6, this.getZ(), 40, 1.5, 1.0, 1.5, 0.3);
        }
        ModLog.debug(null, this, "【日冕核心·烈焰统领】生命值低于 40%！进入【第二阶段·日冕暴怒】！恢复完全的四面盾牌并转化为高速切削利刃！第二阶段盾牌不再提供护甲加成与免伤！");
    }

    private void revertToPhase1() {
        this.entityData.set(IS_PHASE_2, false);
        updateArmorAttributes();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.8F, 0.8F);
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.6, this.getZ(), 30, 1.2, 0.8, 1.2, 0.1);
        }
        ModLog.debug(null, this, "【日冕核心·烈焰统领】生命值回升至 50% 以上 (" + String.format("%.1f", this.getHealth()) + "/" + this.getMaxHealth() + ")！暴怒状态平息，恢复为【第一阶段·常态凝聚】！");
    }

    /**
     * 计算第 index 面中环黑石重盾在世界坐标系中的精准绝对坐标：
     * - 一阶段：公转半径 2.0 格，待机动画 6.0s 两周旋转 (+pi/30 rad/tick)，60 ticks 垂直浮动 (振幅 0.15625 格)
     * - 二阶段：公转半径 4.5 格，高速逆时针回旋 1.0s 一周 (-pi/10 rad/tick)，20 ticks 垂直浮动
     * - 朝向联动：严格基于 Boss 身体偏航角 (yBodyRot / getYRot()) 进行世界坐标系变换
     */
    public Vec3 calculateShieldPos(int index) {
        float bodyYaw = this.getYRot();
        long t = this.tickCount;

        double radius;
        double rotBone;
        double yOffset;

        if (this.isCruciblePiledriverActive()) {
            if (this.piledriverPhase == 1) {
                // 阶段一：骤停并收拢，在核心正下方如重锤/金字塔桩头般咬合锁死
                double progress = Math.min(1.0, this.stateTicks / 16.0);
                radius = 2.0 * (1.0 - progress) + 0.5 * progress;
                rotBone = index * 90.0;
                yOffset = 1.125 * (1.0 - progress) + (-0.5) * progress;
            } else if (this.piledriverPhase == 2 || this.piledriverPhase == 3) {
                // 阶段二与三：4面黑石重盾咬合且深陷入地面 0.8 格深
                radius = 0.5;
                rotBone = index * 90.0;
                yOffset = -0.8;
            } else if (this.piledriverPhase == 4) {
                // 阶段四：从地里拔出弹开
                double progress = Math.min(1.0, Math.max(0.0, (this.stateTicks - 95.0) / 12.0));
                radius = 0.5 * (1.0 - progress) + 2.0 * progress;
                rotBone = index * 90.0;
                yOffset = -0.8 * (1.0 - progress) + 1.125 * progress;
            } else {
                radius = 2.0;
                rotBone = index * 90.0;
                yOffset = 1.125;
            }
        } else if (this.isPhase2()) {
            radius = 4.0;
            // 二阶段：1.0s (20 ticks) 一周高速逆向回旋 (-360°/s -> -18°/tick)
            rotBone = -t * 18.0 - index * 90.0;
            double bob = 0.15625 * Math.sin(t * (Math.PI / 10.0));
            yOffset = 1.0 + bob;
        } else {
            radius = 2.0;
            // 一阶段：待机动画 6.0s 两周旋转 (+720°/6s -> +6°/tick)
            rotBone = t * 6.0 - index * 90.0;
            double bob = 0.15625 * Math.sin(t * (Math.PI / 30.0));
            yOffset = 1.125 + bob;
        }

        double angleRad = Math.toRadians(bodyYaw - rotBone);
        double x = this.getX() + radius * Math.sin(angleRad);
        double z = this.getZ() - radius * Math.cos(angleRad);
        double y = this.getY() + yOffset;

        return new Vec3(x, y, z);
    }

    /**
     * 计算第 index 根外环日冕烈焰棒在世界坐标系中的精准绝对坐标：
     * - 数量 12 根，公转半径固定为 2.75 格 (44 像素)
     * - 一阶段：待机动画 6.0s 一周反向公转 (-360°/6s -> -3°/tick)
     * - 二阶段：1.0s 一周顺向高速回旋 (+360°/1s -> +18°/tick)
     * - 偶数棒/奇数棒基准高度交错 (1.75 格 / 1.50 格)，垂直浮动振幅 0.15625 格
     */
    public Vec3 calculateRodPos(int index) {
        float bodyYaw = this.getYRot();
        long t = this.tickCount;

        double radius = 2.75;
        double rotBone;
        double bob;

        if (this.isCruciblePiledriverActive()) {
            if (this.piledriverPhase == 1) {
                // 阶段一：12 根烈焰棒反转向上 90° 直立，化为高压推进喷口
                radius = 1.8;
                rotBone = index * 30.0;
                double yOffset = 1.9;
                double angleRad = Math.toRadians(bodyYaw - rotBone);
                double x = this.getX() + radius * Math.sin(angleRad);
                double z = this.getZ() - radius * Math.cos(angleRad);
                return new Vec3(x, this.getY() + yOffset, z);
            } else if (this.piledriverPhase == 2) {
                // 阶段二：直立跟随极速下坠
                radius = 1.8;
                rotBone = index * 30.0;
                double yOffset = 1.9;
                double angleRad = Math.toRadians(bodyYaw - rotBone);
                double x = this.getX() + radius * Math.sin(angleRad);
                double z = this.getZ() - radius * Math.cos(angleRad);
                return new Vec3(x, this.getY() + yOffset, z);
            } else if (this.piledriverPhase == 3) {
                // 阶段三：外围烈焰棒熄火垂落
                radius = 2.2;
                rotBone = index * 30.0;
                double yOffset = 0.4;
                double angleRad = Math.toRadians(bodyYaw - rotBone);
                double x = this.getX() + radius * Math.sin(angleRad);
                double z = this.getZ() - radius * Math.cos(angleRad);
                return new Vec3(x, this.getY() + yOffset, z);
            } else if (this.piledriverPhase == 4) {
                // 阶段四：重新点火复位
                double progress = Math.min(1.0, Math.max(0.0, (this.stateTicks - 95.0) / 12.0));
                radius = 2.2 * (1.0 - progress) + 2.75 * progress;
                rotBone = index * 30.0;
                double baseH = (index % 2 == 0) ? 1.75 : 1.50;
                double yOffset = 0.4 * (1.0 - progress) + baseH * progress;
                double angleRad = Math.toRadians(bodyYaw - rotBone);
                double x = this.getX() + radius * Math.sin(angleRad);
                double z = this.getZ() - radius * Math.cos(angleRad);
                return new Vec3(x, this.getY() + yOffset, z);
            }
        }

        if (this.isPhase2()) {
            // 二阶段：1.0s 一周顺向高速回旋 (+360°/s -> +18°/tick)
            rotBone = t * 18.0 - index * 30.0;
            bob = 0.15625 * Math.sin(t * (Math.PI / 10.0));
        } else {
            // 一阶段：6.0s 一周反向公转 (-360°/6s -> -3°/tick)
            rotBone = -t * 3.0 - index * 30.0;
            bob = 0.15625 * Math.sin(t * (Math.PI / 30.0));
        }

        double baseHeight = (index % 2 == 0) ? 1.75 : 1.50;
        double yOffset = baseHeight + bob;

        double angleRad = Math.toRadians(bodyYaw - rotBone);
        double x = this.getX() + radius * Math.sin(angleRad);
        double z = this.getZ() - radius * Math.cos(angleRad);
        double y = this.getY() + yOffset;

        return new Vec3(x, y, z);
    }

    /**
     * 更新复合部位实体位置与几何同步：
     * 中环 4 面盾牌与外环 12 根烈焰棒高精世界坐标同步，并在二阶段执行利刃接触伤害判定
     */
    private void updatePartEntities() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 1. 同步 4 面黑石重盾
        for (int i = 0; i < 4; i++) {
            if (shields[i] == null || shields[i].isRemoved()) {
                shields[i] = new CinderShieldPartEntity(serverLevel, this, i);
                serverLevel.addFreshEntity(shields[i]);
            }

            boolean isAlive = (this.getShieldBitmask() & (1 << i)) != 0;
            if (isAlive) {
                Vec3 sPos = this.calculateShieldPos(i);
                float shieldYaw = (float) Math.toDegrees(Math.atan2(-(sPos.x - this.getX()), sPos.z - this.getZ()));
                shields[i].updatePositionAndRotation(sPos.x, sPos.y, sPos.z, shieldYaw);

                // 二阶段切削利刃：接触碰撞伤害判定
                if (this.isPhase2()) {
                    checkBladeContactDamage(serverLevel, shields[i]);
                }
            } else {
                // 已破损盾牌移至地底避让碰撞
                double subY = this.getY() - 50.0;
                shields[i].updatePositionAndRotation(this.getX(), subY, this.getZ(), this.getYRot());
            }
        }

        // 2. 同步 12 根日冕烈焰棒
        for (int i = 0; i < 12; i++) {
            if (blazeRods[i] == null || blazeRods[i].isRemoved()) {
                blazeRods[i] = new CinderBlazeRodPartEntity(serverLevel, this, i);
                serverLevel.addFreshEntity(blazeRods[i]);
            }

            boolean isAlive = (this.getRodBitmask() & (1 << i)) != 0;
            if (isAlive) {
                Vec3 rPos = this.calculateRodPos(i);
                blazeRods[i].updatePositionAndRotation(rPos.x, rPos.y, rPos.z, this.getYRot());
            } else {
                double subY = this.getY() - 50.0;
                blazeRods[i].updatePositionAndRotation(this.getX(), subY, this.getZ(), this.getYRot());
            }
        }
    }

    /**
     * 二阶段切削利刃接触碰撞伤害：
     * 伤害：简单 12 / 普通 16 / 困难 22，40% 破甲，命中 CD 12 ticks
     * 举盾破盾 4s + 60 耐久损耗 + 30% 穿透伤
     */
    private void checkBladeContactDamage(ServerLevel level, CinderShieldPartEntity blade) {
        AABB box = blade.getBoundingBox().inflate(0.4);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive());

        long now = level.getGameTime();
        float baseDmg = switch (level.getDifficulty()) {
            case PEACEFUL -> 0.0F;
            case EASY -> 12.0F;
            case NORMAL -> 16.0F;
            case HARD -> 22.0F;
        };

        for (LivingEntity target : targets) {
            UUID id = target.getUUID();
            if (now < bladeHitCooldowns.getOrDefault(id, 0L)) continue;

            bladeHitCooldowns.put(id, now + 12); // 12 ticks 命中冷却
            DamageSource source = this.damageSources().mobAttack(this);

            if (target.isBlocking()) {
                // 格挡惩罚：破盾 4s，扣 60 耐久，30% 穿透伤害
                ItemStack shield = target.getItemBlockingWith();
                if (shield != null) {
                    EquipmentSlot slot = (target instanceof Player player && player.getUsedItemHand() == InteractionHand.MAIN_HAND)
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    shield.hurtAndBreak(60, target, slot);

                    BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
                    if (blocksAttacks != null && target instanceof Player player) {
                        blocksAttacks.disable(level, player, 4.0F, shield);
                    } else if (target instanceof Player player) {
                        player.getCooldowns().addCooldown(shield, 80);
                    }
                    target.stopUsingItem();
                }

                float piercedDmg = DamageCalculator.calculateDamage(baseDmg * 0.3F, source, target);
                target.hurtServer(level, source, piercedDmg);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 0.9F);
                ModLog.debug(this, target, "【二阶段·切削利刃】破防举盾目标！扣除 60 耐久，破盾 4.0 秒并造成 "
                        + String.format("%.2f", piercedDmg) + " 点穿透伤！");
            } else {
                // 40% 破甲普通伤害
                float finalDmg = DamageCalculator.calculateDamage(baseDmg, source, target);
                target.hurtServer(level, source, finalDmg);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.8F);
                ModLog.debug(this, target, "【二阶段·切削利刃】高速回旋利刃切中目标！造成 "
                        + String.format("%.2f", finalDmg) + " 点伤害！");
            }
        }
    }

    /**
     * 外环烈焰棒独立 9.0s (180 ticks) 重铸与全损应急倒计时
     */
    private void updateRodReconstruction() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        int currentRodMask = this.getRodBitmask();
        for (int i = 0; i < 12; i++) {
            if ((currentRodMask & (1 << i)) == 0) {
                rodRebuildTimers[i]++;
                // 后 1.5 秒 (150 ~ 180 ticks)：该槽位出现高亮火球充能提示
                if (rodRebuildTimers[i] >= 150) {
                    double angle = -this.orbitAngle * 1.5 + (i * Math.PI * 2.0 / 12.0);
                    double rx = this.getX() + Math.cos(angle) * 2.8;
                    double rz = this.getZ() + Math.sin(angle) * 2.8;
                    double ry = this.getY() + 1.2;
                    serverLevel.sendParticles(ParticleTypes.FLAME, rx, ry, rz, 3, 0.1, 0.1, 0.1, 0.05);
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, rx, ry, rz, 1, 0.05, 0.05, 0.05, 0.02);
                }

                // 180 ticks 整：重铸完毕满血归位
                if (rodRebuildTimers[i] >= 180) {
                    rodRebuildTimers[i] = 0;
                    currentRodMask |= (1 << i);
                    this.entityData.set(ROD_BITMASK, currentRodMask);
                    if (blazeRods[i] != null) {
                        blazeRods[i].restoreRod();
                    }
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 1.5F);
                    ModLog.debug(null, this, "【日冕烈焰棒 " + i + "】9.0 秒重铸完成！满血回归！当前存活: " + getAliveRodCount() + "/12");
                }
            }
        }

        // 全损应急检测：若 10s (200 ticks) 内外环全部损毁且脉冲冷却完毕
        if (getAliveRodCount() == 0) {
            allRodsBrokenWindowTimer++;
            if (allRodsBrokenWindowTimer <= 200 && emergencyPulseCooldown <= 0) {
                triggerEmergencyOverloadPulse(serverLevel);
            }
        } else {
            allRodsBrokenWindowTimer = 0;
        }
    }

    private void triggerEmergencyOverloadPulse(ServerLevel level) {
        emergencyPulseCooldown = 500; // 25s 全局冷却
        allRodsBrokenWindowTimer = 0;

        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 1.4F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.6, this.getZ(), 1, 0, 0, 0, 0);

        // 冲击波伤害与击退 (4 格范围)
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0),
                e -> e != this && e.isAlive());
        DamageSource pulseSrc = this.damageSources().mobAttack(this);

        for (LivingEntity e : nearby) {
            float dmg = DamageCalculator.calculateDamage(8.0F, pulseSrc, e);
            e.hurtServer(level, pulseSrc, dmg);
            e.igniteForSeconds(4.0F);
            Vec3 knockback = e.position().subtract(this.position()).normalize().scale(1.5).add(0, 0.4, 0);
            e.setDeltaMovement(knockback);
        }

        // 强制重构半数 (6 根) 烈焰棒
        int mask = this.getRodBitmask();
        int restoredCount = 0;
        for (int i = 0; i < 12 && restoredCount < 6; i++) {
            if ((mask & (1 << i)) == 0) {
                mask |= (1 << i);
                rodRebuildTimers[i] = 0;
                if (blazeRods[i] != null) blazeRods[i].restoreRod();
                restoredCount++;
            }
        }
        this.entityData.set(ROD_BITMASK, mask);
        onSkillFinished();
        ModLog.debug(null, this, "【全损应急脉冲触发】外环全毁！核心过载爆发 4 格排斥波，强制重构 6 根烈焰棒！");
    }

    public void onShieldDestroyed(int index) {
        int mask = this.getShieldBitmask() & ~(1 << index);
        this.entityData.set(SHIELD_BITMASK, mask);

        // 动态同步更新护甲值属性 (一阶段基础 12.0 + 每存活盾 3.0，二阶段盾牌不提供护甲加成固定为 12.0)
        int alive = getAliveShieldCount();
        updateArmorAttributes();
        ModLog.debug(null, this, "【黑石重盾 " + index + "】损毁！当前剩余重盾: " + alive + "/4，本体当前基础护甲值: "
                + (this.getAttribute(Attributes.ARMOR) != null ? this.getAttribute(Attributes.ARMOR).getBaseValue() : 12.0));

        // 4 面重盾全毁：触发本体【过载眩晕】5.0 秒
        if (alive == 0) {
            setBossState(STATE_STUNNED);
        }
    }

    public void onBlazeRodDestroyed(int index) {
        int mask = this.getRodBitmask() & ~(1 << index);
        this.entityData.set(ROD_BITMASK, mask);
        rodRebuildTimers[index] = 0;
        ModLog.debug(null, this, "【日冕烈焰棒 " + index + "】损毁！进入 9.0s 独立重铸倒计时！当前存活: " + getAliveRodCount() + "/12");
    }

    /**
     * AI 状态机与技能执行
     */
    private void tickBossStateMachine() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        stateTicks++;
        LivingEntity target = this.getTarget();
        if (!isValidTarget(target)) {
            target = findNearestValidPlayer();
            this.setTarget(target);
        }

        // 索敌与警戒音效：首次锁定目标时触发古老日冕号角合成警报 (音量 2.0, 音调 1.1)
        if (target != null && target.isAlive()) {
            if (!this.hadTarget) {
                this.hadTarget = true;
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CINDER_SOVEREIGN_SPOT_TARGET, SoundSource.HOSTILE, 2.0F, 1.1F);
                ModLog.debug(this, target, "【日冕核心·烈焰统领】首次锁定目标，鸣响日冕号角警报 (音量 2.0, 音调 1.1)！");
            }
        } else {
            this.hadTarget = false;
        }

        // 状态分发处理
        switch (this.getBossState()) {
            case STATE_STUNNED -> handleStunnedState(serverLevel);
            case STATE_SHOCKWAVE -> handleShockwaveState(serverLevel);
            case STATE_BARRAGE -> handleBarrageState(serverLevel, target);
            case STATE_BEAM_CHARGE -> handleBeamChargeState(serverLevel, target);
            case STATE_BEAM_FIRE -> handleBeamFireState(serverLevel, target);
            case STATE_METEOR -> handleMeteorState(serverLevel, target);
            case STATE_CRUCIBLE_PILEDRIVER -> handleCruciblePiledriverState(serverLevel, target);
            case STATE_IDLE -> handleIdleAndKitingState(serverLevel, target);
        }
    }

    private void handleStunnedState(ServerLevel level) {
        // 落地瘫痪无法行动 (持续 100 ticks = 5.0 秒，若被重锤打断则延长至 130 ticks = 6.5 秒)
        this.setDeltaMovement(0, -0.4, 0);
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.2, this.getZ(), 3, 0.3, 0.3, 0.3, 0.05);

        int maxStunTicks = 100 + this.piledriverStunExtraTicks;
        if (stateTicks >= maxStunTicks) {
            this.piledriverStunExtraTicks = 0;
            // 眩晕结束，爆发冲击波并聚变重组 4 面重盾
            setBossState(STATE_SHOCKWAVE);
        }
    }

    private void handleShockwaveState(ServerLevel level) {
        if (stateTicks == 1) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.8F, 1.2F);
            level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.6, this.getZ(), 1, 0, 0, 0, 0);

            // 击退近战玩家 3 格并造成 6 点伤害
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.5),
                    e -> e != this && e.isAlive());
            DamageSource src = this.damageSources().mobAttack(this);

            for (LivingEntity v : victims) {
                float dmg = DamageCalculator.calculateDamage(6.0F, src, v);
                v.hurtServer(level, src, dmg);
                Vec3 push = v.position().subtract(this.position()).normalize().scale(1.8).add(0, 0.3, 0);
                v.setDeltaMovement(push);
            }

            // 4 面黑石重盾瞬间满血重组！
            this.entityData.set(SHIELD_BITMASK, 0b1111);
            for (CinderShieldPartEntity shield : shields) {
                if (shield != null) shield.restoreShield();
            }
            updateArmorAttributes();
            ModLog.debug(null, this, "【过载眩晕结束】核心爆发聚变环震退玩家，4 面黑石重盾瞬间满血重组！");
        }

        if (stateTicks >= 20) {
            setBossState(STATE_IDLE);
            onSkillFinished();
        }
    }

    private void handleBarrageState(ServerLevel level, LivingEntity target) {
        if (!isValidTarget(target)) {
            target = findNearestValidPlayer();
            if (target != null) this.setTarget(target);
        }
        if (target == null) {
            setBossState(STATE_IDLE);
            onSkillFinished();
            return;
        }
        // 蓄力 30 ticks (1.5s) 后开始连射
        if (stateTicks >= 30) {
            int aliveRods = getAliveRodCount();
            if (aliveRods > 0) {
                // 每 2 ticks 射出一枚微型日冕火球
                int shotIndex = (stateTicks - 30) / 2;
                if ((stateTicks - 30) % 2 == 0 && shotIndex < aliveRods && target != null) {
                    Vec3 dir = target.getEyePosition().subtract(this.position().add(0, 1.6, 0));
                    CinderFireballEntity fireball = new CinderFireballEntity(level, this, dir);
                    level.addFreshEntity(fireball);
                    level.playSound(null, this.getX(), this.getY() + 1.6, this.getZ(),
                            SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.2F + (shotIndex * 0.05F));
                }

                if (shotIndex >= aliveRods) {
                    setBossState(STATE_IDLE);
                    barrageCooldown = 240; // 12s
                    onSkillFinished();
                }
            } else {
                // 外环全毁降级：核心直接喷吐不稳定熔渣火球 (10 点伤，2 格燃烧)
                if (stateTicks == 32 && target != null) {
                    Vec3 dir = target.getEyePosition().subtract(this.position().add(0, 1.6, 0));
                    CinderFireballEntity slagBall = new CinderFireballEntity(level, this, dir);
                    level.addFreshEntity(slagBall);
                    level.playSound(null, this.getX(), this.getY() + 1.6, this.getZ(),
                            SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1.5F, 0.8F);
                    ModLog.debug(null, this, "【日冕弹幕降级】外环烈焰棒全毁，核心喷吐巨型不稳定熔渣火球！");
                }
                if (stateTicks >= 40) {
                    setBossState(STATE_IDLE);
                    barrageCooldown = 240;
                    onSkillFinished();
                }
            }
        }
    }

    private void handleBeamChargeState(ServerLevel level, LivingEntity target) {
        // 1.8s (36 ticks) 蓄力音效与粒子聚集
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.6, this.getZ(), 4, 0.2, 0.2, 0.2, 0.05);

        if (stateTicks == 1) {
            level.playSound(null, this.getX(), this.getY() + 1.6, this.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.5F);
        }

        if (stateTicks >= 36) {
            int aliveRods = getAliveRodCount();
            if (aliveRods >= 4) {
                setBossState(STATE_BEAM_FIRE);
            } else {
                // 阶梯 III (0 ~ 3 根)：缺乏棱镜，聚能失败反噬自爆！
                triggerBeamBacklash(level);
            }
        }
    }

    private void handleBeamFireState(ServerLevel level, LivingEntity target) {
        int aliveRods = getAliveRodCount();
        Vec3 look = target != null ? target.getEyePosition().subtract(this.position().add(0, 1.6, 0)).normalize() : this.getViewVector(1.0F);

        if (aliveRods >= 8) {
            // 阶梯 I (8 ~ 12 根)：完全聚焦死光！直径 1.2 格，射程 32 格，持续 50 ticks (2.5s)
            // 每 5 ticks 造成 8 点真实破甲贯穿伤，穿透掩体与破盾
            if (stateTicks % 5 == 0) {
                performFocusedLaserBeam(level, look, 32.0, 8.0F);
            }
        } else {
            // 阶梯 II (4 ~ 7 根)：散焦热风暴！16 格 60° 扇形热风暴
            if (stateTicks % 10 == 0) {
                performConeHeatstorm(level, look, 16.0, 6.0F);
            }
        }

        if (stateTicks >= 50) {
            setBossState(STATE_IDLE);
            beamCooldown = 440; // 22s
            onSkillFinished();
        }
    }

    private void triggerBeamBacklash(ServerLevel level) {
        // 反噬：本体受 25 点无视护甲伤害，自身眩晕 2 秒
        level.playSound(null, this.getX(), this.getY() + 1.6, this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.6, this.getZ(), 1, 0, 0, 0, 0);

        this.hurtServer(level, this.damageSources().generic(), 25.0F);
        ModLog.debug(null, this, "【聚变射线反噬】外环仅存 " + getAliveRodCount() + " 根！约束崩溃，本体承受 25 点反噬伤害并眩晕 2 秒！");

        setBossState(STATE_STUNNED);
        beamCooldown = 440;
    }

    private void performFocusedLaserBeam(ServerLevel level, Vec3 dir, double range, float damage) {
        Vec3 start = this.position().add(0, 1.6, 0);
        Vec3 end = start.add(dir.scale(range));

        // 沿光束轨迹发射高能等离子粒子
        int steps = (int) (range * 2);
        for (int i = 0; i < steps; i++) {
            Vec3 p = start.add(dir.scale(i * 0.5));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        level.playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.8F);

        // 贯穿检测与破盾昏迷
        AABB beamBox = new AABB(start, end).inflate(1.2);
        List<LivingEntity> hitList = level.getEntitiesOfClass(LivingEntity.class, beamBox, e -> e != this && e.isAlive());

        DamageSource src = this.damageSources().magic();
        for (LivingEntity victim : hitList) {
            if (victim.isBlocking()) {
                ItemStack shield = victim.getItemBlockingWith();
                if (shield != null && victim instanceof Player player) {
                    player.getCooldowns().addCooldown(shield, 60); // 昏迷破防 3 秒
                    victim.stopUsingItem();
                }
            }
            float finalDmg = DamageCalculator.calculateDamage(damage, src, victim);
            victim.hurtServer(level, src, finalDmg);
            victim.igniteForSeconds(4.0F);
            ModLog.debug(this, victim, "【聚变死光】贯穿扫射命中目标！造成 " + String.format("%.2f", finalDmg) + " 点真实伤害！");
        }
    }

    private void performConeHeatstorm(ServerLevel level, Vec3 look, double range, float damage) {
        Vec3 origin = this.position().add(0, 1.6, 0);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.8F);

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range),
                e -> e != this && e.isAlive());
        DamageSource src = this.damageSources().onFire();

        for (LivingEntity v : victims) {
            Vec3 toV = v.position().subtract(origin);
            if (toV.length() <= range && toV.normalize().dot(look) >= 0.5) { // 60° 锥形范围
                float finalDmg = DamageCalculator.calculateDamage(damage, src, v);
                v.hurtServer(level, src, finalDmg);
                v.igniteForSeconds(3.0F);
                v.setDeltaMovement(toV.normalize().scale(1.0).add(0, 0.2, 0));
                ModLog.debug(this, v, "【散焦热风暴】热风波及目标！造成 " + String.format("%.2f", finalDmg) + " 点火焰击退伤害！");
            }
        }
    }

    private void handleMeteorState(ServerLevel level, LivingEntity target) {
        if (!isValidTarget(target)) {
            target = findNearestValidPlayer();
            if (target != null) this.setTarget(target);
        }
        if (target == null) {
            setBossState(STATE_IDLE);
            onSkillFinished();
            return;
        }
        // 2.0s (40 ticks) 引导，核心升空
        this.setDeltaMovement(0, 0.2, 0);

        if (stateTicks == 20 && target != null) {
            // 在目标及周围 12 格范围内召唤 3 ~ 5 颗从天而降的熔岩陨石
            int count = 3 + this.random.nextInt(3);
            for (int i = 0; i < count; i++) {
                double ox = (this.random.nextDouble() - 0.5) * 12.0;
                double oz = (this.random.nextDouble() - 0.5) * 12.0;
                double mx = target.getX() + ox;
                double mz = target.getZ() + oz;
                double my = target.getY() + 20.0 + (i * 2.5);

                CinderMeteorEntity meteor = new CinderMeteorEntity(level, this, mx, my, mz, target.getY());
                level.addFreshEntity(meteor);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.5F);
            ModLog.debug(null, this, "【地狱熔岩流星雨】召唤 " + count + " 颗熔岩流星向目标区域轰击！");
        }

        if (stateTicks >= 40) {
            setBossState(STATE_IDLE);
            meteorCooldown = 560; // 28s
            onSkillFinished();
        }
    }

    private void handleIdleAndKitingState(ServerLevel level, LivingEntity target) {
        if (!isValidTarget(target)) {
            Player nearest = findNearestValidPlayer();
            this.setTarget(nearest);
            if (nearest == null) {
                // 索敌范围内无有效玩家（处于脱战状态），平滑减速维持低速悬浮浮动
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0, Math.sin(this.tickCount * 0.05) * 0.02, 0));
                return;
            }
            target = nearest;
        }

        double dist = this.distanceTo(target);

        // 公共技能间隔检测：需等待上个技能结束后 2.0 秒 (40 ticks) 才能释放下一个技能
        if (this.skillIntervalCooldown <= 0) {
            // 近战技能：日冕破城杵 · 炽核坠碾 (Crucible Piledriver) 触发 AI 条件检测：
            // 1. 冷却时间：14.0 秒 (280 ticks)
            // 2. 贴脸判定：目标玩家与 BOSS 的水平欧氏距离连续 1.2 秒 (24 ticks) 小于 4.0 格
            // 3. 正下方盲区判定：目标玩家位于 BOSS 正下方圆柱体内 (垂直距离 < 4.5 格，水平偏移 < 2.0 格)
            if (piledriverCooldown <= 0 && target instanceof Player player && player.isAlive()) {
                double dx = player.getX() - this.getX();
                double dz = player.getZ() - this.getZ();
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                double dy = this.getY() - player.getY(); // 玩家在 Boss 正下方的垂直距离

                boolean isBlindSpot = dy > 0.0 && dy < 4.5 && horizontalDist < 2.0;

                if (horizontalDist < 4.0) {
                    closeProximityTicks++;
                } else {
                    closeProximityTicks = 0;
                }

                boolean isCloseFace = closeProximityTicks >= 24;

                if (isBlindSpot || isCloseFace) {
                    startCruciblePiledriver(level, player, isBlindSpot);
                    return;
                }
            } else {
                closeProximityTicks = 0;
            }

            // 主动技能触发判断
            if (barrageCooldown <= 0) {
                setBossState(STATE_BARRAGE);
                return;
            }
            if (beamCooldown <= 0) {
                setBossState(STATE_BEAM_CHARGE);
                return;
            }
            if (meteorCooldown <= 0) {
                setBossState(STATE_METEOR);
                return;
            }
            if (isPhase2() && flameWaveCooldown <= 0) {
                performGroundFlameWave(level);
                flameWaveCooldown = 280; // 14s
                onSkillFinished();
                return;
            }
        } else {
            closeProximityTicks = 0;
        }

        // 防作弊 1：高空垫高下压 (玩家比 Boss 高 4 格以上)
        if (target.getY() - this.getY() > 4.0) {
            this.setDeltaMovement(0, 0.6, 0); // 垂直冲刺上升
            if (this.altitudeSoundCooldown <= 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CINDER_SOVEREIGN_ALTITUDE_CHANGE, SoundSource.HOSTILE, 2.0F, 0.55F);
                this.altitudeSoundCooldown = 40; // 2s 冷却
            }
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 2.0, target.getZ(), 10, 1.0, 0.2, 1.0, 0.05);
            target.setDeltaMovement(0, -0.8, 0); // 重力火环下压
            ModLog.debug(this, target, "【防作弊触发】玩家处于高空垫高！Boss 垂直冲刺并施加重力火环将其下压！");
        }

        // 悬浮巡航走位 (Kiting)：保持 12 ~ 18 格距离 (巡航移动速度全时期降至 80%)
        Vec3 diff = target.position().subtract(this.position());
        Vec3 moveVec = Vec3.ZERO;

        if (dist < 8.0) {
            // 突进太近，向后拉开距离 (-0.25 * 0.8 = -0.20)
            moveVec = diff.normalize().scale(-0.20);
            if (this.dashSoundCooldown <= 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.CINDER_SOVEREIGN_DASH, SoundSource.HOSTILE, 1.5F, 1.0F);
                this.dashSoundCooldown = 30; // 1.5s 冷却
            }
        } else if (dist > 22.0) {
            // 距离太远，向前逼近 (0.28 * 0.8 = 0.224)
            moveVec = diff.normalize().scale(0.224);
        } else {
            // 中远距离侧向环绕走位 (0.20 * 0.8 = 0.16)
            Vec3 side = new Vec3(-diff.z, 0, diff.x).normalize().scale(0.16);
            moveVec = side;
        }

        // 维持离地低频简谐浮动 (巡航基准高度再度降低 0.6 格：从 3.3 下调至 2.7，避免飞行过高导致玩家攻击困难)
        double desiredY = target.getY() + 2.7 + Math.sin(this.tickCount * 0.08) * 0.35;
        double yStep = (desiredY - this.getY()) * 0.064; // 垂直跟踪步长降至 80% (0.08 * 0.8 = 0.064)

        this.setDeltaMovement(moveVec.x, yStep, moveVec.z);
    }

    /**
     * 二阶段特技：贴地烈焰跳绳 (Ground Flame Wave)
     */
    private void performGroundFlameWave(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.8F, 1.5F);

        // 辐射贴地扩散火浪 (高度 1.2 格，以 8 格/s 速度扩散至 20 格)
        BlockPos ground = this.blockPosition();
        while (ground.getY() > level.getMinY() && level.isEmptyBlock(ground)) {
            ground = ground.below();
        }

        double waveY = ground.getY() + 1.0;
        List<LivingEntity> players = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20.0, 2.0, 20.0),
                e -> e instanceof Player && e.isAlive());

        DamageSource flameSrc = this.damageSources().onFire();
        for (LivingEntity p : players) {
            // 玩家必须起跳躲避！若高度未脱离 1.2 格，受到 14 点火焰伤害并击飞浮空 3 秒
            if (p.getY() <= waveY + 1.2 && p.getDeltaMovement().y <= 0.05) {
                float dmg = DamageCalculator.calculateDamage(14.0F, flameSrc, p);
                p.hurtServer(level, flameSrc, dmg);
                p.igniteForSeconds(5.0F);
                p.setDeltaMovement(0, 0.85, 0); // 击飞浮空
                ModLog.debug(this, p, "【烈焰跳绳扫中】玩家未及时起跳！承受 14 点火焰伤害并被击飞浮空！");
            }
        }

        // 地面扩散环形粒子
        for (int r = 2; r <= 18; r += 2) {
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                level.sendParticles(ParticleTypes.FLAME, this.getX() + Math.cos(rad) * r, waveY + 0.1, this.getZ() + Math.sin(rad) * r, 2, 0.1, 0.05, 0.1, 0.02);
            }
        }
    }

    /**
     * 启动近战重型惩戒技能：日冕破城杵 · 炽核坠碾 (Crucible Piledriver)
     */
    private void startCruciblePiledriver(ServerLevel level, Player target, boolean isBlindSpot) {
        setBossState(STATE_CRUCIBLE_PILEDRIVER);
        this.piledriverCooldown = 280; // 冷却 14.0 秒 (280 ticks)
        this.closeProximityTicks = 0;
        this.piledriverPhase = 1;
        this.stateTicks = 0;

        // 探测目标地面高度：垂直向下寻找首个非空气非流体的固体方块
        BlockPos currentPos = this.blockPosition();
        BlockPos.MutableBlockPos cursor = currentPos.mutable();
        while (cursor.getY() > level.getMinY() && (level.isEmptyBlock(cursor) || level.getBlockState(cursor).liquid())) {
            cursor.move(Direction.DOWN);
        }
        this.piledriverTargetGroundY = cursor.getY() + 1.0;

        ModLog.debug(this, target, "【日冕核心·烈焰统领】触发近战重型惩戒技能【日冕破城杵 · 炽核坠碾】！触发诱因: "
                + (isBlindSpot ? "正下方盲区偷伤害判定" : "连续 1.2 秒贴脸近身判定")
                + "，目标地面 Y: " + String.format("%.2f", this.piledriverTargetGroundY));
    }

    /**
     * 近战技能：日冕破城杵 · 炽核坠碾 (Crucible Piledriver) 四阶段分步执行逻辑
     * [前摇蓄势: 1.6 秒] ──► [超重力砸地: 0.15 秒] ──► [过载卡地/虚弱窗口: 3.0 秒] ──► [拔出重升: 0.6 秒]
     */
    private void handleCruciblePiledriverState(ServerLevel level, LivingEntity target) {
        if (this.stateTicks <= 32) {
            // === 阶段一：蓄势下压（Telegraph / 前摇）—— 1.6 秒 (32 ticks) ===
            this.piledriverPhase = 1;

            // 1. 盾牌合拢音效 (第 1 tick 播放沉重的卡扣重响)
            if (this.stateTicks == 1) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 1.6F, 0.75F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 1.8F, 0.6F);
            }

            // 2. 机身姿态：悬停在原地微微向上吸附后坐 0.8 格 (在普通巡航高度 2.7 格基础上吸附后坐 0.8 格)，全壳裂缝爆发出超高频红光闪烁
            double hoverTargetY = this.piledriverTargetGroundY + 2.7 + 0.8;
            double dy = (hoverTargetY - this.getY()) * 0.15;
            this.setDeltaMovement(0, dy, 0);

            // 全壳裂缝高频红光与熔岩粒子
            level.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 1.2, this.getZ(), 3, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.2, this.getZ(), 4, 0.3, 0.3, 0.3, 0.05);

            // 3. 外环烈焰棒：12 根反转向上直立，化为 12 根垂直喷射高热白炽尾焰的高压推进喷口
            // minecraft:flame 粒子以 1.5 格/tick 速度向正上方极速喷涌
            int rodMask = this.getRodBitmask();
            for (int i = 0; i < 12; i++) {
                if ((rodMask & (1 << i)) != 0) {
                    Vec3 rPos = calculateRodPos(i);
                    level.sendParticles(ParticleTypes.FLAME, rPos.x, rPos.y + 0.3, rPos.z, 2, 0.05, 0.0, 0.05, 0.0);
                    level.sendParticles(ParticleTypes.FLAME, rPos.x, rPos.y + 0.4, rPos.z, 0, 0.0, 1.5, 0.0, 1.0);
                }
            }

            // 4. 地面预警指示 (Telegraph Decal)：以目标地面为圆心，投射出一个半径 3.5 格的高亮橙红色日冕裂隙光圈
            // 光圈边缘粒子在 1.6 秒内迅速向中心收缩，收缩至中心点的瞬间即为下砸瞬间
            double shrinkRadius = 3.5 * Math.max(0.0, 1.0 - (this.stateTicks / 32.0));
            DustParticleOptions orangeDust = new DustParticleOptions(0xFF640F, 1.6F);
            for (int deg = 0; deg < 360; deg += 15) {
                double rad = Math.toRadians(deg);
                double px = this.getX() + shrinkRadius * Math.cos(rad);
                double pz = this.getZ() + shrinkRadius * Math.sin(rad);
                level.sendParticles(orangeDust, px, this.piledriverTargetGroundY + 0.05, pz, 1, 0.02, 0.01, 0.02, 0.0);
                if (deg % 30 == 0) {
                    level.sendParticles(ParticleTypes.SMALL_FLAME, px, this.piledriverTargetGroundY + 0.05, pz, 1, 0.01, 0.01, 0.01, 0.01);
                }
            }

            // 5. 蓄力音效：持续 1.6 秒反重力涡轮升压鸣响 (trial_spawner.ominous_activate 音调由 0.6 线性爬升至 1.8，混入 lava.extinguish)
            float pitch = 0.6F + (this.stateTicks / 32.0F) * 1.2F;
            if (this.stateTicks % 4 == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE, SoundSource.HOSTILE, 1.8F, pitch);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 0.6F, 1.0F + (pitch * 0.3F));
            }

            if (this.stateTicks == 32) {
                this.piledriverPhase = 2;
                ModLog.debug(null, this, "【炽核坠碾·前摇结束】蓄力达极限，黑石重锤桩头锁定地面，即将发动超重力贯穿砸地！");
            }
        } else if (this.piledriverPhase == 2) {
            // === 阶段二：超重力穿刺砸地（Impact / 判定）—— 约 0.15 秒 (3 ticks) ===
            // 动力学表现：蓄势结束瞬间，以 2.8 格/tick 的下坠速度笔直砸向地面，完全无法被击退打断
            this.setDeltaMovement(0, -2.8, 0);

            boolean reachedGround = this.onGround() || this.getY() <= this.piledriverTargetGroundY + 0.3;
            if (reachedGround || this.stateTicks >= 35) {
                executePiledriverImpact(level);
                this.piledriverPhase = 3;
            }
        } else if (this.piledriverPhase == 3) {
            // === 阶段三：卡地卡死与反击窗口（Recovery / 玩家爆发期）—— 3.0 秒 (60 ticks) ===
            // 完全失去移动力：本体卡在地表无法浮空或自转
            this.setDeltaMovement(0, 0, 0);
            this.setPos(this.getX(), this.piledriverTargetGroundY + 0.4, this.getZ());

            // 视觉：核心剧烈颤抖，缝隙狂喷黑烟粒子 (minecraft:large_smoke)
            double jitterX = (this.random.nextDouble() - 0.5) * 0.08;
            double jitterZ = (this.random.nextDouble() - 0.5) * 0.08;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX() + jitterX, this.getY() + 1.2, this.getZ() + jitterZ, 4, 0.25, 0.3, 0.25, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 3, 0.3, 0.2, 0.3, 0.02);

            // 音效：发出齿轮卡死受阻的机械咬合摩擦声 (block.grindstone.use 音调 0.7 循环播放)
            if (this.stateTicks % 8 == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 1.5F, 0.7F);
            }

            // 阶段三持续满 60 ticks (从 36 持续到 95 ticks)
            if (this.stateTicks >= 95) {
                this.piledriverPhase = 4;
                ModLog.debug(null, this, "【炽核坠碾·硬直结束】卡地 3.0 秒虚弱窗口闭合，核心启动过载排气重升！");
            }
        } else if (this.piledriverPhase == 4) {
            // === 阶段四：起爆重升（Reset / 复位）—— 0.6 秒 (12 ticks) ===
            int resetTick = this.stateTicks - 95;

            // 收招动作：第 1 tick 核心爆发一次无伤害的排气脉冲 (仅产生推开玩家 1.5 格的轻微气浪推力)
            if (resetTick == 1) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 2.0F, 0.8F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1.5F, 0.9F);
                level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 25, 0.8, 0.4, 0.8, 0.15);

                List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(4.0), LivingEntity::isAlive);
                for (Player p : nearbyPlayers) {
                    Vec3 push = p.position().subtract(this.position()).horizontal().normalize().scale(0.65).add(0, 0.2, 0);
                    p.setDeltaMovement(push);
                }
            }

            // 4 面黑石盾从地里强行拔出并弹开，烈焰棒重新点火，BOSS 平滑拉升回与普通巡航高度一致的离地高度 (离地 2.7 格)
            double targetCruiseY = (target != null && target.isAlive()) ? target.getY() + 2.7 : this.piledriverTargetGroundY + 2.7;
            double remainingTicks = Math.max(1, 13 - resetTick);
            double vy = (targetCruiseY - this.getY()) / remainingTicks;
            this.setDeltaMovement(0, vy, 0);

            if (resetTick >= 12) {
                // 恢复正常巡航 IDLE，高度与普通巡航高度完全平滑衔接
                this.setPos(this.getX(), targetCruiseY, this.getZ());
                this.setDeltaMovement(0, 0, 0);
                setBossState(STATE_IDLE);
                this.piledriverPhase = 0;
                onSkillFinished();
                ModLog.debug(null, this, "【炽核坠碾·重升复位完成】拉升至普通巡航高度 (离地 2.7 格)，重置技能轴并进入 2.0 秒公共技能间隔。");
            }
        }
    }

    /**
     * 执行超重力穿刺砸地 (Impact) 判定与地形瓦解
     */
    private void executePiledriverImpact(ServerLevel level) {
        Vec3 impactPos = new Vec3(this.getX(), this.piledriverTargetGroundY, this.getZ());
        this.setPos(this.getX(), this.piledriverTargetGroundY + 0.4, this.getZ());

        // 1. 音效：类似万吨锻造气锤砸地的极端重击轰鸣 (block.heavy_core.fall 叠加 entity.iron_golem.attack 音量 3.0，音调 0.5)
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.HEAVY_CORE_FALL, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.7F);

        // 2. 粒子表现：全屏震撼冲击波与玄武岩碎屑飞溅
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactPos.x, impactPos.y + 0.5, impactPos.z, 2, 0, 0, 0, 0);
        BlockParticleOption basaltParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BASALT.defaultBlockState());
        level.sendParticles(basaltParticle, impactPos.x, impactPos.y + 0.5, impactPos.z, 60, 1.2, 0.6, 1.2, 0.3);
        level.sendParticles(ParticleTypes.LAVA, impactPos.x, impactPos.y + 0.5, impactPos.z, 30, 1.5, 0.8, 1.5, 0.2);

        // 3. 伤害与判定半径：
        // 判定 1：重核贯穿中心 (Direct Hit) 0.0 ~ 1.5 格
        // 判定 2：地脉冲击波 (Shockwave) 1.6 ~ 3.5 格
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.5),
                e -> e != this && e.isAlive());

        DamageSource physicalSrc = this.damageSources().mobAttack(this);
        DamageSource fireMagicSrc = this.damageSources().magic();

        for (LivingEntity v : victims) {
            double hDist = Math.sqrt(v.distanceToSqr(this.getX(), v.getY(), this.getZ()));

            if (hDist <= 1.5) {
                // === 重核贯穿中心 (Direct Hit) ===
                // 24.0 点 (12 颗心：18 物理钝击 + 6 火焰魔法)
                // 强行碎盾：无视盾牌防御，打断举盾并施加 6.0 秒盾牌瘫痪
                boolean wasBlocking = v.isBlocking();
                if (wasBlocking) {
                    ItemStack shield = v.getItemBlockingWith();
                    if (shield != null && v instanceof Player player) {
                        EquipmentSlot slot = (player.getUsedItemHand() == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        shield.hurtAndBreak(40, player, slot);

                        BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
                        if (blocksAttacks != null) {
                            blocksAttacks.disable(level, player, 6.0F, shield);
                        } else {
                            player.getCooldowns().addCooldown(shield, 120);
                        }
                        v.stopUsingItem();
                        level.playSound(null, v.getX(), v.getY(), v.getZ(),
                                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.5F, 0.8F);
                    }
                }

                float physDmg = DamageCalculator.calculateDamage(18.0F, physicalSrc, v);
                float magicDmg = DamageCalculator.calculateDamage(6.0F, fireMagicSrc, v);
                v.hurtServer(level, physicalSrc, physDmg);
                v.hurtServer(level, fireMagicSrc, magicDmg);
                v.igniteForSeconds(4.0F);

                // 直接击中未举盾玩家造成 1.2 格/tick 极高浮空
                if (!wasBlocking) {
                    v.setDeltaMovement(v.getDeltaMovement().x, 1.2, v.getDeltaMovement().z);
                }

                ModLog.debug(this, v, "【炽核坠碾·重核直击】直接贯穿！造成物理伤害 " + String.format("%.2f", physDmg)
                        + " + 火焰魔法伤害 " + String.format("%.2f", magicDmg)
                        + (wasBlocking ? "，强行碎盾并瘫痪盾牌 6.0 秒！" : "，未举盾造成 1.2 格/tick 极高浮空！"));
            } else if (hDist <= 3.5) {
                // === 地脉冲击波 (Shockwave) ===
                // 10.0 点 (5 颗心：纯物理冲击波)
                // 可完全举盾格挡 (扣除盾牌 25 点耐久)；未格挡玩家击退 4 格并浮空 2 格
                if (v.isBlocking()) {
                    ItemStack shield = v.getItemBlockingWith();
                    if (shield != null && v instanceof Player player) {
                        EquipmentSlot slot = (player.getUsedItemHand() == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        shield.hurtAndBreak(25, player, slot);
                    }
                    level.playSound(null, v.getX(), v.getY(), v.getZ(),
                            SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.2F, 0.9F);
                    ModLog.debug(this, v, "【炽核坠碾·地脉冲击波】玩家成功格挡！扣除盾牌 25 点耐久！");
                } else {
                    float shockDmg = DamageCalculator.calculateDamage(10.0F, physicalSrc, v);
                    v.hurtServer(level, physicalSrc, shockDmg);
                    Vec3 push = v.position().subtract(this.position()).horizontal().normalize().scale(1.2).add(0, 0.65, 0);
                    v.setDeltaMovement(push);
                    ModLog.debug(this, v, "【炽核坠碾·地脉冲击波】未格挡命中！造成 " + String.format("%.2f", shockDmg)
                            + " 点物理冲击波伤害，击退 4 格并浮空 2 格！");
                }
            }
        }

        // 4. 地形与方块影响：
        // 4.1 撞击点周围 3×3 范围内的所有非防爆方块 (硬度 < 3.0，如泥土、圆石、木头、脚手架) 直接震碎瓦解
        BlockPos centerBlock = BlockPos.containing(impactPos);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                for (int oy = -1; oy <= 1; oy++) {
                    BlockPos targetPos = centerBlock.offset(ox, oy, oz);
                    BlockState bs = level.getBlockState(targetPos);
                    float destroySpeed = bs.getDestroySpeed(level, targetPos);
                    if (!bs.isAir() && destroySpeed >= 0.0F && destroySpeed < 3.0F && !bs.is(Blocks.BEDROCK) && !bs.is(Blocks.BARRIER)) {
                        level.destroyBlock(targetPos, true, this);
                    }
                }
            }
        }

        // 4.2 撞击中心原地产生直径 3 格的微型玄武岩裂坑 (将地面表层方块转换为玄武岩)
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                BlockPos surfacePos = centerBlock.offset(ox, -1, oz);
                BlockState currentGround = level.getBlockState(surfacePos);
                if (!currentGround.isAir() && currentGround.getDestroySpeed(level, surfacePos) >= 0 && currentGround.getDestroySpeed(level, surfacePos) < 50.0F) {
                    level.setBlockAndUpdate(surfacePos, Blocks.BASALT.defaultBlockState());
                }
            }
        }

        // 4.3 裂隙处喷出 3 团短暂存在 3 秒的地表烈焰
        int placedFires = 0;
        for (int ox = -1; ox <= 1 && placedFires < 3; ox++) {
            for (int oz = -1; oz <= 1 && placedFires < 3; oz++) {
                BlockPos firePos = centerBlock.offset(ox, 0, oz);
                if (level.isEmptyBlock(firePos) && level.getBlockState(firePos.below()).isSolid()) {
                    level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                    placedFires++;
                }
            }
        }
    }

    /**
     * 日冕过热环境机制 (Arena Heat Gauge) 与 48 格高温融冰：
     * - 48 格领域，基础 +2/s (二阶段 +4/s)，近身 6 格额外 +5/s (二阶段 +10/s)。
     * - 降温判定：站在水里、细雪里、滞留型药水云中、或冰块/浮冰/蓝冰 2 格范围内降温 -8/s。
     * - 48 格领域内冰块随时间融化：普通冰 3 秒 (60 ticks)、浮冰 9 秒 (180 ticks)、蓝冰 27 秒 (540 ticks) 融化为水源。
     * - 离开 Boss 48 格领域后，过热值每秒主动衰减 8 点，降至 0 及以下彻底清除 HUD。
     */
    private void tickArenaHeatGauge() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (this.tickCount % 20 != 0) return; // 每秒结算一次

        // 1. 48 格领域内冰方块扫描与融化计时
        tickArenaIceMelting(serverLevel);

        // 2. 将领域内活跃玩家纳入过热追踪
        List<ServerPlayer> nearbyPlayers = serverLevel.getPlayers(p -> p.distanceTo(this) <= 48.0);
        for (ServerPlayer player : nearbyPlayers) {
            playerHeatMap.putIfAbsent(player.getUUID(), 0);
        }

        int baseGain = this.isPhase2() ? 4 : 2;
        int coreBonus = this.isPhase2() ? 10 : 5;

        // 3. 全局迭代过热玩家映射表，处理升降温与同步
        Iterator<Map.Entry<UUID, Integer>> iterator = playerHeatMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID id = entry.getKey();
            int currentHeat = entry.getValue();

            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (player == null || player.level() != serverLevel || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            double dist = player.distanceTo(this);
            boolean inArena = dist <= 48.0;

            if (inArena) {
                // 降温判定：站在水里、细雪里、滞留型药水云内、或冰块/浮冰/蓝冰 2 格范围内
                boolean inWaterOrSnow = player.isInWater() || player.isInPowderSnow;
                boolean inLingeringCloud = !serverLevel.getEntitiesOfClass(AreaEffectCloud.class, player.getBoundingBox().inflate(0.5)).isEmpty();
                boolean nearIce = checkNearIce(serverLevel, player.blockPosition(), 2);

                boolean cooling = inWaterOrSnow || inLingeringCloud || nearIce;
                if (cooling) {
                    currentHeat = Math.max(0, currentHeat - 8);
                } else {
                    int gain = baseGain + (dist <= 6.0 ? coreBonus : 0);
                    currentHeat = Math.min(100, currentHeat + gain);
                }
            } else {
                // 离开 48 格领域（无论多远）：主动快速降低过热值！
                currentHeat = Math.max(0, currentHeat - 8);
            }

            entry.setValue(currentHeat);

            // 应用惩罚状态
            if (currentHeat >= 80) {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, false, false, false));
            }
            if (currentHeat >= 50 && player.isSprinting()) {
                player.causeFoodExhaustion(0.2F); // 疾跑虚脱饥饿翻倍
            }
            if (currentHeat >= 100) {
                // 自燃极限过热：每 2 秒扣 2 点真实伤害
                if (this.tickCount % 40 == 0) {
                    player.hurtServer(serverLevel, this.damageSources().onFire(), 2.0F);
                    player.igniteForSeconds(2.0F);
                    ModLog.debug(this, player, "【日冕极限过热】玩家热度达 100% 自燃！承受 2 点真实灼烧伤害！");
                }
            }

            // 同步给客户端 HUD
            ServerPlayNetworking.send(player, new CinderHeatSyncPacket(currentHeat, inArena));

            // 如果已经脱离领域且过热值降为 0，彻底从追踪列表中移除
            if (!inArena && currentHeat <= 0) {
                iterator.remove();
            }
        }
    }

    private boolean checkNearIce(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
                return true;
            }
        }
        return false;
    }

    private void tickArenaIceMelting(ServerLevel level) {
        // 发现冰块：优先扫描领域内玩家周围 8 格范围
        for (ServerPlayer player : level.getPlayers(p -> p.distanceTo(this) <= 48.0)) {
            BlockPos pPos = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(pPos.offset(-8, -4, -8), pPos.offset(8, 4, 8))) {
                if (pos.closerToCenterThan(this.position(), 48.0) && !meltingIceTimers.containsKey(pos)) {
                    registerMeltingIceIfPresent(level, pos);
                }
            }
        }

        // 随机采样 Boss 48 格领域内的 20 个方块
        for (int i = 0; i < 20; i++) {
            double rx = this.getX() + (this.random.nextDouble() - 0.5) * 96.0;
            double ry = this.getY() + (this.random.nextDouble() - 0.5) * 48.0;
            double rz = this.getZ() + (this.random.nextDouble() - 0.5) * 96.0;
            BlockPos samplePos = BlockPos.containing(rx, ry, rz);
            if (samplePos.closerToCenterThan(this.position(), 48.0) && !meltingIceTimers.containsKey(samplePos)) {
                registerMeltingIceIfPresent(level, samplePos);
            }
        }

        // 倒计时更新与融化执行
        Iterator<Map.Entry<BlockPos, Integer>> it = meltingIceTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            BlockPos pos = entry.getKey();
            int remaining = entry.getValue() - 20;

            BlockState state = level.getBlockState(pos);
            boolean isIce = state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);

            if (!isIce || !pos.closerToCenterThan(this.position(), 52.0)) {
                it.remove();
                continue;
            }

            if (remaining <= 0) {
                // 到期准时化成水！
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
                ModLog.debug(null, this, "【日冕高温融冰】坐标 " + pos + " 的冰块融化为水源方块！");
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private void registerMeltingIceIfPresent(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int duration = -1;
        if (state.is(Blocks.ICE)) {
            duration = 60; // 普通冰：3 秒 (60 ticks)
        } else if (state.is(Blocks.PACKED_ICE)) {
            duration = 180; // 浮冰：9 秒 (180 ticks)
        } else if (state.is(Blocks.BLUE_ICE)) {
            duration = 540; // 蓝冰：27 秒 (540 ticks)
        }

        if (duration > 0) {
            meltingIceTimers.put(pos.immutable(), duration);
            ModLog.debug(null, this, "【日冕高温检测到冰块】" + state.getBlock().getName().getString() + " at " + pos + "，开始 " + (duration / 20) + " 秒融化倒计时！");
        }
    }

    /**
     * 近战攻击
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float damage = switch (this.level().getDifficulty()) {
            case PEACEFUL -> 0.0F;
            case EASY -> 8.0F;
            case NORMAL -> 10.0F;
            case HARD -> 12.0F;
        };

        DamageSource damageSource = this.damageSources().mobAttack(this);
        Vec3 oldMovement = target.getDeltaMovement();
        boolean wasHurt = target.hurtServer(level, damageSource, damage);

        if (wasHurt) {
            this.causeExtraKnockback(target, this.getKnockback(target, damageSource), oldMovement, damageSource, damage, true);
            this.setLastHurtMob(target);
            this.playAttackSound();
            ModLog.debug(this, target, "【日冕核心·烈焰统领】近战攻击命中！当前难度: " + this.level().getDifficulty()
                    + "，造成伤害: " + String.format("%.2f", damage));
        }

        return wasHurt;
    }

    /**
     * 伤害类型与药水免疫
     */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.CAMPFIRE)
                || source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }

        if (source.is(DamageTypes.FALL)
                || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypeTags.IS_FALL)) {
            return true;
        }

        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            return true;
        }

        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        // 炽核坠碾·阶段三卡地硬直被锤 (Mace) 打断机制：
        // 若玩家使用锤 (Mace) 从高处下落重击砸中暴露的核心，可直接打断该硬直，额外扣除 BOSS 35 点韧性并强制延长眩晕 1.5 秒
        if (this.isCrucibleVulnerable()) {
            boolean isMaceSmash = damageSource.is(DamageTypes.MACE_SMASH)
                    || damageSource.is(DamageTypeTags.IS_MACE_SMASH)
                    || (damageSource.getEntity() instanceof Player p && p.getMainHandItem().is(Items.MACE) && p.fallDistance > 1.0F);

            if (isMaceSmash) {
                // 1. 播放破韧受击音效与震荡粒子
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 2.5F, 0.8F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.6F);
                level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.2, this.getZ(), 35, 0.5, 0.5, 0.5, 0.2);

                // 2. 额外扣除 BOSS 35 点韧性：若有存活黑石重盾则破坏 1 面重盾 (35 HP)，若盾全毁则直接扣除 35 点真实伤害！
                int aliveShields = this.getAliveShieldCount();
                if (aliveShields > 0) {
                    for (int i = 0; i < 4; i++) {
                        if ((this.getShieldBitmask() & (1 << i)) != 0) {
                            this.onShieldDestroyed(i);
                            break;
                        }
                    }
                } else {
                    float extraDmg = DamageCalculator.calculateDamage(35.0F, damageSource, this);
                    super.hurtServer(level, this.damageSources().generic(), extraDmg);
                }

                // 3. 强制延长眩晕 1.5 秒 (30 ticks)，打断硬直并直接切入 STATE_STUNNED
                this.piledriverStunExtraTicks = 30;
                this.piledriverPhase = 0;
                setBossState(STATE_STUNNED);
                ModLog.debug(damageSource.getEntity(), this, "【重锤打断破绽】玩家使用重锤从高处下落重击命中暴露核心！直接打断硬直，额外扣除 35 点韧性并强制延长眩晕 1.5 秒！");
            }
        }

        boolean wasHurt = super.hurtServer(level, damageSource, amount);
        if (wasHurt) {
            this.notifyAttacked();
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CINDER_SOVEREIGN_HURT_CORE, SoundSource.HOSTILE, 1.8F, 1.2F + this.random.nextFloat() * 0.2F);
        }
        return wasHurt;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance newEffect) {
        if (newEffect.is(MobEffects.POISON)
                || newEffect.is(MobEffects.REGENERATION)
                || newEffect.is(MobEffects.WITHER)) {
            return false;
        }
        return super.canBeAffected(newEffect);
    }

    @Override
    public boolean isInvertedHealAndHarm() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        handleEnvironmentalReactions(level);
    }

    private void handleEnvironmentalReactions(ServerLevel level) {
        AABB box = this.getBoundingBox();

        // 1. 常驻蒸发被动保护
        AABB vaporizeBox = box.inflate(1.2);
        BlockPos minPos = BlockPos.containing(vaporizeBox.minX, vaporizeBox.minY, vaporizeBox.minZ);
        BlockPos maxPos = BlockPos.containing(vaporizeBox.maxX, vaporizeBox.maxY, vaporizeBox.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.7F, 1.2F + this.random.nextFloat() * 0.4F);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // 2. 水相真实伤害
        if (this.isInWaterOrRain()) {
            this.waterDamageTickTimer++;
            if (this.waterDamageTickTimer >= 10) {
                this.waterDamageTickTimer = 0;
                this.hurtServer(level, this.damageSources().drown(), 2.0F);
                level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 0.8F, 1.0F);
            }
        } else {
            this.waterDamageTickTimer = 0;
        }

        // 3. 细雪融化与冲击伤害
        BlockPos entityMin = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos entityMax = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        boolean touchedPowderSnow = false;

        for (BlockPos pos : BlockPos.betweenClosed(entityMin, entityMax)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.POWDER_SNOW)) {
                touchedPowderSnow = true;
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.playSound(null, pos, SoundEvents.POWDER_SNOW_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        if (touchedPowderSnow) {
            this.hurtServer(level, this.damageSources().freeze(), 4.0F);
        }
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CINDER_SOVEREIGN_DEATH, SoundSource.HOSTILE, 4.0F, 0.85F);
            ModLog.debug(null, this, "【日冕核心·烈焰统领】生命值归零！爆发湮灭解体与微型超新星核爆音效 (音量 4.0, 音调 0.85)！");

            // Boss 死亡瞬间，立即通知所有在场玩家领域结束，重置过热状态并平滑淡出停止战斗BGM
            for (UUID pid : playerHeatMap.keySet()) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(pid);
                if (player != null) {
                    ServerPlayNetworking.send(player, new CinderHeatSyncPacket(0, false));
                }
            }
            playerHeatMap.clear();
        }
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 275;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        // 清理子部位实体
        for (CinderShieldPartEntity s : shields) {
            if (s != null) s.discard();
        }
        for (CinderBlazeRodPartEntity r : blazeRods) {
            if (r != null) r.discard();
        }

        // 清理所有记录玩家的过热 HUD
        if (this.level() instanceof ServerLevel serverLevel) {
            for (UUID pid : playerHeatMap.keySet()) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(pid);
                if (player != null) {
                    ServerPlayNetworking.send(player, new CinderHeatSyncPacket(0, false));
                }
            }
        }
        playerHeatMap.clear();
        meltingIceTimers.clear();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main_controller", 4, state -> {
            int bs = this.getBossState();
            if (bs == STATE_STUNNED) {
                return state.setAndContinue(ANIM_STUNNED);
            }
            if (bs == STATE_SHOCKWAVE) {
                return state.setAndContinue(ANIM_SHOCKWAVE);
            }
            if (bs == STATE_BARRAGE) {
                return state.setAndContinue(ANIM_BARRAGE);
            }
            if (bs == STATE_BEAM_CHARGE) {
                return state.setAndContinue(ANIM_BEAM_CHARGE);
            }
            if (bs == STATE_BEAM_FIRE) {
                return state.setAndContinue(ANIM_BEAM_FIRE);
            }
            if (bs == STATE_METEOR) {
                return state.setAndContinue(ANIM_METEOR);
            }
            if (this.isPhase2()) {
                return state.setAndContinue(ANIM_PHASE2_IDLE);
            }
            return state.setAndContinue(ANIM_IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
