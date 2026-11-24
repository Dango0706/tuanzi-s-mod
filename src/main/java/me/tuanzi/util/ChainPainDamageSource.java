package me.tuanzi.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class ChainPainDamageSource extends DamageSource {
    public ChainPainDamageSource(Holder<DamageType> type, @Nullable Entity attacker) {
        super(type, attacker);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String deathMsg = "death.attack.tuanzis_mod.chain_pain";
        return Component.translatable(deathMsg, victim.getDisplayName());
    }
}
