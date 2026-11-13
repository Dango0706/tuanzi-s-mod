package me.tuanzi.item;

import me.tuanzi.entity.CrescentAuraEntity;
import me.tuanzi.init.ModEnchantments;
import me.tuanzi.network.NodachiSyncPacket;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.ModLog;
import me.tuanzi.util.NodachiPlayerTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class NodachiItem extends Item {
    private final ToolMaterial material;
    private final float baseAttackDamage;

    public NodachiItem(Properties properties, ToolMaterial material, float baseAttackDamage) {
        super(applyNodachiProperties(properties, material, baseAttackDamage));
        this.material = material;
        this.baseAttackDamage = baseAttackDamage;
    }

    private static Properties applyNodachiProperties(Properties properties, ToolMaterial material, float baseAttackDamage) {
        // 先应用标准剑属性（包含蜘蛛网挖掘规则、耐久、修复物、附魔度、Weapon等）
        material.applySwordProperties(properties, baseAttackDamage - material.attackDamageBonus() - 1.0F, -2.7F);

        // 设置攻击范围：基础攻击距离 3.6 格
        properties.component(DataComponents.ATTACK_RANGE, new AttackRange(0.0F, 3.6F, 0.0F, 4.5F, 0.3F, 1.0F));

        // 基础面板属性：攻击速度 1.3 (modifier = 1.3 - 4.0 = -2.7)，基础攻击距离加成 +0.6 (3.0 + 0.6 = 3.6)
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, baseAttackDamage - 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.7, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "nodachi_reach"), 0.6, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();

        properties.attributes(modifiers);
        return properties;
    }

    /**
     * 当玩家使用斩马刀命中实体时触发的核心判定（甜点区、节奏窗口与势层数累积）
     */
    public static void onPlayerHitEntity(Player player, Entity target, ItemStack weapon, float damage) {
        if (player.level().isClientSide()) {
            return;
        }

        if (!(player instanceof NodachiPlayerTracker tracker)) {
            return;
        }

        // 脱力软僵直中禁止造成有效攻击效果
        if (tracker.tuanzis_mod$getNodachiExhaustionTicks() > 0) {
            ModLog.debug(player, target, "【野太刀】玩家处于脱力软僵直中，命中无效！");
            return;
        }

        double distance = player.distanceTo(target);

        // 判定 QTE 节奏窗口：严格要求本次攻击处于节拍器的节奏宽限刻内
        boolean onBeat = false;
        if (tracker.tuanzis_mod$getNodachiRhythmGraceTicks() > 0) {
            onBeat = true;
            tracker.tuanzis_mod$setNodachiRhythmGraceTicks(0); // 消耗本次合拍状态
        }

        int currentMomentum = tracker.tuanzis_mod$getNodachiMomentum();

        if (distance >= 2.2) {
            // 刀锋甜点区 (>= 2.2 格)：造成 1.15 倍伤害，节奏窗口内积累“势”
            if (onBeat) {
                int nextMomentum = Math.min(3, currentMomentum + 1);
                tracker.tuanzis_mod$setNodachiMomentum(nextMomentum);
                tracker.tuanzis_mod$setNodachiInactiveTicks(0);

                if (player.level() instanceof ServerLevel serverLevel) {
                    float pitch = 1.0f + 0.3f * nextMomentum;
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, pitch);
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5), target.getZ(),
                            10 * nextMomentum, 0.25, 0.25, 0.25, 0.15);
                }

                ModLog.debug(player, target, "【野太刀·势】刃尖甜点区命中 (距离: " + String.format("%.2f", distance)
                        + " 格 >= 2.2格)，在 QTE 节奏窗口内！势层数提升: " + currentMomentum + " -> " + nextMomentum);
            } else {
                tracker.tuanzis_mod$setNodachiInactiveTicks(0);
                ModLog.debug(player, target, "【野太刀·势】刃尖命中 (距离: " + String.format("%.2f", distance)
                        + " 格 >= 2.2格)，但未在节奏窗口内，保持当前势层数: " + currentMomentum);
            }
        } else if (distance <= 1.5) {
            // 刀根贴脸 (0 ~ 1.5 格)：造成 85% 伤害 (在 DamageCalculator 处理)，无法积累“势”，保持当前层数
            tracker.tuanzis_mod$setNodachiInactiveTicks(0);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6f, 0.7f);
            }
            ModLog.debug(player, target, "【野太刀·势】刀根贴脸命中 (距离: " + String.format("%.2f", distance)
                    + " 格 <= 1.5格)，伤害衰减 15% (造成 85% 伤害)，击退降低，势层数保持: " + currentMomentum);
        } else {
            // 中段 (1.5 ~ 2.2 格)：造成 100% 完整伤害 (在 DamageCalculator 处理)，无法积累势，仅保持当前势
            tracker.tuanzis_mod$setNodachiInactiveTicks(0);
            ModLog.debug(player, target, "【野太刀·势】刀身中段命中 (距离: " + String.format("%.2f", distance)
                    + " 格)，造成 100% 完整伤害，势层数保持: " + currentMomentum);
        }

        // 同步给客户端以更新 HUD
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new NodachiSyncPacket(tracker.tuanzis_mod$getNodachiMomentum(), tracker.tuanzis_mod$getNodachiExhaustionTicks()));
        }

        // 执行形态特化的扇形横扫与月牙气刃
        performNodachiSweep(player, target, weapon);
    }

    /**
     * 根据当前“势”层数执行专属横扫判定与形态变化
     */
    public static void performNodachiSweep(Player player, Entity mainTarget, ItemStack weapon) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(player instanceof NodachiPlayerTracker tracker)) {
            return;
        }

        int momentum = tracker.tuanzis_mod$getNodachiMomentum();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 horizontalLook = new Vec3(look.x, 0, look.z).normalize();

        double radius;
        double halfAngleDeg;
        float cleaveDamage;

        if (momentum <= 1) {
            // 0 ~ 1 层“势”：前方 120° 扇形、半径 2.8 格，仅击退杂兵，无横扫伤害
            radius = 2.8;
            halfAngleDeg = 60.0;
            cleaveDamage = 0.0f;
        } else {
            // 2 ~ 3 层“势”：前方 150° 扇形、半径 3.4 格，对扇区内所有副目标造成 1 点顺劈伤害 + 击退
            radius = 3.4;
            halfAngleDeg = 75.0;
            cleaveDamage = 1.0f;
        }

        double maxAngleRad = Math.toRadians(halfAngleDeg);
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius, 1.5, radius),
                e -> e.isAlive() && e != player && e != mainTarget && !player.isAlliedTo(e) && !(e instanceof ArmorStand stand && stand.isMarker())
        );

        int affectedCount = 0;
        DamageSource sweepSource = serverLevel.damageSources().playerAttack(player);

        for (LivingEntity nearby : nearbyEntities) {
            Vec3 toNearby = new Vec3(nearby.getX() - player.getX(), 0, nearby.getZ() - player.getZ());
            double distSq = toNearby.lengthSqr();
            if (distSq > radius * radius || distSq < 0.0001) {
                continue;
            }

            Vec3 dir = toNearby.normalize();
            double dot = horizontalLook.dot(dir);
            double angleRad = Math.acos(Mth.clamp(dot, -1.0, 1.0));

            if (angleRad <= maxAngleRad) {
                // 击退
                nearby.knockback(0.45, Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(player.getYRot() * (float) (Math.PI / 180.0)), sweepSource, cleaveDamage);

                // 2层以上顺劈伤害结算（通过 DamageCalculator 计算）
                if (cleaveDamage > 0.0f) {
                    float finalDamage = DamageCalculator.calculateDamage(cleaveDamage, sweepSource, nearby);
                    nearby.hurtServer(serverLevel, sweepSource, finalDamage);
                    serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, nearby.getX(), nearby.getY(0.5), nearby.getZ(), 1, 0.1, 0.0, 0.1, 0.2);
                }

                affectedCount++;
            }
        }

        // 播放横扫粒子效果
        double dx = -Mth.sin(player.getYRot() * (float) (Math.PI / 180.0));
        double dz = Mth.cos(player.getYRot() * (float) (Math.PI / 180.0));
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dx, player.getY(0.5), player.getZ() + dz, 1, dx, 0.0, dz, 0.0);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);

        ModLog.debug(player, mainTarget, "【野太刀·横扫】势层数: " + momentum + "，扇区半角: " + halfAngleDeg + "°，半径: " + radius
                + " 格，造成顺劈伤害: " + cleaveDamage + " 点，波及副目标数: " + affectedCount);

        // 3 层满势（顺劈气刃）：普通挥砍附带向正前方推进的实体波形气刃，内置 5 秒 (100 ticks) 独立冷却，释放时不脱力
        if (momentum >= 3 && tracker.tuanzis_mod$getCrescentAuraCooldownTicks() <= 0) {
            float baseDmg = weapon.getItem() instanceof NodachiItem nodachi ? nodachi.getBaseAttackDamage() : 7.0f;
            CrescentAuraEntity aura = new CrescentAuraEntity(serverLevel, player, player.getLookAngle(), baseDmg);
            serverLevel.addFreshEntity(aura);

            tracker.tuanzis_mod$setCrescentAuraCooldownTicks(100); // 5 秒内置冷却

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.4f, 1.8f);

            ModLog.debug(player, mainTarget, "【野太刀·满势】顺劈气刃斩出！进入 5 秒内置冷却，未进入脱力。");
        }
    }

    public static boolean hasFinalityEnchantment(Level level, ItemStack stack) {
        var registry = level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (registry.isEmpty()) return false;
        var ench = registry.get().get(ModEnchantments.FINALITY);
        if (ench.isEmpty()) return false;
        return EnchantmentHelper.getItemEnchantmentLevel(ench.get(), stack) > 0;
    }

    private static void performFinalitySpinAttack(Player player, ItemStack weapon, ServerLevel serverLevel, NodachiPlayerTracker tracker) {
        float baseDmg = weapon.getItem() instanceof NodachiItem nodachi ? nodachi.getBaseAttackDamage() : 7.0f;
        float rawDamage = baseDmg * 1.80f; // 180% 基础攻击力毁灭打击

        DamageSource source = serverLevel.damageSources().playerAttack(player);

        // 360° 全方位大回旋斩粒子特效 (双环粒子，展现大回旋气浪)
        double px = player.getX();
        double py = player.getY() + 0.5;
        double pz = player.getZ();

        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px + cos * 2.2, py, pz + sin * 2.2, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CRIT, px + cos * 3.5, py + 0.2, pz + sin * 3.5, 2, 0.05, 0.05, 0.05, 0.1);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, px + cos * 2.8, py + 0.1, pz + sin * 2.8, 1, 0.05, 0.05, 0.05, 0.1);
        }

        // 震撼音效
        serverLevel.playSound(null, px, py, pz, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.6f);
        serverLevel.playSound(null, px, py, pz, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.7f);
        serverLevel.playSound(null, px, py, pz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.8f);

        // 4 格内所有敌对目标
        double radius = 4.0;
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius, 2.0, radius),
                e -> e.isAlive() && e != player && !player.isAlliedTo(e) && !(e instanceof ArmorStand stand && stand.isMarker())
        );

        int hitCount = 0;
        for (LivingEntity target : nearbyEntities) {
            double dist = player.distanceTo(target);
            if (dist > radius) {
                continue;
            }

            // 判断敌对目标：怪物/敌人、主动锁定玩家的生物、或敌对玩家
            boolean isHostile = (target instanceof Enemy)
                    || (target instanceof Mob mob && mob.getTarget() == player)
                    || (target instanceof Player otherPlayer && !player.isAlliedTo(otherPlayer) && player.canHarmPlayer(otherPlayer));

            if (!isHostile) {
                continue;
            }

            // 伤害计算：必须严格使用 DamageCalculator 结算
            float finalDamage = DamageCalculator.calculateDamage(rawDamage, source, target);
            target.hurtServer(serverLevel, source, finalDamage);

            // 强行击退 3 格 (结合标准击退与直接速度向量，确保击退 3 格)
            Vec3 diff = new Vec3(target.getX() - player.getX(), 0, target.getZ() - player.getZ());
            Vec3 knockDir = diff.lengthSqr() > 1e-4 ? diff.normalize() : player.getLookAngle();
            target.knockback(1.5, -knockDir.x, -knockDir.z, source, finalDamage);
            target.setDeltaMovement(target.getDeltaMovement().add(knockDir.x * 1.2, 0.2, knockDir.z * 1.2));

            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
            hitCount++;

            ModLog.debug(player, target, "【野太刀·绝式】360° 大回旋斩命中敌对目标！基础攻击力: " + baseDmg
                    + "，180% 原始伤害: " + String.format("%.2f", rawDamage) + " 点，DamageCalculator 结算伤害: "
                    + String.format("%.2f", finalDamage) + " 点，强行击退 3 格！");
        }

        // 扣除耐久
        weapon.hurtAndBreak(2, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);

        // 释放后进入 1 秒 (20 ticks) 脱力状态
        tracker.tuanzis_mod$triggerNodachiExhaustion("释放绝式·大回旋斩", 20);

        ModLog.debug(player, null, "【野太刀·绝式】大回旋斩释放完毕！消耗全部“势”，波及敌对目标数: "
                + hitCount + "，进入 1.0 秒脱力软僵直状态。");
    }

    public ToolMaterial getMaterial() {
        return this.material;
    }

    public float getBaseAttackDamage() {
        return this.baseAttackDamage;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof NodachiPlayerTracker tracker)) {
            return InteractionResult.PASS;
        }
        if (tracker.tuanzis_mod$getNodachiExhaustionTicks() > 0) {
            return InteractionResult.FAIL;
        }
        // 满层“势” (3 层) 且持有【绝式】附魔方可开始蓄力
        if (tracker.tuanzis_mod$getNodachiMomentum() < 3 || !hasFinalityEnchantment(level, stack)) {
            return InteractionResult.PASS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (livingEntity instanceof Player player && player instanceof NodachiPlayerTracker tracker) {
            if (tracker.tuanzis_mod$getNodachiExhaustionTicks() > 0 || tracker.tuanzis_mod$getNodachiMomentum() < 3) {
                player.stopUsingItem();
                return;
            }
            // 蓄力期间重置空闲刻，防止在蓄力瞄准过程中超时断势
            tracker.tuanzis_mod$setNodachiInactiveTicks(0);

            int chargeTicks = this.getUseDuration(stack, livingEntity) - count;
            // 恰好达到 0.8 秒 (16 刻) 蓄力完成的瞬间，播放音效与粒子提示
            if (chargeTicks == 16) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 2.0f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.5f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.5, 0.5, 0.2);
                    serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.4, 0.4, 0.4, 0.1);
                }
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || !(player instanceof NodachiPlayerTracker tracker)) {
            return false;
        }

        int chargeTicks = this.getUseDuration(stack, entity) - timeLeft;
        // 蓄力必须达到 0.8 秒 (16 ticks) 且满层“势”
        if (chargeTicks < 16) {
            return false;
        }

        if (tracker.tuanzis_mod$getNodachiMomentum() < 3 || !hasFinalityEnchantment(level, stack)) {
            return false;
        }

        // 消耗全部“势”
        tracker.tuanzis_mod$setNodachiMomentum(0);
        tracker.tuanzis_mod$setNodachiInactiveTicks(0);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            performFinalitySpinAttack(player, stack, serverLevel, tracker);
        }

        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        // 移除所有自定义 lore 显示
    }
}
