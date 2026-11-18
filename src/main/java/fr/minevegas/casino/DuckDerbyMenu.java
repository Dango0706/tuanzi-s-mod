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

public final class DuckDerbyMenu extends AbstractContainerMenu {
   public static final int[] BET_X = new int[]{26, 78, 130, 182};
   public static final int[] BET_Y = new int[]{74, 94, 114};
   public static final int INVENTORY_X = 31;
   public static final int INVENTORY_Y = 176;
   public static final int SEVERAL = -2;
   private final SimpleContainer bets;
   private final DuckDerbyBlockEntity machine;
   private int dragLane;
   private final Player viewer;
   private boolean returned;
   private boolean escrowView;
   private String lastLobby;
   private Set<Slot> screenDrag;

   public DuckDerbyMenu(int id, Inventory inventory) {
      this(id, inventory, (DuckDerbyBlockEntity)null, new SimpleContainer(12));
   }

   public DuckDerbyMenu(int id, Inventory inventory, DuckDerbyBlockEntity machine, SimpleContainer bets) {
      super(RouletteMod.DERBY_MENU, id);
      this.dragLane = -1;
      this.lastLobby = "";
      this.machine = machine;
      this.viewer = inventory.player;
      this.escrowView = machine != null && machine.confirmed(this.viewer.getUUID());
      this.bets = bets;

      for(int i = 0; i < 12; ++i) {
         this.addSlot(new Slot(bets, i, BET_X[i % 4], BET_Y[i / 4]) {
            {
               Objects.requireNonNull(DuckDerbyMenu.this);
            }

            public boolean mayPlace(ItemStack stack) {
               return !DuckDerbyMenu.this.legacy() && !DuckDerbyMenu.this.confirmed() && DuckDerbyMenu.this.acceptsBet(stack) && DuckDerbyMenu.open(this.container, this.getContainerSlot());
            }

            public boolean mayPickup(Player player) {
               return !DuckDerbyMenu.this.confirmed();
            }

            public boolean isHighlightable() {
               return this.hasItem() || DuckDerbyMenu.open(this.container, this.getContainerSlot());
            }
         });
      }

      this.addStandardInventorySlots(inventory, 31, 176);
   }

   public DuckDerbyBlockEntity machine() {
      return this.machine;
   }

   public SimpleContainer bets() {
      return this.bets;
   }

   public boolean legacy() {
      return this.machine == null ? DerbyLobby.view(this.containerId).legacy() : this.bets == this.machine.bets();
   }

   public boolean confirmed() {
      return this.machine == null ? DerbyLobby.view(this.containerId).confirmed() : this.escrowView || this.machine.confirmed(this.viewer.getUUID());
   }

   public void escrowView(boolean value) {
      this.escrowView = value;
   }

   public void broadcastChanges() {
      if (this.machine != null && this.escrowView && !this.machine.confirmed(this.viewer.getUUID()) && !this.machine.isRacing()) {
         this.bets.clearContent();
         this.escrowView = false;
         this.machine.resumeDraft(this.viewer.getUUID(), this.bets);
      }

      super.broadcastChanges();
      if (this.machine != null) {
         Player var2 = this.viewer;
         if (var2 instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)var2;
            String json = DerbyLobby.json(this.machine, player);
            if (!json.equals(this.lastLobby)) {
               this.lastLobby = json;
               DerbyLobby.send(player, this.containerId, json);
            }
         }
      }

   }

   public void removed(Player player) {
      super.removed(player);
      if (!this.returned) {
         this.returned = true;
         if (this.machine != null && !this.legacy() && player instanceof ServerPlayer) {
            ServerPlayer server = (ServerPlayer)player;
            this.machine.closeDraft(server, this.bets);
         }

      }
   }

   public boolean acceptsBet(ItemStack stack) {
      return CasinoSettings.accepts(this.machine == null ? CasinoSettings.forMenu(this.containerId) : CasinoSettings.settings(this.machine), stack);
   }

   public static boolean validBet(ItemStack stack) {
      return CasinoBets.accepts(stack);
   }

   public static int stakedLane(Container bets) {
      int lane = -1;

      for(int i = 0; i < 12; ++i) {
         if (!bets.getItem(i).isEmpty()) {
            if (lane == -1) {
               lane = i % 4;
            } else if (lane != i % 4) {
               return -2;
            }
         }
      }

      return lane;
   }

   public int stakedLane() {
      return stakedLane(this.bets);
   }

   private static boolean open(Container bets, int slot) {
      int lane = stakedLane(bets);
      return lane == -1 || lane == slot % 4;
   }

   public void followDrag(Set<Slot> slots) {
      this.screenDrag = slots;
   }

   public boolean canDragTo(Slot slot) {
      if (this.screenDrag != null && slot.container == this.bets) {
         for(Slot s : this.screenDrag) {
            if (s.container == this.bets && s.getContainerSlot() % 4 != slot.getContainerSlot() % 4) {
               return false;
            }
         }
      }

      return super.canDragTo(slot);
   }

   public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
      if (!this.confirmed() || (slotIndex < 0 || slotIndex >= 12) && input != ContainerInput.QUICK_CRAFT) {
         if (input == ContainerInput.QUICK_CRAFT) {
            int header = getQuickcraftHeader(buttonNum);
            if (header == 0) {
               this.dragLane = -1;
            } else if (header == 1 && slotIndex >= 0 && slotIndex < 12 && open(this.bets, slotIndex)) {
               int lane = slotIndex % 4;
               if (this.dragLane == -1) {
                  this.dragLane = lane;
               } else if (lane != this.dragLane) {
                  return;
               }
            }
         }

         super.clicked(slotIndex, buttonNum, input, player);
      }
   }

   public boolean clickMenuButton(Player player, int button) {
      if (this.machine != null && this.stillValid(player) && player.containerMenu == this && player instanceof ServerPlayer server) {
         if (button == 3) {
            DerbyCredits.get(server.level().getServer()).pay(server, true);
            return true;
         } else if (this.legacy()) {
            return false;
         } else {
            boolean var10000;
            switch (button) {
               case 0 -> var10000 = this.machine.startSolo(server, this, -1);
               case 1 -> var10000 = this.machine.joinGroup(server, this);
               case 2 -> var10000 = this.machine.withdraw(server, this);
               default -> var10000 = false;
            }

            boolean changed = var10000;
            this.broadcastChanges();
            return changed;
         }
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (this.confirmed()) {
         return ItemStack.EMPTY;
      } else if (index >= 0 && index < this.slots.size()) {
         Slot slot = this.slots.get(index);
         if (!slot.hasItem()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < 12) {
               if (!this.moveItemStackTo(stack, 12, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.acceptsBet(stack) || stakedLane(this.bets) == -2 || !this.moveItemStackTo(stack, 0, 12, false)) {
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
      if (this.machine == null) {
         return true;
      } else {
         BlockPos pos = this.machine.getBlockPos();
         return !this.machine.isRacing() && player.level().getBlockEntity(pos) == this.machine && player.isWithinBlockInteractionRange(pos, (double)4.5F);
      }
   }
}
