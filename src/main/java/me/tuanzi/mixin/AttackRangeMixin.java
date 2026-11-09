package me.tuanzi.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttackRange.class)
public class AttackRangeMixin {

    @Inject(method = "effectiveMaxRange", at = @At("RETURN"), cancellable = true)
    private void tuanzis_mod$applyAttributeReachBonus(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof LivingEntity livingEntity) {
            double currentReach = livingEntity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            double bonus = currentReach - Player.DEFAULT_ENTITY_INTERACTION_RANGE;
            if (bonus > 1e-4) {
                cir.setReturnValue((float) (cir.getReturnValueF() + bonus));
            }
        }
    }
}
