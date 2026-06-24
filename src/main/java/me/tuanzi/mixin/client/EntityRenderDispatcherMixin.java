package me.tuanzi.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.client.renderer.GhostSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Redirect(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"
        )
    )
    private void tuanzis_mod$redirectSubmit(
        EntityRenderer renderer,
        EntityRenderState renderState,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        SubmitNodeCollector collectorToUse = submitNodeCollector;
        boolean isGhost = ((me.tuanzi.util.BlueprintGhostHolder) renderState).tuanzis_mod$isBlueprintGhost();
        if (isGhost) {
            collectorToUse = new GhostSubmitNodeCollector(submitNodeCollector, 0.4F);
            me.tuanzi.util.ModLog.debug("EntityRenderDispatcherMixin redirectSubmit: isGhost=true, wrapping renderer=" + renderer.getClass().getName() + ", state=" + renderState.getClass().getName());
        }
        renderer.submit(renderState, poseStack, collectorToUse, camera);
    }
}
