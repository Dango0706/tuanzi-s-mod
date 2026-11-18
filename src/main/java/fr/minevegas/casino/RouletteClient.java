package fr.minevegas.casino;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class RouletteClient {
   private RouletteClient() {
   }

   static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
      if (level.isClientSide() && player.getItemInHand(hand).is(RouletteMod.CASINO_GUIDE)) {
         Minecraft.getInstance().setScreenAndShow(new CasinoGuideScreen());
         return InteractionResult.SUCCESS;
      } else if (level.isClientSide() && player.isCreative() && player.getItemInHand(hand).is(CroupierHandItem.ITEM)) {
         if (!player.isShiftKeyDown()) {
            CasinoSettingsClient.request("world", 0L);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   static void registerScreens(MineVegasClient.ScreenRegistrar screens) {
      screens.register(RouletteMod.MENU, RouletteScreen::new);
      screens.register(RouletteMod.BLACKJACK_MENU, BlackjackScreen::new);
      screens.register(RouletteMod.POKER_MENU, PokerScreen::new);
      screens.register(RouletteMod.DERBY_MENU, DuckDerbyScreen::new);
      screens.register(RouletteMod.SLOT_MENU, SlotMachineScreen::new);
      screens.register(RouletteMod.WHEEL_MENU, FortuneWheelScreen::new);
      screens.register(RouletteMod.PLINKO_MENU, PlinkoScreen::new);
   }

   static void registerBlockEntityRenderers(MineVegasClient.BlockEntityRendererRegistrar renderers) {
      renderers.register(RouletteMod.TABLE_ENTITY, RouletteBetRenderer::new);
      renderers.register(RouletteMod.BLACKJACK_ENTITY, BlackjackTableRenderer::new);
      renderers.register(RouletteMod.POKER_ENTITY, PokerTableRenderer::new);
      renderers.register(RouletteMod.DERBY_ENTITY, DuckDerbyRenderer::new);
      renderers.register(RouletteMod.SLOT_ENTITY, SlotMachineRenderer::new);
      renderers.register(RouletteMod.WHEEL_ENTITY, FortuneWheelRenderer::new);
      renderers.register(RouletteMod.PLINKO_ENTITY, PlinkoRenderer::new);
   }

   static void registerEntityRenderers(MineVegasClient.EntityRendererRegistrar renderers) {
      renderers.register(RouletteMod.SEAT_ENTITY, NoopRenderer::new);
   }
}
