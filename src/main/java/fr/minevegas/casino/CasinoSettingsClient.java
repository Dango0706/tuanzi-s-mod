package fr.minevegas.casino;

import fr.minevegas.casino.platform.CasinoClientPlatform;
import fr.minevegas.casino.platform.Services;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CasinoSettingsClient {
   private static final CasinoClientPlatform PLATFORM = (CasinoClientPlatform)Services.load(CasinoClientPlatform.class);
   private static CasinoSettings.View current;

   private CasinoSettingsClient() {
   }

   public static void init() {
      CasinoSettings.viewReceiver = CasinoSettingsClient::receive;
   }

   public static CasinoSettings.View view() {
      return current;
   }

   public static void request(String target, long position, String dimension) {
      if (CasinoSettings.allowed(Minecraft.getInstance().player)) {
         PLATFORM.sendToServer(new CasinoSettings.EditPayload(target, position, "open", "", "", 0, false, dimension));
      }
   }

   public static void request(String target, long position) {
      request(target, position, "");
   }

   public static void send(String action, String item, String result, int value, boolean flag) {
      if (current != null && valid()) {
         PLATFORM.sendToServer(new CasinoSettings.EditPayload(current.target(), current.position(), action, item, result, value, flag, current.dimension()));
      }
   }

   public static void sendType(String type, String action, int value, boolean flag) {
      if (current != null && valid()) {
         PLATFORM.sendToServer(new CasinoSettings.EditPayload(type, 0L, action, "", "", value, flag));
      }
   }

   public static void sendBet(int action, Identifier item) {
      if (current != null && valid()) {
         PLATFORM.sendToServer(new CasinoBetRules.EditPayload(action, item));
      }
   }

   private static void receive(CasinoSettings.View view) {
      Minecraft minecraft = Minecraft.getInstance();
      if (CasinoSettings.allowed(minecraft.player)) {
         Screen var4 = minecraft.gui.screen();
         CasinoSettingsScreen var10000;
         if (var4 instanceof CasinoSettingsScreen) {
            CasinoSettingsScreen shown = (CasinoSettingsScreen)var4;
            var10000 = shown;
         } else {
            var10000 = null;
         }

         CasinoSettingsScreen screen = var10000;
         if (view.machines() == null) {
            view = view.withMachines(current != null ? current.machines() : List.of());
         }

         if (!view.open()) {
            if (screen != null && current != null) {
               if (current.sameTarget(view)) {
                  current = view;
                  screen.refresh(view);
               } else {
                  current = current.withMachines(view.machines());
                  screen.machines(current);
               }

            }
         } else {
            current = view;
            if (screen != null) {
               screen.show(view);
            } else {
               minecraft.setScreenAndShow(new CasinoSettingsScreen(view));
            }

         }
      }
   }

   public static boolean hasCopy() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return false;
      } else {
         ItemStack hand = player.getMainHandItem().is(CroupierHandItem.ITEM) ? player.getMainHandItem() : player.getOffhandItem();
         return ((CustomData)hand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().contains("MineVegasSettings");
      }
   }

   public static boolean valid() {
      Minecraft minecraft = Minecraft.getInstance();
      return current != null && CasinoSettings.allowed(minecraft.player) && minecraft.level != null;
   }

   public static void close() {
      current = null;
   }
}
