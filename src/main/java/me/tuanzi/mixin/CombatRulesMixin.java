package me.tuanzi.mixin;

import me.tuanzi.util.DamageCalculator;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {

    @ModifyVariable(
            method = "getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private static float tuanzis_mod$modifyArmorValue(
            float totalArmor,
            LivingEntity victim,
            float damage,
            DamageSource source,
            float totalArmorArg,
            float armorToughness
    ) {
        return DamageCalculator.calculateArmorIgnored(totalArmor, source, victim);
    }
}
