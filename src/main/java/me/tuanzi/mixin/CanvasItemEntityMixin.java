package me.tuanzi.mixin;

import me.tuanzi.util.ModLog;
import me.tuanzi.world.canvas.CanvasItemProtectionHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 保护画布掉落物实体免疫火焰、岩浆、仙人掌、爆炸等一切伤害（仅保留原版 5 分钟自然消失）。
 */
@Mixin(ItemEntity.class)
public abstract class CanvasItemEntityMixin {

    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$canvasFireImmune(CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (CanvasItemProtectionHandler.isProtectedCanvas(self.getItem())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hurtClient", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$canvasHurtClientImmune(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (CanvasItemProtectionHandler.shouldIgnoreDamage(self.getItem(), source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$canvasHurtServerImmune(
            ServerLevel level,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (CanvasItemProtectionHandler.shouldIgnoreDamage(self.getItem(), source)) {
            ModLog.debug("【画布保护】免疫掉落物伤害: source=" + source.getMsgId());
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$canvasIgnoreExplosion(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (CanvasItemProtectionHandler.isProtectedCanvas(self.getItem())) {
            cir.setReturnValue(true);
        }
    }
}
