package me.tuanzi.block;

import net.minecraft.util.StringRepresentable;

/**
 * 方块放置器正面状态灯枚举
 */
public enum PlacerLampState implements StringRepresentable {
    GREEN("green"),
    YELLOW("yellow"),
    RED("red");

    private final String name;

    PlacerLampState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
