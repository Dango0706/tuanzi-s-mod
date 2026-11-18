package fr.minevegas.casino.mixin;

import fr.minevegas.casino.RouletteMod;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MerchantOffer.class})
public abstract class CroupierOfferXpMixin {
   @Inject(
      method = {"getXp"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void roulettecasino$upgradeSavedOffer(CallbackInfoReturnable<Integer> result) {
      MerchantOffer offer = (MerchantOffer)(Object)this;
      if ((Integer)result.getReturnValue() < 2 && offer.getResult().is(RouletteMod.HIRING_CARD)) {
         result.setReturnValue(2);
      }

   }
}

