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
        // 使用代理拦截模式动态在方块模型生成阶段注入带有幽蓝半透明力场屏障的 3D 实体模型
        java.util.function.BiConsumer<net.minecraft.resources.Identifier, net.minecraft.client.data.models.model.ModelInstance> decoratedOutput = 
            (id, modelInstance) -> {
                if (id.getNamespace().equals("tuanzis_mod") && id.getPath().equals("block/soul_merchant_station")) {
                    net.minecraft.client.data.models.model.ModelInstance customInstance = () -> {
                        com.google.gson.JsonObject modelJson = new com.google.gson.JsonObject();
                        modelJson.addProperty("parent", "minecraft:block/block");
                        // 设为半透明渲染类型以允许完美的百分比半透明颜色混合
                        modelJson.addProperty("render_type", "minecraft:translucent");

                        com.google.gson.JsonObject texturesJson = new com.google.gson.JsonObject();
                        texturesJson.addProperty("all", "tuanzis_mod:block/soul_merchant_station");
                        texturesJson.addProperty("particle", "tuanzis_mod:block/soul_merchant_station");
                        modelJson.add("textures", texturesJson);
                        
                        // 创建具有精细 UV 坐标的轻盈镂空仪式圆笼元素列表（已修复顶盖底面漏空透明问题）
                        com.google.gson.JsonArray elementsArray = new com.google.gson.JsonArray();
                        
                        // 准备精密 UV 局部切图（折算为 MC 0-16 网格）
                        double[] base1Down = {4.0, 10.5, 12.0, 16.0};
                        double[] base1Up = {4.0, 7.5, 12.0, 10.0};
                        double[] base1Side = {4.0, 7.5, 12.0, 8.5};
                        
                        double[] base2Up = {5.0, 8.0, 11.0, 9.5};
                        double[] base2Side = {5.0, 8.0, 11.0, 8.5};
                        
                        // 顶盘第一层底面（Y=13）是玩家从下往上看直接目视的天花板。
                        // 原先使用的 cap1Down 含有大范围透明背景拉伸，导致从底往上看时显示为一个全透明大窟窿。
                        // 现精准变更为 base1Up（与底盘台面一致的完全实心不透明石质/金属纹理），以彻底解决漏空透明 Bug。
                        double[] cap1Down = {4.0, 7.5, 12.0, 10.0}; 
                        double[] cap1Side = {5.5, 0.5, 10.5, 1.0};
                        
                        double[] cap2Side = {6.0, 0.0, 10.0, 0.5};
                        
                        double[] cap3Up = {5.5, 0.0, 10.5, 2.0};
                        double[] cap3Side = {6.5, 0.0, 9.5, 0.5};
                        
                        double[] pillarNwDown = {5.0, 6.0, 6.0, 7.5};
                        double[] pillarNwUp = {5.0, 2.0, 6.0, 3.5};
                        double[] pillarNwSide = {5.0, 2.0, 6.0, 7.5};
                        
                        double[] pillarNeDown = {11.0, 6.0, 12.0, 7.5};
                        double[] pillarNeUp = {11.0, 2.0, 12.0, 3.5};
                        double[] pillarNeSide = {11.0, 2.0, 12.0, 7.5};
                        
                        double[] coreDown = {6.0, 4.5, 10.0, 6.5};
                        double[] coreUp = {6.0, 4.5, 10.0, 6.5};
                        double[] coreSide = {6.0, 4.5, 10.0, 5.0};
                        
                        // 幽蓝半透明科幻力场屏障专属 UV 贴图（X: 3.0 ~ 13.0, Y: 11.0 ~ 15.0）
                        double[] shieldUv = {3.0, 11.0, 13.0, 15.0};

                        // 1. 底座第一层 Base 1 (14x2x14, Y: 0 ~ 2)
                        elementsArray.add(createBoxElement("base_1", 1, 0, 1, 15, 2, 15, 
                            base1Down, base1Up, base1Side, base1Side, base1Side, base1Side));
                            
                        // 2. 底座第二层 Base 2 (12x1x12, Y: 2 ~ 3)
                        elementsArray.add(createBoxElement("base_2", 2, 2, 2, 14, 3, 14, 
                            base1Down, base2Up, base2Side, base2Side, base2Side, base2Side));
                            
                        // 3. 顶盖第一层 Cap 1 (12x1x12, Y: 13 ~ 14)
                        elementsArray.add(createBoxElement("cap_1", 2, 13, 2, 14, 14, 14, 
                            cap1Down, base2Up, cap1Side, cap1Side, cap1Side, cap1Side));
                            
                        // 4. 顶盖第二层 Cap 2 (10x1x10, Y: 14 ~ 15)
                        elementsArray.add(createBoxElement("cap_2", 3, 14, 3, 13, 15, 13, 
                            cap1Down, base2Up, cap2Side, cap2Side, cap2Side, cap2Side));
                            
                        // 5. 顶盖第三层 Cap 3 (6x1x6, Y: 15 ~ 16)
                        elementsArray.add(createBoxElement("cap_3", 5, 15, 5, 11, 16, 11, 
                            cap1Down, cap3Up, cap3Side, cap3Side, cap3Side, cap3Side));
                            
                        // 6. 西北角精致细柱 (1.5x10x1.5, Y: 3 ~ 13)
                        elementsArray.add(createBoxElement("pillar_nw", 2.0, 3, 2.0, 3.5, 13, 3.5, 
                            pillarNwDown, pillarNwUp, pillarNwSide, pillarNwSide, pillarNwSide, pillarNwSide));
                            
                        // 7. 东北角精致细柱 (1.5x10x1.5, Y: 3 ~ 13)
                        elementsArray.add(createBoxElement("pillar_ne", 12.5, 3, 2.0, 14.0, 13, 3.5, 
                            pillarNeDown, pillarNeUp, pillarNeSide, pillarNeSide, pillarNeSide, pillarNeSide));
                            
                        // 8. 西南角精致细柱 (1.5x10x1.5, Y: 3 ~ 13)
                        elementsArray.add(createBoxElement("pillar_sw", 2.0, 3, 12.5, 3.5, 13, 14.0, 
                            pillarNwDown, pillarNwUp, pillarNwSide, pillarNwSide, pillarNwSide, pillarNwSide));
                            
                        // 9. 东南角精致细柱 (1.5x10x1.5, Y: 3 ~ 13)
                        elementsArray.add(createBoxElement("pillar_se", 12.5, 3, 12.5, 14.0, 13, 14.0, 
                            pillarNeDown, pillarNeUp, pillarNeSide, pillarNeSide, pillarNeSide, pillarNeSide));
                            
                        // 10. 微型中央悬浮融合台 Core Matrix (4x1x4, Y: 5 ~ 6)
                        elementsArray.add(createBoxElement("inner_core", 6, 5, 6, 10, 6, 10, 
                            coreDown, coreUp, coreSide, coreSide, coreSide, coreSide));
                            
                        // ==========================================
                        // 新增四面幽蓝科幻半透明能量力场光幕 (厚度 0.1, 封在角柱之间)
                        // ==========================================
                        // 11. 北面屏障 (连接西北/东北柱, Z: 2.5)
                        elementsArray.add(createBoxElement("shield_north", 3.5, 3, 2.45, 12.5, 13, 2.55,
                            shieldUv, shieldUv, shieldUv, shieldUv, shieldUv, shieldUv));
                            
                        // 12. 南面屏障 (连接西南/东南柱, Z: 13.5)
                        elementsArray.add(createBoxElement("shield_south", 3.5, 3, 13.45, 12.5, 13, 13.55,
                            shieldUv, shieldUv, shieldUv, shieldUv, shieldUv, shieldUv));
                            
                        // 13. 西面屏障 (连接西北/西南柱, X: 2.5)
                        elementsArray.add(createBoxElement("shield_west", 2.45, 3, 3.5, 2.55, 13, 12.5,
                            shieldUv, shieldUv, shieldUv, shieldUv, shieldUv, shieldUv));
                            
                        // 14. 东面屏障 (连接东北/东南柱, X: 13.5)
                        elementsArray.add(createBoxElement("shield_east", 13.45, 3, 3.5, 13.55, 13, 12.5,
                            shieldUv, shieldUv, shieldUv, shieldUv, shieldUv, shieldUv));

                        modelJson.add("elements", elementsArray);
                        
                        return modelJson;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, customInstance);
                } else {
                    blockStateModelGenerator.modelOutput.accept(id, modelInstance);
                }
            };

        // 创建临时生成器代理原本 of 输出
        BlockModelGenerators temporaryGenerator = new BlockModelGenerators(
            blockStateModelGenerator.blockStateOutput,
            blockStateModelGenerator.itemModelOutput,
            decoratedOutput
        );

        // 调用生成器以生成标准的方块状态和拦截后的完美 3D 立体模型
        temporaryGenerator.createTrivialCube(me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION);
    }

    /**
     * 自动构建 Minecraft 方块模型中 element 几何立方体组件的辅助方法 (包含极其真实的 UV 投影算法)
     */
    private static com.google.gson.JsonObject createBoxElement(
        String name,
        double x1, double y1, double z1, 
        double x2, double y2, double z2,
        double[] downUv, double[] upUv,
        double[] northUv, double[] southUv,
        double[] westUv, double[] eastUv
    ) {
        com.google.gson.JsonObject element = new com.google.gson.JsonObject();
        element.addProperty("name", name);
        
        com.google.gson.JsonArray fromArray = new com.google.gson.JsonArray();
        fromArray.add(x1); fromArray.add(y1); fromArray.add(z1);
        element.add("from", fromArray);
        
        com.google.gson.JsonArray toArray = new com.google.gson.JsonArray();
        toArray.add(x2); toArray.add(y2); toArray.add(z2);
        element.add("to", toArray);
        
        com.google.gson.JsonObject faces = new com.google.gson.JsonObject();
        String[] directions = {"down", "up", "north", "south", "west", "east"};
        double[][] uvs = {downUv, upUv, northUv, southUv, westUv, eastUv};
        
        for (int i = 0; i < directions.length; i++) {
            String dir = directions[i];
            double[] uv = uvs[i];
            
            com.google.gson.JsonObject face = new com.google.gson.JsonObject();
            face.addProperty("texture", "#all");
            
            if (uv != null && uv.length == 4) {
                com.google.gson.JsonArray uvArray = new com.google.gson.JsonArray();
                uvArray.add(uv[0]); uvArray.add(uv[1]); uvArray.add(uv[2]); uvArray.add(uv[3]);
                face.add("uv", uvArray);
            }
            
            // 智能设置相邻面遮挡剔除 (Cullface)，优化面渲染性能与底层黑洞剔除
            if (dir.equals("down") && y1 == 0) {
                face.addProperty("cullface", "down");
            }
            if (dir.equals("up") && y2 == 16) {
                face.addProperty("cullface", "up");
            }
            
            faces.add(dir, face);
        }
        element.add("faces", faces);

        return element;
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        // 使用 Datagen 自动生成 trial_dummy 物品模型 JSON 文件
        itemModelGenerator.generateFlatItem(ModItems.TRIAL_DUMMY, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.TRAVELERS_NOTEBOOK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.TELEPORTATION_PAPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SIGNPOST_RUNE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BERSERK_CHARM, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.WOLF_COMMAND, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SHURIKEN, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHER_STEW, ModelTemplates.FLAT_ITEM);
    }
}
