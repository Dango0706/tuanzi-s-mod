package me.tuanzi.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public class ExecuteBacklashDamageSource extends DamageSource {
    public ExecuteBacklashDamageSource(Holder<DamageType> type) {
        super(type);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String deathMsg = "death.attack.tuanzis_mod.execute_backlash";
        return Component.translatable(deathMsg, victim.getDisplayName());
    }
}
