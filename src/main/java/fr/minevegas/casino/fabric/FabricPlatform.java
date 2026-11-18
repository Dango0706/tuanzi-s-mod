package fr.minevegas.casino.fabric;

import fr.minevegas.casino.platform.CasinoPlatform;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class FabricPlatform implements CasinoPlatform {
   public String loaderName() {
      return "Fabric";
   }

   public <T extends AbstractContainerMenu> MenuType<T> menuType(CasinoPlatform.MenuFactory<T> factory) {
      Objects.requireNonNull(factory);
      return new MenuType<T>(factory::create, FeatureFlagSet.of());
   }

   public PoiType registerPoi(Identifier id, int maxTickets, int validRange, Block block) {
      return PoiHelper.register(id, maxTickets, validRange, block);
   }

   public CreativeModeTab creativeTab(Component title, Supplier<ItemStack> icon, Consumer<Consumer<ItemLike>> items) {
      return FabricCreativeModeTab.builder().title(title).icon(icon).displayItems((parameters, output) -> {
         Objects.requireNonNull(output);
         items.accept(output::accept);
      }).build();
   }

   public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
      ServerPlayNetworking.send(player, payload);
   }

   public void flammable(Block block, int igniteOdds, int burnOdds) {
      FlammableBlockRegistry.getDefaultInstance().add(block, igniteOdds, burnOdds);
   }
}
