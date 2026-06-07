package me.tuanzi.client;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TideCleaverEnergyTintSource implements ItemTintSource {
    public static final TideCleaverEnergyTintSource INSTANCE = new TideCleaverEnergyTintSource();
    public static final MapCodec<TideCleaverEnergyTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    public TideCleaverEnergyTintSource() {}

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (owner instanceof net.minecraft.world.entity.player.Player player) {
            me.tuanzi.util.TideCleaverPlayerTracker tracker = (me.tuanzi.util.TideCleaverPlayerTracker) player;
            float strength = player.getAttackStrengthScale(0.0f);
            int currentFullyChargedTicks = tracker.tuanzis_mod$getCurrentFullyChargedTicks();

            boolean inWindow = false;
            if (strength >= 0.8f && strength < 1.0f) {
                inWindow = true;
            } else if (strength >= 1.0f && currentFullyChargedTicks <= 4) {
                inWindow = true;
            }

            if (inWindow) {
                // 合拍窗口内：明亮的珍珠白 (0xFFFFFFFF)
                return 0xFFFFFFFF;
            } else {
                if (strength >= 1.0f) {
                    // 冷却已满但超出合拍窗口：普通的、饱满的深水蓝色
                    return 0xFF00BFFF; // 深天蓝 0x00BFFF
                } else {
                    // 冷却未满：透明度根据冷却进度渐变，颜色为水蓝色
                    int alpha = (int) (strength * 255.0f);
                    alpha = Math.max(0, Math.min(255, alpha));
                    return (alpha << 24) | 0x00BFFF;
                }
            }
        }
        // 没有持有者时默认显示水蓝色
        return 0xFF00BFFF;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
