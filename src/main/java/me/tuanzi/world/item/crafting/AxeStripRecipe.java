package me.tuanzi.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.util.ModLog;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

public class AxeStripRecipe extends ShapelessRecipe {
    private final ItemStackTemplate myResult;
    private final List<Ingredient> myIngredients;

    public static final MapCodec<AxeStripRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.myResult),
                Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.myIngredients)
            )
            .apply(i, AxeStripRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AxeStripRecipe> STREAM_CODEC = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC,
        o -> o.commonInfo,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
        o -> o.bookInfo,
        ItemStackTemplate.STREAM_CODEC,
        o -> o.myResult,
        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
        o -> o.myIngredients,
        AxeStripRecipe::new
    );

    public static final RecipeSerializer<AxeStripRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public AxeStripRecipe(
        Recipe.CommonInfo commonInfo,
        CraftingRecipe.CraftingBookInfo bookInfo,
        ItemStackTemplate result,
        List<Ingredient> ingredients
    ) {
        super(commonInfo, bookInfo, result, ingredients);
        this.myResult = result;
        this.myIngredients = ingredients;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    public ItemStackTemplate getMyResult() {
        return myResult;
    }

    public List<Ingredient> getMyIngredients() {
        return myIngredients;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(this.myIngredients);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != this.myIngredients.size()) {
            return false;
        } else {
            return input.size() == 1 && this.myIngredients.size() == 1
                ? this.myIngredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.myResult.create();
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
            new ShapelessCraftingRecipeDisplay(
                this.myIngredients.stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(this.myResult),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
        );
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ItemTags.AXES)) {
                    ItemStack axeCopy = stack.copy();
                    axeCopy.setCount(1);
                    if (axeCopy.isDamageableItem()) {
                        int currentDamage = axeCopy.getDamageValue();
                        int maxDamage = axeCopy.getMaxDamage();
                        int newDamage = currentDamage + 1;
                        ModLog.debug(String.format("去皮合成配方：斧头 [%s] 消耗 1 点耐久，消耗前耐久: %d/%d，消耗后耐久: %d/%d",
                            axeCopy.getHoverName().getString(), maxDamage - currentDamage, maxDamage, maxDamage - newDamage, maxDamage));
                        if (newDamage < maxDamage) {
                            axeCopy.setDamageValue(newDamage);
                            remaining.set(i, axeCopy);
                        } else {
                            ModLog.debug(String.format("去皮合成配方：斧头 [%s] 耐久耗尽，已损坏消失！",
                                axeCopy.getHoverName().getString()));
                            remaining.set(i, ItemStack.EMPTY);
                        }
                    } else {
                        remaining.set(i, axeCopy);
                    }
                } else {
                    ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
                    remaining.set(i, remainder != null ? remainder.create() : ItemStack.EMPTY);
                }
            }
        }
        return remaining;
    }
}
