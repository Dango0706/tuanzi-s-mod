package me.tuanzi.mixin;

import me.tuanzi.world.scavenger.team.ScavengerTeamCollisionHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {

    @Inject(method = "pushableBy", at = @At("RETURN"), cancellable = true)
    private static void tuanzis_mod$ignoreTeammatePushableBy(Entity entity, CallbackInfoReturnable<Predicate<Entity>> cir) {
        Predicate<Entity> original = cir.getReturnValue();
        cir.setReturnValue(input -> {
            if (ScavengerTeamCollisionHelper.areTeammates(entity, input)) {
                return false;
            }
            return original.test(input);
        });
    }
}
