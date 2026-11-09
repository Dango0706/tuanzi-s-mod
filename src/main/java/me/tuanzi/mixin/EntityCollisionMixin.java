package me.tuanzi.mixin;

import me.tuanzi.world.scavenger.team.ScavengerTeamCollisionHelper;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$cancelTeammatePush(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (ScavengerTeamCollisionHelper.areTeammates(self, entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$cancelTeammateCollide(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (ScavengerTeamCollisionHelper.areTeammates(self, entity)) {
            cir.setReturnValue(false);
        }
    }
}
