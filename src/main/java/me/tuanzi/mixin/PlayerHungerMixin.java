package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerHungerMixin {

    @ModifyVariable(
        method = "causeFoodExhaustion(F)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private float tuanzis_mod$modifyExhaustion(float exhaustion) {
        Player player = (Player) (Object) this;
        if (player.hasEffect(ModStatusEffects.BLAZING_HUNGER)) {
            return exhaustion * 2.0f;
        }
        return exhaustion;
    }
}
