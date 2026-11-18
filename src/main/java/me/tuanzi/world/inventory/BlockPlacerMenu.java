package me.tuanzi.world.inventory;

import me.tuanzi.block.entity.BlockPlacerBlockEntity;
import me.tuanzi.init.ModMenuTypes;
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
 * 方块放置器容器菜单
 */
public class BlockPlacerMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOT = BlockPlacerBlockEntity.FILTER_SLOT;
    public static final int STORAGE_START = BlockPlacerBlockEntity.STORAGE_START;
    public static final int STORAGE_COUNT = BlockPlacerBlockEntity.STORAGE_COUNT;
    public static final int TOTAL_CONTAINER_SLOTS = BlockPlacerBlockEntity.TOTAL_SLOTS;

    private final Container container;
    private final ContainerData data;

    public BlockPlacerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_CONTAINER_SLOTS), new SimpleContainerData(8));
    }

    public BlockPlacerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.BLOCK_PLACER, containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, TOTAL_CONTAINER_SLOTS);
        checkContainerDataCount(data, 8);
        container.startOpen(playerInventory.player);

        // 1. 独立过滤槽 (Slot 0)
        this.addSlot(new Slot(container, FILTER_SLOT, 12, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BlockPlacerBlockEntity.isSafeBlockItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 2. 3x3 库存槽 (Slot 1 ~ 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = STORAGE_START + col + row * 3;
                this.addSlot(new Slot(container, slotIndex, 114 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return BlockPlacerBlockEntity.isSafeBlockItem(stack);
                    }
                });
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

    public int getRedstoneMode() {
        return this.data.get(0);
    }

    public int getContinuousInterval() {
        return this.data.get(1);
    }

    public boolean isSimulateSneak() {
        return this.data.get(2) == 1;
    }

    public boolean isAllowBottomExtract() {
        return this.data.get(3) == 1;
    }

    public int getStatusCode() {
        return this.data.get(4);
    }

    public BlockPos getBlockPos() {
        return new BlockPos(this.data.get(5), this.data.get(6), this.data.get(7));
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

            if (index < TOTAL_CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(slotStack, TOTAL_CONTAINER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Shift 点击只移入 1~9 库存槽，避免误入过滤槽
                if (!BlockPlacerBlockEntity.isSafeBlockItem(slotStack)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(slotStack, STORAGE_START, TOTAL_CONTAINER_SLOTS, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
