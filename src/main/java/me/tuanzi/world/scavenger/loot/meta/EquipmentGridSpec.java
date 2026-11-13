package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

/**
 * 拾荒回廊装备槽位（背包槽 / 安全箱槽）展开的内部二维网格规格
 */
public record EquipmentGridSpec(SlotType slotType, int columns, int rows) {

    public static final Codec<EquipmentGridSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SlotType.CODEC.fieldOf("slot_type").forGetter(EquipmentGridSpec::slotType),
            ExtraCodecs.intRange(1, 9).fieldOf("columns").forGetter(EquipmentGridSpec::columns),
            ExtraCodecs.intRange(1, 12).fieldOf("rows").forGetter(EquipmentGridSpec::rows)
    ).apply(instance, EquipmentGridSpec::new));
    public static final StreamCodec<ByteBuf, EquipmentGridSpec> STREAM_CODEC = StreamCodec.composite(
            SlotType.STREAM_CODEC,
            EquipmentGridSpec::slotType,
            ByteBufCodecs.VAR_INT,
            EquipmentGridSpec::columns,
            ByteBufCodecs.VAR_INT,
            EquipmentGridSpec::rows,
            EquipmentGridSpec::new
    );

    public EquipmentGridSpec {
        columns = Mth.clamp(columns, 1, 9);
        rows = Mth.clamp(rows, 1, 12);
    }

    public int totalCells() {
        return this.columns * this.rows;
    }

    public enum SlotType implements StringRepresentable {
        BACKPACK("BACKPACK"),
        SAFEBOX("SAFEBOX");

        public static final Codec<SlotType> CODEC = new StringRepresentable.EnumCodec<>(
                values(),
                name -> {
                    if (name == null) return null;
                    String upper = name.toUpperCase(Locale.ROOT);
                    for (SlotType v : values()) {
                        if (v.serializedName.equals(upper)) return v;
                    }
                    return null;
                }
        );

        private static final IntFunction<SlotType> BY_ID = ByIdMap.continuous(
                Enum::ordinal,
                values(),
                ByIdMap.OutOfBoundsStrategy.ZERO
        );

        public static final StreamCodec<ByteBuf, SlotType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

        private final String serializedName;

        SlotType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}
