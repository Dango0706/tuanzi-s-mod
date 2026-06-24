package me.tuanzi.world.inventory;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BlueprintTableMenu extends AbstractContainerMenu {
    private final Container container;
    private final net.minecraft.world.inventory.ContainerData data;

    public BlueprintTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2), new net.minecraft.world.inventory.SimpleContainerData(3));
    }

    public net.minecraft.core.BlockPos getTablePos() {
        return new net.minecraft.core.BlockPos(this.data.get(0), this.data.get(1), this.data.get(2));
    }

    public BlueprintTableMenu(int containerId, Inventory playerInventory, Container container, net.minecraft.world.inventory.ContainerData data) {
        super(ModMenuTypes.BLUEPRINT_TABLE, containerId);
        this.container = container;
        this.data = data;
        this.addDataSlots(data);
        checkContainerSize(container, 2);

        // Slot 0: 结构蓝图槽（用于导出）- (X=62, Y=35)
        this.addSlot(new Slot(container, 0, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.STRUCTURE_BLUEPRINT);
            }
        });

        // Slot 1: 空白蓝图槽（用于导入记录）- (X=98, Y=35)
        this.addSlot(new Slot(container, 1, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BLANK_BLUEPRINT);
            }
        });

        // 玩家背包与快捷栏
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public ItemStack getExportBlueprint() {
        return this.container.getItem(0);
    }

    public ItemStack getImportBlueprint() {
        return this.container.getItem(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (index < 2) {
                // 从蓝图桌移入玩家背包
                if (!this.moveItemStackTo(slotStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移入蓝图桌
                if (slotStack.is(ModItems.STRUCTURE_BLUEPRINT)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(ModItems.BLANK_BLUEPRINT)) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }
        return itemStack;
    }
}
