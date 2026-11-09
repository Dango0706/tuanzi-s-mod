package me.tuanzi.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 日冕过热 (Arena Heat Gauge) HUD 与屏幕热浪晕影渲染器
 */
@Environment(EnvType.CLIENT)
public class CinderHeatHud {
    private static int clientHeat = 0;
    private static boolean clientInArena = false;
    private static float displayHeat = 0.0F;
    private static long lastUpdateTime = 0L;

    public static void update(int heat, boolean inArena) {
        clientHeat = heat;
        clientInArena = inArena;
        lastUpdateTime = System.currentTimeMillis();

        if (inArena) {
            me.tuanzi.client.sound.CinderBossMusicManager.startMusic();
        } else {
            me.tuanzi.client.sound.CinderBossMusicManager.stopMusic();
        }
    }

    public static int getClientHeat() {
        return clientHeat;
    }

    public static boolean isInArena() {
        return clientInArena;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player, Font font) {
        // 离场主动衰减保底：脱离领域且超过 1.2 秒未收到服务端新包时，平滑递减归零
        if (!clientInArena && clientHeat > 0) {
            long now = System.currentTimeMillis();
            if (now - lastUpdateTime > 1200L) {
                clientHeat = Math.max(0, clientHeat - 1);
                lastUpdateTime = now - 1050L;
            }
        }

        if (!clientInArena && clientHeat <= 0 && displayHeat < 0.5F) {
            displayHeat = 0.0F;
            return;
        }

        // 平滑热度动画过渡
        displayHeat = Mth.lerp(0.15F, displayHeat, (float) clientHeat);

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // 1. 屏幕边缘橙红热浪暗角 (Heat Vignette)
        if (displayHeat > 15.0F) {
            float heatProgress = (displayHeat - 15.0F) / 85.0F; // 0.0 ~ 1.0
            float baseAlpha = Math.min(0.65F, heatProgress * 0.65F);
            // 极限过热时呼吸搏动
            if (clientHeat >= 100) {
                float pulse = (float) Math.sin((double) System.currentTimeMillis() / 150.0) * 0.1F;
                baseAlpha = Math.min(0.85F, baseAlpha + pulse);
            }

            int vignetteColor = ARGB.colorFromFloat(baseAlpha, 0.2F, 0.6F, 0.05F);
            graphics.blit(
                    RenderPipelines.VIGNETTE,
                    Identifier.withDefaultNamespace("textures/misc/vignette.png"),
                    0, 0, 0.0F, 0.0F,
                    screenWidth, screenHeight,
                    screenWidth, screenHeight,
                    vignetteColor
            );
        }

        // 2. 快捷栏上方日冕过热仪表盘
        int barWidth = 84;
        int barHeight = 5;
        int barX = (screenWidth - barWidth) / 2;
        int barY = screenHeight - 48; // 位于快捷栏上方、经验条上方安全区

        // 底框背景 (0xCC181008) 与边框
        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xDD120C06);
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF22160C);

        // 50% 警戒线与 80% 危险线标记
        int mark50X = barX + barWidth * 50 / 100;
        int mark80X = barX + barWidth * 80 / 100;
        graphics.fill(mark50X, barY - 1, mark50X + 1, barY + barHeight + 1, 0xFF7A6010);
        graphics.fill(mark80X, barY - 1, mark80X + 1, barY + barHeight + 1, 0xFF8A2510);

        // 填充进度条
        int fillWidth = (int) (barWidth * (Mth.clamp(displayHeat, 0.0F, 100.0F) / 100.0F));
        if (fillWidth > 0) {
            int fillColor;
            if (clientHeat < 50) {
                fillColor = 0xFFFFA010; // 亮橙金
            } else if (clientHeat < 80) {
                fillColor = 0xFFFF5500; // 熔岩橙红
            } else if (clientHeat < 100) {
                fillColor = 0xFFFF2000; // 危险烈红
            } else {
                // 100% 极限自燃过热：白金色与赤红交替闪烁
                boolean flash = (System.currentTimeMillis() / 120) % 2 == 0;
                fillColor = flash ? 0xFFFFF090 : 0xFFFF0000;
            }
            graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
        }

        // 文本提示
        String text;
        int textColor;
        if (clientHeat >= 100) {
            text = "§c§l[!] 日冕极限过热: 100% (自燃真实灼烧)";
            textColor = 0xFFFF3333;
        } else if (clientHeat >= 80) {
            text = "日冕严重过热: " + clientHeat + "% (疲劳I)";
            textColor = 0xFFFF6622;
        } else if (clientHeat >= 50) {
            text = "日冕中度过热: " + clientHeat + "% (疾跑虚脱)";
            textColor = 0xFFFFAA33;
        } else {
            text = "日冕过热度: " + clientHeat + "%";
            textColor = 0xFFFFCC66;
        }

        Component title = Component.literal(text);
        int textY = barY - 9;
        graphics.centeredText(font, title, screenWidth / 2, textY, textColor);
    }
}
