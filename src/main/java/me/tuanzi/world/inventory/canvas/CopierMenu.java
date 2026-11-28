package me.tuanzi.world.inventory.canvas;

import me.tuanzi.block.entity.CopierBlockEntity;
import me.tuanzi.init.ModBlocks;
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
 * 复印机容器菜单：母版模型槽、打印原料槽、复制输出槽 + 玩家背包。
 */
public class CopierMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public CopierMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(8));
    }

    public CopierMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.COPIER, containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 8);
        container.startOpen(playerInventory.player);

        // Slot 0: 母版模型
        this.addSlot(new Slot(container, CopierBlockEntity.SLOT_MODEL, 16, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModBlocks.PRINTED_MODEL.asItem());
            }
        });

        // Slot 1: 打印原料
        this.addSlot(new Slot(container, CopierBlockEntity.SLOT_MATERIAL, 52, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PRINT_MATERIAL);
            }
        });

        // Slot 2: 输出
        this.addSlot(new Slot(container, CopierBlockEntity.SLOT_OUTPUT, 140, 26) {
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

    public int getStatusCode() {
        return data.get(5);
    }

    public int getCost() {
        return BlueprintTableMenu.reconstructInt(data.get(6), data.get(7));
    }

    public ItemStack getModelStack() {
        return this.slots.get(0).getItem();
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
            } else if (stack.is(ModBlocks.PRINTED_MODEL.asItem())) {
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
