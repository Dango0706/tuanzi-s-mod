package me.tuanzi.client;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.item.NodachiItem;
import me.tuanzi.util.NodachiPlayerTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Random;

public class NodachiMetronome {
    private static final Random RANDOM = new Random();
    // 底轨半宽（像素），总跨度 [-14, 14]，共 28 像素宽
    private static final int RADIUS_X = 14;
    // 记录最大脱力刻数以实现动态倒计时条渲染
    private static int maxExhaustionObserved = 10;
    // 当前摆动周期的起始毫秒时间戳
    private static long cycleStartTimeMs = System.currentTimeMillis();

    // 当前整圈往返摆动总周期时长（毫秒），单程 = cycleDurationMs / 2.0
    // 每摆动一圈随机在 850ms ~ 1350ms 之间变更，保证每圈在恒定匀速的同时具有速度变化
    private static float cycleDurationMs = 1100.0f;

    // 高亮节奏窗口在底轨上的占比（20% ~ 30% 之间随机数）
    private static float windowRatio = 0.25f;

    // 高亮节奏窗口在底轨归一化范围 [0.0, 1.0] 上的起点与终点
    private static float windowStartFrac = 0.375f;
    private static float windowEndFrac = 0.625f;

    static {
        // 初始化随机高亮窗口
        refreshRandomWindow();
    }

    /**
     * 刷新随机高亮节奏窗口：占比约为 20% ~ 30% 之间的随机数，位置随机分布于 [0.0, 1.0]
     */
    private static void refreshRandomWindow() {
        windowRatio = 0.20f + RANDOM.nextFloat() * 0.10f; // 20% ~ 30% 之间随机浮点数
        windowStartFrac = RANDOM.nextFloat() * (1.0f - windowRatio);
        windowEndFrac = windowStartFrac + windowRatio;
    }

    /**
     * 刷新新一圈的摆动速度与节奏窗口
     */
    private static void startNewCycle(long now) {
        cycleStartTimeMs = now;
        // 每摆动一圈，速度可以在合理区间内变更 (整圈 850ms ~ 1350ms 随机)
        cycleDurationMs = 850.0f + RANDOM.nextFloat() * 500.0f;
        // 同时随机生成新的一圈的高亮窗口
        refreshRandomWindow();
    }

    /**
     * 获取当前指针在底轨上的归一化进度 [0.0, 1.0] (0.0 为最左侧，1.0 为最右侧)
     * 摆动速度绝对恒定 (匀速三角波往返运动)，在经过高亮区时绝对不会加快或突变
     */
    public static float getCurrentPointerProgress() {
        long now = System.currentTimeMillis();
        long elapsed = now - cycleStartTimeMs;

        if (elapsed >= (long) cycleDurationMs) {
            // 完成一整圈摆动！开始新的一圈，并变更速度和高亮窗口
            startNewCycle(now);
            elapsed = 0;
        }

        float halfDuration = cycleDurationMs / 2.0f;
        if (elapsed < (long) halfDuration) {
            // 前半圈：从左 (0.0) 匀速移动到右 (1.0)
            return (float) elapsed / halfDuration;
        } else {
            // 后半圈：从右 (1.0) 匀速移动回左 (0.0)
            return 1.0f - ((float) (elapsed - (long) halfDuration) / halfDuration);
        }
    }

    /**
     * 计算考虑“回流 (Flow State)”附魔后的实际有效 QTE 节奏窗口 [start, end]
     */
    public static float[] getEffectiveWindow(Player player) {
        if (player == null) {
            return new float[]{windowStartFrac, windowEndFrac};
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof NodachiItem)) {
            return new float[]{windowStartFrac, windowEndFrac};
        }
        int flowStateLevel = 0;
        var registry = player.level().registryAccess().lookup(Registries.ENCHANTMENT);
        if (registry.isPresent()) {
            var flowStateHolder = registry.get().get(ModEnchantments.FLOW_STATE);
            if (flowStateHolder.isPresent()) {
                flowStateLevel = EnchantmentHelper.getItemEnchantmentLevel(flowStateHolder.get(), held);
            }
        }
        if (flowStateLevel <= 0) {
            return new float[]{windowStartFrac, windowEndFrac};
        }

        // 回流附魔：每级扩大 5% 判定窗口 (5% * level)
        float bonusRatio = flowStateLevel * 0.05f;
        float expandedRatio = Math.min(1.0f, windowRatio + bonusRatio);
        float center = (windowStartFrac + windowEndFrac) / 2.0f;
        float half = expandedRatio / 2.0f;
        float start = center - half;
        float end = center + half;

        if (start < 0.0f) {
            end = Math.min(1.0f, end - start);
            start = 0.0f;
        } else if (end > 1.0f) {
            start = Math.max(0.0f, start - (end - 1.0f));
            end = 1.0f;
        }

        return new float[]{start, end};
    }

    /**
     * 判断当前归一化进度是否落在高亮 QTE 窗口内（含半像素容差以匹配离散像素渲染）
     */
    public static boolean isPointerInWindow(float progress, Player player) {
        float[] win = getEffectiveWindow(player);
        float tolerance = 0.5f / (2.0f * RADIUS_X);
        return (progress >= (win[0] - tolerance) && progress <= (win[1] + tolerance));
    }

    public static boolean isPointerInWindow(float progress) {
        return isPointerInWindow(progress, null);
    }

    /**
     * 判定当前出刀时是否正处于节拍器的高亮 QTE 窗口内（且攻击冷却已满 100% 且未脱力）
     */
    public static boolean isCurrentAttackInRhythm(LocalPlayer player) {
        if (player == null) {
            return false;
        }

        float cooldown = player.getAttackStrengthScale(0.0f);
        if (cooldown < 0.99f) {
            // 武器冷却未达 100%，不能算在节奏窗口内
            return false;
        }

        if (player instanceof NodachiPlayerTracker tracker) {
            if (tracker.tuanzis_mod$getNodachiExhaustionTicks() > 0) {
                // 处于脱力状态中不能算
                return false;
            }
        }

        float progress = getCurrentPointerProgress();
        return isPointerInWindow(progress, player);
    }

    /**
     * 当玩家使用野太刀攻击（出刀）时响应
     */
    public static void onAttack() {
        // 攻击出刀后可以平稳过渡，若需要也可在此处刷新下一次的窗口
    }

    public static float getWindowStartFrac() {
        return windowStartFrac;
    }

    public static float getWindowEndFrac() {
        return windowEndFrac;
    }

    public static float getWindowRatio() {
        return windowRatio;
    }

    /**
     * 渲染野太刀专属的准星弧形节拍器 UI
     */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, LocalPlayer player, Font font) {
        if (player == null || player.isSpectator()) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof NodachiItem)) {
            return;
        }

        if (!(player instanceof NodachiPlayerTracker tracker)) {
            return;
        }

        int momentum = tracker.tuanzis_mod$getNodachiMomentum();
        int exhaustion = tracker.tuanzis_mod$getNodachiExhaustionTicks();
        float cooldown = player.getAttackStrengthScale(0.0f);

        // 获取当前指针归一化位置 [0.0, 1.0]
        float pointerProgress = getCurrentPointerProgress();
        boolean pointerInWindow = isPointerInWindow(pointerProgress, player);
        boolean inRhythm = (cooldown >= 0.99f && pointerInWindow && exhaustion == 0);

        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int baseY = cy + 10; // 准星下方偏移

        float totalTrackWidth = 2.0f * RADIUS_X;
        float[] win = getEffectiveWindow(player);
        float winMinX = (cx - RADIUS_X) + win[0] * totalTrackWidth;
        float winMaxX = (cx - RADIUS_X) + win[1] * totalTrackWidth;

        // 1. 绘制弧形底轨与随机 20%~30% 高亮节奏窗口
        for (int dx = -RADIUS_X; dx <= RADIUS_X; dx++) {
            int px = cx + dx;
            // 弧形下凹/两端微上翘抛物线：dx = 0 时 dy = 0；dx = +-14 时 dy = 2
            int dy = (int) Math.round(2.0 * (dx * dx) / (double) (RADIUS_X * RADIUS_X));
            int trackY = baseY + dy;

            // 绘制底轨阴影外框
            graphics.fill(px, trackY - 1, px + 1, trackY + 3, 0x88000000);

            // 判断当前点是否落在随机高亮 QTE 窗口内
            boolean isHitZone = (px >= winMinX && px <= winMaxX);

            if (exhaustion > 0) {
                // 脱力状态底轨呈暗红
                graphics.fill(px, trackY, px + 1, trackY + 2, 0x80551111);
            } else if (isHitZone) {
                // 高亮 QTE 区域：
                if (inRhythm) {
                    // 指针正好扫入高亮窗口且冷却已满 100%：高光闪烁白亮！
                    graphics.fill(px, trackY, px + 1, trackY + 2, 0xFFFFFFFF);
                } else if (cooldown >= 1.0f) {
                    // 冷却已满 100%，窗口激活为耀眼金黄
                    graphics.fill(px, trackY, px + 1, trackY + 2, 0xFFFFD700);
                } else {
                    // 冷却未达 100%，窗口处于待充能状态 (半透明金黄)
                    graphics.fill(px, trackY, px + 1, trackY + 2, 0x99D4AF37);
                }
            } else {
                // 常规底轨槽
                graphics.fill(px, trackY, px + 1, trackY + 2, 0x80222222);
            }
        }

        // 2. 计算并绘制摆动的节拍指针 (匀速移动)
        float currentPointerX = (cx - RADIUS_X) + pointerProgress * totalTrackWidth;
        int ptrX = Math.round(currentPointerX);
        int ptrDx = Mth.clamp(ptrX - cx, -RADIUS_X, RADIUS_X);
        int ptrDy = (int) Math.round(2.0 * (ptrDx * ptrDx) / (double) (RADIUS_X * RADIUS_X));
        int pointerCenterY = baseY + ptrDy;

        // 指针颜色
        int pointerColor;
        if (exhaustion > 0) {
            pointerColor = 0xFFFF3333; // 脱力暗红
        } else if (inRhythm) {
            pointerColor = 0xFFFFFFFF; // 完美节拍命中白光
        } else {
            pointerColor = 0xFFDDDDDD; // 金属银白
        }

        // 指针背景阴影与主体 (宽 2 像素，高 7 像素)
        graphics.fill(ptrX - 1, pointerCenterY - 3, ptrX + 2, pointerCenterY + 4, 0x99000000);
        graphics.fill(ptrX, pointerCenterY - 2, ptrX + 1, pointerCenterY + 3, pointerColor);
        if (inRhythm) {
            // 节奏窗口内外发光光晕
            graphics.fill(ptrX - 1, pointerCenterY - 1, ptrX + 2, pointerCenterY + 2, 0x8055FFFF);
        }

        // 3. 节拍器下方显示状态（“势”槽指示点或脱力倒计时）
        int statusY = baseY + 6;
        if (exhaustion > 0) {
            if (exhaustion > maxExhaustionObserved) {
                maxExhaustionObserved = exhaustion;
            }
            // 脱力状态：展示微型脱力文字与倒计时条
            Component exText = Component.literal("脱力").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD);
            int textW = font.width(exText);
            graphics.text(font, exText, cx - textW / 2, statusY, 0xFFFF3333, false);

            int barW = 20;
            int currentBarW = Mth.clamp((int) Math.round(((double) exhaustion / Math.max(1, maxExhaustionObserved)) * barW), 0, barW);
            graphics.fill(cx - barW / 2, statusY + 8, cx + barW / 2, statusY + 9, 0x80000000);
            graphics.fill(cx - barW / 2, statusY + 8, cx - barW / 2 + currentBarW, statusY + 9, 0xFFFF2222);
        } else {
            maxExhaustionObserved = 10;
            // 3 格气势槽微型指示器 (3 个小方块，居中排列)
            int slotWidth = 4;
            int slotSpacing = 6;
            int totalSlotWidth = 3 * slotSpacing - 2;
            int slotStartX = cx - totalSlotWidth / 2;

            for (int i = 0; i < 3; i++) {
                int sx = slotStartX + i * slotSpacing;
                // 底槽
                graphics.fill(sx - 1, statusY - 1, sx + slotWidth + 1, statusY + 3, 0x80000000);
                graphics.fill(sx, statusY, sx + slotWidth, statusY + 2, 0x80333333);

                if (momentum > i) {
                    int slotColor = switch (i) {
                        case 0 -> 0xFF00E5FF; // 1 层：亮青
                        case 1 -> 0xFFFFC400; // 2 层：亮金
                        case 2 -> 0xFFFF1744; // 3 层：紫红
                        default -> 0xFFFFFFFF;
                    };
                    graphics.fill(sx, statusY, sx + slotWidth, statusY + 2, slotColor);
                }
            }

            // 若满 3 层且顺劈气刃可用 (无冷却)，在两旁展示金色微粒提示
            if (momentum >= 3 && tracker.tuanzis_mod$getCrescentAuraCooldownTicks() <= 0) {
                graphics.fill(slotStartX - 4, statusY, slotStartX - 2, statusY + 2, 0xFFFFD700);
                graphics.fill(slotStartX + totalSlotWidth + 2, statusY, slotStartX + totalSlotWidth + 4, statusY + 2, 0xFFFFD700);
            }

            // 若玩家正在长按右键为【绝式】蓄力，显示蓄力提示
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof NodachiItem) {
                int useTicks = player.getTicksUsingItem();
                float chargePct = Math.min(1.0f, useTicks / 16.0f);
                int chargeY = statusY + 10;
                if (chargePct >= 1.0f) {
                    Component readyText = Component.literal("【绝式就绪】松开释放").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
                    int tw = font.width(readyText);
                    graphics.text(font, readyText, cx - tw / 2, chargeY, 0xFFFFD700, true);
                } else {
                    int chargeBarW = 28;
                    int curW = (int) (chargePct * chargeBarW);
                    graphics.fill(cx - chargeBarW / 2 - 1, chargeY - 1, cx + chargeBarW / 2 + 1, chargeY + 3, 0x80000000);
                    graphics.fill(cx - chargeBarW / 2, chargeY, cx - chargeBarW / 2 + curW, chargeY + 2, 0xFFFFC400);
                }
            }
        }
    }
}
