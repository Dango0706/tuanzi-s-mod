package fr.minevegas.casino.platform;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public interface CasinoPlatform {
   String loaderName();

   <T extends AbstractContainerMenu> MenuType<T> menuType(MenuFactory<T> var1);

   PoiType registerPoi(Identifier var1, int var2, int var3, Block var4);

   CreativeModeTab creativeTab(Component var1, Supplier<ItemStack> var2, Consumer<Consumer<ItemLike>> var3);

   void sendToPlayer(ServerPlayer var1, CustomPacketPayload var2);

   void flammable(Block var1, int var2, int var3);

   @FunctionalInterface
   public interface MenuFactory<T extends AbstractContainerMenu> {
      T create(int var1, Inventory var2);
   }
}
