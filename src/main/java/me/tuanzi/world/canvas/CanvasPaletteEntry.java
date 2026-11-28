package me.tuanzi.world.canvas;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import me.tuanzi.util.LitematicaParser;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 画布调色板条目：记录方块状态字符串与可选彩色方块 RGB 染色数据。
 */
public record CanvasPaletteEntry(String blockStateStr, int colorRgb, int altColorRgb) {
    public static final int NO_COLOR = -1;

    public CanvasPaletteEntry {
        blockStateStr = blockStateStr != null ? blockStateStr : "minecraft:air";
    }

    public static CanvasPaletteEntry of(BlockState state) {
        return of(state, NO_COLOR, NO_COLOR);
    }

    public static CanvasPaletteEntry of(BlockState state, int colorRgb, int altColorRgb) {
        return new CanvasPaletteEntry(serializeBlockState(state), colorRgb, altColorRgb);
    }

    public static String serializeBlockState(BlockState state) {
        if (state == null || state.isAir()) {
            return "minecraft:air";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(state.getBlock().builtInRegistryHolder().key().identifier());
        if (!state.isSingletonState()) {
            sb.append('[');
            sb.append(state.getValues().map(Property.Value::toString).collect(Collectors.joining(",")));
            sb.append(']');
        }
        return sb.toString();
    }

    public BlockState resolveBlockState() {
        BlockState parsed = LitematicaParser.parseBlockStateFromTag(StringTag.valueOf(this.blockStateStr));
        if (parsed == null || (parsed.isAir() && !"minecraft:air".equals(this.blockStateStr))) {
            return Blocks.STONE.defaultBlockState();
        }
        return parsed;
    }

    public boolean isColorableModBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ColorBlock || block instanceof ColorStairs || block instanceof ColorSlab;
    }

    public boolean isTransparent() {
        BlockState state = resolveBlockState();
        return !state.canOcclude() || !state.getFluidState().isEmpty();
    }

    public boolean isEmissive() {
        return resolveBlockState().getLightEmission() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanvasPaletteEntry other)) return false;
        return colorRgb == other.colorRgb
                && altColorRgb == other.altColorRgb
                && Objects.equals(blockStateStr, other.blockStateStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockStateStr, colorRgb, altColorRgb);
    }
}
