package me.tuanzi.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.client.renderer.GhostSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.class)
public abstract class DisplayRendererMixin {

    @Shadow
    protected abstract void submitInner(
        DisplayEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        float interpolationProgress
    );

    @Redirect(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/DisplayRenderer;submitInner(Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V"
        )
    )
    private void tuanzis_mod$redirectSubmitInner(
        DisplayRenderer instance,
        DisplayEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        float interpolationProgress
    ) {
        SubmitNodeCollector collectorToUse = submitNodeCollector;
        boolean isGhost = ((me.tuanzi.util.BlueprintGhostHolder) state).tuanzis_mod$isBlueprintGhost();
        me.tuanzi.util.ModLog.debug("DisplayRendererMixin redirectSubmitInner: isGhost=" + isGhost + ", state=" + state.getClass().getName());
        if (isGhost) {
            collectorToUse = new GhostSubmitNodeCollector(submitNodeCollector, 0.4F);
            me.tuanzi.util.ModLog.debug("DisplayRendererMixin redirectSubmitInner: Wrapped with GhostSubmitNodeCollector");
        }
        this.submitInner(state, poseStack, collectorToUse, lightCoords, interpolationProgress);
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Display;Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void tuanzis_mod$onExtractRenderState(Display entity, DisplayEntityRenderState state, float partialTicks, CallbackInfo ci) {
        boolean hasTag = entity.entityTags().contains("blueprint_ghost");
        String details = "";
        if (entity instanceof Display.BlockDisplay blockDisplay) {
            details = "Block: " + blockDisplay.getBlockState().toString();
        } else if (entity instanceof Display.ItemDisplay itemDisplay) {
            details = "Item: " + itemDisplay.getItemStack().toString();
        } else if (entity instanceof Display.TextDisplay textDisplay) {
            details = "Text: " + textDisplay.getText().getString();
        }
        me.tuanzi.util.ModLog.debug("DisplayRendererMixin extractRenderState: entity=" + entity.getClass().getName() + ", ID=" + entity.getId() + ", hasTag=" + hasTag + " [" + details + "]");
        me.tuanzi.util.ModLog.debug("DisplayRendererMixin extractRenderState: entity.renderState()=" + entity.renderState() + ", state.renderState=" + state.renderState);
        ((me.tuanzi.util.BlueprintGhostHolder) state).tuanzis_mod$setBlueprintGhost(hasTag);
    }
}
