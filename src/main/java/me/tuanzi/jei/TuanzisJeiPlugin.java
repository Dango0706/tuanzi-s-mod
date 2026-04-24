package me.tuanzi.jei;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class TuanzisJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() { return PLUGIN_ID; }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), RecipeTypes.ANVIL);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var jeiHelpers = registration.getJeiHelpers();
        var factory = jeiHelpers.getVanillaRecipeFactory();
        List<mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe> recipes = new ArrayList<>();

        ItemStack rawSponge = new ItemStack(ModItems.COLOR_SPONGE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        String sName = Items.DIAMOND_SWORD.getDescriptionId();

        // --- 1. 唯一的重命名示范：基础指令编写 (Hex 示例) ---
        Style redStyle = Style.EMPTY.withColor(0xFF5555);
        ItemStack hexSponge = rawSponge.copy();
        hexSponge.set(DataComponents.CUSTOM_NAME, Component.literal("#FF5555").withStyle(redStyle));
        recipes.add(factory.createAnvilRecipe(List.of(rawSponge), List.of(ItemStack.EMPTY), List.of(hexSponge), 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "anvil/naming_demo")));

        // --- 2. 各类应用效果展示 ---

        // 场景 A: 叠加效果 (#55FF55&l)
        Style mixStyle = Style.EMPTY.withColor(0x55FF55).withBold(true);
        ItemStack mixSponge = rawSponge.copy();
        mixSponge.set(DataComponents.CUSTOM_NAME, Component.literal("#55FF55&l").withStyle(mixStyle));
        ItemStack mixSword = sword.copy();
        mixSword.set(DataComponents.CUSTOM_NAME, Component.translatable(sName).withStyle(mixStyle));
        recipes.add(factory.createAnvilRecipe(List.of(sword), List.of(mixSponge), List.of(mixSword), 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "anvil/dyeing_mix")));

        // 场景 B: 分割系统 (&c,&e,&b) - 修正海绵名字渲染
        ItemStack splitSponge = rawSponge.copy();
        // 构造分段颜色的预览名称
        var spongePreview = Component.empty()
            .append(Component.literal("&c").withStyle(ChatFormatting.RED))
            .append(Component.literal(",").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("&e").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(",").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("&b").withStyle(ChatFormatting.AQUA));
        splitSponge.set(DataComponents.CUSTOM_NAME, spongePreview);
        
        var splitResult = Component.empty()
            .append(Component.literal("钻").withStyle(ChatFormatting.RED))
            .append(Component.literal("石").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("剑").withStyle(ChatFormatting.AQUA));
        ItemStack splitSword = sword.copy();
        splitSword.set(DataComponents.CUSTOM_NAME, splitResult);
        recipes.add(factory.createAnvilRecipe(List.of(sword), List.of(splitSponge), List.of(splitSword), 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "anvil/dyeing_split")));

        // 场景 C: 渐变色 (&q3#FF0000-#0000FF)
        ItemStack gradSponge = rawSponge.copy();
        gradSponge.set(DataComponents.CUSTOM_NAME, Component.literal("&q3#FF0000-#0000FF"));
        var gradResult = Component.empty()
            .append(Component.literal("钻").withStyle(Style.EMPTY.withColor(0xFF0000)))
            .append(Component.literal("石").withStyle(Style.EMPTY.withColor(0x800080)))
            .append(Component.literal("剑").withStyle(Style.EMPTY.withColor(0x0000FF)));
        ItemStack gradSword = sword.copy();
        gradSword.set(DataComponents.CUSTOM_NAME, gradResult);
        recipes.add(factory.createAnvilRecipe(List.of(sword), List.of(gradSponge), List.of(gradSword), 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "anvil/dyeing_grad")));

        // 场景 D: 重置 (&r)
        ItemStack resetSponge = rawSponge.copy();
        resetSponge.set(DataComponents.CUSTOM_NAME, Component.literal("&r"));
        recipes.add(factory.createAnvilRecipe(List.of(mixSword), List.of(resetSponge), List.of(sword), 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "anvil/dyeing_reset")));

        registration.addRecipes(RecipeTypes.ANVIL, recipes);
        
        registration.addIngredientInfo(new ItemStack(ModItems.COLOR_SPONGE), VanillaTypes.ITEM_STACK, 
            Component.translatable("jei.tuanzis_mod.color_sponge.description"));
    }
}
