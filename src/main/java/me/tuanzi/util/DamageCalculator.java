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
        float attackerMultiplier = 1.0f;
        float victimMultiplier = 1.0f;

        // 1. 攻击者输出伤害乘数计算（加法累加模型）
        if (source.getEntity() instanceof LivingEntity attacker) {
            // 攻击者拥有“血怒”状态效果，攻击伤害乘算提升 40% (累加值 +0.4f)
            if (attacker.hasEffect(ModStatusEffects.BLOOD_RAGE)) {
                attackerMultiplier += 0.4f;
            }
            
            // 攻击者穿戴狂战士附魔胸甲，近战伤害随已损失生命值比例获得提升 (I级每损失10%增伤5%，II级增伤8%)
            boolean isMelee = source.is(DamageTypes.PLAYER_ATTACK) 
                || source.is(DamageTypes.MOB_ATTACK) 
                || source.is(DamageTypes.MACE_SMASH);
            if (isMelee) {
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
                        attackerMultiplier += stepCount * percentPerStep;
                    }
                }
            }
            
            // 此处可预留并累加未来新增的攻击者增伤/降伤效果，如：
            // if (attacker.hasEffect(ModStatusEffects.SOME_OTHER_BUFF)) {
            //     attackerMultiplier += 0.2f;
            // }
        }

        // 2. 受击者受伤加深乘数计算（加法累加模型）
        // 被攻击者拥有“血怒”状态效果，且伤害非血怒反馈伤害，所受伤害乘算提升 30% (累加值 +0.3f)
        if (target.hasEffect(ModStatusEffects.BLOOD_RAGE) && !(source instanceof BloodRageDamageSource)) {
            victimMultiplier += 0.3f;
        }

        // 此处可预留并累加未来新增的受击者易伤/免伤效果，如：
        // if (target.hasEffect(ModStatusEffects.SOME_DEFENSIVE_BUFF)) {
        //     victimMultiplier -= 0.2f;
        // }

        return amount * attackerMultiplier * victimMultiplier;
    }
}
