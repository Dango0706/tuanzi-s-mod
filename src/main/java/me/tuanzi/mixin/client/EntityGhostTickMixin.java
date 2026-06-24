package me.tuanzi.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityGhostTickMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.entityTags().contains("blueprint_ghost")) {
            ci.cancel();
        }
    }
}
