package me.tuanzi.util;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * 伤害计算专用工具类，统一管理和计算模组中所有的伤害加成与调整逻辑。
 * 支持多重伤害修改效果的加法累加（Additive）计算。
 */
public class DamageCalculator {

    /**
     * 计算并调整最终伤害。
     *
     * @param amount 原始伤害值
     * @param source 伤害源
     * @param target 受击的实体
     * @return 调整后的最终伤害值
     */
    public static float calculateDamage(float amount, DamageSource source, LivingEntity target) {
        float initialDamage = amount;

        // 0. 免疫火焰与岩浆伤害
        if (target.hasEffect(ModStatusEffects.HELL_FIRE) && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害计算完毕！触发地狱之火免疫，计算前伤害: " + String.format("%.2f", initialDamage) + "，计算后最终伤害: 0.00");
            return 0.0f;
        }

        float attackerMultiplier = 1.0f;
        float victimMultiplier = 1.0f;

        // 1. 攻击者输出伤害乘数计算（加法累加模型）
        if (source.getEntity() instanceof LivingEntity attacker) {
            // 攻击者拥有“血怒”状态效果，攻击伤害乘算提升 40% (累加值 +0.4f)
            if (attacker.hasEffect(ModStatusEffects.BLOOD_RAGE)) {
                attackerMultiplier += 0.4f;
                me.tuanzi.util.ModLog.debug(attacker, target, "伤害更改触发：攻击者拥有血怒效果，攻击力增加 40% (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
            }
            
            // 攻击者穿戴狂战士附魔胸甲，近战伤害随已损失生命值比例获得提升 (I级每损失10%增伤5%，II级增伤8%)
            boolean isMelee = source.is(DamageTypes.PLAYER_ATTACK) 
                || source.is(DamageTypes.MOB_ATTACK) 
                || source.is(DamageTypes.MACE_SMASH);
            if (isMelee) {
                if (attacker.hasEffect(ModStatusEffects.HELL_FIRE)) {
                    // 60% 概率点燃目标 2 秒 (40 ticks)
                    if (!attacker.level().isClientSide() && attacker.getRandom().nextFloat() < 0.60f) {
                        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
                    }
                    // 对着火的目标伤害额外 +1
                    if (target.isOnFire()) {
                        amount += 1.0f;
                    }
                }

                var registry = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var berserkerEnch = registry.getOrThrow(ModEnchantments.BERSERKER);
                ItemStack chest = attacker.getItemBySlot(EquipmentSlot.CHEST);
                int level = EnchantmentHelper.getItemEnchantmentLevel(berserkerEnch, chest);
                if (level > 0) {
                    float maxHealth = attacker.getMaxHealth();
                    if (maxHealth > 0.0f) {
                        float lostHealth = maxHealth - attacker.getHealth();
                        float lostPercent = lostHealth / maxHealth;
                        // 限制最多在生命值低于5%时达到上限，即最大损失比例为95%
                        lostPercent = Math.min(lostPercent, 0.95f);
                        float stepCount = lostPercent / 0.10f;
                        float percentPerStep = level == 1 ? 0.05f : 0.08f;
                        float berserkerBonus = stepCount * percentPerStep;
                        attackerMultiplier += berserkerBonus;
                        me.tuanzi.util.ModLog.debug(attacker, target, "伤害更改触发：攻击者拥有狂战士附魔（等级: " + level + "），当前损失生命比例: " + String.format("%.1f", lostPercent * 100) + "%，攻击力增加 " + String.format("%.1f", berserkerBonus * 100) + "% (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
                    }
                }

                // 处决 (Execute) 附魔逻辑
                var executeEnch = registry.getOrThrow(ModEnchantments.EXECUTE);
                ItemStack mainHand = attacker.getMainHandItem();
                int execLevel = EnchantmentHelper.getItemEnchantmentLevel(executeEnch, mainHand);
                if (execLevel > 0) {
                    // 目标生命值低于 ( 5 + 等级×10% ) = (0.05f + execLevel * 0.10f) * maxHealth
                    float targetThreshold = target.getMaxHealth() * (0.05f + execLevel * 0.10f);
                    if (target.getHealth() < targetThreshold) {
                        // 伤害提升 等级 * 25% (加到攻击者乘数)
                        attackerMultiplier += execLevel * 0.25f;
                        me.tuanzi.util.ModLog.debug(attacker, target, "伤害更改触发：触发处决附魔（等级: " + execLevel + "，目标血量低于阈值），攻击力增加 " + String.format("%.1f", execLevel * 0.25f * 100) + "% (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");

                        // 嗜血反噬：受到 等级×0.5点伤害（最高1.5心，即 execLevel * 1.0f 点真实伤害）
                        if (!attacker.level().isClientSide()) {
                            float backlashDamage = execLevel * 1.0f;
                            var typeHolder = attacker.registryAccess()
                                .lookupOrThrow(Registries.DAMAGE_TYPE)
                                .getOrThrow(DamageTypes.GENERIC);
                            me.tuanzi.util.ExecuteBacklashDamageSource backlashSrc = new me.tuanzi.util.ExecuteBacklashDamageSource(typeHolder);

                            float newHealth = attacker.getHealth() - backlashDamage;
                            attacker.setHealth(Math.max(0.0f, newHealth));
                            if (newHealth <= 0.0f) {
                                attacker.die(backlashSrc);
                            }
                        }
                    }
                }

                // 连锁苦痛 (Chain Pain) 附魔逻辑
                var chainPainEnch = registry.getOrThrow(ModEnchantments.CHAIN_PAIN);
                int chainLevel = EnchantmentHelper.getItemEnchantmentLevel(chainPainEnch, mainHand);
                if (chainLevel > 0) {
                    float finalDamage = amount * attackerMultiplier * victimMultiplier;
                    if (finalDamage >= target.getHealth()) {
                        float overflow = finalDamage - target.getHealth();
                        if (overflow > 0.0f) {
                            float splashDamage = overflow * 0.5f;
                            // 击杀目标后，对其周围 5 格内敌人造成该次溢出伤害的 50%，最多连锁 5 个目标，由近到远排序。
                            if (!attacker.level().isClientSide() && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                // 查找目标周围 5 格内的敌人（非自身，非攻击者，必须是 LivingEntity 且存活）
                                var targets = serverLevel.getEntitiesOfClass(LivingEntity.class, 
                                    target.getBoundingBox().inflate(5.0), 
                                    entity -> entity != target && entity != attacker && entity.isAlive()
                                );
                                
                                // 由近到远排序
                                targets.sort((e1, e2) -> Double.compare(target.distanceToSqr(e1), target.distanceToSqr(e2)));
                                
                                // 最多连锁 5 个目标
                                int count = 0;
                                for (LivingEntity victim : targets) {
                                    if (count >= 5) break;
                                    
                                    // 造成伤害
                                    var typeHolder = attacker.registryAccess()
                                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                                        .getOrThrow(DamageTypes.MAGIC);
                                    
                                    me.tuanzi.util.ChainPainDamageSource chainSrc = new me.tuanzi.util.ChainPainDamageSource(typeHolder);
                                    
                                    victim.hurtServer(serverLevel, chainSrc, splashDamage);
                                    
                                    me.tuanzi.util.ModLog.debug(attacker, victim, "连锁苦痛：苦痛震荡波伤害连锁触发！对其造成了 " + String.format("%.2f", splashDamage) + " 点溢出波及伤害（连锁计数: " + (count + 1) + "/5）。");
                                    
                                    // 产生附魔打击粒子效果
                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, 
                                        victim.getX(), victim.getY() + 1.0, victim.getZ(), 
                                        5, 0.2, 0.2, 0.2, 0.1);
                                        
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 受击者受伤加深乘数计算（加法累加模型）
        // 被攻击者拥有“血怒”状态效果，且伤害非血怒反馈伤害，所受伤害乘算提升 30% (累加值 +0.3f)
        if (target.hasEffect(ModStatusEffects.BLOOD_RAGE) && !(source instanceof BloodRageDamageSource)) {
            victimMultiplier += 0.3f;
            me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：受击者拥有血怒效果，受到的伤害增加 30% (当前受伤乘数累加值: " + String.format("%.2f", victimMultiplier) + ")。");
        }

        float finalDamage = amount * attackerMultiplier * victimMultiplier;
        me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害计算完毕！计算前伤害: " + String.format("%.2f", initialDamage) + "，计算后最终伤害: " + String.format("%.2f", finalDamage));

        return finalDamage;
    }
}
