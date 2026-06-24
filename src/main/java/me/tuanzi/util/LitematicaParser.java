package me.tuanzi.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.util.*;

public class LitematicaParser {

    /**
     * 将 Litematica / Schematic NBT 格式统一解析为模组自定义的结构蓝图 NBT 格式。
     * 自定义蓝图 NBT 结构：
     * {
     *   Width: int,
     *   Height: int,
     *   Length: int,
     *   Palette: [BlockState NBTs],
     *   Blocks: int[] (大小为 Width * Height * Length),
     *   BlockEntities: [ {pos: [x,y,z], nbt: CompoundTag} ]
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

        // 转换 Palette
        ListTag resultPalette = new ListTag();
        for (int i = 0; i < paletteSize; i++) {
            // 直接将 Litematica 的 BlockStatePalette 中的 CompoundTag 转换为原生 BlockState 并重新写出
            CompoundTag litematicaState = blockStatePalette.getCompoundOrEmpty(i);
            // 复制为一个规范 of BlockState NBT（Litematica 的结构通常是 Name + Properties，正好与原版 Codec 匹配）
            CompoundTag blockStateNbt = new CompoundTag();
            blockStateNbt.putString("Name", litematicaState.getStringOr("Name", "minecraft:air"));
            if (litematicaState.contains("Properties")) {
                blockStateNbt.put("Properties", litematicaState.getCompoundOrEmpty("Properties"));
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

            CompoundTag sizeTag = region.getCompoundOrEmpty("Size");
            int minX = Math.min(regionX, regionX + sizeTag.getIntOr("x", 0));
            int minY = Math.min(regionY, regionY + sizeTag.getIntOr("y", 0));
            int minZ = Math.min(regionZ, regionZ + sizeTag.getIntOr("z", 0));

            int teX = te.getIntOr("x", 0) - minX;
            int teY = te.getIntOr("y", 0) - minY;
            int teZ = te.getIntOr("z", 0) - minZ;

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
            for (int i = 0; i < entities.size(); i++) {
                CompoundTag entity = entities.getCompoundOrEmpty(i);
                CompoundTag entityNbt = entity.copy();
                
                // 获取实体的世界绝对坐标 Pos
                ListTag posList = entity.getListOrEmpty("Pos");
                if (posList.size() >= 3) {
                    double ex = posList.getDoubleOr(0, 0.0);
                    double ey = posList.getDoubleOr(1, 0.0);
                    double ez = posList.getDoubleOr(2, 0.0);
                    
                    // 转换局部坐标
                    CompoundTag regionPos = region.getCompoundOrEmpty("Position");
                    int regionX = regionPos.getIntOr("x", 0);
                    int regionY = regionPos.getIntOr("y", 0);
                    int regionZ = regionPos.getIntOr("z", 0);
                    
                    CompoundTag sizeTag = region.getCompoundOrEmpty("Size");
                    double minX = Math.min(regionX, regionX + sizeTag.getIntOr("x", 0));
                    double minY = Math.min(regionY, regionY + sizeTag.getIntOr("y", 0));
                    double minZ = Math.min(regionZ, regionZ + sizeTag.getIntOr("z", 0));
                    
                    double rx = ex - minX;
                    double ry = ey - minY;
                    double rz = ez - minZ;
                    
                    ListTag relativePos = new ListTag();
                    relativePos.add(DoubleTag.valueOf(rx));
                    relativePos.add(DoubleTag.valueOf(ry));
                    relativePos.add(DoubleTag.valueOf(rz));
                    entityNbt.put("Pos", relativePos);
                }

                // 转换 NBT 中的附着方块坐标为局部相对坐标
                if (entityNbt.contains("block_pos")) {
                    int[] bp = entityNbt.getIntArray("block_pos").orElse(new int[0]);
                    if (bp.length == 3) {
                        CompoundTag regionPos = region.getCompoundOrEmpty("Position");
                        int regionX = regionPos.getIntOr("x", 0);
                        int regionY = regionPos.getIntOr("y", 0);
                        int regionZ = regionPos.getIntOr("z", 0);
                        
                        CompoundTag sizeTag = region.getCompoundOrEmpty("Size");
                        int minX = Math.min(regionX, regionX + sizeTag.getIntOr("x", 0));
                        int minY = Math.min(regionY, regionY + sizeTag.getIntOr("y", 0));
                        int minZ = Math.min(regionZ, regionZ + sizeTag.getIntOr("z", 0));
                        
                        int rbx = bp[0] - minX;
                        int rby = bp[1] - minY;
                        int rbz = bp[2] - minZ;
                        entityNbt.putIntArray("block_pos", new int[]{rbx, rby, rbz});
                    }
                }
                if (entityNbt.contains("TileX") && entityNbt.contains("TileY") && entityNbt.contains("TileZ")) {
                    CompoundTag regionPos = region.getCompoundOrEmpty("Position");
                    int regionX = regionPos.getIntOr("x", 0);
                    int regionY = regionPos.getIntOr("y", 0);
                    int regionZ = regionPos.getIntOr("z", 0);
                    
                    CompoundTag sizeTag = region.getCompoundOrEmpty("Size");
                    int minX = Math.min(regionX, regionX + sizeTag.getIntOr("x", 0));
                    int minY = Math.min(regionY, regionY + sizeTag.getIntOr("y", 0));
                    int minZ = Math.min(regionZ, regionZ + sizeTag.getIntOr("z", 0));
                    
                    entityNbt.putInt("TileX", entityNbt.getIntOr("TileX", 0) - minX);
                    entityNbt.putInt("TileY", entityNbt.getIntOr("TileY", 0) - minY);
                    entityNbt.putInt("TileZ", entityNbt.getIntOr("TileZ", 0) - minZ);
                }
                
                // 剔除 UUID，避免放置冲突
                entityNbt.remove("UUID");
                resultEntitiesList.add(entityNbt);
            }
            result.put("Entities", resultEntitiesList);
        }

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
            CompoundTag state = palette.getCompoundOrEmpty(i);
            CompoundTag litematicaState = new CompoundTag();
            litematicaState.putString("Name", state.getStringOr("Name", "minecraft:air"));
            if (state.contains("Properties")) {
                litematicaState.put("Properties", state.getCompoundOrEmpty("Properties").copy());
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
        return root;
    }

    private static CompoundTag convertStringToBlockStateNbt(String stateStr) {
        CompoundTag nbt = new CompoundTag();
        int bracketIndex = stateStr.indexOf('[');
        if (bracketIndex == -1) {
            nbt.putString("Name", stateStr);
        } else {
            String name = stateStr.substring(0, bracketIndex);
            nbt.putString("Name", name);
            CompoundTag properties = new CompoundTag();
            String propStr = stateStr.substring(bracketIndex + 1, stateStr.length() - 1);
            String[] parts = propStr.split(",");
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    properties.putString(kv[0], kv[1]);
                }
            }
            nbt.put("Properties", properties);
        }
        return nbt;
    }
}
