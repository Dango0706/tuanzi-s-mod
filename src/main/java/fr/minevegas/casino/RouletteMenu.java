package fr.minevegas.casino;

import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RouletteMenu extends AbstractContainerMenu {
   public static final int SEVERAL = -2;
   private int dragColor;
   private Set<Slot> screenDrag;
   private final SimpleContainer bets;
   private final RouletteBlockEntity table;
   private final Inventory inventory;

   public RouletteMenu(int id, Inventory inventory) {
      this(id, inventory, (RouletteBlockEntity)null, new SimpleContainer(9));
   }

   public RouletteMenu(int id, Inventory inventory, RouletteBlockEntity table, SimpleContainer bets) {
      super(RouletteMod.MENU, id);
      this.dragColor = -1;
      this.table = table;
      this.inventory = inventory;
      this.bets = bets;

      for(int i = 0; i < 9; ++i) {
         int col = i % 3;
         int row = i / 3;
         this.addSlot(new Slot(bets, i, 48 + col * 56, 70 + row * 20) {
            {
               Objects.requireNonNull(RouletteMenu.this);
            }

            public boolean mayPlace(ItemStack stack) {
               return RouletteMenu.this.acceptsBet(stack) && RouletteMenu.open(this.container, this.getContainerSlot());
            }

            public boolean isHighlightable() {
               return this.hasItem() || RouletteMenu.open(this.container, this.getContainerSlot());
            }
         });
      }

      this.addStandardInventorySlots(inventory, 31, 176);
   }

   public RouletteBlockEntity table() {
      return this.table;
   }

   public static int stakedColor(Container bets) {
      int color = -1;

      for(int i = 0; i < 9; ++i) {
         if (!bets.getItem(i).isEmpty()) {
            if (color == -1) {
               color = i % 3;
            } else if (color != i % 3) {
               return -2;
            }
         }
      }

      return color;
   }

   public int stakedColor() {
      return stakedColor(this.bets);
   }

   private static boolean open(Container bets, int slot) {
      int color = stakedColor(bets);
      return color == -1 || color == slot % 3;
   }

   public void followDrag(Set<Slot> slots) {
      this.screenDrag = slots;
   }

   public boolean canDragTo(Slot slot) {
      if (this.screenDrag != null && slot.container == this.bets) {
         for(Slot s : this.screenDrag) {
            if (s.container == this.bets && s.getContainerSlot() % 3 != slot.getContainerSlot() % 3) {
               return false;
            }
         }
      }

      return super.canDragTo(slot);
   }

   public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
      if (this.stillValid(player) && CasinoWagerRouting.mayUse(this.table, this.bets, player)) {
         boolean wasEmpty = this.bets.isEmpty();
         if (input == ContainerInput.QUICK_CRAFT) {
            int header = getQuickcraftHeader(buttonNum);
            if (header == 0) {
               this.dragColor = -1;
            } else if (header == 1 && slotIndex >= 0 && slotIndex < 9 && open(this.bets, slotIndex)) {
               int color = slotIndex % 3;
               if (this.dragColor == -1) {
                  this.dragColor = color;
               } else if (color != this.dragColor) {
                  return;
               }
            }
         }

         super.clicked(slotIndex, buttonNum, input, player);
         CasinoWagerRouting.changed(this.table, this.bets, player, wasEmpty);
      }
   }

   public boolean acceptsBet(ItemStack stack) {
      return CasinoSettings.accepts(this.table == null ? CasinoSettings.forMenu(this.containerId) : CasinoSettings.settings(this.table), stack);
   }

   public static boolean validBet(ItemStack stack) {
      return CasinoBets.accepts(stack);
   }

   public boolean clickMenuButton(Player player, int button) {
      if (button == 0 && this.table != null && !this.table.isSpinning() && this.stillValid(player) && player.containerMenu == this && player instanceof ServerPlayer server) {
         if (!CasinoWagerRouting.mayUse(this.table, this.bets, player)) {
            return false;
         } else {
            this.table.start(server);
            return true;
         }
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (this.stillValid(player) && CasinoWagerRouting.mayUse(this.table, this.bets, player)) {
         boolean wasEmpty = this.bets.isEmpty();
         if (index >= 0 && index < this.slots.size()) {
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) {
               return ItemStack.EMPTY;
            } else {
               ItemStack stack = slot.getItem();
               ItemStack original = stack.copy();
               if (index < 9) {
                  if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
                     return ItemStack.EMPTY;
                  }
               } else if (!this.acceptsBet(stack) || this.stakedColor() == -2 || !this.moveItemStackTo(stack, 0, 9, false)) {
                  return ItemStack.EMPTY;
               }

               if (stack.isEmpty()) {
                  slot.setByPlayer(ItemStack.EMPTY);
               } else {
                  slot.setChanged();
               }

               CasinoWagerRouting.changed(this.table, this.bets, player, wasEmpty);
               return original;
            }
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean stillValid(Player player) {
      if (this.table == null) {
         return true;
      } else {
         BlockPos pos = this.table.getBlockPos();
         return !this.table.isSpinning() && player.level().getBlockEntity(pos) == this.table && player.isWithinBlockInteractionRange(pos, (double)1.5F);
      }
   }

   public void removed(Player player) {
      super.removed(player);
   }
}
