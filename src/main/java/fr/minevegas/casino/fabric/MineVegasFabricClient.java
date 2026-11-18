package fr.minevegas.casino.fabric;

import fr.minevegas.casino.CasinoBetRules;
import fr.minevegas.casino.CasinoSettings;
import fr.minevegas.casino.CasinoSettingsClient;
import fr.minevegas.casino.DerbyLobby;
import fr.minevegas.casino.GuideSceneRenderer;
import fr.minevegas.casino.MineVegas;
import fr.minevegas.casino.MineVegasClient;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class MineVegasFabricClient implements ClientModInitializer {
   public void onInitializeClient() {
      UseItemCallback.EVENT.register(MineVegasClient::onUseItem);
      MineVegasClient.registerScreens(MineVegasFabricClient::screen);
      PictureInPictureRendererRegistry.register((context) -> new GuideSceneRenderer());
      MineVegasClient.registerBlockEntityRenderers(BlockEntityRenderers::register);
      MineVegasClient.registerEntityRenderers(EntityRenderers::register);
      ClientTickEvents.END_CLIENT_TICK.register(MineVegasClient::onClientTick);
      CasinoSettingsClient.init();
      ClientPlayNetworking.registerGlobalReceiver(CasinoSettings.Payload.TYPE, (rules, context) -> CasinoSettings.receive(rules));
      ClientPlayNetworking.registerGlobalReceiver(DerbyLobby.Payload.TYPE, (data, context) -> DerbyLobby.receive(data));
      ClientPlayNetworking.registerGlobalReceiver(CasinoBetRules.RulesPayload.TYPE, (rules, context) -> MineVegas.onBetRules(rules));
      ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayConnectionEvents.Disconnect)(listener, client) -> MineVegasClient.onDisconnect());
   }

   private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void screen(MenuType<? extends M> type, MineVegasClient.ScreenFactory<M, U> factory) {
      Objects.requireNonNull(factory);
      MenuScreens.register(type, factory::create);
   }
}
