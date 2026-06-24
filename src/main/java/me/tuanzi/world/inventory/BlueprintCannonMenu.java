package me.tuanzi.world.inventory;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class BlueprintCannonMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public BlueprintCannonMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(11));
    }

    public BlockPos getCannonPos() {
        return new BlockPos(this.data.get(6), this.data.get(7), this.data.get(8));
    }

    public BlueprintCannonMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.BLUEPRINT_CANNON, containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 3);
        this.addDataSlots(data);

        // 蓝图槽 Slot 0 (X=62, Y=35)
        this.addSlot(new Slot(container, 0, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.STRUCTURE_BLUEPRINT);
            }
        });

        // 浆料槽 Slot 1 (X=98, Y=35)
        this.addSlot(new Slot(container, 1, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.COMPRESSED_BUILD_SLURRY);
            }
        });

        // 书与笔槽 Slot 2 (X=80, Y=53)
        this.addSlot(new Slot(container, 2, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(net.minecraft.world.item.Items.WRITABLE_BOOK)
                    || stack.is(net.minecraft.world.item.Items.WRITTEN_BOOK);
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

    public int getBuildState() {
        return this.data.get(0);
    }

    /**
     * 返回当前已处理的蓝图格子索引（即进度分子）。
     * 返回值 == getTotalBlocks() 时表示建造已完成（进度 100%）。
     */
    public int getCurrentIndex() {
        return this.data.get(1);
    }

    /**
     * 返回蓝图 blocks 数组总长度（进度分母）。
     * 为 0 表示蓝图未载入或已被取走。
     */
    public int getTotalBlocks() {
        return this.data.get(2);
    }

    public boolean isDestroyMode() {
        return this.data.get(3) == 1;
    }

    public boolean isExactMatch() {
        return this.data.get(4) == 1;
    }

    public int getSlurryDurability() {
        return this.data.get(5);
    }

    public int getPauseReason() {
        return this.data.get(9);
    }

    public ItemStack getBlueprint() {
        return this.container.getItem(0);
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

            if (index < 3) {
                // 从大炮移入玩家背包
                if (!this.moveItemStackTo(slotStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移入大炮
                if (slotStack.is(ModItems.STRUCTURE_BLUEPRINT)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(ModItems.COMPRESSED_BUILD_SLURRY)) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(net.minecraft.world.item.Items.WRITABLE_BOOK)
                        || slotStack.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) {
                    if (!this.moveItemStackTo(slotStack, 2, 3, false)) {
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
