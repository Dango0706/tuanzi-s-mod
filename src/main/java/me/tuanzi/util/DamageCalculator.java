package me.tuanzi.util;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.init.ModEnchantments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

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

        // 0.1 潮汐织靴免受摔落伤害
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) 
                && target.getItemBySlot(EquipmentSlot.FEET).is(me.tuanzi.init.ModItems.TIDAL_WEAVE_BOOTS)) {
            me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害计算完毕！触发【潮汐织靴】摔落免疫，计算前伤害: " + String.format("%.2f", initialDamage) + "，计算后最终伤害: 0.00");
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
                // 坚盾之赐 (Steel Shield Gift) 附魔逻辑
                ItemStack steelShieldGiftStack = attacker.getMainHandItem();
                if (!steelShieldGiftStack.isEmpty()) {
                    var registry = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var steelShieldGiftEnch = registry.getOrThrow(ModEnchantments.STEEL_SHIELD_GIFT);
                    int level = EnchantmentHelper.getItemEnchantmentLevel(steelShieldGiftEnch, steelShieldGiftStack);
                    if (level > 0) {
                        float armor = attacker.getArmorValue();
                        if (armor > 20.0f) {
                            float overflowArmor = armor - 20.0f;
                            float bonusDmg = overflowArmor * (0.0625f * level);
                            amount += bonusDmg;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【坚盾之赐】附魔触发！等级: " + level + "，攻击者护甲值: " + String.format("%.2f", armor) + "，溢出护甲值: " + String.format("%.2f", overflowArmor) + "，造成额外伤害: " + String.format("%.2f", bonusDmg) + " 点。");
                        }
                    }
                }

                // 虚无共鸣 (Void Resonance) 附魔逻辑
                ItemStack voidResonanceStack = attacker.getMainHandItem();
                if (!voidResonanceStack.isEmpty()) {
                    var registry = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var voidResonanceEnch = registry.getOrThrow(ModEnchantments.VOID_RESONANCE);
                    int level = EnchantmentHelper.getItemEnchantmentLevel(voidResonanceEnch, voidResonanceStack);
                    if (level > 0 && target.getArmorValue() == 0) {
                        final float[] attackDamageBox = {1.0f};
                        final float[] attackSpeedBox = {4.0f};
                        voidResonanceStack.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                                if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                    attackDamageBox[0] += (float) modifier.amount();
                                }
                            } else if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)) {
                                if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                    attackSpeedBox[0] += (float) modifier.amount();
                                }
                            }
                        });
                        float baseDmg = attackDamageBox[0];
                        float speed = attackSpeedBox[0];
                        float bonusDmg = (0.8f * level) * (speed / Math.max(0.0001f, baseDmg));
                        amount += bonusDmg;

                        me.tuanzi.util.ModLog.debug(attacker, target, "【虚无共鸣】附魔触发！等级: " + level + "，武器基础攻击力: " + String.format("%.2f", baseDmg) + "，武器攻击速度: " + String.format("%.2f", speed) + "，基础伤害增加: " + String.format("%.2f", bonusDmg) + " 点。");
                    }
                }

                if (attacker.getMainHandItem().getItem() instanceof me.tuanzi.item.TideCleaverItem) {
                    ItemStack mainHand = attacker.getMainHandItem();
                    net.minecraft.world.item.component.CustomData customData = mainHand.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
                    
                    int charge = tag.getIntOr("ChargeLevel", 0);
                    long gameTime = attacker.level().getGameTime();
                    long cooldownEnd = tag.getLongOr("BurstCooldownEnd", 0L);
                    boolean hasCooldown = gameTime < cooldownEnd;
                    
                    int nextCharge = charge;
                    boolean onBeat = false;
                    boolean offBeat = false;
                    
                    if (attacker instanceof net.minecraft.world.entity.player.Player player) {
                        me.tuanzi.util.TideCleaverPlayerTracker tracker = (me.tuanzi.util.TideCleaverPlayerTracker) player;
                        float strength = tracker.tuanzis_mod$getLastAttackStrength();
                        int fullyChargedTicks = tracker.tuanzis_mod$getLastAttackFullyChargedTicks();
                        
                        if (strength >= 0.8f && strength < 1.0f) {
                            onBeat = true;
                        } else if (strength >= 1.0f && fullyChargedTicks <= 4) {
                            onBeat = true;
                        } else if (strength < 0.8f) {
                            offBeat = true;
                        }
                    } else {
                        onBeat = true;
                    }
                    
                    if (onBeat) {
                        if (charge == 5 && !hasCooldown) {
                            nextCharge = 1;
                            attackerMultiplier += 5 * 0.02f; // 享受 5 层 10% 增伤
                            
                            if (!attacker.level().isClientSide() && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                var magicType = attacker.registryAccess()
                                    .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                                    .getOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC);
                                net.minecraft.world.damagesource.DamageSource burstSrc = new net.minecraft.world.damagesource.DamageSource(magicType, attacker);
                                
                                int invTime = target.invulnerableTime;
                                target.invulnerableTime = 0;
                                target.hurtServer(serverLevel, burstSrc, 3.0f);
                                target.invulnerableTime = invTime;
                                
                                tag.putLong("BurstCooldownEnd", gameTime + 140);
                                if (attacker instanceof net.minecraft.world.entity.player.Player player) {
                                    player.getCooldowns().addCooldown(mainHand, 140);
                                }
                                
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                                    target.getX(), target.getY() + 1.0, target.getZ(),
                                    20, 0.4, 0.4, 0.4, 0.2);
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                                    target.getX(), target.getY() + 1.0, target.getZ(),
                                    15, 0.3, 0.3, 0.3, 0.1);
                                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    net.minecraft.sounds.SoundEvents.PLAYER_SPLASH_HIGH_SPEED, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
                                
                                me.tuanzi.util.ModLog.debug(attacker, target, "【潮涌爆发】额外造成 3.0 点魔法伤害，冷却重置 7 秒，充能重置为 1。");
                            }
                        } else {
                            nextCharge = Math.min(5, charge + 1);
                            attackerMultiplier += nextCharge * 0.02f;
                            
                            if (!attacker.level().isClientSide()) {
                                attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                            }
                            me.tuanzi.util.ModLog.debug(attacker, target, "【潮汐节拍】合拍击中！充能等级：" + nextCharge);
                        }
                    } else if (offBeat) {
                        nextCharge = Math.max(0, charge - 1);
                        attackerMultiplier -= 0.15f;
                        
                        if (!attacker.level().isClientSide()) {
                            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                                net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.5f);
                        }
                        me.tuanzi.util.ModLog.debug(attacker, target, "【潮汐节拍】失拍击中！充能等级：" + nextCharge + "，伤害降低 15%");
                    } else {
                        me.tuanzi.util.ModLog.debug(attacker, target, "【潮汐节拍】正常击中（冷却充能超过宽限期），充能等级：" + charge);
                    }
                    
                    tag.putInt("ChargeLevel", nextCharge);
                    mainHand.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    
                    if (nextCharge >= 2) {
                        if (!attacker.level().isClientSide()) {
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                me.tuanzi.init.ModStatusEffects.TIDE_EROSION, 40, 0, false, false, true
                            ));
                            me.tuanzi.util.ModLog.debug(attacker, target, "【潮汐侵蚀】附带魔法伤害 DOT 施加/刷新。");
                        }
                    }
                }

                if (attacker.getMainHandItem().getItem() instanceof me.tuanzi.item.RiftScarItem) {
                    if (target.getArmorValue() == 0) {
                        attackerMultiplier += 0.75f;
                        me.tuanzi.util.ModLog.debug(attacker, target, "【裂虚之痕】触发虚无切割！目标护甲为 0，造成的伤害提升 75% (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
                    }
                }

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

                // 蜂鸣节律 (Buzzing Rhythm) 附魔逻辑
                var buzzingRhythmEnch = registry.getOrThrow(ModEnchantments.BUZZING_RHYTHM);
                int rhythmLevel = EnchantmentHelper.getItemEnchantmentLevel(buzzingRhythmEnch, mainHand);
                if (rhythmLevel > 0) {
                    boolean hasBeePoison = target.hasEffect(ModStatusEffects.BEE_POISON) 
                        || target.hasEffect(ModStatusEffects.BEE_POISON_COOLDOWN);
                    if (hasBeePoison) {
                        if (attacker instanceof me.tuanzi.util.RhythmTracker tracker) {
                            java.util.UUID lastTargetUuid = tracker.tuanzis_mod$getLastRhythmTarget();
                            java.util.UUID currentTargetUuid = target.getUUID();
                            net.minecraft.world.effect.MobEffectInstance currentEffect = attacker.getEffect(ModStatusEffects.BUZZING_RHYTHM);
                            if (lastTargetUuid != null && lastTargetUuid.equals(currentTargetUuid) && currentEffect != null) {
                                int currentAmp = currentEffect.getAmplifier();
                                int maxAmp = rhythmLevel; // 最多 1+level 层，即 amplifier 最大值为 rhythmLevel
                                int nextAmp = Math.min(maxAmp, currentAmp + 1);
                                attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModStatusEffects.BUZZING_RHYTHM, 60, nextAmp, false, false, true));
                                me.tuanzi.util.ModLog.debug(attacker, target, "【蜂鸣节律】连续攻击带毒目标，节律层数叠加至: " + (nextAmp + 1) + " 层（最大: " + (rhythmLevel + 1) + " 层）。");
                            } else {
                                attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModStatusEffects.BUZZING_RHYTHM, 60, 0, false, false, true));
                                tracker.tuanzis_mod$setLastRhythmTarget(currentTargetUuid);
                                me.tuanzi.util.ModLog.debug(attacker, target, "【蜂鸣节律】开始攻击新的带毒目标，重置并施加 1 层节律。");
                            }
                        }
                    } else {
                        if (attacker instanceof me.tuanzi.util.RhythmTracker tracker) {
                            tracker.tuanzis_mod$setLastRhythmTarget(null);
                        }
                    }
                } else {
                    // 如果攻击者未持有蜂鸣节律武器攻击，也应当清除其身上的蜂鸣节律
                    if (attacker.hasEffect(ModStatusEffects.BUZZING_RHYTHM)) {
                        attacker.removeEffect(ModStatusEffects.BUZZING_RHYTHM);
                        me.tuanzi.util.ModLog.debug(attacker, target, "【蜂鸣节律】未持有附魔武器进行攻击，节律清零。");
                    }
                    if (attacker instanceof me.tuanzi.util.RhythmTracker tracker) {
                        tracker.tuanzis_mod$setLastRhythmTarget(null);
                    }
                }

                // 幽匿裂片 (Scully Shard) 与 共振脉冲 (Resonance Pulse) 附魔逻辑
                if (!attacker.level().isClientSide() && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    boolean isScullyShard = mainHand.getItem() instanceof me.tuanzi.item.ScullyShardItem;
                    
                    var resonancePulseEnch = registry.getOrThrow(ModEnchantments.RESONANCE_PULSE);
                    int pulseLevel = EnchantmentHelper.getItemEnchantmentLevel(resonancePulseEnch, mainHand);
                    
                    if (isScullyShard || pulseLevel > 0) {
                        boolean hasResonance = target.hasEffect(ModStatusEffects.RESONANCE);
                        
                        if (hasResonance) {
                            // 移除共鸣效果
                            target.removeEffect(ModStatusEffects.RESONANCE);
                            me.tuanzi.util.ModLog.debug(attacker, target, "【共鸣消耗】目标已带共鸣，移除其共鸣效果！");
                            
                            // 重置无敌时间，确保音波魔法伤害不被免疫
                            int originalInvulnerableTime = target.invulnerableTime;
                            target.invulnerableTime = 0;
                            
                            var magicType = attacker.registryAccess()
                                .lookupOrThrow(Registries.DAMAGE_TYPE)
                                .getOrThrow(DamageTypes.MAGIC);
                            me.tuanzi.util.SonicDamageSource sonicSrc = new me.tuanzi.util.SonicDamageSource(magicType, attacker);
                            
                            // 1. 触发幽匿裂片 2.0 点单体音波魔法伤害
                            if (isScullyShard) {
                                target.hurtServer(serverLevel, sonicSrc, 2.0f);
                                me.tuanzi.util.ModLog.debug(attacker, target, "【幽匿裂片】造成额外 2 点音波魔法伤害！");
                            }
                            
                            // 2. 触发共振脉冲附魔伤害及周围 2 格敌人波及
                            if (pulseLevel > 0) {
                                float pulseDamage = 0.2f + 0.4f * pulseLevel;
                                
                                // 查找目标及周围 2 格内所有 LivingEntity 敌人（非攻击者，非目标本身在循环中由 hurtServer 重新处理）
                                var area = target.getBoundingBox().inflate(2.0);
                                var nearbyEntities = serverLevel.getEntitiesOfClass(LivingEntity.class, area, 
                                    entity -> entity.isAlive() && entity != attacker
                                );
                                
                                me.tuanzi.util.ModLog.debug(attacker, target, "【共振脉冲】附魔触发！等级: " + pulseLevel + "，对范围 " + nearbyEntities.size() + " 个敌人造成 " + String.format("%.2f", pulseDamage) + " 点音波魔法伤害。");
                                
                                for (LivingEntity nearby : nearbyEntities) {
                                    // 同样重置周围敌人的受伤冷却，以防被吞
                                    nearby.invulnerableTime = 0;
                                    nearby.hurtServer(serverLevel, sonicSrc, pulseDamage);
                                    
                                    // 产生声波爆轰粒子效果
                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, 
                                        nearby.getX(), nearby.getY() + 1.0, nearby.getZ(), 
                                        1, 0.0, 0.0, 0.0, 0.0);
                                }
                                
                                // 播放共鸣声波爆震音效
                                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
                            }
                            
                            // 恢复原本的无敌时间
                            target.invulnerableTime = originalInvulnerableTime;
                            
                        } else {
                            // 没有共鸣，施加共鸣
                            boolean applyResonance = false;
                            if (isScullyShard) {
                                applyResonance = true;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【幽匿共鸣】施加判定成功：幽匿裂片 100% 触发施加！");
                            } else {
                                float chance = 0.10f * pulseLevel;
                                if (attacker.getRandom().nextFloat() < chance) {
                                    applyResonance = true;
                                    me.tuanzi.util.ModLog.debug(attacker, target, "【共振脉冲】共鸣施加判定成功：概率 " + String.format("%.1f", chance * 100) + "% 触发施加！");
                                } else {
                                    me.tuanzi.util.ModLog.debug(attacker, target, "【共振脉冲】共鸣施加判定未命中（概率: " + String.format("%.1f", chance * 100) + "%）。");
                                }
                            }
                            
                            if (applyResonance) {
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModStatusEffects.RESONANCE, 50, 0, false, false, true));
                                
                                // 播放清脆的回响声音，作为施加提示
                                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.7f);
                            }
                        }
                    }

                    // 深渊律动 (Abyssal Rhythm) 附魔逻辑
                    var abyssalRhythmEnch = registry.getOrThrow(ModEnchantments.ABYSSAL_RHYTHM);
                    int abyssalLevel = EnchantmentHelper.getItemEnchantmentLevel(abyssalRhythmEnch, mainHand);
                    if (abyssalLevel > 0 && amount > 0.0f && !source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
                        int charge = 0;
                        net.minecraft.world.item.component.CustomData customData = mainHand.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                        if (customData != null) {
                            CompoundTag tag = customData.copyTag();
                            charge = tag.getIntOr("ChargeLevel", 0);
                        }
                        
                        float baseDmg = 0.0f;
                        switch (abyssalLevel) {
                            case 1 -> baseDmg = 0.1f;
                            case 2 -> baseDmg = 0.2f;
                            case 3 -> baseDmg = 0.25f;
                            case 4 -> baseDmg = 0.3f;
                            default -> baseDmg = 0.3f;
                        }
                        float bonusDmg = charge > 0 ? (0.05f * abyssalLevel) * charge : 0.0f;
                        float totalExtraDmg = baseDmg + bonusDmg;
                        
                        int originalInvulnerableTime = target.invulnerableTime;
                        target.invulnerableTime = 0;
                        
                        var magicType = attacker.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC);
                        net.minecraft.world.damagesource.DamageSource magicSrc = new net.minecraft.world.damagesource.DamageSource(magicType, attacker);
                        
                        target.hurtServer(serverLevel, magicSrc, totalExtraDmg);
                        
                        target.invulnerableTime = originalInvulnerableTime;
                        
                        me.tuanzi.util.ModLog.debug(attacker, target, "【深渊律动】附魔触发！等级: " + abyssalLevel + "，当前潮汐共鸣层数: " + charge + "，造成额外魔法伤害: " + String.format("%.2f", totalExtraDmg) + " 点。");
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

        // 被攻击者拥有蜂毒效果，受到的伤害增加 (层数 * 3%)
        if (target.hasEffect(ModStatusEffects.BEE_POISON)) {
            int amp = target.getEffect(ModStatusEffects.BEE_POISON).getAmplifier();
            int layers = amp + 1;
            float poisonMultiplier = layers * 0.03f;
            victimMultiplier += poisonMultiplier;
            me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：受击者拥有蜂毒效果（层数: " + layers + "），受到的伤害增加 " + String.format("%.1f", poisonMultiplier * 100) + "% (当前受伤乘数累加值: " + String.format("%.2f", victimMultiplier) + ")。");
        }

        float finalDamage = amount * attackerMultiplier * victimMultiplier;

        // 蜂毒引爆与消耗处理
        if (target.hasEffect(ModStatusEffects.BEE_POISON) && !(source instanceof me.tuanzi.util.BeeStingExplosionDamageSource)) {
            if (source.getEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().getItem() instanceof me.tuanzi.item.BeeStingEchoItem) {
                int amp = target.getEffect(ModStatusEffects.BEE_POISON).getAmplifier();
                if (amp == 3) { // 之前是 4 层，本次是第 5 次命中，触发引爆！
                    // 清空蜂毒
                    target.removeEffect(ModStatusEffects.BEE_POISON);
                    // 施加冷却
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModStatusEffects.BEE_POISON_COOLDOWN, 160, 0, false, false, true));

                    if (!target.level().isClientSide() && target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        float explodeDamage = finalDamage * 0.15f;
                        
                        var typeHolder = attacker.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(DamageTypes.GENERIC);
                        me.tuanzi.util.BeeStingExplosionDamageSource explosionSrc = new me.tuanzi.util.BeeStingExplosionDamageSource(typeHolder, attacker);

                        target.hurtServer(serverLevel, explosionSrc, explodeDamage);

                        // 输出调试日志
                        me.tuanzi.util.ModLog.debug(attacker, target, "【蜂刺余响】叠满5层蜂毒！引爆所有层数，造成 15% 额外真实伤害: " + String.format("%.2f", explodeDamage));

                        // 播放引爆粒子效果
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                            target.getX(), target.getY() + 1.0, target.getZ(),
                            15, 0.3, 0.3, 0.3, 0.1);
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            target.getX(), target.getY() + 1.0, target.getZ(),
                            10, 0.2, 0.2, 0.2, 0.05);
                        // 播放引爆音效
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.BEE_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.6f);
                    }
                }
            } else {
                // 如果是其它伤害来源，生效后需要消耗掉（清空）蜂毒层数
                target.removeEffect(ModStatusEffects.BEE_POISON);
                me.tuanzi.util.ModLog.debug(source.getEntity(), target, "【蜂毒】被非蜂刺余响的伤害触发，已消耗清空蜂毒效果。");
            }
        }

        me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害计算完毕！计算前伤害: " + String.format("%.2f", initialDamage) + "，计算后最终伤害: " + String.format("%.2f", finalDamage));

        return finalDamage;
    }
}
