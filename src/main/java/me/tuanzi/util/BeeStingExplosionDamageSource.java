package me.tuanzi.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class BeeStingExplosionDamageSource extends DamageSource {
    public BeeStingExplosionDamageSource(Holder<DamageType> type, Entity attacker) {
        super(type, attacker, attacker);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String deathMsg = "death.attack.tuanzis_mod.bee_sting_explosion";
        Entity attacker = this.getEntity();
        if (attacker != null) {
            return Component.translatable(deathMsg + ".player", victim.getDisplayName(), attacker.getDisplayName());
        }
        return Component.translatable(deathMsg, victim.getDisplayName());
    }
}
