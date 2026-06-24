package me.tuanzi.mixin.client;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.class)
public abstract class DisplayMixin {
    @Shadow
    private Display.RenderState renderState;

    @Shadow
    protected boolean updateRenderState;

    @Shadow
    protected abstract Display.RenderState createFreshRenderState();

    @Shadow
    protected abstract void updateRenderSubState(boolean shouldInterpolate, float progress);

    @Inject(method = "renderState", at = @At("HEAD"))
    private void tuanzis_mod$onGetRenderState(CallbackInfoReturnable<Display.RenderState> cir) {
        if (this.renderState == null || this.updateRenderState) {
            this.updateRenderState = false;
            String details = "";
            if ((Object) this instanceof Display.BlockDisplay blockDisplay) {
                details = "Block: " + blockDisplay.getBlockState().toString();
            } else if ((Object) this instanceof Display.ItemDisplay itemDisplay) {
                details = "Item: " + itemDisplay.getItemStack().toString();
            } else if ((Object) this instanceof Display.TextDisplay textDisplay) {
                details = "Text: " + textDisplay.getText().getString();
            }
            me.tuanzi.util.ModLog.debug("DisplayMixin: renderState is null or needs update! Lazy initializing/updating for display subclass: " + this.getClass().getName() + ", ID: " + ((Display)(Object)this).getId() + " [" + details + "]");
            
            this.renderState = this.createFreshRenderState();
            this.updateRenderSubState(false, 1.0F);
            me.tuanzi.util.ModLog.debug("DisplayMixin: Lazy initialized/updated! renderState=" + this.renderState);
        }
    }
}
