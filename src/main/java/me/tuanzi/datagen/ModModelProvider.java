package me.tuanzi.datagen;

import me.tuanzi.init.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        // 不需要生成方块状态模型
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        // 使用 Datagen 自动生成 trial_dummy 物品模型 JSON 文件
        itemModelGenerator.generateFlatItem(ModItems.TRIAL_DUMMY, ModelTemplates.FLAT_ITEM);
    }
}
