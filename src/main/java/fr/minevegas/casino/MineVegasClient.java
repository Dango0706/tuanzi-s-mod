package fr.minevegas.casino;

import fr.minevegas.casinodeco.CasinoDecoClient;
import fr.minevegas.casinosound.client.CasinoSoundClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class MineVegasClient {
   private MineVegasClient() {
   }

   public static void registerScreens(ScreenRegistrar screens) {
      RouletteClient.registerScreens(screens);
   }

   public static void registerBlockEntityRenderers(BlockEntityRendererRegistrar renderers) {
      RouletteClient.registerBlockEntityRenderers(renderers);
   }

   public static void registerEntityRenderers(EntityRendererRegistrar renderers) {
      RouletteClient.registerEntityRenderers(renderers);
      CasinoDecoClient.registerEntityRenderers(renderers);
   }

   public static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
      return RouletteClient.onUseItem(player, level, hand);
   }

   public static void onClientTick(Minecraft minecraft) {
      CasinoSoundClient.tick(minecraft);
      EasyNpcDealerRenderer.onClientLevel(minecraft.level);
   }

   public static void onDisconnect() {
      CasinoBetRules.forget();
      DerbyLobby.clear();
      EasyNpcDealerRenderer.onClientLevel((Level)null);
   }

   public interface BlockEntityRendererRegistrar {
      <T extends BlockEntity, S extends BlockEntityRenderState> void register(BlockEntityType<? extends T> var1, BlockEntityRendererProvider<T, S> var2);
   }

   public interface EntityRendererRegistrar {
      <T extends Entity> void register(EntityType<? extends T> var1, EntityRendererProvider<T> var2);
   }

   @FunctionalInterface
   public interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
      U create(M var1, Inventory var2, Component var3);
   }

   public interface ScreenRegistrar {
      <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> var1, ScreenFactory<M, U> var2);
   }
}
