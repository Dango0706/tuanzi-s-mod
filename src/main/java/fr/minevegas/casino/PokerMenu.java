package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PokerMenu extends AbstractContainerMenu {
   public static final int BET_SLOTS = 3;
   public static final int DEAL = 0;
   public static final int CALL = 1;
   public static final int FOLD = 2;
   public static final int D_PHASE = 0;
   public static final int D_PLAYER_COUNT = 1;
   public static final int D_PLAYER = 2;
   public static final int D_DEALER_COUNT = 4;
   public static final int D_DEALER = 5;
   public static final int D_BOARD_COUNT = 7;
   public static final int D_BOARD = 8;
   public static final int D_TURN_LEFT = 13;
   public static final int D_RESULT = 14;
   public static final int D_FLAGS = 15;
   public static final int D_PAYOUT = 16;
   public static final int D_AIM_YAW = 17;
   public static final int D_AIM_PITCH = 18;
   public static final int D_AIM_SPREAD = 19;
   public static final int FLAG_CALLED = 2;
   public static final int FLAG_AIM = 4;
   public static final int NET_SLOTS = 6;
   public static final int D_NET_COUNT = 20;
   public static final int D_NET_FIRST = 21;
   public static final int NET_FIELDS = 5;
   public static final int DATA_COUNT = 51;
   public static final int PHASE_BETTING = 0;
   public static final int PHASE_DEALING = 1;
   public static final int PHASE_DECISION = 2;
   public static final int PHASE_BOARD = 3;
   public static final int PHASE_SHOWDOWN = 4;
   public static final int PHASE_RESULT = 5;
   public static final int[] BET_X = new int[]{72, 103, 134};
   public static final int[] BET_Y = new int[]{89, 89, 89};
   public static final int INVENTORY_X = 31;
   public static final int INVENTORY_Y = 176;
   private final SimpleContainer bets;
   private final PokerTableBlockEntity table;
   private final ContainerData data;
   private boolean phaseKnown;

   public PokerMenu(int id, Inventory inventory) {
      this(id, inventory, (PokerTableBlockEntity)null, new SimpleContainer(3), new SimpleContainerData(51));
   }

   public PokerMenu(int id, Inventory inventory, PokerTableBlockEntity table, SimpleContainer bets, ContainerData data) {
      super(RouletteMod.POKER_MENU, id);
      this.table = table;
      this.bets = bets;
      this.data = data;

      for(int i = 0; i < 3; ++i) {
         this.addSlot(new Slot(bets, i, BET_X[i], BET_Y[i]) {
            {
               Objects.requireNonNull(PokerMenu.this);
            }

            public boolean mayPlace(ItemStack stack) {
               return PokerMenu.this.betting() && PokerMenu.this.acceptsBet(stack);
            }

            public boolean mayPickup(Player player) {
               return PokerMenu.this.betting();
            }

            public boolean isActive() {
               return PokerMenu.this.betting();
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

   public PokerTableBlockEntity table() {
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

   private int[] cards(int countIndex, int firstIndex, int max) {
      int count = Math.max(0, Math.min(max, this.data.get(countIndex)));
      int[] out = new int[count];

      for(int i = 0; i < count; ++i) {
         out[i] = this.data.get(firstIndex + i);
      }

      return out;
   }

   public int[] playerCards() {
      return this.cards(1, 2, 2);
   }

   public int[] dealerCards() {
      return this.cards(4, 5, 2);
   }

   public int[] boardCards() {
      return this.cards(7, 8, 5);
   }

   public boolean can(int flagBit) {
      return (this.data.get(15) & 1 << flagBit) != 0;
   }

   public List<ResultDelta> resultDeltas() {
      List<ResultDelta> out = new ArrayList();
      int count = Math.max(0, Math.min(6, this.data.get(20)));

      for(int i = 0; i < count; ++i) {
         int at = 21 + i * 5;
         int source = this.data.get(at);
         int item = words(this.data.get(at + 1), this.data.get(at + 2));
         int delta = words(this.data.get(at + 3), this.data.get(at + 4));
         ItemStack prototype = source >= 1 && source <= 3 ? this.bets.getItem(source - 1).copyWithCount(1) : new ItemStack(BuiltInRegistries.ITEM.byId(item));
         if (!prototype.isEmpty()) {
            out.add(new ResultDelta(prototype, delta));
         }
      }

      return out;
   }

   private static int words(int low, int high) {
      return low & '\uffff' | (high & '\uffff') << 16;
   }

   public boolean acceptsBet(ItemStack stack) {
      return CasinoSettings.accepts(this.table == null ? CasinoSettings.forMenu(this.containerId) : CasinoSettings.settings(this.table), stack);
   }

   public boolean canAffordCall(List<ItemStack> inventory) {
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

            if (have < 2 * needed) {
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

   public static record ResultDelta(ItemStack prototype, int count) {
   }

   private final class HideableSlot extends Slot {
      HideableSlot(Inventory inventory, int index, int x, int y) {
         Objects.requireNonNull(PokerMenu.this);
         super(inventory, index, x, y);
      }

      public boolean isActive() {
         return PokerMenu.this.betting();
      }

      public boolean mayPickup(Player player) {
         return PokerMenu.this.betting();
      }

      public boolean mayPlace(ItemStack stack) {
         return PokerMenu.this.betting();
      }
   }
}
