package me.tuanzi.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void tuanzis_mod$onExtractRenderState(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        boolean hasTag = entity.entityTags().contains("blueprint_ghost");
        if (hasTag) {
            String entityTypeName = entity.getType().getDescription().getString();
            String entityCustomName = entity.hasCustomName() ? entity.getCustomName().getString() : "";
            me.tuanzi.util.ModLog.debug("EntityRendererMixin extractRenderState: entity=" + entityTypeName + ", Name=" + entityCustomName + ", ID=" + entity.getId() + ", hasTag=true");
        }
        ((me.tuanzi.util.BlueprintGhostHolder) state).tuanzis_mod$setBlueprintGhost(hasTag);
    }
}
