package me.tuanzi.mixin;

import me.tuanzi.init.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void tuanzis_mod$increaseUndyingPotionMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemInstance self = (ItemInstance) (Object) this;
        if (self.is(Items.POTION) || self.is(Items.SPLASH_POTION) || self.is(Items.LINGERING_POTION)) {
            PotionContents contents = self.get(DataComponents.POTION_CONTENTS);
            if (contents != null && (contents.is(ModPotions.UNDYING_POTION) || contents.is(ModPotions.LONG_UNDYING_POTION))) {
                if (cir.getReturnValueI() < 16) {
                    cir.setReturnValue(16);
                }
            }
        }
    }
}
