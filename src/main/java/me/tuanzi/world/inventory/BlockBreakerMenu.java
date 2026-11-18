package me.tuanzi.world.inventory;

import me.tuanzi.init.ModItemTags;
import me.tuanzi.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BlockBreakerMenu extends AbstractContainerMenu {
    public static final int TOOL_SLOT = 0;
    public static final int BUFFER_START = 1;
    public static final int BUFFER_COUNT = 9;
    public static final int TOTAL_CONTAINER_SLOTS = 10;

    private final Container container;
    private final ContainerData data;

    public BlockBreakerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_CONTAINER_SLOTS), new SimpleContainerData(7));
    }

    public BlockBreakerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.BLOCK_BREAKER, containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, TOTAL_CONTAINER_SLOTS);
        checkContainerDataCount(data, 7);
        container.startOpen(playerInventory.player);

        // 1. 工具槽 (Slot 0)
        this.addSlot(new Slot(container, TOOL_SLOT, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return me.tuanzi.block.entity.BlockBreakerBlockEntity.isToolItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 2. 3x3 输出缓冲槽 (Slot 1 ~ 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = BUFFER_START + col + row * 3;
                this.addSlot(new Slot(container, slotIndex, 98 + col * 18, 17 + row * 18));
            }
        }

        // 3. 玩家背包 (3行 x 9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 4. 玩家快捷栏 (1行 x 9列)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public boolean isContinuousMode() {
        return this.data.get(0) == 1;
    }

    public int getMiningProgress() {
        return this.data.get(1);
    }

    public int getCooldown() {
        return this.data.get(2);
    }

    public int getStatusCode() {
        return this.data.get(3);
    }

    public net.minecraft.core.BlockPos getBlockPos() {
        return new net.minecraft.core.BlockPos(this.data.get(4), this.data.get(5), this.data.get(6));
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

            // 如果点击的是方块破坏器内部槽位 (0 到 9)
            if (index < TOTAL_CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(slotStack, TOTAL_CONTAINER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 点击玩家背包/快捷栏
                // 优先检查是否可以放入工具槽
                if (me.tuanzi.block.entity.BlockBreakerBlockEntity.isToolItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                        // 工具槽放不下，尝试放入缓冲槽
                        if (!this.moveItemStackTo(slotStack, BUFFER_START, BUFFER_START + BUFFER_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    // 非工具，尝试放入缓冲槽
                    if (!this.moveItemStackTo(slotStack, BUFFER_START, BUFFER_START + BUFFER_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
