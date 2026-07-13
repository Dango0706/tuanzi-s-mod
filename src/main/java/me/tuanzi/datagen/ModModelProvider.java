package me.tuanzi.datagen;

import me.tuanzi.init.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.resources.model.sprite.Material;

public class ModModelProvider extends FabricModelProvider {
    private static final net.minecraft.client.data.models.model.ModelTemplate TINTED_CUBE_ALL = new net.minecraft.client.data.models.model.ModelTemplate(
        java.util.Optional.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/tinted_cube_all")),
        java.util.Optional.empty(),
        TextureSlot.ALL
    );
    private static final net.minecraft.client.data.models.model.ModelTemplate TINTED_SLAB_BOTTOM = new net.minecraft.client.data.models.model.ModelTemplate(
        java.util.Optional.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/tinted_slab")),
        java.util.Optional.empty(),
        TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final net.minecraft.client.data.models.model.ModelTemplate TINTED_SLAB_TOP = new net.minecraft.client.data.models.model.ModelTemplate(
        java.util.Optional.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/tinted_slab_top")),
        java.util.Optional.of("_top"),
        TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final net.minecraft.client.data.models.model.ModelTemplate TINTED_STAIRS_STRAIGHT = new net.minecraft.client.data.models.model.ModelTemplate(
        java.util.Optional.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/tinted_stairs")),
        java.util.Optional.empty(),
        TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final net.minecraft.client.data.models.model.ModelTemplate TINTED_STAIRS_INNER = new net.minecraft.client.data.models.model.ModelTemplate(
        java.util.Optional.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/tinted_inner_stairs")),
        java.util.Optional.of("_inner"),
        TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );
    private static final net.minecraft.client.data.models.model.ModelTemplate TINTED_STAIRS_OUTER = new net.minecraft.client.data.models.model.ModelTemplate(
        java.util.Optional.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/tinted_outer_stairs")),
        java.util.Optional.of("_outer"),
        TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE
    );

    public ModModelProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        // 使用代理拦截模式动态在方块模型生成阶段注入带有幽蓝半透明力场屏障的 3D 实体模型
        java.util.function.Function<String, com.google.gson.JsonObject> readModelJson = (filename) -> {
            try {
                java.io.File rootDir = new java.io.File(".").getAbsoluteFile();
                java.io.File srcDir = null;
                for (int i = 0; i < 5; i++) {
                    java.io.File check = new java.io.File(rootDir, "src/main/resources");
                    if (check.exists() && check.isDirectory()) {
                        srcDir = check;
                        break;
                    }
                    rootDir = rootDir.getParentFile();
                    if (rootDir == null) break;
                }
                if (srcDir != null) {
                    java.io.File file = new java.io.File(srcDir, "assets/tuanzis_mod/models/block/" + filename);
                    if (file.exists()) {
                        try (java.io.FileReader reader = new java.io.FileReader(file)) {
                            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        java.util.function.BiConsumer<net.minecraft.resources.Identifier, net.minecraft.client.data.models.model.ModelInstance> decoratedOutput = 
            (id, modelInstance) -> {
                String path = id.getPath();
                if (id.getNamespace().equals("tuanzis_mod") && path.equals("block/soul_merchant_station")) {
                    net.minecraft.client.data.models.model.ModelInstance customInstance = () -> {
                        com.google.gson.JsonObject obj = readModelJson.apply("soul_merchant_station.json");
                        if (obj == null) obj = new com.google.gson.JsonObject();
                        obj.addProperty("parent", "minecraft:block/block");
                        obj.addProperty("render_type", "minecraft:translucent");
                        return obj;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, customInstance);
                } else if (id.getNamespace().equals("tuanzis_mod") && path.equals("block/blueprint_cannon")) {
                    net.minecraft.client.data.models.model.ModelInstance customInstance = () -> {
                        com.google.gson.JsonObject obj = readModelJson.apply("blueprint_cannon.json");
                        if (obj == null) obj = new com.google.gson.JsonObject();
                        obj.addProperty("parent", "minecraft:block/block");
                        return obj;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, customInstance);
                } else if (id.getNamespace().equals("tuanzis_mod") && path.equals("block/blueprint_table")) {
                    net.minecraft.client.data.models.model.ModelInstance customInstance = () -> {
                        com.google.gson.JsonObject obj = readModelJson.apply("blueprint_table.json");
                        if (obj == null) obj = new com.google.gson.JsonObject();
                        obj.addProperty("parent", "minecraft:block/block");
                        return obj;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, customInstance);
                } else if (id.getNamespace().equals("tuanzis_mod") && 
                           (path.equals("item/soul_merchant_station") ||
                            path.equals("item/blueprint_cannon") ||
                            path.equals("item/blueprint_table"))) {
                    net.minecraft.client.data.models.model.ModelInstance customItemInstance = () -> {
                        com.google.gson.JsonObject modelJson = new com.google.gson.JsonObject();
                        String parentName = "tuanzis_mod:" + path.replace("item/", "block/");
                        modelJson.addProperty("parent", parentName);
                        
                        com.google.gson.JsonObject displayJson = new com.google.gson.JsonObject();
                        
                        // thirdperson_righthand
                        com.google.gson.JsonObject thirdpersonRight = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot1 = new com.google.gson.JsonArray();
                        rot1.add(75); rot1.add(45); rot1.add(0);
                        thirdpersonRight.add("rotation", rot1);
                        com.google.gson.JsonArray trans1 = new com.google.gson.JsonArray();
                        trans1.add(0); trans1.add(1.5); trans1.add(0);
                        thirdpersonRight.add("translation", trans1);
                        com.google.gson.JsonArray scale1 = new com.google.gson.JsonArray();
                        scale1.add(0.25); scale1.add(0.25); scale1.add(0.25);
                        thirdpersonRight.add("scale", scale1);
                        displayJson.add("thirdperson_righthand", thirdpersonRight);
                        
                        // thirdperson_lefthand
                        com.google.gson.JsonObject thirdpersonLeft = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot2 = new com.google.gson.JsonArray();
                        rot2.add(75); rot2.add(45); rot2.add(0);
                        thirdpersonLeft.add("rotation", rot2);
                        com.google.gson.JsonArray trans2 = new com.google.gson.JsonArray();
                        trans2.add(0); trans2.add(1.5); trans2.add(0);
                        thirdpersonLeft.add("translation", trans2);
                        com.google.gson.JsonArray scale2 = new com.google.gson.JsonArray();
                        scale2.add(0.25); scale2.add(0.25); scale2.add(0.25);
                        thirdpersonLeft.add("scale", scale2);
                        displayJson.add("thirdperson_lefthand", thirdpersonLeft);
                        
                        // firstperson_righthand
                        com.google.gson.JsonObject firstpersonRight = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot3 = new com.google.gson.JsonArray();
                        rot3.add(0); rot3.add(45); rot3.add(0);
                        firstpersonRight.add("rotation", rot3);
                        com.google.gson.JsonArray trans3 = new com.google.gson.JsonArray();
                        trans3.add(1.13); trans3.add(1.2); trans3.add(1.13);
                        firstpersonRight.add("translation", trans3);
                        com.google.gson.JsonArray scale3 = new com.google.gson.JsonArray();
                        scale3.add(0.25); scale3.add(0.25); scale3.add(0.25);
                        firstpersonRight.add("scale", scale3);
                        displayJson.add("firstperson_righthand", firstpersonRight);
                        
                        // firstperson_lefthand
                        com.google.gson.JsonObject firstpersonLeft = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot4 = new com.google.gson.JsonArray();
                        rot4.add(0); rot4.add(225); rot4.add(0);
                        firstpersonLeft.add("rotation", rot4);
                        com.google.gson.JsonArray trans4 = new com.google.gson.JsonArray();
                        trans4.add(1.13); trans4.add(1.2); trans4.add(1.13);
                        firstpersonLeft.add("translation", trans4);
                        com.google.gson.JsonArray scale4 = new com.google.gson.JsonArray();
                        scale4.add(0.25); scale4.add(0.25); scale4.add(0.25);
                        firstpersonLeft.add("scale", scale4);
                        displayJson.add("firstperson_lefthand", firstpersonLeft);
                        
                        // ground
                        com.google.gson.JsonObject ground = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot5 = new com.google.gson.JsonArray();
                        rot5.add(0); rot5.add(0); rot5.add(0);
                        ground.add("rotation", rot5);
                        com.google.gson.JsonArray trans5 = new com.google.gson.JsonArray();
                        trans5.add(0); trans5.add(3); trans5.add(0);
                        ground.add("translation", trans5);
                        com.google.gson.JsonArray scale5 = new com.google.gson.JsonArray();
                        scale5.add(0.2); scale5.add(0.2); scale5.add(0.2);
                        ground.add("scale", scale5);
                        displayJson.add("ground", ground);
                        
                        // gui
                        com.google.gson.JsonObject gui = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot6 = new com.google.gson.JsonArray();
                        rot6.add(30); rot6.add(225); rot6.add(0);
                        gui.add("rotation", rot6);
                        com.google.gson.JsonArray trans6Arr = new com.google.gson.JsonArray();
                        trans6Arr.add(0); trans6Arr.add(-1.0); trans6Arr.add(0);
                        gui.add("translation", trans6Arr);
                        com.google.gson.JsonArray scale6 = new com.google.gson.JsonArray();
                        scale6.add(0.55); scale6.add(0.55); scale6.add(0.55);
                        gui.add("scale", scale6);
                        displayJson.add("gui", gui);
                        
                        // fixed
                        com.google.gson.JsonObject fixed = new com.google.gson.JsonObject();
                        com.google.gson.JsonArray rot7 = new com.google.gson.JsonArray();
                        rot7.add(0); rot7.add(0); rot7.add(0);
                        fixed.add("rotation", rot7);
                        com.google.gson.JsonArray trans7 = new com.google.gson.JsonArray();
                        trans7.add(0); trans7.add(0); trans7.add(0);
                        fixed.add("translation", trans7);
                        com.google.gson.JsonArray scale7 = new com.google.gson.JsonArray();
                        scale7.add(0.4); scale7.add(0.4); scale7.add(0.4);
                        fixed.add("scale", scale7);
                        displayJson.add("fixed", fixed);
                        
                        modelJson.add("display", displayJson);
                        return modelJson;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, customItemInstance);

                } else if (id.getNamespace().equals("tuanzis_mod") && path.equals("item/paint_bucket")) {
                    net.minecraft.client.data.models.model.ModelInstance bucketInstance = () -> {
                        com.google.gson.JsonObject modelJson = new com.google.gson.JsonObject();
                        modelJson.addProperty("parent", "minecraft:item/generated");
                        
                        com.google.gson.JsonObject textures = new com.google.gson.JsonObject();
                        textures.addProperty("layer0", "tuanzis_mod:item/paint_bucket_under");
                        textures.addProperty("layer1", "tuanzis_mod:item/paint_bucket_over");
                        modelJson.add("textures", textures);
                        
                        com.google.gson.JsonArray tints = new com.google.gson.JsonArray();
                        
                        com.google.gson.JsonObject tint0 = new com.google.gson.JsonObject();
                        tint0.addProperty("type", "minecraft:constant");
                        tint0.addProperty("value", -1);
                        tints.add(tint0);
                        
                        com.google.gson.JsonObject tint1 = new com.google.gson.JsonObject();
                        tint1.addProperty("type", "minecraft:dye");
                        tint1.addProperty("default", -1);
                        tints.add(tint1);
                        
                        modelJson.add("tints", tints);
                        return modelJson;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, bucketInstance);


                } else if (id.getNamespace().equals("tuanzis_mod") && path.equals("item/paint_bucket")) {
                    net.minecraft.client.data.models.model.ModelInstance bucketInstance = () -> {
                        com.google.gson.JsonObject modelJson = new com.google.gson.JsonObject();
                        modelJson.addProperty("parent", "minecraft:item/generated");
                        
                        com.google.gson.JsonObject textures = new com.google.gson.JsonObject();
                        textures.addProperty("layer0", "tuanzis_mod:item/paint_bucket_under");
                        textures.addProperty("layer1", "tuanzis_mod:item/paint_bucket_over");
                        modelJson.add("textures", textures);

                        com.google.gson.JsonArray tints = new com.google.gson.JsonArray();
                        
                        com.google.gson.JsonObject tint0 = new com.google.gson.JsonObject();
                        tint0.addProperty("type", "minecraft:constant");
                        tint0.addProperty("value", -1);
                        tints.add(tint0);

                        com.google.gson.JsonObject tint1 = new com.google.gson.JsonObject();
                        tint1.addProperty("type", "minecraft:dye");
                        tint1.addProperty("default", -1);
                        tints.add(tint1);

                        modelJson.add("tints", tints);
                        return modelJson;
                    };
                    blockStateModelGenerator.modelOutput.accept(id, bucketInstance);
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

        // 显式触发生成包含完美手持 display 的 item 物品模型 JSON
        decoratedOutput.accept(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "item/soul_merchant_station"),
            () -> new com.google.gson.JsonObject()
        );
        decoratedOutput.accept(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "item/blueprint_cannon"),
            () -> new com.google.gson.JsonObject()
        );
        decoratedOutput.accept(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "item/blueprint_table"),
            () -> new com.google.gson.JsonObject()
        );

        // 自动生成蓝图大炮与蓝图桌的简单 blockstate 并关联自定义拦截模型
        temporaryGenerator.createTrivialCube(me.tuanzi.init.ModBlocks.BLUEPRINT_CANNON);
        temporaryGenerator.createTrivialCube(me.tuanzi.init.ModBlocks.BLUEPRINT_TABLE);

        // 自动生成玩家模拟器的方向性方块模型和状态
        TextureMapping simulatorHorizontalTextures = new TextureMapping()
            .put(TextureSlot.TOP, new Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "block/furnace_top")))
            .put(TextureSlot.BOTTOM, new Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "block/furnace_top")))
            .put(TextureSlot.SIDE, new Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/player_simulator_side")))
            .put(TextureSlot.FRONT, new Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/player_simulator_front")));

        TextureMapping simulatorVerticalTextures = new TextureMapping()
            .put(TextureSlot.SIDE, new Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/player_simulator_side")))
            .put(TextureSlot.FRONT, new Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "block/dispenser_front_vertical")));

        MultiVariant horizontalModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR, simulatorHorizontalTextures, blockStateModelGenerator.modelOutput));
        MultiVariant verticalModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR, simulatorVerticalTextures, blockStateModelGenerator.modelOutput));

        blockStateModelGenerator.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR)
                .with(
                    PropertyDispatch.initial(BlockStateProperties.FACING)
                        .select(Direction.DOWN, verticalModel.with(BlockModelGenerators.X_ROT_180))
                        .select(Direction.UP, verticalModel)
                        .select(Direction.NORTH, horizontalModel)
                        .select(Direction.EAST, horizontalModel.with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, horizontalModel.with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, horizontalModel.with(BlockModelGenerators.Y_ROT_270))
                )
        );
        blockStateModelGenerator.registerSimpleItemModel(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR, net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation(me.tuanzi.init.ModBlocks.PLAYER_SIMULATOR));

        // 自动生成彩色方块、半砖、楼梯的模型与状态 (使用自定义带 tintindex 的模型基底)
        net.minecraft.resources.Identifier blockModelId = TINTED_CUBE_ALL.create(me.tuanzi.init.ModBlocks.COLOR_BLOCK, TextureMapping.cube(me.tuanzi.init.ModBlocks.COLOR_BLOCK), blockStateModelGenerator.modelOutput);
        MultiVariant blockVariant = BlockModelGenerators.plainVariant(blockModelId);

        // 注册半砖
        net.minecraft.resources.Identifier slabBottomId = TINTED_SLAB_BOTTOM.create(me.tuanzi.init.ModBlocks.COLOR_SLAB, TextureMapping.cube(me.tuanzi.init.ModBlocks.COLOR_BLOCK), blockStateModelGenerator.modelOutput);
        net.minecraft.resources.Identifier slabTopId = TINTED_SLAB_TOP.create(me.tuanzi.init.ModBlocks.COLOR_SLAB, TextureMapping.cube(me.tuanzi.init.ModBlocks.COLOR_BLOCK), blockStateModelGenerator.modelOutput);
        blockStateModelGenerator.blockStateOutput.accept(
            BlockModelGenerators.createSlab(
                me.tuanzi.init.ModBlocks.COLOR_SLAB, 
                BlockModelGenerators.plainVariant(slabBottomId), 
                BlockModelGenerators.plainVariant(slabTopId), 
                BlockModelGenerators.plainVariant(net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/color_slab_double"))
            )
        );

        // 注册楼梯
        net.minecraft.resources.Identifier stairsStraightId = TINTED_STAIRS_STRAIGHT.create(me.tuanzi.init.ModBlocks.COLOR_STAIRS, TextureMapping.cube(me.tuanzi.init.ModBlocks.COLOR_BLOCK), blockStateModelGenerator.modelOutput);
        net.minecraft.resources.Identifier stairsInnerId = TINTED_STAIRS_INNER.create(me.tuanzi.init.ModBlocks.COLOR_STAIRS, TextureMapping.cube(me.tuanzi.init.ModBlocks.COLOR_BLOCK), blockStateModelGenerator.modelOutput);
        net.minecraft.resources.Identifier stairsOuterId = TINTED_STAIRS_OUTER.create(me.tuanzi.init.ModBlocks.COLOR_STAIRS, TextureMapping.cube(me.tuanzi.init.ModBlocks.COLOR_BLOCK), blockStateModelGenerator.modelOutput);
        blockStateModelGenerator.blockStateOutput.accept(
            BlockModelGenerators.createStairs(
                me.tuanzi.init.ModBlocks.COLOR_STAIRS, 
                BlockModelGenerators.plainVariant(stairsInnerId), 
                BlockModelGenerators.plainVariant(stairsStraightId), 
                BlockModelGenerators.plainVariant(stairsOuterId)
            )
        );

        // 注册彩色方块的 BlockState
        blockStateModelGenerator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(me.tuanzi.init.ModBlocks.COLOR_BLOCK, blockVariant));

        // 显式触发生成油漆桶物品模型 JSON（由代理拦截覆写为双层 tints 局部着色）
        decoratedOutput.accept(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "item/paint_bucket"),
            () -> new com.google.gson.JsonObject()
        );
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        // 使用 Datagen 自动生成 trial_dummy 物品模型 JSON 文件
        itemModelGenerator.generateFlatItem(ModItems.TRIAL_DUMMY, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.TRAVELERS_NOTEBOOK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.CRAFTSMAN_CHARM, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.TELEPORTATION_PAPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SIGNPOST_RUNE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BERSERK_CHARM, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.WOLF_COMMAND, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SHURIKEN, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHER_STEW, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.LOGIC_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.PLAYER_CONTROL_CORE, ModelTemplates.FLAT_ITEM);

        // 抽卡包/箱模型生成
        itemModelGenerator.generateFlatItem(ModItems.STAR_TRAVEL_CARD_PACK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.STAR_TRAVEL_CARD_CHEST, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SAKURA_FESTIVAL_CARD_PACK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SAKURA_FESTIVAL_CARD_CHEST, ModelTemplates.FLAT_ITEM);

        // 潮汐织靴物品模型生成
        itemModelGenerator.generateFlatItem(ModItems.TIDAL_WEAVE_BOOTS, ModelTemplates.FLAT_ITEM);

        // 塑世之笔物品模型生成
        itemModelGenerator.generateFlatItem(ModItems.WORLD_SCULPTORS_PEN, ModelTemplates.FLAT_ITEM);

        // 虚空墨锭物品模型生成
        itemModelGenerator.generateFlatItem(ModItems.VOID_INK_INGOT, ModelTemplates.FLAT_ITEM);

        // 蓝图系统物品扁平模型生成
        itemModelGenerator.generateFlatItem(ModItems.COMPRESSED_BUILD_SLURRY, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BLANK_BLUEPRINT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.STRUCTURE_BLUEPRINT, ModelTemplates.FLAT_ITEM);

        // 注册油漆桶带有双层 tints (常色+染色) 的物品配置
        itemModelGenerator.itemModelOutput.accept(
            ModItems.PAINT_BUCKET,
            net.minecraft.client.data.models.model.ItemModelUtils.tintedModel(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "item/paint_bucket"),
                net.minecraft.client.data.models.model.ItemModelUtils.constantTint(-1),
                new net.minecraft.client.color.item.Dye(-1)
            )
        );

        // 注册彩色方块、半砖、楼梯带有染色 tints 的物品配置
        itemModelGenerator.itemModelOutput.accept(
            me.tuanzi.init.ModBlocks.COLOR_BLOCK.asItem(),
            net.minecraft.client.data.models.model.ItemModelUtils.tintedModel(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/color_block"),
                new net.minecraft.client.color.item.Dye(-1)
            )
        );
        itemModelGenerator.itemModelOutput.accept(
            me.tuanzi.init.ModBlocks.COLOR_SLAB.asItem(),
            net.minecraft.client.data.models.model.ItemModelUtils.tintedModel(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/color_slab"),
                new net.minecraft.client.color.item.Dye(-1)
            )
        );
        itemModelGenerator.itemModelOutput.accept(
            me.tuanzi.init.ModBlocks.COLOR_STAIRS.asItem(),
            net.minecraft.client.data.models.model.ItemModelUtils.tintedModel(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "block/color_stairs"),
                new net.minecraft.client.color.item.Dye(-1)
            )
        );
    }
}
