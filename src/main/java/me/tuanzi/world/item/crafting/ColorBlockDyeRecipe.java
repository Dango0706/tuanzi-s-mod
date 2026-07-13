package me.tuanzi.world.item.crafting;

import com.mojang.serialization.MapCodec;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ColorBlockDyeRecipe extends CustomRecipe {
    public static final MapCodec<ColorBlockDyeRecipe> MAP_CODEC = MapCodec.unit(ColorBlockDyeRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ColorBlockDyeRecipe> STREAM_CODEC = StreamCodec.unit(new ColorBlockDyeRecipe());
    public static final RecipeSerializer<ColorBlockDyeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ColorBlockDyeRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int targetCount = 0;
        int bucketCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModBlocks.COLOR_BLOCK.asItem()) || 
                    stack.is(ModBlocks.COLOR_SLAB.asItem()) || 
                    stack.is(ModBlocks.COLOR_STAIRS.asItem())) {
                    targetCount++;
                } else if (stack.is(ModItems.PAINT_BUCKET)) {
                    if (stack.getDamageValue() >= 256) {
                        return false;
                    }
                    bucketCount++;
                } else {
                    return false;
                }
            }
        }
        return targetCount == 1 && bucketCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        ItemStack bucket = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModBlocks.COLOR_BLOCK.asItem()) || 
                    stack.is(ModBlocks.COLOR_SLAB.asItem()) || 
                    stack.is(ModBlocks.COLOR_STAIRS.asItem())) {
                    target = stack;
                } else if (stack.is(ModItems.PAINT_BUCKET)) {
                    bucket = stack;
                }
            }
        }

        if (target.isEmpty() || bucket.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(target.getItem());
        int color = DyedItemColor.getOrDefault(bucket, 0xFFFFFF);
        result.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.PAINT_BUCKET)) {
                    ItemStack bucketCopy = stack.copy();
                    int maxDamage = bucketCopy.getMaxDamage();
                    int currentDamage = bucketCopy.getDamageValue();
                    if (currentDamage + 1 >= maxDamage) {
                        remaining.set(i, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    } else {
                        bucketCopy.setDamageValue(currentDamage + 1);
                        bucketCopy.setCount(1);
                        remaining.set(i, bucketCopy);
                    }
                } else {
                    net.minecraft.world.item.ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
                    remaining.set(i, remainder != null ? remainder.create() : ItemStack.EMPTY);
                }
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
