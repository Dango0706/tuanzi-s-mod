package me.tuanzi.mixin;

import me.tuanzi.util.ExperiencedEnchantmentHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Shadow
    private int repairPlayerItems(ServerPlayer player, int amount) {
        throw new AssertionError();
    }

    @Redirect(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I"
        )
    )
    private int tuanzis_mod$boostExperienceOnTouch(ExperienceOrb instance, ServerPlayer player, int amount) {
        int boostedAmount = ExperiencedEnchantmentHelper.calculateExperience(player, amount);
        return this.repairPlayerItems(player, boostedAmount);
    }
}
