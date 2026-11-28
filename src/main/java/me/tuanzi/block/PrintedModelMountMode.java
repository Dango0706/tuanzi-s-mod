package me.tuanzi.block;

import net.minecraft.util.StringRepresentable;

/**
 * 打印模型方块的安装方式（地面平放、贴墙立放、天花板倒挂）。
 */
public enum PrintedModelMountMode implements StringRepresentable {
    FLOOR("floor"),
    WALL("wall"),
    CEILING("ceiling");

    private final String name;

    PrintedModelMountMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
