package fr.minevegas.casino;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class CasinoJeiPlugin implements IModPlugin {
   private static final Identifier UID = Identifier.fromNamespaceAndPath("roulettecasino", "jei");

   public Identifier getPluginUid() {
      return UID;
   }

   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addGenericGuiScreenHandler(CasinoBetScreen.class, new WholeScreen());
   }

   private static final class WholeScreen implements IScreenHandler<CasinoBetScreen<?>> {
      public IGuiProperties apply(CasinoBetScreen<?> screen) {
         return new Properties(screen.getClass(), Math.max(1, screen.width), Math.max(1, screen.height));
      }
   }

   private static record Properties(Class<? extends Screen> screenClass, int screenWidth, int screenHeight) implements IGuiProperties {
      public int guiLeft() {
         return 0;
      }

      public int guiTop() {
         return 0;
      }

      public int guiXSize() {
         return this.screenWidth;
      }

      public int guiYSize() {
         return this.screenHeight;
      }
   }
}
