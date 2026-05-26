package me.tuanzi.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public class ItemStackNbtHelper {
    public static ItemStack read(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.OPTIONAL_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag)
                .result()
                .orElse(ItemStack.EMPTY);
    }

    public static CompoundTag write(HolderLookup.Provider registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack)
                .getOrThrow();
    }
}
