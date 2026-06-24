package me.tuanzi.mixin.client;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.ItemDisplay.class)
public abstract class ItemDisplayMixin {
    @Shadow
    private Display.ItemDisplay.ItemRenderState itemRenderState;

    @Inject(method = "itemRenderState", at = @At("HEAD"))
    private void tuanzis_mod$onGetItemRenderState(CallbackInfoReturnable<Display.ItemDisplay.ItemRenderState> cir) {
        Display.ItemDisplay itemDisplay = (Display.ItemDisplay) (Object) this;
        net.minecraft.world.item.ItemStack stack = itemDisplay.getItemStack();
        net.minecraft.world.item.ItemDisplayContext transform = itemDisplay.getItemTransform();
        if (this.itemRenderState == null || this.itemRenderState.itemStack() != stack || this.itemRenderState.itemTransform() != transform) {
            this.itemRenderState = new Display.ItemDisplay.ItemRenderState(stack, transform);
            me.tuanzi.util.ModLog.debug("ItemDisplayMixin: Updated/Lazy initialized itemRenderState for item: " + stack);
        }
    }
}
