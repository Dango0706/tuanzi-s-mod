package me.tuanzi.util;

import me.tuanzi.entity.TrialDummyEntity;
import me.tuanzi.init.ModEnchantments;
import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import me.tuanzi.item.EarthPileHammerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;

/**
 * 伤害计算专用工具类，统一管理和计算模组中所有的伤害加成与调整逻辑。
 * 支持多重伤害修改效果的加法累加（Additive）计算。
 */
public class DamageCalculator {

        public static final ThreadLocal<Boolean> IS_SHOCKWAVE_ATTACK = ThreadLocal.withInitial(() -> false);

/**
     * 计算弩附魔力量时的箭矢基础伤害加成。
     * 原版弓力量增伤公式：0.5 + 0.5 * level（等价于 1.0 + 0.5 * (level - 1)）。
     * 附魔在弩上的增加伤害效果为弓上的一半：0.25 + 0.25 * level。
     *
     * @param level 附魔等级
     * @return 增加的箭矢基础伤害
     */
    public static float getCrossbowPowerBonus(int level) {
        if (level <= 0) return 0.0f;
        return 0.25f + 0.25f * level;
    }

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

        // 0.0 拾荒回廊队友全方位友伤免疫
        if (target instanceof ServerPlayer targetPlayer && source.getEntity() instanceof ServerPlayer attackerPlayer) {
            if (ScavengerTeamManager.areTeammates(attackerPlayer, targetPlayer)) {
                me.tuanzi.util.ModLog.debug(attackerPlayer, targetPlayer, "【拾荒回廊】队伍友伤免疫：伤害来源与目标为同一队伍，伤害减免为 0.00。");
                return 0.0f;
            }
        }

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

        // 预备弹仓副箭伤害衰减：第一箭之后的每一箭伤害同第一箭减少 (60 - 10 * level)%
        if (source.getDirectEntity() instanceof ReservedChamberProjectileAccessor accessor) {
            float penalty = accessor.tuanzis_mod$getReservedChamberDamagePenalty();
            if (penalty > 0.0f) {
                attackerMultiplier -= penalty;
                attackerMultiplier = Math.max(0.0f, attackerMultiplier);
                me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：【预备弹仓】副箭伤害衰减 " + String.format("%.0f%%", penalty * 100) + " (附魔等级: " + accessor.tuanzis_mod$getReservedChamberLevel() + "，当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
            }
        }

        // 1. 攻击者输出伤害乘数计算（加法累加模型）
        // 听涛 (Wave Listener)：在水中或雨中投掷三叉戟，伤害每级提高10%/20%/30%；只增加投掷伤害，不增加近战伤害。
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident trident) {
            ItemStack tridentWeapon = trident.getWeaponItem();
            if (tridentWeapon != null && !tridentWeapon.isEmpty()) {
                var enchRegistry = trident.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var waveHolder = enchRegistry.getOrThrow(ModEnchantments.WAVE_LISTENER);
                int waveLevel = EnchantmentHelper.getItemEnchantmentLevel(waveHolder, tridentWeapon);
                if (waveLevel > 0) {
                    boolean inWaterOrRain = trident.isInWaterOrRain()
                            || (source.getEntity() != null && source.getEntity().isInWaterOrRain());
                    if (inWaterOrRain) {
                        float waveBonus = 0.10f * waveLevel; // 10%, 20%, 30%
                        attackerMultiplier += waveBonus;
                        me.tuanzi.util.ModLog.debug(source.getEntity(), target, "【听涛】水/雨中投掷三叉戟增伤生效（等级: " + waveLevel + "）！伤害提升 "
                                + String.format(java.util.Locale.ROOT, "%.0f%%", waveBonus * 100.0f) + " (当前攻击乘数: " + String.format(java.util.Locale.ROOT, "%.2f", attackerMultiplier) + ")");
                    }
                }
            }
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            // 攻击者拥有“血怒”状态效果，攻击伤害乘算提升 40% (累加值 +0.4f)
            if (attacker.hasEffect(ModStatusEffects.BLOOD_RAGE)) {
                attackerMultiplier += 0.4f;
                me.tuanzi.util.ModLog.debug(attacker, target, "伤害更改触发：攻击者拥有血怒效果，攻击力增加 40% (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
            }

            // 攻击者穿戴狂战士附魔胸甲，近战伤害随已损失生命值比例获得提升 (I级每损失10%增伤5%，II级增伤8%)
            boolean isMelee = (source.is(DamageTypes.PLAYER_ATTACK)
                    || source.is(DamageTypes.MOB_ATTACK)
                    || source.is(DamageTypes.MACE_SMASH))
                    && !IS_SHOCKWAVE_ATTACK.get();
            if (isMelee) {
                // 天气附魔逻辑：玄冥雨刃 (Rain Blade)、青女霜刃 (Frost Blade)、雷公霆刃 (Thunder Blade)、金乌炽刃 (Sun Blade)
                ItemStack mainHandWeapon = attacker.getMainHandItem();
                if (!mainHandWeapon.isEmpty()) {
                    var enchRegistry = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var rainBladeEnch = enchRegistry.getOrThrow(ModEnchantments.RAIN_BLADE);
                    var frostBladeEnch = enchRegistry.getOrThrow(ModEnchantments.FROST_BLADE);
                    var thunderBladeEnch = enchRegistry.getOrThrow(ModEnchantments.THUNDER_BLADE);
                    var sunBladeEnch = enchRegistry.getOrThrow(ModEnchantments.SUN_BLADE);

                    int rainLevel = EnchantmentHelper.getItemEnchantmentLevel(rainBladeEnch, mainHandWeapon);
                    int frostLevel = EnchantmentHelper.getItemEnchantmentLevel(frostBladeEnch, mainHandWeapon);
                    int thunderLevel = EnchantmentHelper.getItemEnchantmentLevel(thunderBladeEnch, mainHandWeapon);
                    int sunLevel = EnchantmentHelper.getItemEnchantmentLevel(sunBladeEnch, mainHandWeapon);

                    if (rainLevel > 0 || frostLevel > 0 || thunderLevel > 0 || sunLevel > 0) {
                        float baseWeaponDmg = getBaseWeaponDamage(mainHandWeapon);
                        var level = attacker.level();
                        var pos = attacker.blockPosition();

                        net.minecraft.world.level.biome.Biome.Precipitation precipitation = net.minecraft.world.level.biome.Biome.Precipitation.NONE;
                        if (level.canHaveWeather()) {
                            precipitation = level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel());
                        }
                        boolean isRaining = level.isRaining();
                        boolean isThundering = level.isThundering();

                        boolean hasRain = isRaining && precipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN;
                        boolean hasSnow = isRaining && precipitation == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
                        boolean hasThunder = isThundering && (precipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN || precipitation == net.minecraft.world.level.biome.Biome.Precipitation.SNOW);
                        boolean isClear = !isRaining || precipitation == net.minecraft.world.level.biome.Biome.Precipitation.NONE;

                        if (rainLevel > 0) {
                            if (hasRain) {
                                float bonus = rainLevel * 1.15f + 1.0f;
                                amount += bonus;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【玄冥雨刃】生效！等级: " + rainLevel + "，处于下雨天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，增加基础伤害: " + String.format("%.2f", bonus) + " 点，当前伤害: " + String.format("%.2f", amount));
                            } else {
                                float penalty = 0.1625f * (6 - rainLevel);
                                amount = Math.max(1.0f, amount - penalty);
                                me.tuanzi.util.ModLog.debug(attacker, target, "【玄冥雨刃】生效！等级: " + rainLevel + "，非下雨天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，降低伤害: " + String.format("%.2f", penalty) + " 点，当前伤害: " + String.format("%.2f", amount));
                            }
                        }

                        if (frostLevel > 0) {
                            if (hasSnow) {
                                float bonus = frostLevel * 1.15f + 1.0f;
                                amount += bonus;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【青女霜刃】生效！等级: " + frostLevel + "，处于下雪天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，增加基础伤害: " + String.format("%.2f", bonus) + " 点，当前伤害: " + String.format("%.2f", amount));
                            } else {
                                float penalty = 0.1625f * (6 - frostLevel);
                                amount = Math.max(1.0f, amount - penalty);
                                me.tuanzi.util.ModLog.debug(attacker, target, "【青女霜刃】生效！等级: " + frostLevel + "，非下雪天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，降低伤害: " + String.format("%.2f", penalty) + " 点，当前伤害: " + String.format("%.2f", amount));
                            }
                        }

                        if (thunderLevel > 0) {
                            if (hasThunder) {
                                float bonus = thunderLevel * 1.45f + 1.0f;
                                amount += bonus;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【雷公霆刃】生效！等级: " + thunderLevel + "，处于雷雨/雷雪天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，增加基础伤害: " + String.format("%.2f", bonus) + " 点，当前伤害: " + String.format("%.2f", amount));
                            } else {
                                float penalty = 0.1825f * (6 - thunderLevel);
                                amount = Math.max(1.0f, amount - penalty);
                                me.tuanzi.util.ModLog.debug(attacker, target, "【雷公霆刃】生效！等级: " + thunderLevel + "，非雷雨/雷雪天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，降低伤害: " + String.format("%.2f", penalty) + " 点，当前伤害: " + String.format("%.2f", amount));
                            }
                        }

                        if (sunLevel > 0) {
                            if (isClear) {
                                float bonus = sunLevel * 1.15f + 1.0f;
                                amount += bonus;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【金乌炽刃】生效！等级: " + sunLevel + "，处于晴天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，增加基础伤害: " + String.format("%.2f", bonus) + " 点，当前伤害: " + String.format("%.2f", amount));
                            } else {
                                float penalty = 0.1625f * (6 - sunLevel);
                                amount = Math.max(1.0f, amount - penalty);
                                me.tuanzi.util.ModLog.debug(attacker, target, "【金乌炽刃】生效！等级: " + sunLevel + "，非晴天，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，降低伤害: " + String.format("%.2f", penalty) + " 点，当前伤害: " + String.format("%.2f", amount));
                            }
                        }
                    }

                    // 昼夜附魔逻辑：羲和昼刃 (Day Blade)、望舒夜刃 (Night Blade)
                    var dayBladeEnch = enchRegistry.getOrThrow(ModEnchantments.DAY_BLADE);
                    var nightBladeEnch = enchRegistry.getOrThrow(ModEnchantments.NIGHT_BLADE);

                    int dayLevel = EnchantmentHelper.getItemEnchantmentLevel(dayBladeEnch, mainHandWeapon);
                    int nightLevel = EnchantmentHelper.getItemEnchantmentLevel(nightBladeEnch, mainHandWeapon);

                    if (dayLevel > 0 || nightLevel > 0) {
                        var level = attacker.level();
                        boolean hasDayNightCycle = !level.dimensionType().hasFixedTime();
                        long dayTime = level.getDefaultClockTime() % 24000L;
                        if (dayTime < 0) dayTime += 24000L;
                        boolean isDay = hasDayNightCycle && (dayTime < 12000L);
                        boolean isNight = hasDayNightCycle && (dayTime >= 12000L);

                        float baseWeaponDmg = getBaseWeaponDamage(mainHandWeapon);

                        if (dayLevel > 0) {
                            if (isDay) {
                                float bonus = dayLevel * 0.675f + 0.2f;
                                amount += bonus;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【羲和昼刃】生效！等级: " + dayLevel + "，处于白天 (时间刻: " + dayTime + ")，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，增加基础伤害: " + String.format("%.2f", bonus) + " 点，当前伤害: " + String.format("%.2f", amount));
                            } else {
                                me.tuanzi.util.ModLog.debug(attacker, target, "【羲和昼刃】未触发！等级: " + dayLevel + "，非白天 (时间刻: " + dayTime + ")，不产生基础伤害增幅。");
                            }
                        }

                        if (nightLevel > 0) {
                            if (isNight) {
                                float bonus = nightLevel * 0.675f + 0.2f;
                                amount += bonus;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【望舒夜刃】生效！等级: " + nightLevel + "，处于黑夜 (时间刻: " + dayTime + ")，武器基础攻击力: " + String.format("%.2f", baseWeaponDmg) + "，增加基础伤害: " + String.format("%.2f", bonus) + " 点，当前伤害: " + String.format("%.2f", amount));
                            } else {
                                me.tuanzi.util.ModLog.debug(attacker, target, "【望舒夜刃】未触发！等级: " + nightLevel + "，非黑夜 (时间刻: " + dayTime + ")，不产生基础伤害增幅。");
                            }
                        }
                    }
                }

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

                // 脉冲谐振剑 (Pulse Resonance Sword) 与 过载协议 (Overload Protocol) 附魔逻辑
                ItemStack pulseWeaponStack = attacker.getMainHandItem();
                boolean isPulseSword = pulseWeaponStack.getItem() instanceof me.tuanzi.item.PulseResonanceSwordItem;
                int overloadLevel = 0;
                if (!pulseWeaponStack.isEmpty()) {
                    var registry = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var overloadProtocolEnch = registry.getOrThrow(ModEnchantments.OVERLOAD_PROTOCOL);
                    overloadLevel = EnchantmentHelper.getItemEnchantmentLevel(overloadProtocolEnch, pulseWeaponStack);
                }

                if (isPulseSword || overloadLevel > 0) {
                    net.minecraft.world.item.component.CustomData customData = pulseWeaponStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
                    int pulse = tag.getIntOr("PulseLevel", 0);
                    double wearValue = tag.getDoubleOr("WearValue", 0.0);

                    if (pulse >= 3) {
                        // 获取武器基础面板攻击力
                        final float[] dmgBox = {1.0f};
                        pulseWeaponStack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                                if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                    dmgBox[0] += (float) modifier.amount();
                                }
                            }
                        });
                        float baseWeaponDmg = Math.max(1.0f, dmgBox[0]);

                        // 满 3 层脉冲：触发脉冲电击爆发！
                        // 1. 脉冲谐振剑固有武器被动伤害 (+3.0)
                        float pulseSwordPassiveDmg = isPulseSword ? 3.0f : 0.0f;
                        // 2. 过载协议基础固定伤害：武器基础伤害 * (10% + 5% * 等级)
                        float overloadFixedDmg = (overloadLevel > 0) ? (baseWeaponDmg * (0.10f + 0.05f * overloadLevel)) : 0.0f;
                        // 3. 过载协议附魔等级额外伤害：0.85 * 等级
                        float overloadBonusDmg = (overloadLevel > 0) ? (0.85f * overloadLevel) : 0.0f;

                        float totalElectricDmg = pulseSwordPassiveDmg + overloadFixedDmg + overloadBonusDmg;

                        amount += totalElectricDmg;
                        tag.putInt("PulseLevel", 0);
                        pulseWeaponStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

                        if (!attacker.level().isClientSide() && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            float pitch = 1.3f - (float) wearValue * 0.5f;
                            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, pitch);
                            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.COPPER_BULB_TURN_ON, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, pitch);

                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                    target.getX(), target.getY() + 1.0, target.getZ(),
                                    24, 0.35, 0.35, 0.35, 0.15);
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                                    target.getX(), target.getY() + 1.0, target.getZ(),
                                    12, 0.25, 0.25, 0.25, 0.1);
                            if (wearValue >= 0.6) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                                        target.getX(), target.getY() + 1.0, target.getZ(),
                                        8, 0.2, 0.2, 0.2, 0.02);
                            }
                        }

                        me.tuanzi.util.ModLog.debug(attacker, target, "【脉冲电击】满层释放！武器基础攻击力: " + String.format("%.2f", baseWeaponDmg)
                                + "，武器被动(脉冲谐振剑): " + String.format("%.2f", pulseSwordPassiveDmg)
                                + "，过载协议固定值(基础伤害*(10%+5%*等级)): " + String.format("%.2f", overloadFixedDmg)
                                + "，过载协议等级加成(0.85*等级): " + String.format("%.2f", overloadBonusDmg)
                                + "，总电击额外伤害: " + String.format("%.2f", totalElectricDmg)
                                + " 点，算上普攻最终预期伤害: " + String.format("%.2f", amount)
                                + " 点，脉冲层数重置为 0。");
                    } else {
                        // 未满 3 层脉冲：充能判定！
                        boolean canCharge = true;
                        if (attacker instanceof net.minecraft.world.entity.player.Player player) {
                            me.tuanzi.util.TideCleaverPlayerTracker tracker = (me.tuanzi.util.TideCleaverPlayerTracker) player;
                            float attackStrength = tracker.tuanzis_mod$getLastAttackStrength();
                            if (attackStrength < 0.8f) {
                                canCharge = false;
                                me.tuanzi.util.ModLog.debug(attacker, target, "【脉冲充能】攻击蓄力时间不足 80%（当前蓄力比例: " + String.format("%.2f%%", attackStrength * 100) + "），无法叠加电容脉冲。");
                            }
                        }

                        if (canCharge) {
                            int add = 0;
                            if (isPulseSword) {
                                // 脉冲谐振剑固有机制：每次命中固定叠加 1 层脉冲，附魔过载协议时不叠加概率充能
                                add = 1;
                            } else if (overloadLevel > 0) {
                                // 普通武器附魔过载协议：按概率判定是否叠加 1 层脉冲
                                float chance = 0.05f + 0.05f * overloadLevel;
                                if (attacker.getRandom().nextFloat() < chance) {
                                    add = 1;
                                    me.tuanzi.util.ModLog.debug(attacker, target, "【过载协议】概率判定成功 (概率 " + String.format("%.0f", chance * 100) + "%)，充能 1 层！");
                                } else {
                                    me.tuanzi.util.ModLog.debug(attacker, target, "【过载协议】概率判定未触发 (概率 " + String.format("%.0f", chance * 100) + "%)。");
                                }
                            }

                            if (add > 0) {
                                int newPulse = Math.min(3, pulse + add);
                                tag.putInt("PulseLevel", newPulse);
                                pulseWeaponStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

                                if (!attacker.level().isClientSide() && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                    float basePitch = 0.9f + 0.3f * newPulse - (float) wearValue * 0.25f;
                                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                            SoundEvents.NOTE_BLOCK_CHIME.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, basePitch);
                                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                            SoundEvents.REDSTONE_TORCH_BURNOUT, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, basePitch);

                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                            target.getX(), target.getY() + 1.0, target.getZ(),
                                            5 * newPulse, 0.2, 0.2, 0.2, 0.05);
                                }

                                me.tuanzi.util.ModLog.debug(attacker, target, "【脉冲充能】成功！增加 " + add + " 层，当前脉冲层数: " + newPulse + "/3。");
                            }
                        }
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

                                int invTime = target.getInvulnerableTime();
                                target.setInvulnerableTime(0);
                                target.hurtServer(serverLevel, burstSrc, 3.0f);
                                target.setInvulnerableTime(invTime);

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

                // 星隙裁刃 (Star Rift Blade) 与 隙间裁决 (Gap Verdict) 机制
                ItemStack currentHeldWeapon = attacker.getMainHandItem();
                boolean isStarRiftBlade = currentHeldWeapon.getItem() instanceof me.tuanzi.item.StarRiftBladeItem;
                boolean targetHadStarRift = target.hasEffect(me.tuanzi.init.ModStatusEffects.STAR_RIFT);
                int initialRiftAmp = targetHadStarRift ? target.getEffect(me.tuanzi.init.ModStatusEffects.STAR_RIFT).getAmplifier() : -1;

                if (isStarRiftBlade) {
                    if (targetHadStarRift) {
                        // 命中已带有“星隙”的目标：额外造成基础攻击力 20% 的物理伤害 (7.0 * 0.20 = 1.4) 并刷新星隙持续时间至 4 秒
                        float starRiftBonus = 7.0f * 0.20f;
                        amount += starRiftBonus;
                        if (!attacker.level().isClientSide()) {
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    me.tuanzi.init.ModStatusEffects.STAR_RIFT, 80, 0, false, false, true
                            ));
                        }
                        me.tuanzi.util.ModLog.debug(attacker, target, "【星隙裁刃】命中已带有星隙的目标！额外造成基础攻击力 20% 物理伤害 (" + String.format("%.2f", starRiftBonus) + " 点)，并刷新本武器来源星隙持续时间 (当前伤害: " + String.format("%.2f", amount) + ")。");
                    } else {
                        // 第一击：只负责留下星隙，不享受额外伤害
                        if (!attacker.level().isClientSide()) {
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    me.tuanzi.init.ModStatusEffects.STAR_RIFT, 80, 0, false, false, true
                            ));
                        }
                        me.tuanzi.util.ModLog.debug(attacker, target, "【星隙裁刃】第一击命中无星隙目标！施加本武器来源星隙 4 秒 (不享受额外伤害)。");
                    }
                }

                // 隙间裁决 (Gap Verdict) 附魔结算
                var enchLookup = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var gapVerdictHolder = enchLookup.get(me.tuanzi.init.ModEnchantments.GAP_VERDICT);
                int gapVerdictLevel = 0;
                if (gapVerdictHolder.isPresent()) {
                    gapVerdictLevel = EnchantmentHelper.getItemEnchantmentLevel(gapVerdictHolder.get(), currentHeldWeapon);
                }

                if (gapVerdictLevel > 0) {
                    if (!targetHadStarRift && !isStarRiftBlade) {
                        // 目标没有“星隙”，且非星隙裁刃已由武器施加：检查附魔 4 秒冷却
                        if (!attacker.hasEffect(me.tuanzi.init.ModStatusEffects.GAP_VERDICT_COOLDOWN)) {
                            if (!attacker.level().isClientSide()) {
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        me.tuanzi.init.ModStatusEffects.STAR_RIFT, 80, 1, false, false, true
                                ));
                                attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        me.tuanzi.init.ModStatusEffects.GAP_VERDICT_COOLDOWN, 80, 0, false, false, false
                                ));
                            }
                            me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】附魔命中无星隙目标！施加其他来源星隙 4 秒，攻击者进入 4 秒施加冷却。");
                        } else {
                            me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】附魔处于 4 秒施加冷却中，未能对无星隙目标施加星隙。");
                        }
                    } else if (targetHadStarRift) {
                        // 攻击带有“星隙”的目标
                        boolean isFromThisWeapon = (initialRiftAmp == 0) && isStarRiftBlade;
                        float speedBonusRate;
                        float damageBonusRate;

                        if (isFromThisWeapon) {
                            speedBonusRate = 0.05f * gapVerdictLevel;
                            damageBonusRate = 0.02f * gapVerdictLevel;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】星隙由本武器本身施加！基准加成: 攻速 +" + String.format("%.2f%%", speedBonusRate * 100) + "，伤害 +" + String.format("%.2f%%", damageBonusRate * 100));
                        } else {
                            speedBonusRate = 0.0075f * gapVerdictLevel;
                            damageBonusRate = 0.0075f * gapVerdictLevel;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】星隙由其他来源施加！基准加成: 攻速 +" + String.format("%.2f%%", speedBonusRate * 100) + "，伤害 +" + String.format("%.2f%%", damageBonusRate * 100));
                        }

                        final float[] weaponDmgBox = {1.0f};
                        final float[] weaponSpdBox = {4.0f};
                        currentHeldWeapon.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) && modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                weaponDmgBox[0] += (float) modifier.amount();
                            } else if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED) && modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                weaponSpdBox[0] += (float) modifier.amount();
                            }
                        });

                        GapVerdictBonus gvBonus = calcGapVerdictBonus(gapVerdictLevel, isFromThisWeapon, weaponSpdBox[0], weaponDmgBox[0]);
                        speedBonusRate = gvBonus.speedBonusRate();
                        damageBonusRate = gvBonus.damageBonusRate();

                        if (weaponSpdBox[0] > 1.6001f) {
                            me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】武器攻击速度 (" + String.format("%.2f", weaponSpdBox[0]) + ") 高于 1.6，攻速加成减半至 " + String.format("%.4f%%", speedBonusRate * 100));
                        }
                        if (weaponDmgBox[0] > 7.0001f) {
                            me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】武器基础攻击力 (" + String.format("%.2f", weaponDmgBox[0]) + ") 高于 7.0，伤害加成减半至 " + String.format("%.4f%%", damageBonusRate * 100));
                        }

                        // 加法累加伤害乘数
                        attackerMultiplier += damageBonusRate;
                        me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】伤害提高加成生效: +" + String.format("%.4f%%", damageBonusRate * 100) + " (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");

                        // 攻击速度提升 (持续 4 秒)
                        me.tuanzi.item.StarRiftBladeItem.applyGapVerdictSpeedBuff(attacker, speedBonusRate, 80);
                        me.tuanzi.util.ModLog.debug(attacker, target, "【隙间裁决】攻速提高加成生效: +" + String.format("%.4f%%", speedBonusRate * 100) + "，持续 80 刻 (4秒)。");
                    }
                }

                // 贯震铭刻 (Pervading Vibration) 附魔结算
                var pervadingHolder = enchLookup.get(ModEnchantments.PERVADING_VIBRATION);
                int pervadingLevel = 0;
                if (pervadingHolder.isPresent()) {
                    pervadingLevel = EnchantmentHelper.getItemEnchantmentLevel(pervadingHolder.get(), currentHeldWeapon);
                }
                if (pervadingLevel > 0) {
                    final float[] atkSpeedBox = {4.0f};
                    currentHeldWeapon.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
                        if (attr.is(Attributes.ATTACK_SPEED) && mod.operation() == AttributeModifier.Operation.ADD_VALUE) {
                            atkSpeedBox[0] += (float) mod.amount();
                        }
                    });
                    float weaponSpeed = atkSpeedBox[0];
                    float speedEfficiency = weaponSpeed > 1.0f ? 0.175f : 1.0f;

                    float cooldownScale = 1.0f;
                    if (attacker instanceof TideCleaverPlayerTracker tracker) {
                        cooldownScale = tracker.tuanzis_mod$getLastAttackStrength();
                    }

                    if (cooldownScale >= 0.8f) {
                        // 冷却 >= 80%：在伤害结算前，将“贯震”层数直接设为 5 层 (amplifier = 4)，刷新 4 秒 (80 ticks)
                        if (!attacker.level().isClientSide()) {
                            attacker.addEffect(new MobEffectInstance(ModStatusEffects.PERVADING_VIBRATION, 80, 4, false, true, true));
                        }
                        // 本次攻击直接按 5 层结算加成
                        float bonusDmg = 5 * (0.24f * pervadingLevel) * speedEfficiency;
                        amount += bonusDmg;
                        me.tuanzi.util.ModLog.debug(attacker, target, "【贯震铭刻】冷却 >= 80% (当前蓄力: " + String.format("%.2f", cooldownScale)
                                + ")！直接设为5层贯震并刷新4秒，武器攻速: " + String.format("%.2f", weaponSpeed)
                                + " (降效系数: " + String.format("%.3f", speedEfficiency) + ")，造成额外伤害: +"
                                + String.format("%.2f", bonusDmg) + " 点 (当前伤害: " + String.format("%.2f", amount) + ")。");
                    } else {
                        // 冷却 < 80%：本次攻击不享受“命中后获得 1 层”，只读取命中前已有层数
                        int existingStacks = 0;
                        if (attacker.hasEffect(ModStatusEffects.PERVADING_VIBRATION)) {
                            MobEffectInstance effect = attacker.getEffect(ModStatusEffects.PERVADING_VIBRATION);
                            if (effect != null) {
                                existingStacks = effect.getAmplifier() + 1;
                            }
                        }
                        if (existingStacks > 0) {
                            float bonusDmg = existingStacks * (0.24f * pervadingLevel) * speedEfficiency;
                            amount += bonusDmg;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【贯震铭刻】冷却 < 80% (当前蓄力: " + String.format("%.2f", cooldownScale)
                                    + ")！按命中前已有层数 (" + existingStacks + " 层) 结算，额外伤害: +"
                                    + String.format("%.2f", bonusDmg) + " 点 (当前伤害: " + String.format("%.2f", amount) + ")。");
                        }
                        // 命中后获得 1 层 (最多 5 层)，刷新 4 秒
                        int newStacks = Math.min(5, existingStacks + 1);
                        if (!attacker.level().isClientSide()) {
                            attacker.addEffect(new MobEffectInstance(ModStatusEffects.PERVADING_VIBRATION, 80, newStacks - 1, false, true, true));
                        }
                        me.tuanzi.util.ModLog.debug(attacker, target, "【贯震铭刻】命中后获得 1 层贯震 (现为 " + newStacks + " 层)，持续时间刷新为 4 秒。");
                    }
                }

                // 地脉定桩锤 (Earth Pile Hammer) 专属机制结算
                if (currentHeldWeapon.getItem() instanceof EarthPileHammerItem) {
                    float cooldownScale = 1.0f;
                    if (attacker instanceof TideCleaverPlayerTracker tracker) {
                        cooldownScale = tracker.tuanzis_mod$getLastAttackStrength();
                    }

                    CustomData customData = currentHeldWeapon.get(DataComponents.CUSTOM_DATA);
                    CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
                    double wearValue = tag.getDoubleOr("WearValue", 0.0);

                    if (cooldownScale >= 0.8f) {
                        // 定桩重击：造成 100% 攻击力 (amount 保持 100%)
                        // 触发命中点半径 2.5 格震荡波
                        EarthPileHammerItem.triggerEarthShockwave(attacker, target, amount, wearValue);

                        // 启动“地脉回响”静止判定窗口 (2 秒内未移动，下一次攻击冷却恢复速度 +10%)
                        if (attacker instanceof Player player) {
                            EarthEchoTracker.startWindow(player);
                        }

                        me.tuanzi.util.ModLog.debug(attacker, target, "【地脉定桩锤】定桩重击触发 (冷却: " + String.format("%.2f", cooldownScale)
                                + " >= 80%)！造成 100% 完整伤害: " + String.format("%.2f", amount)
                                + " 点，已释放震荡波并启动地脉回响 2 秒静止判定窗口。");
                    } else {
                        // 冷却 < 80%：本次攻击仅造成 65% 攻击力，且自身获得 0.5 秒 (10 ticks) 缓慢 I
                        float fullDmg = amount;
                        amount *= 0.65f;
                        if (!attacker.level().isClientSide()) {
                            attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 0, false, true, true));
                        }
                        me.tuanzi.util.ModLog.debug(attacker, target, "【地脉定桩锤】攻击冷却不足 (冷却: " + String.format("%.2f", cooldownScale)
                                + " < 80%)！伤害折减为 65% (" + String.format("%.2f", fullDmg) + " -> " + String.format("%.2f", amount)
                                + ")，自身获得 0.5 秒缓慢I惩罚。");
                    }
                }

                // 野太刀 (Nodachi) 刀锋甜点区与势连击伤害加成
                if (attacker.getMainHandItem().getItem() instanceof me.tuanzi.item.NodachiItem) {
                    if (attacker instanceof me.tuanzi.util.NodachiPlayerTracker tracker) {
                        int momentum = tracker.tuanzis_mod$getNodachiMomentum();
                        double dist = attacker.distanceTo(target);

                        // 刀根贴脸（0 ~ 1.5 格）：造成 85% 伤害 (乘数 -0.15f)
                        if (dist <= 1.5) {
                            attackerMultiplier -= 0.15f;
                            attackerMultiplier = Math.max(0.0f, attackerMultiplier);
                            me.tuanzi.util.ModLog.debug(attacker, target, "【野太刀·刀根贴脸】目标距离 " + String.format("%.2f", dist) + " 格 (<= 1.5格)，造成 85% 伤害 (衰减 15%，当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
                        } else if (dist < 2.2) {
                            // 中段击中（1.5 ~ 2.2 格）：造成 100% 完整伤害 (基准不增不减)
                            me.tuanzi.util.ModLog.debug(attacker, target, "【野太刀·刀身中段】目标距离 " + String.format("%.2f", dist) + " 格 (1.5 ~ 2.2格)，造成 100% 完整伤害 (当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
                        } else {
                            // 刃尖击中（>= 2.2 格）：造成 1.15 倍伤害 (乘数 +0.15f)
                            attackerMultiplier += 0.15f;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【野太刀·刃尖甜点】目标距离 " + String.format("%.2f", dist) + " 格 (>= 2.2格)，造成 1.15 倍伤害 (提升 15%，当前攻击乘数累加值: " + String.format("%.2f", attackerMultiplier) + ")。");
                        }

                        // 连击 1 层：伤害 +1
                        // 连击 2 层 / 满层（3 层）：伤害 +2
                        if (momentum == 1) {
                            amount += 1.0f;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【野太刀·势】连击 1 层生效，伤害 +1 点 (当前伤害: " + String.format("%.2f", amount) + ")。");
                        } else if (momentum >= 2) {
                            amount += 2.0f;
                            me.tuanzi.util.ModLog.debug(attacker, target, "【野太刀·势】连击 " + momentum + " 层生效，伤害 +2 点 (当前伤害: " + String.format("%.2f", amount) + ")。");
                        }
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
                            int originalInvulnerableTime = target.getInvulnerableTime();
                            target.setInvulnerableTime(0);

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
                                    nearby.setInvulnerableTime(0);
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
                            target.setInvulnerableTime(originalInvulnerableTime);

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

                        int originalInvulnerableTime = target.getInvulnerableTime();
                        target.setInvulnerableTime(0);

                        var magicType = attacker.registryAccess()
                                .lookupOrThrow(Registries.DAMAGE_TYPE)
                                .getOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC);
                        net.minecraft.world.damagesource.DamageSource magicSrc = new net.minecraft.world.damagesource.DamageSource(magicType, attacker);

                        target.hurtServer(serverLevel, magicSrc, totalExtraDmg);

                        target.setInvulnerableTime(originalInvulnerableTime);

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
        }

        // 被攻击者拥有“鸿痕”标记，受到玩家造成的伤害每级提高 3%/6%/9%
        if (target.hasEffect(ModStatusEffects.HONG_HEN) && source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            var effect = target.getEffect(ModStatusEffects.HONG_HEN);
            int markLevel = effect != null ? effect.getAmplifier() + 1 : 1;
            float markBonus = 0.03f * markLevel; // 3%, 6%, 9%
            victimMultiplier += markBonus;
            me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：受击者拥有【鸿痕】标记（等级: " + markLevel + "），受到玩家伤害提升 "
                    + String.format(java.util.Locale.ROOT, "%.0f%%", markBonus * 100.0f) + " (当前受伤乘数累加值: " + String.format(java.util.Locale.ROOT, "%.2f", victimMultiplier) + ")。");
        }

        // 日冕核心 · 烈焰统领 黑石重盾防御联动减伤 (一阶段每存活 1 面盾提供 8.75% 伤害减免，4 面满额 35%；二阶段盾牌转化为切削利刃不再具有免伤)
        if (target instanceof me.tuanzi.entity.CinderSovereignEntity boss) {
            if (boss.isCrucibleVulnerable()) {
                // 阶段三：卡地卡死虚弱窗口，防御剥夺，全额弹道偏折与减伤被动失效，且玩家近战攻击享受 +30% 额外弱点增伤！
                if (source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                    victimMultiplier += 0.3f;
                    me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：【日冕核心·烈焰统领】处于卡地暴露虚弱窗口，玩家近战攻击享受 +30% 额外弱点增伤！(当前受伤乘数累加值: "
                            + String.format("%.2f", victimMultiplier) + ")");
                }
                me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：【日冕核心·烈焰统领】处于卡地受限状态，黑石重盾防御减免被动完全失效！");
            } else if (boss.isPhase2()) {
                me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：【日冕核心·烈焰统领】处于第二阶段·日冕暴怒，黑石盾牌已转化为切削利刃，不再提供免伤与护甲加成！");
            } else {
                int aliveShields = boss.getAliveShieldCount();
                if (aliveShields > 0) {
                    // 检查是否为携带“穿透”附魔的箭矢伤害：穿透箭矢贯穿黑石重盾，无视重盾免伤
                    boolean isPiercingArrow = false;
                    int pierceLevel = 0;
                    if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
                        pierceLevel = arrow.getPierceLevel();
                        if (pierceLevel <= 0 && arrow.getWeaponItem() != null && target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            pierceLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getPiercingCount(
                                    serverLevel, arrow.getWeaponItem(), arrow.getPickupItemStackOrigin());
                        }
                        if (pierceLevel > 0) {
                            isPiercingArrow = true;
                        }
                    }

                    if (isPiercingArrow) {
                        me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：【日冕核心·烈焰统领】遭受携带“穿透”附魔的箭矢攻击 (穿透等级: "
                                + pierceLevel + ")！穿透弹道无视全部 " + aliveShields + " 面悬浮黑石重盾免伤效果！");
                    } else {
                        float shieldReduction = aliveShields * 0.0875f;
                        victimMultiplier -= shieldReduction;
                        victimMultiplier = Math.max(0.0f, victimMultiplier);
                        me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害更改触发：【日冕核心·烈焰统领】悬浮黑石重盾防御联动生效！存活重盾: "
                                + aliveShields + "/4，减免伤害 " + String.format("%.2f%%", shieldReduction * 100)
                                + " (当前受伤乘数累加值: " + String.format("%.2f", victimMultiplier) + ")。");
                    }
                }
            }
        }

        float finalDamage = amount * attackerMultiplier * victimMultiplier;

        // 【旅者】(Wayfarer) 附魔减免探险伤害逻辑 (鞘翅撞击 / 仙人掌 / 末影珍珠 / 蜜蜂攻击 / 摔落)
        ItemStack legsStack = target.getItemBySlot(EquipmentSlot.LEGS);
        if (!legsStack.isEmpty() && finalDamage > 0.0f) {
            var enchRegistry = target.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var wayfarerEnch = enchRegistry.getOrThrow(ModEnchantments.WAYFARER);
            int wayfarerLevel = EnchantmentHelper.getItemEnchantmentLevel(wayfarerEnch, legsStack);
            if (wayfarerLevel > 0) {
                float flatReduction = 0.0f;
                String damageName = "";
                if (source.is(DamageTypes.FLY_INTO_WALL)) {
                    flatReduction = getWayfarerFlyIntoWallReduction(wayfarerLevel);
                    damageName = "鞘翅撞击伤害";
                } else if (source.is(DamageTypes.CACTUS)) {
                    flatReduction = getWayfarerCactusReduction(wayfarerLevel);
                    damageName = "仙人掌伤害";
                } else if (source.is(DamageTypes.ENDER_PEARL)) {
                    flatReduction = getWayfarerEnderPearlReduction(wayfarerLevel);
                    damageName = "末影珍珠传送伤害";
                } else if (source.is(DamageTypes.STING) || source.getEntity() instanceof net.minecraft.world.entity.animal.bee.Bee) {
                    flatReduction = getWayfarerBeeAttackReduction(wayfarerLevel);
                    damageName = "蜜蜂攻击伤害";
                } else if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                    flatReduction = getWayfarerFallReduction(wayfarerLevel);
                    damageName = "摔落伤害";
                }

                if (flatReduction > 0.0f) {
                    float beforeReduction = finalDamage;
                    finalDamage = Math.max(0.0f, finalDamage - flatReduction);
                    me.tuanzi.util.ModLog.debug(source.getEntity(), target, "【旅者】附魔生效（等级: " + wayfarerLevel + "）！减免" + damageName + " "
                            + String.format(java.util.Locale.ROOT, "%.2f", flatReduction) + " 点，减免前伤害: "
                            + String.format(java.util.Locale.ROOT, "%.2f", beforeReduction) + "，减免后最终伤害: "
                            + String.format(java.util.Locale.ROOT, "%.2f", finalDamage));
                }
            }
        }

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

        // 古卷附魔-汲血 (Ancient Scroll - Blood Leech) 逻辑
        if (source.getEntity() instanceof LivingEntity attacker && finalDamage > 0.0f) {
            if (target instanceof TrialDummyEntity) {
                me.tuanzi.util.ModLog.debug(attacker, target, "【古卷附魔-汲血】目标为试炼人偶，无法吸取生命值。");
            } else {
                ItemStack bloodLeechWeapon = attacker.getMainHandItem();
                if (!bloodLeechWeapon.isEmpty()) {
                    var registry = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var bloodLeechEnch = registry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_BLOOD_LEECH);
                    int bloodLeechLevel = EnchantmentHelper.getItemEnchantmentLevel(bloodLeechEnch, bloodLeechWeapon);
                    if (bloodLeechLevel > 0) {
                        float healPercent = 0.05f + 0.05f * bloodLeechLevel;
                        float healAmount = finalDamage * healPercent;
                        if (healAmount > 0.0f) {
                            attacker.heal(healAmount);
                            if (!attacker.level().isClientSide() && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                                        attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                                        Math.max(1, (int) (healAmount / 2.0f)), 0.3, 0.3, 0.3, 0.1);
                            }
                            me.tuanzi.util.ModLog.debug(attacker, target, "【古卷附魔-汲血】附魔触发！等级: " + bloodLeechLevel + "，造成伤害: " + String.format("%.2f", finalDamage) + "，吸血比例: " + String.format("%.1f", healPercent * 100) + "%，回复生命值: " + String.format("%.2f", healAmount) + " 点。");
                        }
                    }
                }
            }
        }

        me.tuanzi.util.ModLog.debug(source.getEntity(), target, "伤害计算完毕！计算前伤害: " + String.format("%.2f", initialDamage) + "，计算后最终伤害: " + String.format("%.2f", finalDamage));

        return finalDamage;
    }

    /**
     * 计算受击者被古卷附魔（切金断玉、贯革）无视后的有效护甲值。
     *
     * @param totalArmor 受击者受击前的原始护甲值
     * @param source     伤害来源
     * @param victim     受击者
     * @return 削减后的实际有效护甲值
     */
    public static float calculateArmorIgnored(float totalArmor, DamageSource source, LivingEntity victim) {
        if (totalArmor <= 0.0f) {
            return totalArmor;
        }

        ItemStack weapon = source.getWeaponItem();
        if ((weapon == null || weapon.isEmpty()) && source.getEntity() instanceof LivingEntity attacker) {
            weapon = attacker.getMainHandItem();
        }

        if (weapon == null || weapon.isEmpty()) {
            return totalArmor;
        }

        var registry = victim.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        float ignorePercent = 0.0f;

        // 1. 古卷附魔-切金断玉 (适用于剑、斧)：无视对方 (10 + 5 * level)% 的护甲值
        var metalCutterEnch = registry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_METAL_CUTTER);
        int metalCutterLevel = EnchantmentHelper.getItemEnchantmentLevel(metalCutterEnch, weapon);
        if (metalCutterLevel > 0) {
            float p = 0.10f + 0.05f * metalCutterLevel;
            ignorePercent += p;
            me.tuanzi.util.ModLog.debug(source.getEntity(), victim, "【古卷附魔-切金断玉】生效！等级: " + metalCutterLevel + "，无视护甲累加: " + String.format("%.1f", p * 100) + "%");
        }

        // 2. 古卷附魔-贯革 (适用于弓、弩)：无视对方 (10 + 5 * level)% 的护甲值
        var leatherPiercerEnch = registry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_LEATHER_PIERCER);
        int leatherPiercerLevel = EnchantmentHelper.getItemEnchantmentLevel(leatherPiercerEnch, weapon);
        if (leatherPiercerLevel > 0) {
            float p = 0.10f + 0.05f * leatherPiercerLevel;
            ignorePercent += p;
            me.tuanzi.util.ModLog.debug(source.getEntity(), victim, "【古卷附魔-贯革】生效！等级: " + leatherPiercerLevel + "，无视护甲累加: " + String.format("%.1f", p * 100) + "%");
        }

        if (ignorePercent > 0.0f) {
            ignorePercent = Math.min(1.0f, ignorePercent);
            float newArmor = Math.max(0.0f, totalArmor * (1.0f - ignorePercent));
            me.tuanzi.util.ModLog.debug(source.getEntity(), victim, "古卷破甲结算：总无视比例: " + String.format("%.1f", ignorePercent * 100) + "%，目标护甲值: " + String.format("%.2f", totalArmor) + " -> 削减后有效护甲: " + String.format("%.2f", newArmor));
            return newArmor;
        }

        return totalArmor;
    }

    /**
     * 获取主手武器的基础物理攻击力面板加成值 (包含主手基础 1.0f 加上属性修饰符)
     */
    public static float getBaseWeaponDamage(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return 1.0f;
        }
        final float[] dmgBox = {1.0f};
        weapon.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                    dmgBox[0] += (float) modifier.amount();
                }
            }
        });
        return Math.max(1.0f, dmgBox[0]);
    }

    /**
     * 计算古卷附魔-屹立不倒所需的经验点数 E(x, y)
     * E(x,y) = max(300 - 50 * y, floor( 1 + 30969 * [ min(t, t0)^q + (1 - t0^q) * max(0, (t - t0) / (1 - t0))^p ] ))
     * 其中 t = x / (15y), t0 = 2 / 15, q = 2.0, p = 1.0
     * 判定与消耗均具有保底最小值: 300 - 50 * level
     *
     * @param x 致死超出伤害值 (玩家承受伤害中超出 (health - 1) 的额外伤害部分)
     * @param y 附魔等级 (1 - 5)
     * @return 触发免死所需的经验点数 E
     */
    public static long calcStandingFirmExperience(float x, int y) {
        if (y <= 0) {
            return Long.MAX_VALUE;
        }
        double t = (double) Math.max(0.0f, x) / (15.0 * y);
        double t0 = 2.0 / 15.0;
        double t0Squared = t0 * t0;
        double low = Math.pow(Math.min(t, t0), 2.0);
        double high = Math.max(0.0, (t - t0) / (1.0 - t0));
        double f = low + (1.0 - t0Squared) * high;
        long rawExp = (long) Math.floor(1.0 + 30969.0 * f + 1e-10);
        long minExp = Math.max(0L, 300L - 50L * y);
        return Math.max(minExp, rawExp);
    }

    /**
     * 根据玩家持有的总经验点数与附魔等级，反解当前经验最多可抵挡的致死超出伤害量 x
     * (使得 E(x, y) < totalExp)
     *
     * @param totalExp 玩家当前总经验点数
     * @param level    附魔等级 (1 - 5)
     * @return 最多可抵挡的致死超出伤害量
     */
    public static float calcMaxBlockableDamage(int totalExp, int level) {
        if (level <= 0) {
            return 0.0f;
        }
        long minExp = Math.max(0L, 300L - 50L * level);
        if (totalExp <= minExp) {
            return 0.0f;
        }
        long eCap = totalExp - 1;
        double t0 = 2.0 / 15.0;
        double t0Squared = t0 * t0; // 4.0 / 225.0
        // 临界 E 点数 (x = 2y 时) 为 551
        if (eCap < 551) {
            double f = (double) eCap / 30969.0;
            double t = Math.sqrt(Math.min(f, t0Squared));
            return (float) (t * 15.0 * level);
        } else {
            double f = (double) eCap / 30969.0;
            double t = (15.0 * f + 2.0) / 17.0;
            return (float) (t * 15.0 * level);
        }
    }

    /**
     * 获取玩家当前持有的全部经验点数（等级折算总点数 + 当前经验条进度点数）
     */
    public static int getTotalExperiencePoints(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return 0;
        }
        int levelExp = getExperienceForLevel(player.experienceLevel);
        int progressExp = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return levelExp + progressExp;
    }

    /**
     * 获取达到指定等级所需的累计经验总点数（原版公式）
     */
    public static int getExperienceForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * 古卷附魔-屹立不倒 (Ancient Scroll - Standing Firm)
     * 当玩家受到致死伤害时，有 等级 * 20% 概率进行判定。
     * 若玩家当前持有的总经验点数 > E(x, y)，则消耗 E 点经验，免疫超额致死伤害，使玩家保留 1.0F 生命值。
     * 判定早于不死药水/不死图腾。
     *
     * @param dmg    即将从生命值中扣除的伤害
     * @param source 伤害来源
     * @param player 受击玩家
     * @return 调整后的伤害（若触发则返回 player.getHealth() - 1.0F）
     */
    public static float applyStandingFirmProtection(float dmg, DamageSource source, net.minecraft.world.entity.player.Player player) {
        if (player == null || player.isDeadOrDying()) {
            return dmg;
        }

        float currentHealth = player.getHealth();
        // 如果伤害不足以致死，或者玩家生命值异常，直接返回
        if (dmg < currentHealth || currentHealth <= 0.0f) {
            return dmg;
        }

        // 排除绕过无敌与保护的特殊伤害（例如掉出世界/虚空伤害、/kill 等拥有 BYPASSES_INVULNERABILITY 标签）
        if (source != null && source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            me.tuanzi.util.ModLog.debug(source.getEntity(), player, "【古卷附魔-屹立不倒】伤害来源绕过无敌保护 (BYPASSES_INVULNERABILITY)，无法触发免死。");
            return dmg;
        }

        // 检查胸甲上的附魔等级
        var registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var standingFirmEnch = registry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_STANDING_FIRM);
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        int level = EnchantmentHelper.getItemEnchantmentLevel(standingFirmEnch, chestItem);

        if (level <= 0) {
            return dmg;
        }

        // 1. 概率判定：等级 * 20%
        float chance = Math.min(1.0f, level * 0.20f);
        float roll = player.getRandom().nextFloat();
        me.tuanzi.util.ModLog.debug(source.getEntity(), player, "【古卷附魔-屹立不倒】检测到致死伤害！等级: " + level + "，触发概率: " + String.format("%.1f%%", chance * 100) + "，随机骰值: " + String.format("%.4f", roll));

        if (roll >= chance) {
            me.tuanzi.util.ModLog.debug(source.getEntity(), player, "【古卷附魔-屹立不倒】概率判定未通过，免死保护未触发。");
            return dmg;
        }

        // 2. 计算致死伤害值 x：使玩家保留 1 血，受到 health - 1 伤害，免疫额外伤害 x
        float allowedDamage = Math.max(0.0f, currentHealth - 1.0f);
        float x = dmg - allowedDamage;

        // 3. 计算所需经验点数 E(x, y)
        long requiredExp = calcStandingFirmExperience(x, level);

        // 4. 计算玩家当前持有的总经验点数
        int totalExp = getTotalExperiencePoints(player);
        long minExp = Math.max(0L, 300L - 50L * level);
        me.tuanzi.util.ModLog.debug(source.getEntity(), player, "【古卷附魔-屹立不倒】致死超出伤害 x: " + String.format("%.2f", x) + "，所需经验 E: " + requiredExp + " (保底最小值: " + minExp + ")，玩家当前总经验点数: " + totalExp);

        if (totalExp <= requiredExp) {
            me.tuanzi.util.ModLog.debug(source.getEntity(), player, "【古卷附魔-屹立不倒】总经验不足（需 > " + requiredExp + "，持有 " + totalExp + "），无法触发。");
            return dmg;
        }

        // 5. 触发成功：扣除经验
        player.giveExperiencePoints((int) -requiredExp);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                    serverPlayer.experienceProgress,
                    serverPlayer.totalExperience,
                    serverPlayer.experienceLevel
            ));
        }

        // 6. 赋予受击无敌帧与保护
        player.damageCooldownTime = 20;

        // 7. 视听反馈：专属音效与粒子
        playStandingFirmEffects(player);

        me.tuanzi.util.ModLog.debug(source.getEntity(), player, "【古卷附魔-屹立不倒】成功免死！已扣除 " + requiredExp + " 点经验，将伤害从 " + String.format("%.2f", dmg) + " 限制为 " + String.format("%.2f", allowedDamage) + "，保留生命值 1.0F。");

        return allowedDamage;
    }

    private static void playStandingFirmEffects(net.minecraft.world.entity.player.Player player) {
        var level = player.level();
        // 播放专属音效（沉重格挡 + 魔法共鸣 + 铁砧落地音，绝不触发图腾特效）
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.6F);

        // 服务端生成专属粒子（古卷附魔紫色/传送门符文与光辉）
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(), 45, 0.6, 0.8, 0.6, 0.5);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.6, 0.8, 0.6, 0.2);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                    player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.5, 0.6, 0.5, 0.1);
        }
    }

    /**
     * 计算【旅者】附魔针对鞘翅撞击伤害的减免数值。
     * 公式: 2*level + 2*(level-1)
     */
    public static float getWayfarerFlyIntoWallReduction(int level) {
        if (level <= 0) return 0.0f;
        return 2.0f * level + 2.0f * (level - 1);
    }

    /**
     * 计算【旅者】附魔针对仙人掌伤害的减免数值。
     * 公式: 2*level
     */
    public static float getWayfarerCactusReduction(int level) {
        if (level <= 0) return 0.0f;
        return 2.0f * level;
    }

    /**
     * 计算【旅者】附魔针对末影珍珠传送伤害的减免数值。
     * 公式: 2*level
     */
    public static float getWayfarerEnderPearlReduction(int level) {
        if (level <= 0) return 0.0f;
        return 2.0f * level;
    }

    /**
     * 计算【旅者】附魔针对蜜蜂攻击伤害的减免数值。
     * 公式: 2*level
     */
    public static float getWayfarerBeeAttackReduction(int level) {
        if (level <= 0) return 0.0f;
        return 2.0f * level;
    }

    /**
     * 计算【旅者】附魔针对摔落伤害的减免数值。
     * 公式: 1.5*(level + 1)
     */
    public static float getWayfarerFallReduction(int level) {
        if (level <= 0) return 0.0f;
        return 1.5f * (level + 1);
    }

    /**
     * 计算【星隙裁刃】命中已带有星隙的目标时造成的额外物理伤害。
     * 基础攻击力的 20%
     */
    public static float calcStarRiftBladeBonusDamage(float baseAttackDamage) {
        return baseAttackDamage * 0.20f;
    }

    public record GapVerdictBonus(float speedBonusRate, float damageBonusRate) {}

    /**
     * 计算【隙间裁决】附魔攻击带有星隙的目标时的加成数值。
     */
    public static GapVerdictBonus calcGapVerdictBonus(int level, boolean isFromWeaponSelf, float weaponSpeed, float weaponDamage) {
        if (level <= 0) {
            return new GapVerdictBonus(0.0f, 0.0f);
        }
        float speedBonus = isFromWeaponSelf ? (0.05f * level) : (0.0075f * level);
        float damageBonus = isFromWeaponSelf ? (0.02f * level) : (0.0075f * level);

        if (weaponSpeed > 1.6001f) {
            speedBonus /= 2.0f;
        }
        if (weaponDamage > 7.0001f) {
            damageBonus /= 2.0f;
        }
        return new GapVerdictBonus(speedBonus, damageBonus);
    }


    /**
     * 计算【贯震铭刻】附魔提供的额外攻击伤害加成。
     *
     * @param level 附魔等级 (1~5)
     * @param stacks 贯震层数 (1~5)
     * @param weaponSpeed 武器基础攻击速度
     * @return 增加的物理伤害值
     */
    public static float calcPervadingVibrationBonus(int level, int stacks, float weaponSpeed) {
        if (level <= 0 || stacks <= 0) return 0.0f;
        int clampedStacks = Math.min(5, stacks);
        float efficiency = weaponSpeed > 1.0f ? 0.175f : 1.0f;
        return clampedStacks * (0.24f * level) * efficiency;
    }

    /**
     * 计算【地脉定桩锤】直击主目标的伤害。
     * 冷却 >= 80%：造成 100% 攻击力。
     * 冷却 < 80%：造成 65% 攻击力。
     *
     * @param attackDamage 本次攻击力快照 (含基础攻击力与贯震加成)
     * @param cooldownScale 攻击冷却蓄力比例 (0.0 ~ 1.0)
     * @return 实际主目标直接伤害
     */
    public static float calcEarthPileHammerDirectDamage(float attackDamage, float cooldownScale) {
        if (cooldownScale >= 0.8f) {
            return attackDamage;
        } else {
            return attackDamage * 0.65f;
        }
    }

    /**
     * 计算【地脉定桩锤】定桩重击震荡波单体物理伤害。
     *
     * @param attackDamageSnapshot 定桩重击攻击力快照 (100% 攻击力)
     * @return 震荡波造成的物理伤害 (35%)
     */
    public static float calcEarthPileShockwaveDamage(float attackDamageSnapshot) {
        return attackDamageSnapshot * 0.35f;
    }
}
