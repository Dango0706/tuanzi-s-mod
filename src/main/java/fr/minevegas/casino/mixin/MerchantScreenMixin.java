package fr.minevegas.casino.mixin;

import fr.minevegas.casino.RouletteMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MerchantScreen.class})
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
   private static final Identifier CROUPIER_GUI = RouletteMod.id("textures/gui/croupier_trade.png");

   protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
   }

   private boolean roulettecasino$isCroupier() {
      ComponentContents var2 = this.getTitle().getContents();
      if (var2 instanceof TranslatableContents translation) {
         if (translation.getKey().equals("entity.roulettecasino.villager.croupier")) {
            return true;
         }
      }

      return ((MerchantMenu)this.menu).getOffers().stream().anyMatch((offer) -> offer.getResult().is(RouletteMod.HIRING_CARD));
   }

   @ModifyArg(
      method = {"extractBackground"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
)},
      index = 1
   )
   private Identifier roulettecasino$customBackground(Identifier original) {
      return this.roulettecasino$isCroupier() ? CROUPIER_GUI : original;
   }

   @Inject(
      method = {"extractScroller"},
      at = {@At("TAIL")}
   )
   private void roulettecasino$customScroller(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY, MerchantOffers offers, CallbackInfo callback) {
      if (this.roulettecasino$isCroupier() && offers.size() <= 7) {
         graphics.fill(left + 94, top + 18, left + 100, top + 45, -15325409);
         graphics.fill(left + 95, top + 19, left + 99, top + 44, -6460111);
         graphics.fill(left + 96, top + 20, left + 98, top + 43, -14983099);
      }
   }

   @Inject(
      method = {"extractLabels"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void roulettecasino$customLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo callback) {
      if (this.roulettecasino$isCroupier()) {
         int level = ((MerchantMenu)this.menu).getTraderLevel();
         Component heading = (Component)(level > 0 && level <= 5 && ((MerchantMenu)this.menu).showProgressBar() ? Component.translatable("merchant.title", this.getTitle(), Component.translatable("merchant.level." + level)) : this.getTitle());
         int gold = -10635;
         int cream = -462894;
         graphics.text(this.font, (Component)heading, 187 - this.font.width((FormattedText)heading) / 2, 6, gold, true);
         Component trades = Component.translatable("merchant.trades");
         graphics.text(this.font, (Component)trades, 48 - this.font.width((FormattedText)trades) / 2, 6, gold, true);
         graphics.text(this.font, (Component)this.playerInventoryTitle, this.inventoryLabelX, 69, cream, true);
         callback.cancel();
      }
   }
}
