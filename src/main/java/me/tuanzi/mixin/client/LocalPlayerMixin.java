package me.tuanzi.mixin.client;

import me.tuanzi.util.ModLog;
import me.tuanzi.util.RangerReloadHelper;
import me.tuanzi.util.RangerReloadTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @org.spongepowered.asm.mixin.Unique
    private boolean tuanzis_mod$canDoubleJump = false;
    @org.spongepowered.asm.mixin.Unique
    private boolean tuanzis_mod$lastJumpKeyDown = false;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void tuanzis_mod$checkDoubleJump(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        boolean jumpKeyDown = player.input != null && player.input.keyPresses.jump();
        boolean jumpJustPressed = jumpKeyDown && !this.tuanzis_mod$lastJumpKeyDown;
        this.tuanzis_mod$lastJumpKeyDown = jumpKeyDown;

        // 如果玩家着地、处于液体中或攀爬，重置二段跳状态
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            this.tuanzis_mod$canDoubleJump = true;
        } else if (jumpJustPressed && this.tuanzis_mod$canDoubleJump) {
            // 在空中且上升沿触发跳跃键
            if (!player.isFallFlying() && !player.getAbilities().flying && player.getVehicle() == null) {
                boolean hasEnchant = me.tuanzi.util.DoubleJumpHandler.hasDoubleJumpEnchantment(player);
                ModLog.debug("Air jump triggered! hasEnchant=" + hasEnchant);
                if (hasEnchant) {
                    this.tuanzis_mod$canDoubleJump = false;
                    me.tuanzi.util.DoubleJumpHandler.performClientDoubleJump(player);
                }
            }
        }
    }

    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$isSlowDueToUsingItem(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && RangerReloadHelper.hasRangerReload(player.level(), player.getUseItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$itemUseSpeedMultiplier(CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && RangerReloadHelper.hasRangerReload(player.level(), player.getUseItem())) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "shouldStopRunSprinting", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$shouldStopRunSprinting(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player instanceof RangerReloadTracker tracker && tracker.tuanzis_mod$hasRangerReloadWindow()) {
            if (player.input != null && player.input.hasForwardImpulse() && (!player.horizontalCollision || player.minorHorizontalCollision)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void tuanzis_mod$onAiStep(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof CrossbowItem) {
            int timeHeld = player.getTicksUsingItem();
            int chargeDuration = CrossbowItem.getChargeDuration(player.getUseItem(), player);
            if (timeHeld >= chargeDuration && RangerReloadHelper.hasRangerReload(player.level(), player.getUseItem())) {
                if (player instanceof RangerReloadTracker tracker && !tracker.tuanzis_mod$hasRangerReloadWindow()) {
                    tracker.tuanzis_mod$setRangerReloadWindowTicks(50);
                    if (player.input != null && player.input.hasForwardImpulse()) {
                        player.setSprinting(true);
                    }
                }
            }
        }
    }
}
