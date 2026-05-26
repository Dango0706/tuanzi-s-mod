package me.tuanzi.jei;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModEnchantments;
import me.tuanzi.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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

        ItemStack rawSponge = new ItemStack(ModItems.RAINBOW_SPONGE);
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

        // 场景 B: 分割系统 (&c,&e,&b)
        ItemStack splitSponge = rawSponge.copy();
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

        // 手动注册回响破障者的铁砧修复配方，保证 JEI 界面必定展示铁砧修复
        ItemStack damagedEchoBreaker = new ItemStack(ModItems.ECHO_BREAKER);
        damagedEchoBreaker.setDamageValue(1000); // 损坏 1000 点耐久
        ItemStack repairedEchoBreaker = new ItemStack(ModItems.ECHO_BREAKER);
        repairedEchoBreaker.setDamageValue(613); // 一颗心脏修复 25% 耐久 (1550 * 0.25 = 387.5 点，1000 - 387 = 613)
        recipes.add(factory.createAnvilRecipe(
            List.of(damagedEchoBreaker),
            List.of(new ItemStack(ModItems.WARDEN_HEART)),
            List.of(repairedEchoBreaker),
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "anvil/echo_breaker_repair")
        ));

        registration.addRecipes(RecipeTypes.ANVIL, recipes);
        
        registration.addIngredientInfo(new ItemStack(ModItems.RAINBOW_SPONGE), VanillaTypes.ITEM_STACK, 
            Component.translatable("jei.tuanzis_mod.rainbow_sponge.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.YURIS_REVENGE), VanillaTypes.ITEM_STACK,
            Component.translatable("item.tuanzis_mod.yuris_revenge.jei.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.IMMORTAL_TALISMAN), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.immortal_talisman.description"));
        registration.addIngredientInfo(new ItemStack(Items.WARDEN_SPAWN_EGG), VanillaTypes.ITEM_STACK, 
            Component.translatable("item.minecraft.warden_spawn_egg.jei.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.WARDEN_HEART), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.warden_heart.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.ECHO_BREAKER), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.echo_breaker.description"));

        // 获取所有附有“阅历”附魔的附魔书 (Experience I-IV)
        List<ItemStack> experienceBooks = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> stack.is(Items.ENCHANTED_BOOK))
            .filter(stack -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(ModEnchantments.EXPERIENCE)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toList();

        if (!experienceBooks.isEmpty()) {
            registration.addIngredientInfo(experienceBooks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.experience.description"));
        }

        // 获取所有附有“熔炼”附魔的附魔书 (Smelting)
        List<ItemStack> smeltingBooks = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> stack.is(Items.ENCHANTED_BOOK))
            .filter(stack -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(ModEnchantments.SMELTING)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toList();

        if (!smeltingBooks.isEmpty()) {
            registration.addIngredientInfo(smeltingBooks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.smelting.description"));
        }

        // 注册不死药水 1.0.3 更新说明 (针对所有不死药水/药箭)
        List<ItemStack> undyingPotions = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> {
                var potionContents = stack.get(DataComponents.POTION_CONTENTS);
                if (potionContents != null && potionContents.potion().isPresent()) {
                    var potion = potionContents.potion().get().value();
                    var id = BuiltInRegistries.POTION.getKey(potion);
                    return id != null && id.getPath().contains("undying");
                }
                return false;
            })
            .toList();
        
        if (!undyingPotions.isEmpty()) {
            registration.addIngredientInfo(undyingPotions, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.undying_potion.update_103"));
        }

        // 注册无限与经验修补兼容性说明
        List<ItemStack> infinityMendingBooks = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> stack.is(Items.ENCHANTED_BOOK))
            .filter(stack -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(Enchantments.INFINITY) || entry.getKey().is(Enchantments.MENDING)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toList();

        if (!infinityMendingBooks.isEmpty()) {
            registration.addIngredientInfo(infinityMendingBooks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.compatibility.infinity_mending"));
        }

        // 获取所有附有“连锁采掘”附魔的附魔书 (Chain Mining I-IV)
        List<ItemStack> chainMiningBooks = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> stack.is(Items.ENCHANTED_BOOK))
            .filter(stack -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(ModEnchantments.CHAIN_MINING)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toList();

        if (!chainMiningBooks.isEmpty()) {
            registration.addIngredientInfo(chainMiningBooks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.chain_mining.description"));
        }

        // 获取所有附有“血怒”附魔的附魔书 (Blood Rage)
        List<ItemStack> bloodRageBooks = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> stack.is(Items.ENCHANTED_BOOK))
            .filter(stack -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(ModEnchantments.BLOOD_RAGE)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toList();

        if (!bloodRageBooks.isEmpty()) {
            registration.addIngredientInfo(bloodRageBooks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.blood_rage.description"));
        }

        // 获取所有附有“狂战士”附魔的附魔书 (Berserker)
        List<ItemStack> berserkerBooks = registration.getIngredientManager()
            .getAllIngredients(VanillaTypes.ITEM_STACK)
            .stream()
            .filter(stack -> stack.is(Items.ENCHANTED_BOOK))
            .filter(stack -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(ModEnchantments.BERSERKER)) {
                            return true;
                        }
                    }
                }
                return false;
            })
            .toList();

        if (!berserkerBooks.isEmpty()) {
            registration.addIngredientInfo(berserkerBooks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.tuanzis_mod.berserker.description"));
        }

        registration.addIngredientInfo(new ItemStack(ModItems.SCARECROW), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.scarecrow.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.DECOY_TOTEM), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.decoy_totem.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.TRIAL_DUMMY), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.trial_dummy.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.VILLAGER_CAGE), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.villager_cage.description"));
        registration.addIngredientInfo(new ItemStack(me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.soul_merchant_station.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.TRAVELERS_NOTEBOOK), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.travelers_notebook.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.TELEPORTATION_PAPER), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.teleportation_paper.description"));
        registration.addIngredientInfo(new ItemStack(ModItems.SIGNPOST_RUNE), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.tuanzis_mod.signpost_rune.description"));
    }
}
