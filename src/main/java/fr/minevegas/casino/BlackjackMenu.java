package fr.minevegas.casino;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BlackjackMenu extends AbstractContainerMenu {
   public static final int BET_SLOTS = 3;
   public static final int DEAL = 0;
   public static final int HIT = 1;
   public static final int STAND = 2;
   public static final int DOUBLE = 3;
   public static final int D_PHASE = 0;
   public static final int D_PLAYER_COUNT = 1;
   public static final int D_PLAYER = 2;
   public static final int D_DEALER_COUNT = 14;
   public static final int D_DEALER = 15;
   public static final int D_TURN_LEFT = 27;
   public static final int D_RESULT = 28;
   public static final int D_FLAGS = 29;
   public static final int D_PAYOUT = 30;
   public static final int D_AIM_YAW = 31;
   public static final int D_AIM_PITCH = 32;
   public static final int D_AIM_SPREAD = 33;
   public static final int FLAG_AIM = 4;
   public static final int DATA_COUNT = 34;
   public static final int MAX_CARDS = 12;
   public static final int PHASE_BETTING = 0;
   public static final int PHASE_DEALING = 1;
   public static final int PHASE_TURN = 2;
   public static final int PHASE_DRAWING = 3;
   public static final int PHASE_DEALER = 4;
   public static final int PHASE_RESULT = 5;
   public static final int[] BET_X = new int[]{72, 103, 134};
   public static final int[] BET_Y = new int[]{96, 96, 96};
   public static final int INVENTORY_X = 31;
   public static final int INVENTORY_Y = 176;
   private final SimpleContainer bets;
   private final BlackjackTableBlockEntity table;
   private final ContainerData data;
   private boolean phaseKnown;

   public BlackjackMenu(int id, Inventory inventory) {
      this(id, inventory, (BlackjackTableBlockEntity)null, new SimpleContainer(3), new SimpleContainerData(34));
   }

   public BlackjackMenu(int id, Inventory inventory, BlackjackTableBlockEntity table, SimpleContainer bets, ContainerData data) {
      super(RouletteMod.BLACKJACK_MENU, id);
      this.table = table;
      this.bets = bets;
      this.data = data;

      for(int i = 0; i < 3; ++i) {
         this.addSlot(new Slot(bets, i, BET_X[i], BET_Y[i]) {
            {
               Objects.requireNonNull(BlackjackMenu.this);
            }

            public boolean mayPlace(ItemStack stack) {
               return BlackjackMenu.this.betting() && BlackjackMenu.this.acceptsBet(stack);
            }

            public boolean mayPickup(Player player) {
               return BlackjackMenu.this.betting();
            }

            public boolean isActive() {
               return BlackjackMenu.this.betting();
            }
         });
      }

      for(int row = 0; row < 3; ++row) {
         for(int col = 0; col < 9; ++col) {
            this.addSlot(new HideableSlot(inventory, col + row * 9 + 9, 31 + col * 18, 176 + row * 18));
         }
      }

      for(int col = 0; col < 9; ++col) {
         this.addSlot(new HideableSlot(inventory, col, 31 + col * 18, 234));
      }

      this.addDataSlots(data);
   }

   public BlackjackTableBlockEntity table() {
      return this.table;
   }

   public SimpleContainer bets() {
      return this.bets;
   }

   public int phase() {
      return this.data.get(0);
   }

   public boolean betting() {
      return this.table != null ? !this.table.isRoundOn() : this.data.get(0) == 0;
   }

   public boolean phaseKnown() {
      return this.table != null || this.phaseKnown;
   }

   public void setData(int id, int value) {
      super.setData(id, value);
      if (id == 0) {
         this.phaseKnown = true;
      }

   }

   public int data(int index) {
      return this.data.get(index);
   }

   public int[] cards(boolean dealer) {
      int count = Math.max(0, Math.min(12, this.data.get(dealer ? 14 : 1)));
      int[] out = new int[count];

      for(int i = 0; i < count; ++i) {
         out[i] = this.data.get((dealer ? 15 : 2) + i);
      }

      return out;
   }

   public boolean can(int flagBit) {
      return (this.data.get(29) & 1 << flagBit) != 0;
   }

   public boolean acceptsBet(ItemStack stack) {
      return CasinoSettings.accepts(this.table == null ? CasinoSettings.forMenu(this.containerId) : CasinoSettings.settings(this.table), stack);
   }

   public static boolean validBet(ItemStack stack) {
      return CasinoBets.accepts(stack);
   }

   public boolean canAffordDouble(List<ItemStack> inventory) {
      for(int i = 0; i < 3; ++i) {
         ItemStack kind = this.bets.getItem(i);
         if (!kind.isEmpty()) {
            int needed = 0;

            for(int j = 0; j < 3; ++j) {
               if (ItemStack.isSameItemSameComponents(kind, this.bets.getItem(j))) {
                  needed += this.bets.getItem(j).getCount();
               }
            }

            int have = 0;

            for(ItemStack stack : inventory) {
               if (ItemStack.isSameItemSameComponents(kind, stack)) {
                  have += stack.getCount();
               }
            }

            if (have < needed) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean hasBet() {
      for(int i = 0; i < 3; ++i) {
         if (!this.bets.getItem(i).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public void showWager(List<ItemStack> stakes) {
      for(int i = 0; i < 3; ++i) {
         this.bets.setItem(i, i < stakes.size() ? ((ItemStack)stakes.get(i)).copy() : ItemStack.EMPTY);
      }

      this.broadcastChanges();
   }

   public void clearBets() {
      for(int i = 0; i < 3; ++i) {
         this.bets.setItem(i, ItemStack.EMPTY);
      }

      this.broadcastChanges();
   }

   public boolean clickMenuButton(Player player, int button) {
      if (this.table != null && player instanceof ServerPlayer server) {
         if (player.containerMenu == this && this.stillValid(player)) {
            return this.table.menuButton(server, this, button);
         }
      }

      return false;
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (this.betting() && index >= 0 && index < this.slots.size()) {
         Slot slot = this.slots.get(index);
         if (!slot.hasItem()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < 3) {
               if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.acceptsBet(stack) || !this.moveItemStackTo(stack, 0, 3, false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            return original;
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
         return player.level().getBlockEntity(pos) == this.table && this.table.isInRange(player);
      }
   }

   public void removed(Player player) {
      super.removed(player);
      if (this.table != null && player instanceof ServerPlayer server) {
         this.table.menuClosed(server, this);
      }

   }

   private final class HideableSlot extends Slot {
      HideableSlot(Inventory inventory, int index, int x, int y) {
         Objects.requireNonNull(BlackjackMenu.this);
         super(inventory, index, x, y);
      }

      public boolean isActive() {
         return BlackjackMenu.this.betting();
      }

      public boolean mayPickup(Player player) {
         return BlackjackMenu.this.betting();
      }

      public boolean mayPlace(ItemStack stack) {
         return BlackjackMenu.this.betting();
      }
   }
}
