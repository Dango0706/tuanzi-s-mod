package me.tuanzi.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class InfinityMendingMixin {

    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void tuanzis_mod$allowInfinityMending(Holder<Enchantment> first, Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if ((first.is(Enchantments.INFINITY) && second.is(Enchantments.MENDING)) ||
            (first.is(Enchantments.MENDING) && second.is(Enchantments.INFINITY))) {
            cir.setReturnValue(true);
        }
    }
}
