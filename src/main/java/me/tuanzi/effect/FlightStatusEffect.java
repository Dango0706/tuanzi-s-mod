package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FlightStatusEffect extends MobEffect {
    public FlightStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x87CEEB); // 天蓝色
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }
}
