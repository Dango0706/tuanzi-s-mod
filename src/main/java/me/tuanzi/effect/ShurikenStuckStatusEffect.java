package me.tuanzi.effect;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ShurikenStuckStatusEffect extends MobEffect {
    public ShurikenStuckStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A6268); // 金属深灰色
        // 每一级提供 -10% 的移速。在 26.1 里，属性修改器的值在应用时会自动乘以 (amplifier + 1)
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.shuriken_stuck.movement_speed"), 
            -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
