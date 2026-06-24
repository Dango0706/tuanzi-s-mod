package me.tuanzi.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.tuanzi.client.renderer.GhostRenderRegistry;
import me.tuanzi.client.renderer.TranslucentVertexConsumer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererMixin {

    @Shadow
    protected abstract VertexConsumer getVertexBuilder(RenderType renderType);

    private static final ThreadLocal<Boolean> tuanzis_mod$isRedirecting = ThreadLocal.withInitial(() -> false);

    @Inject(
        method = "getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$onGetVertexBuilder(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        if (tuanzis_mod$isRedirecting.get()) {
            return;
        }

        GhostRenderRegistry.GhostRenderInfo info = GhostRenderRegistry.getInfo(renderType);
        if (info != null) {
            tuanzis_mod$isRedirecting.set(true);
            try {
                VertexConsumer rawBuffer = this.getVertexBuilder(info.original);
                cir.setReturnValue(new TranslucentVertexConsumer(rawBuffer, info.alpha));
            } finally {
                tuanzis_mod$isRedirecting.set(false);
            }
        }
    }
}
