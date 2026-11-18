package fr.minevegas.casino;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FortuneWheelMenu extends AbstractContainerMenu {
   public static final int[] BET_X = new int[]{72, 103, 134};
   public static final int[] BET_Y = new int[]{110, 110, 110};
   public static final int INVENTORY_X = 31;
   public static final int INVENTORY_Y = 176;
   private final SimpleContainer bets;
   private final FortuneWheelBlockEntity wheel;
   private final ContainerData faces;

   public FortuneWheelMenu(int id, Inventory inventory) {
      this(id, inventory, (FortuneWheelBlockEntity)null, new SimpleContainer(3), new SimpleContainerData(1));
   }

   public FortuneWheelMenu(int id, Inventory inventory, FortuneWheelBlockEntity wheel, SimpleContainer bets, ContainerData faces) {
      super(RouletteMod.WHEEL_MENU, id);
      this.wheel = wheel;
      this.bets = bets;
      this.faces = faces;

      for(int i = 0; i < 3; ++i) {
         this.addSlot(new Slot(bets, i, BET_X[i], BET_Y[i]) {
            {
               Objects.requireNonNull(FortuneWheelMenu.this);
            }

            public boolean mayPlace(ItemStack stack) {
               return FortuneWheelMenu.this.acceptsBet(stack);
            }
         });
      }

      this.addStandardInventorySlots(inventory, 31, 176);
      this.addDataSlots(faces);
   }

   public FortuneWheelBlockEntity wheel() {
      return this.wheel;
   }

   public int segment() {
      return Math.floorMod(this.faces.get(0), 16);
   }

   public boolean acceptsBet(ItemStack stack) {
      return CasinoSettings.accepts(this.wheel == null ? CasinoSettings.forMenu(this.containerId) : CasinoSettings.settings(this.wheel), stack);
   }

   public static boolean validBet(ItemStack stack) {
      return CasinoBets.accepts(stack);
   }

   public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
      if (this.stillValid(player) && CasinoWagerRouting.mayUse(this.wheel, this.bets, player)) {
         boolean wasEmpty = this.bets.isEmpty();
         super.clicked(slotIndex, buttonNum, input, player);
         CasinoWagerRouting.changed(this.wheel, this.bets, player, wasEmpty);
      }
   }

   public boolean clickMenuButton(Player player, int button) {
      if (button == 0 && this.wheel != null && !this.wheel.isSpinning() && this.stillValid(player) && player.containerMenu == this && player instanceof ServerPlayer server) {
         if (!CasinoWagerRouting.mayUse(this.wheel, this.bets, player)) {
            return false;
         } else {
            this.wheel.start(server);
            return true;
         }
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (this.stillValid(player) && CasinoWagerRouting.mayUse(this.wheel, this.bets, player)) {
         boolean wasEmpty = this.bets.isEmpty();
         if (index >= 0 && index < this.slots.size()) {
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

               CasinoWagerRouting.changed(this.wheel, this.bets, player, wasEmpty);
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
      if (this.wheel == null) {
         return true;
      } else {
         BlockPos pos = this.wheel.getBlockPos();
         return !this.wheel.isSpinning() && player.level().getBlockEntity(pos) == this.wheel && player.isWithinBlockInteractionRange(pos, (double)4.5F);
      }
   }
}
