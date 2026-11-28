package me.tuanzi.world.inventory.canvas;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.world.inventory.BlueprintTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 画板容器菜单：1 格画布槽位 + 玩家背包。
 */
public class EaselMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public EaselMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(6));
    }

    public EaselMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.EASEL, containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 1);
        checkContainerDataCount(data, 6);
        container.startOpen(playerInventory.player);

        // Slot 0: 画布槽位
        this.addSlot(new Slot(container, 0, 14, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CANVAS);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !isOccupied();
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 154));
        }

        this.addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        int x = BlueprintTableMenu.reconstructInt(data.get(0), data.get(1));
        int y = data.get(2);
        int z = BlueprintTableMenu.reconstructInt(data.get(3), data.get(4));
        return new BlockPos(x, y, z);
    }

    public ItemStack getCanvasStack() {
        return this.slots.get(0).getItem();
    }

    public boolean hasActiveDraft() {
        return (data.get(5) & 1) != 0;
    }

    public boolean isOccupied() {
        return (data.get(5) & 2) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == 0) {
                if (isOccupied()) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!stack.is(ModItems.CANVAS)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
