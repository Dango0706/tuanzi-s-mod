package me.tuanzi.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「背水一战」创伤结算真实伤害源：
 * 无视护甲、抗性提升、保护附魔、盾牌格挡与受击无敌帧（与玩家吸收心在 PlayerActuallyHurtMixin 配合绕过），
 * 同时保留原版不死图腾与「不死」状态效果的免死判定。
 */
public class DesperateStandDamageSource extends DamageSource {
    public DesperateStandDamageSource(Holder<DamageType> type) {
        super(type);
    }

    @Override
    public boolean is(TagKey<DamageType> tag) {
        if (tag == DamageTypeTags.BYPASSES_ARMOR
                || tag == DamageTypeTags.BYPASSES_EFFECTS
                || tag == DamageTypeTags.BYPASSES_RESISTANCE
                || tag == DamageTypeTags.BYPASSES_ENCHANTMENTS
                || tag == DamageTypeTags.BYPASSES_COOLDOWN
                || tag == DamageTypeTags.BYPASSES_SHIELD
                || tag == DamageTypeTags.NO_KNOCKBACK) {
            return true;
        }
        return super.is(tag);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        LivingEntity killer = victim.getKillCredit();
        if (killer != null) {
            return Component.translatable("death.attack.tuanzis_mod.desperate_stand.player",
                    victim.getDisplayName(), killer.getDisplayName());
        }
        return Component.translatable("death.attack.tuanzis_mod.desperate_stand", victim.getDisplayName());
    }
}
