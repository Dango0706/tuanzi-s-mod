package me.tuanzi.world.inventory;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.item.TravelersNotebookItem;
import me.tuanzi.util.ItemStackNbtHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class TravelersNotebookMenu extends AbstractContainerMenu {

    private final ItemStack notebook;
    private final SimpleContainer container = new SimpleContainer(27) {
        @Override
        public void setChanged() {
            super.setChanged();
            TravelersNotebookMenu.this.slotsChanged(this);
        }
    };

    public TravelersNotebookMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public TravelersNotebookMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.TRAVELERS_NOTEBOOK, containerId);

        ItemStack foundNotebook = ItemStack.EMPTY;
        if (hand != null) {
            foundNotebook = playerInventory.player.getItemInHand(hand);
        } else {
            if (playerInventory.player.getMainHandItem().is(ModItems.TRAVELERS_NOTEBOOK)) {
                foundNotebook = playerInventory.player.getMainHandItem();
            } else if (playerInventory.player.getOffhandItem().is(ModItems.TRAVELERS_NOTEBOOK)) {
                foundNotebook = playerInventory.player.getOffhandItem();
            }
        }
        this.notebook = foundNotebook;

        // 加载内部存储物品
        if (!this.notebook.isEmpty()) {
            CustomData customData = this.notebook.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                ListTag listTag = tag.getListOrEmpty("Items");
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag itemTag = listTag.getCompoundOrEmpty(i);
                    int slotIdx = itemTag.getByteOr("Slot", (byte) 0) & 255;
                    if (slotIdx >= 0 && slotIdx < 27) {
                        ItemStack loadedStack = ItemStackNbtHelper.read(playerInventory.player.registryAccess(), itemTag.getCompoundOrEmpty("Item"));
                        this.container.setItem(slotIdx, loadedStack);
                    }
                }
            }
        }

        // 添加 Slot 0 (燃料格)
        this.addSlot(new Slot(this.container, 0, 8, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.TELEPORTATION_PAPER);
            }
        });

        // 添加 Slot 1-26 (道标符石格)
        for (int i = 1; i < 27; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            this.addSlot(new Slot(this.container, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.SIGNPOST_RUNE) && me.tuanzi.item.SignpostRuneItem.isRuneBound(stack);
                }
            });
        }

        // 玩家背包物品栏
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }

        // 玩家快捷栏物品栏
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInventory, c, 8 + c * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.notebook.isEmpty() && (player.getMainHandItem() == this.notebook || player.getOffhandItem() == this.notebook);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (this.notebook.isEmpty()) return;

        // 1. 自动消耗传送纸充能
        ItemStack paperStack = this.container.getItem(0);
        int energy = TravelersNotebookItem.getEnergy(this.notebook);

        if (paperStack.is(ModItems.TELEPORTATION_PAPER) && paperStack.getCount() > 0) {
            int count = paperStack.getCount();
            if (energy < 64) {
                int toConsume = Math.min(64 - energy, count);
                energy += toConsume;
                paperStack.shrink(toConsume);
                TravelersNotebookItem.setEnergy(this.notebook, energy);
            }
        }

        // 2. 返还剩余燃料
        if (!paperStack.isEmpty()) {
            if (!player.getInventory().add(paperStack)) {
                player.drop(paperStack, false);
            }
            this.container.setItem(0, ItemStack.EMPTY);
        }

        // 3. 将 1-26 格物品及最新能量值序列化保存至手札的 CUSTOM_DATA 中
        CompoundTag tag = new CompoundTag();
        CustomData customData = this.notebook.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }

        ListTag listTag = new ListTag();
        for (int i = 0; i < 27; i++) {
            ItemStack stack = this.container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                itemTag.put("Item", ItemStackNbtHelper.write(player.registryAccess(), stack));
                listTag.add(itemTag);
            }
        }
        tag.put("Items", listTag);
        tag.putInt("Energy", energy);

        this.notebook.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (slotIndex < 27) {
                // 从容器移出到玩家背包
                if (!this.moveItemStackTo(slotStack, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移入容器
                if (slotStack.is(ModItems.TELEPORTATION_PAPER)) {
                    // 燃料纸优先放入 Slot 0
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        // 无法移入燃料格，看看能不能移入其他格（但符石格不允许放纸，所以直接跳过）
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(ModItems.SIGNPOST_RUNE)) {
                    // 道标符石放入 Slot 1-26 (必须已绑定)
                    if (!me.tuanzi.item.SignpostRuneItem.isRuneBound(slotStack)) {
                        return ItemStack.EMPTY;
                    }
                    if (!this.moveItemStackTo(slotStack, 1, 27, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY; // 不属于手札可存物品
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
