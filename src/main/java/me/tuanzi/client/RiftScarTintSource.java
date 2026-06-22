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
public class RiftScarTintSource implements ItemTintSource {
    private final boolean isNext;

    public static final MapCodec<RiftScarTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.BOOL.optionalFieldOf("is_next", false).forGetter(source -> source.isNext)
        ).apply(i, RiftScarTintSource::new)
    );

    public RiftScarTintSource(boolean isNext) {
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
                // 等级 I：无形，完美的亮紫色星空微光 (0xBF, 0x55, 0xEC) 到 浅紫蓝色 (0x81, 0xCF, 0xE0) 流动
                double wave = Math.sin(time * 0.06) * 0.5 + 0.5;
                int r = 0xBF - (int) (wave * (0xBF - 0x81));
                int g = 0x55 + (int) (wave * (0xCF - 0x55));
                int b = 0xEC - (int) (wave * (0xEC - 0xE0));
                return (r << 16) | (g << 8) | b;
            }
            case 1: {
                // 等级 II：涟漪，深紫色虚空波纹脉动 (0x8E, 0x44, 0xAD) 到 (0x5B, 0x2C, 0x6F)
                double wave = Math.sin(time * 0.12) * 0.5 + 0.5;
                int r = 0x5B + (int) (wave * (0x8E - 0x5B));
                int g = 0x2C + (int) (wave * (0x44 - 0x2C));
                int b = 0x6F + (int) (wave * (0xAD - 0x6F));
                return (r << 16) | (g << 8) | b;
            }
            case 2: {
                // 等级 III：蚀痕，半透明灰紫 (0x9B, 0x59, 0xB6) 到 黯淡石墨灰 (0x7F, 0x8C, 0x8D)
                double wave = Math.sin(time * 0.08) * 0.5 + 0.5;
                int r = 0x7F + (int) (wave * (0x9B - 0x7F));
                int g = 0x8C - (int) (wave * (0x8C - 0x59));
                int b = 0x8D + (int) (wave * (0xB6 - 0x8D));
                return (r << 16) | (g << 8) | b;
            }
            case 3: {
                // 等级 IV：崩解，波动极剧烈的紫黑色，加入高频暗闪模拟虚空颗粒崩落
                double wave = Math.sin(time * 0.3) * 0.4 + 0.6;
                if (time % 40 < 5) {
                    wave = 0.1; // 崩落瞬间短暂暗灭
                }
                int r = (int) (wave * 0x4A);
                int g = (int) (wave * 0x1F);
                int b = (int) (wave * 0x6D);
                return (r << 16) | (g << 8) | b;
            }
            case 4:
            default: {
                // 等级 V：残响，极其淡薄的暗影，偏纯灰黑色 (0x1A, 0x1C, 0x2A) 偶有幽暗紫色流露
                double wave = Math.sin(time * 0.04) * 0.5 + 0.5;
                int r = 0x1A + (int) (wave * 0x10);
                int g = 0x1C + (int) (wave * 0x0A);
                int b = 0x2A + (int) (wave * 0x0D);
                return (r << 16) | (g << 8) | b;
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
