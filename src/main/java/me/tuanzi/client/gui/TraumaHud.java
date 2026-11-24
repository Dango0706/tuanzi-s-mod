package me.tuanzi.client.gui;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.config.ModConfig;
import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.DesperateStandHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 「背水一战」常驻创伤条 HUD 渲染器：
 * - 位置默认放在生命条（含护甲条）正上方 2px，宽度与生命条一致（10 颗心 = 81px）；
 * - 检测快捷栏上方其他 HUD（如日冕过热仪表盘）冲突时，按配置切换至「冲突 HUD 上方 2px」或「屏幕右侧中部」；
 * - 使用白色心形展示创伤值（与原版血量显示逻辑一致），最右侧显示“创伤 37 | 18s”；
 * - 创伤值为 0 但效果仍在时，条为空，保留图标与倒计时，数字显示 0。
 */
@Environment(EnvType.CLIENT)
public class TraumaHud {
    public static final int HEART_COUNT = 10;
    public static final int HEART_BAR_WIDTH = 81; // (10 - 1) * 8 + 9
    public static final int HEART_SIZE = 9;
    public static final int TEXT_GAP = 4;

    private static final Identifier HEART_CONTAINER_TEX =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/gui/trauma_heart_container.png");
    private static final Identifier HEART_FULL_TEX =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/gui/trauma_heart_full.png");
    private static final Identifier HEART_HALF_TEX =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/gui/trauma_heart_half.png");

    private static float clientTrauma = 0.0f;

    public static void update(float trauma) {
        clientTrauma = Math.max(0.0f, trauma);
    }

    public static void clear() {
        clientTrauma = 0.0f;
    }

    public static float getClientTrauma() {
        return clientTrauma;
    }

    /**
     * 计算玩家生命条（及护甲条）最顶部的 Y 坐标，用于对齐生命条正上方 2px。
     */
    public static int computeVanillaLeftHudTopY(Player player, int screenHeight) {
        int yLineBase = screenHeight - 39;
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        int totalAbsorption = Mth.ceil(player.getAbsorptionAmount());
        int numHealthRows = Math.max(1, Mth.ceil((maxHealth + totalAbsorption) / 2.0F / 10.0F));
        int healthRowHeight = Math.max(10 - (numHealthRows - 2), 3);
        int topHeartY = yLineBase - (numHealthRows - 1) * healthRowHeight;
        if (player.getArmorValue() > 0) {
            return yLineBase - (numHealthRows - 1) * healthRowHeight - 10;
        }
        return topHeartY;
    }

    public record HudPosition(int barX, int barY, boolean conflictDetected) {
    }

    /**
     * 计算创伤条最终显示坐标（供客户端渲染与单元测试验证）。
     */
    public static HudPosition computePosition(
            int screenWidth,
            int screenHeight,
            int vanillaLeftHudTopY,
            int textWidth,
            boolean conflictVisible,
            int conflictLeftX,
            int conflictTopY,
            int conflictRightX,
            int conflictBottomY,
            ModConfig.TraumaHudConflictMode mode
    ) {
        int defaultBarX = screenWidth / 2 - 91;
        int defaultBarY = vanillaLeftHudTopY - 2 - HEART_SIZE;
        int totalWidth = HEART_BAR_WIDTH + TEXT_GAP + textWidth;

        boolean overlaps = false;
        if (conflictVisible) {
            int barRight = defaultBarX + totalWidth;
            int barBottom = defaultBarY + HEART_SIZE;
            overlaps = defaultBarX < conflictRightX
                    && barRight > conflictLeftX
                    && defaultBarY < conflictBottomY
                    && barBottom > conflictTopY;
        }

        if (!overlaps) {
            return new HudPosition(defaultBarX, defaultBarY, false);
        }

        if (mode == ModConfig.TraumaHudConflictMode.RIGHT_CENTER) {
            int rightX = Math.max(4, screenWidth - totalWidth - 6);
            int centerY = screenHeight / 2 - HEART_SIZE / 2;
            return new HudPosition(rightX, centerY, true);
        }

        int aboveY = Math.min(defaultBarY, conflictTopY - 2 - HEART_SIZE);
        return new HudPosition(defaultBarX, aboveY, true);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Player player, Font font) {
        MobEffectInstance effect = player.getEffect(ModStatusEffects.DESPERATE_STAND);
        if (effect == null) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        boolean canHurtPlayer = mc.gameMode == null || mc.gameMode.canHurtPlayer();
        int vanillaTopY = canHurtPlayer ? computeVanillaLeftHudTopY(player, screenHeight) : (screenHeight - 39);

        String traumaStr = DesperateStandHelper.formatTraumaValue(clientTrauma);
        String timeStr = effect.isInfiniteDuration()
                ? "∞"
                : Integer.toString(Math.max(0, (int) Math.ceil(effect.getDuration() / 20.0)));
        Component label = Component.translatable("hud.tuanzis_mod.trauma_bar", traumaStr, timeStr);
        int textWidth = font.width(label);

        boolean cinderVisible = CinderHeatHud.isVisible();
        HudPosition pos = computePosition(
                screenWidth,
                screenHeight,
                vanillaTopY,
                textWidth,
                cinderVisible,
                CinderHeatHud.getHudLeftX(screenWidth),
                CinderHeatHud.getHudTopY(screenHeight),
                CinderHeatHud.getHudRightX(screenWidth),
                CinderHeatHud.getHudBottomY(screenHeight),
                ModConfig.getTraumaHudConflictMode()
        );

        int barX = pos.barX();
        int barY = pos.barY();

        // 与原版血量一致：每颗心 = 2 点，半心 = 1 点，向上取整为半心数，最多渲染 10 颗满心
        int filledHalves = Math.min(HEART_COUNT * 2, Mth.ceil(Math.max(0.0f, clientTrauma)));
        for (int i = 0; i < HEART_COUNT; i++) {
            int xo = barX + i * 8;
            graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_TEX, xo, barY, 0.0F, 0.0F, 9, 9, 9, 9);

            int halves = i * 2;
            if (halves + 1 < filledHalves) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_FULL_TEX, xo, barY, 0.0F, 0.0F, 9, 9, 9, 9);
            } else if (halves + 1 == filledHalves) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_HALF_TEX, xo, barY, 0.0F, 0.0F, 9, 9, 9, 9);
            }
        }

        int textX = barX + HEART_BAR_WIDTH + TEXT_GAP;
        int textY = barY + 1;
        int textColor = clientTrauma > 0.0f ? 0xFFF5F7FA : 0xFFCBD5E1;
        graphics.text(font, label, textX, textY, textColor, true);
    }
}
