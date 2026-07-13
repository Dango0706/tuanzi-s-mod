package me.tuanzi.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.init.ModBlocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class ColorBlockShapedRecipe extends ShapedRecipe {
    private final ShapedRecipePattern myPattern;
    private final ItemStackTemplate myResult;

    public static final MapCodec<ColorBlockShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.myPattern),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.myResult)
            )
            .apply(i, ColorBlockShapedRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ColorBlockShapedRecipe> STREAM_CODEC = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC,
        o -> o.commonInfo,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
        o -> o.bookInfo,
        ShapedRecipePattern.STREAM_CODEC,
        o -> o.myPattern,
        ItemStackTemplate.STREAM_CODEC,
        o -> o.myResult,
        ColorBlockShapedRecipe::new
    );
    public static final RecipeSerializer<ColorBlockShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ColorBlockShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.myPattern = pattern;
        this.myResult = result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        if (result.isEmpty()) return result;

        long rSum = 0;
        long gSum = 0;
        long bSum = 0;
        int colorCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModBlocks.COLOR_BLOCK.asItem()) || 
                    stack.is(ModBlocks.COLOR_SLAB.asItem()) || 
                    stack.is(ModBlocks.COLOR_STAIRS.asItem())) {
                    DyedItemColor dye = stack.get(DataComponents.DYED_COLOR);
                    int rgb = dye != null ? dye.rgb() : 0xFFFFFF;
                    rSum += (rgb >> 16) & 0xFF;
                    gSum += (rgb >> 8) & 0xFF;
                    bSum += rgb & 0xFF;
                    colorCount++;
                }
            }
        }

        if (colorCount > 0) {
            int r = (int)(rSum / colorCount);
            int g = (int)(gSum / colorCount);
            int b = (int)(bSum / colorCount);
            int finalColor = (r << 16) | (g << 8) | b;
            result.set(DataComponents.DYED_COLOR, new DyedItemColor(finalColor));
        }

        return result;
    }
}
