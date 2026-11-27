package me.tuanzi.gametest;

import me.tuanzi.init.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

public class BlueprintRecipesGameTest {

    @GameTest
    public void testCompressedBuildSlurryRecipeAndBucketRemainder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingInput validInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.GUNPOWDER), new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.LAVA_BUCKET)
        ));

        Optional<RecipeHolder<CraftingRecipe>> match = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, validInput, level);
        if (match.isEmpty()) {
            helper.fail(Component.literal("Expected compressed_build_slurry crafting recipe to match"));
            return;
        }

        CraftingRecipe recipe = match.get().value();
        ItemStack assembled = recipe.assemble(validInput);
        if (!assembled.is(ModItems.COMPRESSED_BUILD_SLURRY) || assembled.getCount() != 3) {
            helper.fail(Component.literal("Expected 3x compressed_build_slurry, got: " + assembled));
            return;
        }

        NonNullList<ItemStack> remaining = recipe.getRemainingItems(validInput);
        if (remaining.size() != 9) {
            helper.fail(Component.literal("Expected 9 remaining slots, got: " + remaining.size()));
            return;
        }
        int[] bucketIndices = {4, 6, 8};
        for (int i = 0; i < 9; i++) {
            boolean shouldBeBucket = (i == bucketIndices[0] || i == bucketIndices[1] || i == bucketIndices[2]);
            ItemStack rem = remaining.get(i);
            if (shouldBeBucket) {
                if (!rem.is(Items.BUCKET) || rem.getCount() != 1) {
                    helper.fail(Component.literal("Expected empty bucket remainder at slot " + i + ", got: " + rem));
                    return;
                }
            } else if (!rem.isEmpty()) {
                helper.fail(Component.literal("Expected empty remainder at slot " + i + ", got: " + rem));
                return;
            }
        }

        // 错误配方不得匹配（将底部中间火药替换为空气）
        CraftingInput invalidInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.GUNPOWDER), new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.LAVA_BUCKET), ItemStack.EMPTY, new ItemStack(Items.LAVA_BUCKET)
        ));
        if (level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, invalidInput, level).isPresent()) {
            helper.fail(Component.literal("Invalid slurry pattern should not match any recipe"));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testBlankBlueprintRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingInput validInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.DYE.lightBlue()), new ItemStack(Items.PAPER), new ItemStack(Items.DYE.lightBlue()),
                ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY
        ));

        Optional<RecipeHolder<CraftingRecipe>> match = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, validInput, level);
        if (match.isEmpty()) {
            helper.fail(Component.literal("Expected blank_blueprint crafting recipe to match"));
            return;
        }

        CraftingRecipe recipe = match.get().value();
        ItemStack assembled = recipe.assemble(validInput);
        if (!assembled.is(ModItems.BLANK_BLUEPRINT) || assembled.getCount() != 1) {
            helper.fail(Component.literal("Expected 1x blank_blueprint, got: " + assembled));
            return;
        }

        NonNullList<ItemStack> remaining = recipe.getRemainingItems(validInput);
        for (int i = 0; i < remaining.size(); i++) {
            if (!remaining.get(i).isEmpty()) {
                helper.fail(Component.literal("Expected no remaining item at slot " + i + ", got: " + remaining.get(i)));
                return;
            }
        }

        // 错误染料颜色不得匹配
        CraftingInput wrongDyeInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.DYE.blue()), new ItemStack(Items.PAPER), new ItemStack(Items.DYE.blue()),
                ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY
        ));
        if (level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, wrongDyeInput, level).isPresent()) {
            helper.fail(Component.literal("Wrong dye color should not match blank_blueprint recipe"));
            return;
        }

        helper.succeed();
    }
}
