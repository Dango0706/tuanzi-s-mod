package fr.minevegas.casino.mixin;

import fr.minevegas.casino.CroupierOffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Villager.class})
public abstract class CroupierTradesMixin {
   @Inject(
      method = {"updateTrades"},
      at = {@At("RETURN")}
   )
   private void roulettecasino$keepOfferAfterUpdate(ServerLevel level, CallbackInfo callback) {
      CroupierOffer.keep((Villager)(Object)this);
   }

   @Inject(
      method = {"mobInteract"},
      at = {@At("HEAD")}
   )
   private void roulettecasino$keepOfferOnClick(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
      CroupierOffer.keep((Villager)(Object)this);
   }

   @Inject(
      method = {"startTrading"},
      at = {@At("HEAD")}
   )
   private void roulettecasino$keepOfferOnTrade(Player player, CallbackInfo callback) {
      CroupierOffer.keep((Villager)(Object)this);
   }
}

