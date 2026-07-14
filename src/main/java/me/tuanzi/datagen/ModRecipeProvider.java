package me.tuanzi.datagen;

import me.tuanzi.init.ModItems;
import me.tuanzi.world.item.crafting.ColorBlockDyeRecipe;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.world.item.crafting.ColorBlockShapedRecipe;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput exporter) {
        return new RecipeProvider(registryLookup, exporter) {
            @Override
            public void buildRecipes() {
                shaped(RecipeCategory.COMBAT, ModItems.ECHO_BREAKER)
                    .pattern("EEE")
                    .pattern(" H ")
                    .pattern(" S ")
                    .define('E', Items.ECHO_SHARD)
                    .define('H', ModItems.WARDEN_HEART)
                    .define('S', Items.STICK)
                    .unlockedBy("has_echo_shard", has(Items.ECHO_SHARD))
                    .unlockedBy("has_warden_heart", has(ModItems.WARDEN_HEART))
                    .save(exporter);

                // 稻草人无序合成：干草块 + 木棍 + 雕刻南瓜
                shapeless(RecipeCategory.MISC, ModItems.SCARECROW, 1)
                    .requires(Items.HAY_BLOCK)
                    .requires(Items.STICK)
                    .requires(Items.CARVED_PUMPKIN)
                    .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                    .save(exporter);

                // 假目标有序合成：空,幽匿脉络,空, 幻翼膜,稻草人,幻翼膜, 萤石,红石,萤石
                shaped(RecipeCategory.COMBAT, ModItems.DECOY_TOTEM, 1)
                    .pattern(" C ")
                    .pattern("MSM")
                    .pattern("GRG")
                    .define('C', Items.SCULK_VEIN)
                    .define('M', Items.PHANTOM_MEMBRANE)
                    .define('S', ModItems.SCARECROW)
                    .define('G', Items.GLOWSTONE)
                    .define('R', Items.REDSTONE)
                    .unlockedBy("has_scarecrow", has(ModItems.SCARECROW))
                    .save(exporter);

                // 试炼假人有序合成
                shaped(RecipeCategory.MISC, ModItems.TRIAL_DUMMY, 1)
                    .pattern("HHH")
                    .pattern("SAS")
                    .pattern("HTH")
                    .define('H', Items.HAY_BLOCK)
                    .define('S', Items.STICK)
                    .define('A', ModItems.SCARECROW)
                    .define('T', Items.TARGET)
                    .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                    .unlockedBy("has_scarecrow", has(ModItems.SCARECROW))
                    .save(exporter);
                // 缚灵笼有序合成：铁锭x7 + 绿宝石块 + 灵魂沙
                shaped(RecipeCategory.MISC, ModItems.VILLAGER_CAGE, 1)
                    .pattern("IEI")
                    .pattern("ISI")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('E', Items.EMERALD_BLOCK)
                    .define('S', Items.SOUL_SAND)
                    .unlockedBy("has_soul_sand", has(Items.SOUL_SAND))
                    .unlockedBy("has_emerald_block", has(Items.EMERALD_BLOCK))
                    .save(exporter);

                // 灵笼贸易站有序合成
                shaped(RecipeCategory.MISC, me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION, 1)
                    .pattern("QSQ")
                    .pattern("CRC")
                    .pattern("QSQ")
                    .define('Q', Items.QUARTZ)
                    .define('S', Items.SCULK)
                    .define('C', Items.COMPARATOR)
                    .define('R', ModItems.VILLAGER_CAGE)
                    .unlockedBy("has_villager_cage", has(ModItems.VILLAGER_CAGE))
                    .save(exporter);

                // 逻辑核心有序合成
                shaped(RecipeCategory.MISC, ModItems.LOGIC_CORE, 1)
                    .pattern("RRR")
                    .pattern(" I ")
                    .pattern("RRR")
                    .define('R', Items.REDSTONE)
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy("has_redstone", has(Items.REDSTONE))
                    .save(exporter);

                // 玩家控制核心有序合成
                shaped(RecipeCategory.MISC, ModItems.PLAYER_CONTROL_CORE, 1)
                    .pattern(" R ")
                    .pattern("BH ")
                    .pattern(" R ")
                    .define('R', Items.REDSTONE)
                    .define('B', Items.REDSTONE_BLOCK)
                    .define('H', Items.PLAYER_HEAD)
                    .unlockedBy("has_player_head", has(Items.PLAYER_HEAD))
                    .save(exporter);

                // 玩家模拟器有序合成配方修改
                shaped(RecipeCategory.REDSTONE, me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR, 1)
                    .pattern("ROR")
                    .pattern("PDC")
                    .pattern("ROR")
                    .define('R', Items.REDSTONE)
                    .define('O', Items.OBSERVER)
                    .define('P', ModItems.PLAYER_CONTROL_CORE)
                    .define('D', Items.DISPENSER)
                    .define('C', ModItems.LOGIC_CORE)
                    .unlockedBy("has_player_control_core", has(ModItems.PLAYER_CONTROL_CORE))
                    .unlockedBy("has_logic_core", has(ModItems.LOGIC_CORE))
                    .save(exporter);

                // 玩家模拟器无序净化合成：大炮自身 -> 清除标签大炮自身
                shapeless(RecipeCategory.REDSTONE, me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR, 1)
                    .requires(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR)
                    .unlockedBy("has_player_simulator", has(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR))
                    .save(exporter, "tuanzis_mod:player_simulator_clean");

                // 狂暴护符无序合成：铁锭 + 2个烈焰粉 + 红石
                shapeless(RecipeCategory.MISC, ModItems.BERSERK_CHARM, 1)
                    .requires(Items.IRON_INGOT)
                    .requires(Items.BLAZE_POWDER, 2)
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                    .unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER))
                    .save(exporter);

                // 工匠护符无序合成：1个工作台 + 2个黑曜石 + 1个紫水晶碎片 + 1个下界石英
                shapeless(RecipeCategory.MISC, ModItems.CRAFTSMAN_CHARM, 1)
                    .requires(Items.CRAFTING_TABLE)
                    .requires(Items.OBSIDIAN, 2)
                    .requires(Items.AMETHYST_SHARD)
                    .requires(Items.QUARTZ)
                    .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                    .save(exporter);

                // 战狼护符无序合成
                shapeless(RecipeCategory.COMBAT, ModItems.WOLF_COMMAND, 1)
                    .requires(Items.BONE_BLOCK)
                    .requires(Items.IRON_INGOT, 2)
                    .requires(Items.SCULK_VEIN)
                    .unlockedBy("has_bone_block", has(Items.BONE_BLOCK))
                    .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                    .unlockedBy("has_sculk_vein", has(Items.SCULK_VEIN))
                    .save(exporter);

                // 手里剑有序合成：铁粒在上下左右，石英在中间，产出4枚
                shaped(RecipeCategory.COMBAT, ModItems.SHURIKEN, 4)
                    .pattern(" N ")
                    .pattern("NQN")
                    .pattern(" N ")
                    .define('N', Items.IRON_NUGGET)
                    .define('Q', Items.QUARTZ)
                    .unlockedBy("has_quartz", has(Items.QUARTZ))
                    .save(exporter);

                // 地狱炖菜无序合成：碗 + 绯红菌 + 诡异菌 + 烈焰粉 + 岩浆膏
                shapeless(RecipeCategory.FOOD, ModItems.NETHER_STEW, 1)
                    .requires(Items.BOWL)
                    .requires(Items.CRIMSON_FUNGUS)
                    .requires(Items.WARPED_FUNGUS)
                    .requires(Items.BLAZE_POWDER)
                    .requires(Items.MAGMA_CREAM)
                    .unlockedBy("has_bowl", has(Items.BOWL))
                    .unlockedBy("has_crimson_fungus", has(Items.CRIMSON_FUNGUS))
                    .unlockedBy("has_warped_fungus", has(Items.WARPED_FUNGUS))
                    .save(exporter);

                // 潮汐织靴有序合成：
                shaped(RecipeCategory.COMBAT, ModItems.TIDAL_WEAVE_BOOTS, 1)
                    .pattern(" D ")
                    .pattern("MHM")
                    .pattern("P P")
                    .define('D', Items.DIAMOND_BOOTS)
                    .define('M', Items.NETHERITE_SCRAP)
                    .define('H', Items.HEART_OF_THE_SEA)
                    .define('P', Items.PRISMARINE_CRYSTALS)
                    .unlockedBy("has_diamond_boots", has(Items.DIAMOND_BOOTS))
                    .unlockedBy("has_netherite_scrap", has(Items.NETHERITE_SCRAP))
                    .unlockedBy("has_heart_of_the_sea", has(Items.HEART_OF_THE_SEA))
                    .unlockedBy("has_prismarine_crystals", has(Items.PRISMARINE_CRYSTALS))
                    .save(exporter);

                // 特殊配方：油漆桶染色彩色方块/半砖/楼梯
                SpecialRecipeBuilder.special(ColorBlockDyeRecipe::new).save(exporter, "tuanzis_mod:color_block_dye");

                // 彩色楼梯有序合成继承颜色
                ShapedRecipePattern stairsPattern = ShapedRecipePattern.of(
                    java.util.Map.of('#', Ingredient.of(me.tuanzi.init.ModBlocks.COLOR_BLOCK)),
                    "#  ",
                    "## ",
                    "###"
                );
                ItemStackTemplate stairsResult = new ItemStackTemplate(me.tuanzi.init.ModBlocks.COLOR_STAIRS.asItem(), 4);
                Recipe.CommonInfo stairsInfo = new Recipe.CommonInfo(true);
                CraftingRecipe.CraftingBookInfo stairsBook = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.BUILDING, "");
                ColorBlockShapedRecipe stairsRecipe = new ColorBlockShapedRecipe(stairsInfo, stairsBook, stairsPattern, stairsResult);
                exporter.accept(
                    ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "color_stairs")),
                    stairsRecipe,
                    null
                );

                // 彩色半砖有序合成继承颜色
                ShapedRecipePattern slabPattern = ShapedRecipePattern.of(
                    java.util.Map.of('#', Ingredient.of(me.tuanzi.init.ModBlocks.COLOR_BLOCK)),
                    "###"
                );
                ItemStackTemplate slabResult = new ItemStackTemplate(me.tuanzi.init.ModBlocks.COLOR_SLAB.asItem(), 6);
                Recipe.CommonInfo slabInfo = new Recipe.CommonInfo(true);
                CraftingRecipe.CraftingBookInfo slabBook = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.BUILDING, "");
                ColorBlockShapedRecipe slabRecipe = new ColorBlockShapedRecipe(slabInfo, slabBook, slabPattern, slabResult);
                exporter.accept(
                    ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "color_slab")),
                    slabRecipe,
                    null
                );

                var itemLookup = registryLookup.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM);
                var concretePowderTag = itemLookup.getOrThrow(ItemTags.CONCRETE_POWDERS);

                // 新增彩色方块合成配方（任意颜色混凝土粉末*6 + 水桶*1 + 粘土球*2 => 彩色方块*6，水桶返还）
                shaped(RecipeCategory.BUILDING_BLOCKS, me.tuanzi.init.ModBlocks.COLOR_BLOCK, 6)
                    .pattern("PPP")
                    .pattern("WCW")
                    .pattern("PPP")
                    .define('P', Ingredient.of(concretePowderTag))
                    .define('W', Items.CLAY_BALL)
                    .define('C', Items.WATER_BUCKET)
                    .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                    .save(exporter);

                // 新增油漆桶合成配方
                shaped(RecipeCategory.MISC, ModItems.PAINT_BUCKET, 1)
                    .pattern("YYG")
                    .pattern("KBG")
                    .pattern("KUU")
                    .define('Y', Items.DYE.red())
                    .define('G', Items.DYE.green())
                    .define('K', Items.DYE.black())
                    .define('B', Items.WATER_BUCKET)
                    .define('U', Items.DYE.blue())
                    .unlockedBy("has_water_bucket", has(Items.WATER_BUCKET))
                    .save(exporter);
            }
        };
    }

    @Override
    public String getName() {
        return "ModRecipeProvider";
    }
}
