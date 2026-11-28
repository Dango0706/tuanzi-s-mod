package me.tuanzi.world.inventory.canvas;

import me.tuanzi.block.entity.Printer3DBlockEntity;
import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.world.canvas.CanvasType;
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
 * 3D 打印机容器菜单：画布输入、打印原料输入、打印模型输出 + 玩家背包。
 */
public class Printer3DMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public Printer3DMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(15));
    }

    public Printer3DMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.PRINTER_3D, containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 15);
        container.startOpen(playerInventory.player);

        // Slot 0: 画布输入
        this.addSlot(new Slot(container, Printer3DBlockEntity.SLOT_CANVAS, 16, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CANVAS);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 1: 打印原料输入
        this.addSlot(new Slot(container, Printer3DBlockEntity.SLOT_MATERIAL, 52, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PRINT_MATERIAL);
            }
        });

        // Slot 2: 输出槽
        this.addSlot(new Slot(container, Printer3DBlockEntity.SLOT_OUTPUT, 140, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
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

    public int getSelectedScale() {
        return Math.max(1, data.get(5));
    }

    public int getStatusCode() {
        return data.get(6);
    }

    public int getCost() {
        return BlueprintTableMenu.reconstructInt(data.get(7), data.get(8));
    }

    public int getVoxelLimit() {
        return BlueprintTableMenu.reconstructInt(data.get(9), data.get(10));
    }

    public int getNonEmptyVoxels() {
        return BlueprintTableMenu.reconstructInt(data.get(11), data.get(12));
    }

    public int getCanvasSize() {
        return Math.max(16, data.get(13));
    }

    public CanvasType getCanvasType() {
        int ord = data.get(14);
        CanvasType[] values = CanvasType.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : CanvasType.UNSET;
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
            if (index < 3) {
                if (!this.moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.CANVAS)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.PRINT_MATERIAL)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
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
