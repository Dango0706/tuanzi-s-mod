package fr.minevegas.casino.mixin;

import fr.minevegas.casino.RouletteMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Button.Plain.class})
public abstract class TradeOfferButtonMixin {
   @Inject(
      method = {"extractContents"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void roulettecasino$croupierOffer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo callback) {
      AbstractWidget widget = (AbstractWidget)(Object)this;
      if (this instanceof TradeOfferButtonAccess button) {
         if (button.roulettecasino$index() >= 0 && button.roulettecasino$index() < 7) {
            if (widget.getWidth() == 88 && widget.getHeight() == 20) {
               Screen var9 = Minecraft.getInstance().gui.screen();
               if (!(var9 instanceof MerchantScreen)) {
                  return;
               }

               MerchantScreen screen;
               boolean var10000;
               label46: {
                  screen = (MerchantScreen)var9;
                  ComponentContents var11 = screen.getTitle().getContents();
                  if (var11 instanceof TranslatableContents) {
                     TranslatableContents name = (TranslatableContents)var11;
                     if (name.getKey().equals("entity.roulettecasino.villager.croupier")) {
                        var10000 = true;
                        break label46;
                     }
                  }

                  var10000 = false;
               }

               boolean croupier = var10000;
               if (!croupier) {
                  croupier = ((MerchantMenu)screen.getMenu()).getOffers().stream().anyMatch((offer) -> offer.getResult().is(RouletteMod.HIRING_CARD));
               }

               if (!croupier) {
                  return;
               }

               int x = widget.getX();
               int y = widget.getY();
               boolean highlighted = widget.isHoveredOrFocused();
               graphics.fill(x, y, x + 88, y + 20, -15325409);
               graphics.fill(x + 1, y + 1, x + 87, y + 19, highlighted ? -8580 : -6460111);
               graphics.fill(x + 2, y + 2, x + 86, y + 18, highlighted ? -14849975 : -15185353);
               graphics.fill(x + 3, y + 3, x + 85, y + 4, highlighted ? -10574227 : -12616104);
               graphics.fill(x + 3, y + 17, x + 85, y + 18, -15123668);
               callback.cancel();
               return;
            }

            return;
         }
      }

   }
}

