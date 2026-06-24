package me.tuanzi.mixin.client;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.BlockDisplay.class)
public abstract class BlockDisplayMixin {
    @Shadow
    private Display.BlockDisplay.BlockRenderState blockRenderState;

    @Inject(method = "blockRenderState", at = @At("HEAD"))
    private void tuanzis_mod$onGetBlockRenderState(CallbackInfoReturnable<Display.BlockDisplay.BlockRenderState> cir) {
        Display.BlockDisplay blockDisplay = (Display.BlockDisplay) (Object) this;
        net.minecraft.world.level.block.state.BlockState blockState = blockDisplay.getBlockState();
        if (this.blockRenderState == null || this.blockRenderState.blockState() != blockState) {
            this.blockRenderState = new Display.BlockDisplay.BlockRenderState(blockState);
            me.tuanzi.util.ModLog.debug("BlockDisplayMixin: Updated/Lazy initialized blockRenderState for block: " + blockState);
        }
    }
}
