package me.tuanzi.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ScullyShardTintSource implements ItemTintSource {
    private final boolean isNext;

    public static final MapCodec<ScullyShardTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.BOOL.optionalFieldOf("is_next", false).forGetter(source -> source.isNext)
        ).apply(i, ScullyShardTintSource::new)
    );

    public ScullyShardTintSource(boolean isNext) {
        this.isNext = isNext;
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("WearValue")) {
                double wear = tag.getDoubleOr("WearValue", 0.0);
                long time = level != null ? level.getGameTime() : 0L;
                return getTint(wear, this.isNext, time);
            }
        }
        return this.isNext ? 0x00FFFFFF : 0xFFFFFFFF;
    }

    private int getStageColor(int stage, long time) {
        switch (stage) {
            case 0: {
                // 等级 I：深空回响，柔和的蓝白色脉冲与幽蓝星云
                double wave = Math.sin(time * 0.08) * 0.5 + 0.5;
                int r = 0x80 - (int) (wave * 0x80); // 0x80 -> 0x00
                int g = 0xE0 - (int) (wave * 0x60); // 0xE0 -> 0xA0
                int b = 0xFF - (int) (wave * 0x4F); // 0xFF -> 0xB0
                return (r << 16) | (g << 8) | b;
            }
            case 1: {
                // 等级 II：微纹共鸣，流速加快且略显紊乱的不规则闪烁
                double wave = Math.sin(time * 0.25) * 0.3 + 0.7;
                // 间歇性暗闪
                if (time % 50 < 6) {
                    wave *= 0.3;
                }
                // 加入高频微小颤音
                if ((time / 10) % 2 == 0) {
                    wave = Math.max(0.2, wave - 0.15);
                }
                int r = (int) (wave * 0x50);
                int g = (int) (wave * 0x90);
                int b = (int) (wave * 0xA8);
                return (r << 16) | (g << 8) | b;
            }
            case 2: {
                // 等级 III：脉动裂痕，暗紫色紊流与暗幽蓝，偶尔短暂暗灭
                double wave = Math.sin(time * 0.15) * 0.3 + 0.5;
                if (time % 80 < 12) {
                    wave = 0.05; // 几乎暗灭
                }
                // 混合紫色 (0x4A, 0x0A, 0x6F) 和暗蓝 (0x00, 0x33, 0x44)
                boolean usePurple = (time % 30 < 15);
                int r = usePurple ? (int) (wave * 0x4A) : 0;
                int g = usePurple ? (int) (wave * 0x0A) : (int) (wave * 0x33);
                int b = usePurple ? (int) (wave * 0x6F) : (int) (wave * 0x44);
                return (r << 16) | (g << 8) | b;
            }
            case 3: {
                // 等级 IV：衰变尖啸，原生暗绿色为主，微弱蓝光挣扎微闪
                // 暗绿色基底 (0x08, 0x1A, 0x14)
                int r = 0x08;
                int g = 0x1A;
                int b = 0x14;
                // 垂死蓝光突闪 (0x00, 0x60, 0x70)
                if (time % 120 < 12) {
                    r = 0x00;
                    g = 0x60;
                    b = 0x70;
                }
                return (r << 16) | (g << 8) | b;
            }
            case 4:
            default: {
                // 等级 V：静默残片，极暗无光灰黑色
                return 0x0E100E;
            }
        }
    }

    private int getTint(double wear, boolean isNext, long time) {
        if (wear < 0.0) wear = 0.0;
        if (wear > 1.0) wear = 1.0;

        double t;
        int alpha;
        int rgbCurrent;
        int rgbNext;

        if (wear < 0.2) {
            t = wear / 0.2;
            rgbCurrent = getStageColor(0, time);
            rgbNext = getStageColor(1, time);
            alpha = isNext ? (int) (t * 255) : 255;
            int color = isNext ? rgbNext : rgbCurrent;
            return (alpha << 24) | color;
        } else if (wear < 0.4) {
            t = (wear - 0.2) / 0.2;
            rgbCurrent = getStageColor(1, time);
            rgbNext = getStageColor(2, time);
            alpha = isNext ? (int) (t * 255) : 255;
            int color = isNext ? rgbNext : rgbCurrent;
            return (alpha << 24) | color;
        } else if (wear < 0.6) {
            t = (wear - 0.4) / 0.2;
            rgbCurrent = getStageColor(2, time);
            rgbNext = getStageColor(3, time);
            alpha = isNext ? (int) (t * 255) : 255;
            int color = isNext ? rgbNext : rgbCurrent;
            return (alpha << 24) | color;
        } else if (wear < 0.8) {
            t = (wear - 0.6) / 0.2;
            rgbCurrent = getStageColor(3, time);
            rgbNext = getStageColor(4, time);
            alpha = isNext ? (int) (t * 255) : 255;
            int color = isNext ? rgbNext : rgbCurrent;
            return (alpha << 24) | color;
        } else {
            if (isNext) {
                return 0x00FFFFFF;
            } else {
                rgbCurrent = getStageColor(4, time);
                return (255 << 24) | rgbCurrent;
            }
        }
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
