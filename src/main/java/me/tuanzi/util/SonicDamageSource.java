package me.tuanzi.util;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class SonicDamageSource extends DamageSource {
    public SonicDamageSource(Holder<DamageType> type, @Nullable Entity attacker) {
        super(type, attacker);
    }
}
