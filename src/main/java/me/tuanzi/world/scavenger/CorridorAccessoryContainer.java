package me.tuanzi.world.scavenger;

import me.tuanzi.init.ModItems;
import me.tuanzi.util.ItemStackNbtHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 拾荒回廊专属独立槽位（类似饰品栏，2 个固定槽位）
 * 槽位 0: 归厅信标 (Hall Return Beacon)
 * 槽位 1: 归室信标 (Room Return Beacon)
 * 独立于 36 格常规背包，在回廊内死亡不掉落，出回廊持久化暂存。
 */
public class CorridorAccessoryContainer extends SimpleContainer {

    public static final int SLOT_HALL = 0;
    public static final int SLOT_ROOM = 1;
    public static final int SLOT_MARKET = 2;
    public static final int CONTAINER_SIZE = 3;

    public CorridorAccessoryContainer() {
        super(CONTAINER_SIZE);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_HALL) {
            return stack.is(ModItems.HALL_RETURN_BEACON);
        } else if (slot == SLOT_ROOM) {
            return stack.is(ModItems.ROOM_RETURN_BEACON);
        } else if (slot == SLOT_MARKET) {
            return stack.is(ModItems.MARKET_RETURN_BEACON);
        }
        return false;
    }

    /**
     * 确保槽位具备默认信标
     */
    public void ensureDefaultBeacons() {
        if (getItem(SLOT_HALL).isEmpty()) {
            setItem(SLOT_HALL, new ItemStack(ModItems.HALL_RETURN_BEACON));
        }
        if (getItem(SLOT_ROOM).isEmpty()) {
            setItem(SLOT_ROOM, new ItemStack(ModItems.ROOM_RETURN_BEACON));
        }
        if (getItem(SLOT_MARKET).isEmpty()) {
            setItem(SLOT_MARKET, new ItemStack(ModItems.MARKET_RETURN_BEACON));
        }
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.put("Item", ItemStackNbtHelper.write(registries, stack));
                list.add(itemTag);
            }
        }
        tag.put("Items", list);
        return tag;
    }

    public void fromTag(HolderLookup.Provider registries, CompoundTag tag) {
        clearContent();
        if (tag == null || !tag.contains("Items")) {
            return;
        }
        ListTag list = tag.getListOrEmpty("Items");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompoundOrEmpty(i);
            int slot = itemTag.getIntOr("Slot", -1);
            if (slot >= 0 && slot < getContainerSize()) {
                ItemStack stack = ItemStackNbtHelper.read(registries, itemTag.getCompoundOrEmpty("Item"));
                setItem(slot, stack);
            }
        }
    }
}
