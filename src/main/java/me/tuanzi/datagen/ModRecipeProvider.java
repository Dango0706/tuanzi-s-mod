package me.tuanzi.datagen;

import me.tuanzi.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

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
            }
        };
    }

    @Override
    public String getName() {
        return "ModRecipeProvider";
    }
}
