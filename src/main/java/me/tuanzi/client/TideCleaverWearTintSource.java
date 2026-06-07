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
public class TideCleaverWearTintSource implements ItemTintSource {
    private final boolean isNext;

    public static final MapCodec<TideCleaverWearTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.BOOL.optionalFieldOf("is_next", false).forGetter(source -> source.isNext)
        ).apply(i, TideCleaverWearTintSource::new)
    );

    public TideCleaverWearTintSource(boolean isNext) {
        this.isNext = isNext;
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("WearValue")) {
                double wear = tag.getDoubleOr("WearValue", 0.0);
                return getTint(wear, this.isNext);
            }
        }
        return this.isNext ? 0x00FFFFFF : 0xFFFFFFFF;
    }

    private int getTint(double wear, boolean isNext) {
        if (wear < 0.0) wear = 0.0;
        if (wear > 1.0) wear = 1.0;

        double t;
        int alpha;

        if (wear < 0.2) {
            t = wear / 0.2;
            alpha = isNext ? (int) (t * 255) : 255;
            return (alpha << 24) | 0xFFFFFF;
        } else if (wear < 0.4) {
            t = (wear - 0.2) / 0.2;
            alpha = isNext ? (int) (t * 255) : 255;
            return (alpha << 24) | 0xFFFFFF;
        } else if (wear < 0.6) {
            t = (wear - 0.4) / 0.2;
            alpha = isNext ? (int) (t * 255) : 255;
            return (alpha << 24) | 0xFFFFFF;
        } else if (wear < 0.8) {
            t = (wear - 0.6) / 0.2;
            alpha = isNext ? (int) (t * 255) : 255;
            return (alpha << 24) | 0xFFFFFF;
        } else {
            if (isNext) {
                return 0x00FFFFFF;
            } else {
                return 0xFFFFFFFF;
            }
        }
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
