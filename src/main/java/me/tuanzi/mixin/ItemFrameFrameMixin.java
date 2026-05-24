package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public class ItemFrameFrameMixin {
    
    @Inject(
        method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$preventDummyInFrame(ItemStack stack, boolean update, CallbackInfo ci) {
        if (stack.is(ModItems.TRIAL_DUMMY)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "setItem(Lnet/minecraft/world/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$preventDummyInFrameSimple(ItemStack stack, CallbackInfo ci) {
        if (stack.is(ModItems.TRIAL_DUMMY)) {
            ci.cancel();
        }
    }
}
