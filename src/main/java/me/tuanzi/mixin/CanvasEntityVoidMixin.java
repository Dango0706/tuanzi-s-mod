package me.tuanzi.mixin;

import me.tuanzi.world.canvas.CanvasItemProtectionHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截画布掉落物实体掉入虚空，自动救援传送回画布主人身边或加入离线返还队列。
 */
@Mixin(Entity.class)
public abstract class CanvasEntityVoidMixin {

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$rescueCanvasFromVoid(CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity itemEntity
                && !itemEntity.level().isClientSide()
                && CanvasItemProtectionHandler.isProtectedCanvas(itemEntity.getItem())) {
            if (CanvasItemProtectionHandler.handleVoidRescue(itemEntity)) {
                ci.cancel();
            }
        }
    }
}
