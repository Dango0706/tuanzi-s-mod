package me.tuanzi.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LitematicaParser {

    /**
     * 兼容解析任意 NBT Tag 形式的方块状态：
     * 1) MC 26.3 BlockState.CODEC 默认状态 StringTag ("minecraft:stone_bricks")
     * 2) MC 26.3 BlockState.CODEC 带属性 CompoundTag ({id, properties})
     * 3) 传统 Litematica CompoundTag ({Name, Properties})
     */
    public static BlockState parseBlockStateFromTag(Tag rawTag) {
        if (rawTag == null) {
            return Blocks.AIR.defaultBlockState();
        }
        if (rawTag instanceof StringTag strTag) {
            String stateStr = strTag.asString().orElse("");
            if (stateStr.isEmpty()) {
                return Blocks.AIR.defaultBlockState();
            }
            if (stateStr.indexOf('[') >= 0) {
                return parseBlockStateFromNbt(convertStringToBlockStateNbt(stateStr));
            }
            Identifier identifier = Identifier.tryParse(stateStr);
            if (identifier == null) {
                return Blocks.AIR.defaultBlockState();
            }
            return BuiltInRegistries.BLOCK.getOptional(identifier)
                    .map(Block::defaultBlockState)
                    .orElse(Blocks.AIR.defaultBlockState());
        }
        if (rawTag instanceof CompoundTag compoundTag) {
            return parseBlockStateFromNbt(compoundTag);
        }
        return BlockState.CODEC.parse(NbtOps.INSTANCE, rawTag)
                .result()
                .orElse(Blocks.AIR.defaultBlockState());
    }

    /**
     * 兼容解析 MC 26.3 原生 BlockState.CODEC ({id, properties}) 与传统 Litematica ({Name, Properties}) 格式。
     */
    public static BlockState parseBlockStateFromNbt(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        String name = tag.getStringOr("id", "");
        if (name.isEmpty()) {
            name = tag.getStringOr("Name", "");
        }
        if (name.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }

        Identifier identifier = Identifier.tryParse(name);
        if (identifier == null) {
            return Blocks.AIR.defaultBlockState();
        }

        Optional<Block> optBlock = BuiltInRegistries.BLOCK.getOptional(identifier);
        if (optBlock.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }

        Block block = optBlock.get();
        BlockState state = block.defaultBlockState();

        CompoundTag propertiesTag = null;
        if (tag.contains("properties")) {
            propertiesTag = tag.getCompoundOrEmpty("properties");
        } else if (tag.contains("Properties")) {
            propertiesTag = tag.getCompoundOrEmpty("Properties");
        }

        if (propertiesTag != null && !propertiesTag.isEmpty()) {
            StateDefinition<Block, BlockState> definition = block.getStateDefinition();
            for (String key : propertiesTag.keySet()) {
                Property<?> prop = definition.getProperty(key);
                if (prop != null) {
                    state = setPropertyValue(state, prop, propertiesTag.getStringOr(key, ""));
                }
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> prop, String valStr) {
        Optional<T> val = prop.getValue(valStr);
        return val.map(t -> state.setValue(prop, t)).orElse(state);
    }

    /**
     * 将 Litematica / Schematic NBT 格式统一解析为模组自定义的结构蓝图 NBT 格式。
     * 自定义蓝图 NBT 结构：
     * {
     * Width: int,
     * Height: int,
     * Length: int,
     * Palette: [BlockState NBTs],
     * Blocks: int[] (大小为 Width * Height * Length),
     * BlockEntities: [ {pos: [x,y,z], nbt: CompoundTag} ]
     * }
     */
    public static CompoundTag parseLitematic(CompoundTag rootTag) {
        CompoundTag result = new CompoundTag();
        if (!rootTag.contains("Regions")) {
            return null;
        }

        CompoundTag regions = rootTag.getCompoundOrEmpty("Regions");
        if (regions.isEmpty()) {
            return null;
        }

        // 只取第一个 Region
        String firstKey = regions.keySet().iterator().next();
        CompoundTag region = regions.getCompoundOrEmpty(firstKey);

        CompoundTag size = region.getCompoundOrEmpty("Size");
        int width = Math.abs(size.getIntOr("x", 0));
        int height = Math.abs(size.getIntOr("y", 0));
        int length = Math.abs(size.getIntOr("z", 0));

        result.putInt("Width", width);
        result.putInt("Height", height);
        result.putInt("Length", length);

        ListTag blockStatePalette = region.getListOrEmpty("BlockStatePalette");
        int paletteSize = blockStatePalette.size();

        // 转换 Palette（统一输出为 MC 26.3 BlockState.CODEC 兼容的 id + properties 格式，同时兼容输入 Name/id）
        ListTag resultPalette = new ListTag();
        for (int i = 0; i < paletteSize; i++) {
            CompoundTag litematicaState = blockStatePalette.getCompoundOrEmpty(i);
            CompoundTag blockStateNbt = new CompoundTag();
            String stateName = litematicaState.getStringOr("Name", "");
            if (stateName.isEmpty()) {
                stateName = litematicaState.getStringOr("id", "minecraft:air");
            }
            blockStateNbt.putString("id", stateName);
            if (litematicaState.contains("Properties")) {
                blockStateNbt.put("properties", litematicaState.getCompoundOrEmpty("Properties").copy());
            } else if (litematicaState.contains("properties")) {
                blockStateNbt.put("properties", litematicaState.getCompoundOrEmpty("properties").copy());
            }
            resultPalette.add(blockStateNbt);
        }
        result.put("Palette", resultPalette);

        // Bit Unpacking BlockStates
        long[] longArray = region.getLongArray("BlockStates").orElse(new long[0]);
        int totalBlocks = width * height * length;
        int[] blocks = new int[totalBlocks];

        if (paletteSize > 1) {
            int bits = Math.max(2, (int) Math.ceil(Math.log(paletteSize) / Math.log(2)));
            long mask = (1L << bits) - 1;
            for (int i = 0; i < totalBlocks; i++) {
                long bitOffset = (long) i * bits;
                int firstLongIdx = (int) (bitOffset / 64);
                int bitShift = (int) (bitOffset % 64);
                if (firstLongIdx < longArray.length) {
                    long value = longArray[firstLongIdx] >>> bitShift;
                    if (bitShift + bits > 64 && firstLongIdx + 1 < longArray.length) {
                        value |= longArray[firstLongIdx + 1] << (64 - bitShift);
                    }
                    blocks[i] = (int) (value & mask);
                } else {
                    blocks[i] = 0;
                }
            }
        } else {
            Arrays.fill(blocks, 0);
        }
        result.putIntArray("Blocks", blocks);

        // 方块实体数据
        ListTag tileEntities = region.getListOrEmpty("TileEntities");
        ListTag resultEntities = new ListTag();
        for (int i = 0; i < tileEntities.size(); i++) {
            CompoundTag te = tileEntities.getCompoundOrEmpty(i);
            CompoundTag entityNbt = new CompoundTag();
            // 在 Litematica 中，x, y, z 通常是相对或绝对坐标。我们需要把它们转为以 (0,0,0) 为起点的 Region 局部坐标。
            // 局部坐标 = 世界绝对坐标 - Region 起点坐标。
            // Region 里面包含 Position 字段，代表该 Region 的世界起点。
            CompoundTag regionPos = region.getCompoundOrEmpty("Position");
            int regionX = regionPos.getIntOr("x", 0);
            int regionY = regionPos.getIntOr("y", 0);
            int regionZ = regionPos.getIntOr("z", 0);

            int teX = te.getIntOr("x", 0) - regionX;
            int teY = te.getIntOr("y", 0) - regionY;
            int teZ = te.getIntOr("z", 0) - regionZ;

            // 范围修正（如果是负数，可能说明 size.x/y/z 是负数，即反向划定区域）
            CompoundTag sizeTag = region.getCompoundOrEmpty("Size");
            if (sizeTag.getIntOr("x", 0) < 0) teX = -teX - 1;
            if (sizeTag.getIntOr("y", 0) < 0) teY = -teY - 1;
            if (sizeTag.getIntOr("z", 0) < 0) teZ = -teZ - 1;

            entityNbt.putIntArray("pos", new int[]{teX, teY, teZ});
            CompoundTag innerNbt = te.copy();
            // 剔除绝对坐标，免得放置时冲突
            innerNbt.remove("x");
            innerNbt.remove("y");
            innerNbt.remove("z");
            entityNbt.put("nbt", innerNbt);
            resultEntities.add(entityNbt);
        }
        result.put("BlockEntities", resultEntities);

        // 生物/实体数据解析
        if (region.contains("Entities")) {
            ListTag entities = region.getListOrEmpty("Entities");
            ListTag resultEntitiesList = new ListTag();
            CompoundTag regionPos = region.getCompoundOrEmpty("Position");
            int regionX = regionPos.getIntOr("x", 0);
            int regionY = regionPos.getIntOr("y", 0);
            int regionZ = regionPos.getIntOr("z", 0);
            CompoundTag sizeTag = region.getCompoundOrEmpty("Size");

            for (int i = 0; i < entities.size(); i++) {
                CompoundTag entity = entities.getCompoundOrEmpty(i);
                String id = entity.getStringOr("id", "");
                if (me.tuanzi.block.cannon.BlueprintEntityHelper.isIgnoredEntity(id)) {
                    continue;
                }

                CompoundTag entityNbt = entity.copy();

                // 获取实体的世界绝对坐标 Pos
                ListTag posList = entity.getListOrEmpty("Pos");
                if (posList.size() >= 3) {
                    double ex = posList.getDoubleOr(0, 0.0);
                    double ey = posList.getDoubleOr(1, 0.0);
                    double ez = posList.getDoubleOr(2, 0.0);

                    double rx = ex - regionX;
                    double ry = ey - regionY;
                    double rz = ez - regionZ;

                    if (sizeTag.getIntOr("x", 0) < 0) rx = -rx;
                    if (sizeTag.getIntOr("y", 0) < 0) ry = -ry;
                    if (sizeTag.getIntOr("z", 0) < 0) rz = -rz;

                    ListTag relativePos = new ListTag();
                    relativePos.add(DoubleTag.valueOf(rx));
                    relativePos.add(DoubleTag.valueOf(ry));
                    relativePos.add(DoubleTag.valueOf(rz));
                    entityNbt.put("Pos", relativePos);
                }

                // 转换附着类实体的 block_pos
                if (entity.contains("block_pos")) {
                    int[] bp = entity.getIntArray("block_pos").orElse(new int[0]);
                    if (bp.length == 3) {
                        int bpx = bp[0] - regionX;
                        int bpy = bp[1] - regionY;
                        int bpz = bp[2] - regionZ;
                        if (sizeTag.getIntOr("x", 0) < 0) bpx = -bpx - 1;
                        if (sizeTag.getIntOr("y", 0) < 0) bpy = -bpy - 1;
                        if (sizeTag.getIntOr("z", 0) < 0) bpz = -bpz - 1;
                        entityNbt.putIntArray("block_pos", new int[]{bpx, bpy, bpz});
                    }
                } else if (entity.contains("TileX") && entity.contains("TileY") && entity.contains("TileZ")) {
                    int tx = entity.getIntOr("TileX", 0) - regionX;
                    int ty = entity.getIntOr("TileY", 0) - regionY;
                    int tz = entity.getIntOr("TileZ", 0) - regionZ;
                    if (sizeTag.getIntOr("x", 0) < 0) tx = -tx - 1;
                    if (sizeTag.getIntOr("y", 0) < 0) ty = -ty - 1;
                    if (sizeTag.getIntOr("z", 0) < 0) tz = -tz - 1;
                    entityNbt.putIntArray("block_pos", new int[]{tx, ty, tz});
                }

                // 剔除 UUID，避免放置冲突
                entityNbt.remove("UUID");
                resultEntitiesList.add(entityNbt);
            }
            result.put("Entities", resultEntitiesList);
        }

        ModLog.debug("[LitematicaParser] parseLitematic 完成: 尺寸=" + width + "x" + height + "x" + length + ", 调色板=" + paletteSize + ", 总格数=" + totalBlocks);
        return result;
    }

    public static CompoundTag parseSchematic(CompoundTag rootTag) {
        CompoundTag result = new CompoundTag();
        if (!rootTag.contains("Width") || !rootTag.contains("Height") || !rootTag.contains("Length")) {
            return null;
        }

        int width = rootTag.getShortOr("Width", (short) 0);
        int height = rootTag.getShortOr("Height", (short) 0);
        int length = rootTag.getShortOr("Length", (short) 0);

        result.putInt("Width", width);
        result.putInt("Height", height);
        result.putInt("Length", length);

        // Sponge Schematic 的 Palette 是一个名字到索引映射的 CompoundTag
        CompoundTag palette = rootTag.getCompoundOrEmpty("Palette");
        // 把映射倒过来变成数组
        Map<Integer, String> indexToName = new HashMap<>();
        for (String key : palette.keySet()) {
            int idx = palette.getIntOr(key, 0);
            indexToName.put(idx, key);
        }

        ListTag resultPalette = new ListTag();
        int maxIndex = indexToName.keySet().stream().max(Integer::compare).orElse(0);
        for (int i = 0; i <= maxIndex; i++) {
            String stateStr = indexToName.getOrDefault(i, "minecraft:air");
            // Sponge 的调色板是字符串，如 "minecraft:stone" 或 "minecraft:oak_stairs[facing=east]"
            // 我们需要把它们转为 Name + Properties 节点以与 Codec 一致。
            resultPalette.add(convertStringToBlockStateNbt(stateStr));
        }
        result.put("Palette", resultPalette);

        // BlockData 是 VarInt 数组
        byte[] blockData = rootTag.getByteArray("BlockData").orElse(new byte[0]);
        int[] blocks = new int[width * height * length];
        int dataIdx = 0;
        for (int i = 0; i < blocks.length && dataIdx < blockData.length; i++) {
            int value = 0;
            int shift = 0;
            while (dataIdx < blockData.length) {
                byte b = blockData[dataIdx++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    break;
                }
                shift += 7;
            }
            blocks[i] = value;
        }
        result.putIntArray("Blocks", blocks);

        // 方块实体数据
        ListTag blockEntities = rootTag.getListOrEmpty("BlockEntities");
        ListTag resultEntities = new ListTag();
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag te = blockEntities.getCompoundOrEmpty(i);
            CompoundTag entityNbt = new CompoundTag();
            int[] pos = te.getIntArray("Pos").orElse(new int[0]);
            if (pos.length == 3) {
                entityNbt.putIntArray("pos", pos);
                CompoundTag innerNbt = te.getCompoundOrEmpty("Data").copy();
                // 剔除绝对坐标
                innerNbt.remove("x");
                innerNbt.remove("y");
                innerNbt.remove("z");
                entityNbt.put("nbt", innerNbt);
                resultEntities.add(entityNbt);
            }
        }
        result.put("BlockEntities", resultEntities);

        ModLog.debug("[LitematicaParser] parseSchematic 完成: 尺寸=" + width + "x" + height + "x" + length + ", 调色板=" + resultPalette.size());
        return result;
    }

    public static CompoundTag packLitematic(CompoundTag blueprintTag, String name) {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 5);

        CompoundTag metadata = new CompoundTag();
        metadata.putString("Name", name);
        metadata.putLong("TimeCreated", System.currentTimeMillis());
        root.put("Metadata", metadata);

        CompoundTag regions = new CompoundTag();
        CompoundTag region = new CompoundTag();

        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);

        CompoundTag size = new CompoundTag();
        size.putInt("x", width);
        size.putInt("y", height);
        size.putInt("z", length);
        region.put("Size", size);

        CompoundTag position = new CompoundTag();
        position.putInt("x", 0);
        position.putInt("y", 0);
        position.putInt("z", 0);
        region.put("Position", position);

        ListTag palette = blueprintTag.getListOrEmpty("Palette");
        ListTag blockStatePalette = new ListTag();
        for (int i = 0; i < palette.size(); i++) {
            Tag rawState = palette.get(i);
            CompoundTag litematicaState = new CompoundTag();
            if (rawState instanceof StringTag strTag) {
                String rawStr = strTag.asString().orElse("minecraft:air");
                int bracketIdx = rawStr.indexOf('[');
                if (bracketIdx == -1) {
                    litematicaState.putString("Name", rawStr.isEmpty() ? "minecraft:air" : rawStr);
                } else {
                    CompoundTag converted = convertStringToBlockStateNbt(rawStr);
                    litematicaState.putString("Name", converted.getStringOr("id", "minecraft:air"));
                    if (converted.contains("properties")) {
                        litematicaState.put("Properties", converted.getCompoundOrEmpty("properties"));
                    }
                }
            } else if (rawState instanceof CompoundTag state) {
                String stateName = state.getStringOr("id", "");
                if (stateName.isEmpty()) {
                    stateName = state.getStringOr("Name", "minecraft:air");
                }
                litematicaState.putString("Name", stateName);
                if (state.contains("properties")) {
                    litematicaState.put("Properties", state.getCompoundOrEmpty("properties").copy());
                } else if (state.contains("Properties")) {
                    litematicaState.put("Properties", state.getCompoundOrEmpty("Properties").copy());
                }
            } else {
                litematicaState.putString("Name", "minecraft:air");
            }
            blockStatePalette.add(litematicaState);
        }
        region.put("BlockStatePalette", blockStatePalette);

        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        int totalBlocks = width * height * length;
        int paletteSize = palette.size();

        if (paletteSize > 1) {
            int bits = Math.max(2, (int) Math.ceil(Math.log(paletteSize) / Math.log(2)));
            int longsCount = (int) Math.ceil((double) totalBlocks * bits / 64.0);
            long[] longArray = new long[longsCount];
            for (int i = 0; i < totalBlocks; i++) {
                if (i >= blocks.length) break;
                int val = blocks[i];
                long bitOffset = (long) i * bits;
                int firstLongIdx = (int) (bitOffset / 64);
                int bitShift = (int) (bitOffset % 64);
                longArray[firstLongIdx] |= ((long) val) << bitShift;
                if (bitShift + bits > 64 && firstLongIdx + 1 < longsCount) {
                    longArray[firstLongIdx + 1] |= ((long) val) >>> (64 - bitShift);
                }
            }
            region.putLongArray("BlockStates", longArray);
        } else {
            region.putLongArray("BlockStates", new long[0]);
        }

        // 方块实体数据
        ListTag tileEntities = new ListTag();
        ListTag blockEntities = blueprintTag.getListOrEmpty("BlockEntities");
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag te = blockEntities.getCompoundOrEmpty(i);
            CompoundTag litematicaTe = te.getCompoundOrEmpty("nbt").copy();
            int[] pos = te.getIntArray("pos").orElse(new int[0]);
            if (pos.length == 3) {
                litematicaTe.putInt("x", pos[0]);
                litematicaTe.putInt("y", pos[1]);
                litematicaTe.putInt("z", pos[2]);
                tileEntities.add(litematicaTe);
            }
        }
        region.put("TileEntities", tileEntities);

        // 生物/实体数据打包写入 Litematica 格式
        if (blueprintTag.contains("Entities")) {
            ListTag entities = new ListTag();
            ListTag blueprintEntities = blueprintTag.getListOrEmpty("Entities");
            for (int i = 0; i < blueprintEntities.size(); i++) {
                CompoundTag entity = blueprintEntities.getCompoundOrEmpty(i);
                CompoundTag litematicaEntity = entity.copy();
                // 导出的 region Position 默认是 (0,0,0) 且 size 是正数，所以可以直接使用相对坐标作为绝对坐标
                entities.add(litematicaEntity);
            }
            region.put("Entities", entities);
        }

        regions.put("Region", region);
        root.put("Regions", regions);
        ModLog.debug("[LitematicaParser] packLitematic 完成: 名称=" + name + ", 尺寸=" + width + "x" + height + "x" + length + ", 调色板=" + paletteSize);
        return root;
    }

    private static CompoundTag convertStringToBlockStateNbt(String stateStr) {
        CompoundTag nbt = new CompoundTag();
        int bracketIndex = stateStr.indexOf('[');
        if (bracketIndex == -1) {
            nbt.putString("id", stateStr);
        } else {
            String name = stateStr.substring(0, bracketIndex);
            nbt.putString("id", name);
            CompoundTag properties = new CompoundTag();
            String propStr = stateStr.substring(bracketIndex + 1, stateStr.length() - 1);
            String[] parts = propStr.split(",");
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    properties.putString(kv[0], kv[1]);
                }
            }
            nbt.put("properties", properties);
        }
        return nbt;
    }
}
