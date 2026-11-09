package me.tuanzi.client;

import net.minecraft.world.item.ItemStack;

/**
 * 拾荒回廊专属饰品槽客户端缓存
 */
public class CorridorAccessoryClientState {
    private static ItemStack hallStack = ItemStack.EMPTY;
    private static ItemStack roomStack = ItemStack.EMPTY;
    private static ItemStack marketStack = ItemStack.EMPTY;

    public static ItemStack getHallStack() {
        return hallStack;
    }

    public static void setHallStack(ItemStack stack) {
        hallStack = stack != null ? stack : ItemStack.EMPTY;
    }

    public static ItemStack getRoomStack() {
        return roomStack;
    }

    public static void setRoomStack(ItemStack stack) {
        roomStack = stack != null ? stack : ItemStack.EMPTY;
    }

    public static ItemStack getMarketStack() {
        return marketStack;
    }

    public static void setMarketStack(ItemStack stack) {
        marketStack = stack != null ? stack : ItemStack.EMPTY;
    }

    public static void clear() {
        hallStack = ItemStack.EMPTY;
        roomStack = ItemStack.EMPTY;
        marketStack = ItemStack.EMPTY;
    }
}
