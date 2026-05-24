package me.tuanzi.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public class BloodRageDamageSource extends DamageSource {
    public BloodRageDamageSource(Holder<DamageType> type) {
        super(type);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String deathMsg = "death.attack.tuanzis_mod.blood_rage_feedback";
        return Component.translatable(deathMsg, victim.getDisplayName());
    }
}
