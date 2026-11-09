package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

/**
 * 拾荒回廊物品抽象类别枚举（不包含任何枪械/弹药类别）
 */
public enum ScavengerCategory implements StringRepresentable {
    MEDICAL("MEDICAL", "scavenger.category.tuanzis_mod.medical"),
    ELECTRONICS("ELECTRONICS", "scavenger.category.tuanzis_mod.electronics"),
    INDUSTRIAL("INDUSTRIAL", "scavenger.category.tuanzis_mod.industrial"),
    KEYCARD("KEYCARD", "scavenger.category.tuanzis_mod.keycard"),
    PROVISION("PROVISION", "scavenger.category.tuanzis_mod.provision"),
    TOOL("TOOL", "scavenger.category.tuanzis_mod.tool"),
    RELIC("RELIC", "scavenger.category.tuanzis_mod.relic"),
    BACKPACK("BACKPACK", "scavenger.category.tuanzis_mod.backpack"),
    SAFEBOX("SAFEBOX", "scavenger.category.tuanzis_mod.safebox"),
    MISC("MISC", "scavenger.category.tuanzis_mod.misc");

    public static final Codec<ScavengerCategory> CODEC = new StringRepresentable.EnumCodec<>(
            values(),
            name -> {
                if (name == null) return null;
                String upper = name.toUpperCase(Locale.ROOT);
                for (ScavengerCategory v : values()) {
                    if (v.serializedName.equals(upper)) return v;
                }
                return null;
            }
    );

    private static final IntFunction<ScavengerCategory> BY_ID = ByIdMap.continuous(
            Enum::ordinal,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, ScavengerCategory> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;
    private final String translationKey;

    ScavengerCategory(String serializedName, String translationKey) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey);
    }
}
