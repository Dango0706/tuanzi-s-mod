package me.tuanzi.world.inventory;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.item.GoldenPocketItem;
import me.tuanzi.util.ModLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class GoldenPocketMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 54;
    public static final int ROWS = 6;
    public static final int COLUMNS = 9;

    private final Container container;
    private final ItemStack pocketStack;
    private final int blockedSlot;
    private final Player player;

    // 客户端构造方法
    public GoldenPocketMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), getHeldPocketStack(playerInventory.player), getHeldPocketSlot(playerInventory.player));
    }

    // 服务端与通用构造方法
    public GoldenPocketMenu(int containerId, Inventory playerInventory, Container container, ItemStack pocketStack, int blockedSlot) {
        super(ModMenuTypes.GOLDEN_POCKET, containerId);
        checkContainerSize(container, CONTAINER_SIZE);
        this.container = container;
        this.pocketStack = pocketStack;
        this.blockedSlot = blockedSlot;
        this.player = playerInventory.player;

        container.startOpen(this.player);

        // 1. 口袋内部 54 个槽位 (0..53)
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLUMNS; c++) {
                int slotIndex = c + r * COLUMNS;
                this.addSlot(new Slot(container, slotIndex, 8 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !GoldenPocketItem.isForbiddenInPocket(stack);
                    }
                });
            }
        }

        // 2. 玩家主背包槽位 (54..80) - 对应背包索引 9..35
        int playerInvY = 140;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int invIndex = c + r * 9 + 9;
                if (invIndex == this.blockedSlot) {
                    this.addSlot(new Slot(playerInventory, invIndex, 8 + c * 18, playerInvY + r * 18) {
                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    });
                } else {
                    this.addSlot(new Slot(playerInventory, invIndex, 8 + c * 18, playerInvY + r * 18));
                }
            }
        }

        // 3. 玩家快捷栏槽位 (81..89) - 对应背包索引 0..8
        int hotbarY = 198;
        for (int c = 0; c < 9; c++) {
            int invIndex = c;
            if (invIndex == this.blockedSlot) {
                this.addSlot(new Slot(playerInventory, invIndex, 8 + c * 18, hotbarY) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            } else {
                this.addSlot(new Slot(playerInventory, invIndex, 8 + c * 18, hotbarY));
            }
        }

        ModLog.debug(this.player, null, "黄金口袋容器菜单已创建，锁定槽位: " + this.blockedSlot);
    }

    private static ItemStack getHeldPocketStack(Player player) {
        if (player.getMainHandItem().is(ModItems.GOLDEN_POCKET)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModItems.GOLDEN_POCKET)) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static int getHeldPocketSlot(Player player) {
        if (player.getMainHandItem().is(ModItems.GOLDEN_POCKET)) {
            return player.getInventory().getSelectedSlot();
        }
        if (player.getOffhandItem().is(ModItems.GOLDEN_POCKET)) {
            return Inventory.SLOT_OFFHAND;
        }
        return -1;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        saveContents();
    }

    public void saveContents() {
        if (this.pocketStack.isEmpty() || this.player.level().isClientSide()) {
            return;
        }
        List<ItemStack> items = new ArrayList<>(CONTAINER_SIZE);
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            items.add(this.container.getItem(i));
        }
        this.pocketStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.pocketStack.isEmpty()) {
            return false;
        }
        if (this.blockedSlot == Inventory.SLOT_OFFHAND) {
            return player.getOffhandItem() == this.pocketStack;
        }
        if (this.blockedSlot >= 0 && this.blockedSlot < player.getInventory().getContainerSize()) {
            return player.getInventory().getItem(this.blockedSlot) == this.pocketStack;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (this.blockedSlot != -1) {
            // 阻止通过数字快捷键将物品交换进被锁定的手持槽位
            if (clickType == ContainerInput.SWAP && button == this.blockedSlot) {
                return;
            }
            // 阻止点击被锁定的槽位
            if (slotId >= 0 && slotId < this.slots.size()) {
                Slot slot = this.slots.get(slotId);
                if (slot.container == player.getInventory() && slot.getContainerSlot() == this.blockedSlot) {
                    return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (slotIndex < CONTAINER_SIZE) {
                // 从口袋移动到玩家背包/快捷栏 (54..90)
                if (!this.moveItemStackTo(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包/快捷栏移动到口袋内部 (0..54)
                if (GoldenPocketItem.isForbiddenInPocket(stack)) {
                    // 禁止放入黄金口袋或潜影盒
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, 0, CONTAINER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            saveContents();
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
        saveContents();
        ModLog.debug(player, null, "黄金口袋界面已关闭，数据已安全写入 ItemStack。");
    }

    public Container getContainer() {
        return this.container;
    }

    public int getBlockedSlot() {
        return this.blockedSlot;
    }
}
