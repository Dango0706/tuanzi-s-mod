package me.tuanzi.client.gui.screens.scavenger;

import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.data.ScavengerTooltipHelper;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerRarity;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import me.tuanzi.world.scavenger.loot.search.SearchEntryPhase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * 拾荒回廊二维战术网格渲染核心引擎。
 * 提供品质边框、扫光、迷雾遮罩、旋转雷达扫描、拖拽吸附投影与富文本检视。
 */
@Environment(EnvType.CLIENT)
public final class ScavengerGridRenderer {

    public static final int CELL_SIZE = 18;

    private ScavengerGridRenderer() {
    }

    /**
     * 绘制基础网格背景底框。
     */
    public static void renderGridBackground(
            GuiGraphicsExtractor graphics,
            int startX, int startY,
            int columns, int rows
    ) {
        int totalW = columns * CELL_SIZE;
        int totalH = rows * CELL_SIZE;

        // 大背景
        graphics.fill(startX - 1, startY - 1, startX + totalW + 1, startY + totalH + 1, 0xFF141922);
        graphics.fill(startX, startY, startX + totalW, startY + totalH, 0xD00A0D12);

        // 细格子线
        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                int cx = startX + c * CELL_SIZE;
                int cy = startY + r * CELL_SIZE;
                // 绘制内凹网格单元 (18x18)
                graphics.fill(cx, cy, cx + CELL_SIZE, cy + 1, 0x303E4C5E);
                graphics.fill(cx, cy, cx + 1, cy + CELL_SIZE, 0x303E4C5E);
                graphics.fill(cx + CELL_SIZE - 1, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0x15FFFFFF);
                graphics.fill(cx, cy + CELL_SIZE - 1, cx + CELL_SIZE, cy + CELL_SIZE, 0x15FFFFFF);
            }
        }
    }

    /**
     * 渲染个人装备网格（背包 / 安全箱）中的已确定物品列表。
     */
    public static void renderGridEntries(
            GuiGraphicsExtractor graphics,
            Font font,
            int startX, int startY,
            List<GridItemEntry> entries,
            int mouseX, int mouseY
    ) {
        renderGridEntries(graphics, font, startX, startY, entries, mouseX, mouseY, -1);
    }

    public static void renderGridEntries(
            GuiGraphicsExtractor graphics,
            Font font,
            int startX, int startY,
            List<GridItemEntry> entries,
            int mouseX, int mouseY,
            int draggedEntryId
    ) {
        GridItemEntry hoveredEntry = null;

        for (GridItemEntry entry : entries) {
            int x = startX + entry.anchorX() * CELL_SIZE;
            int y = startY + entry.anchorY() * CELL_SIZE;
            int w = entry.effectiveWidth() * CELL_SIZE;
            int h = entry.effectiveHeight() * CELL_SIZE;

            boolean isDragged = entry.entryId() == draggedEntryId;
            boolean isHovered = !isDragged && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            if (isHovered) {
                hoveredEntry = entry;
            }

            renderItemEntryBox(graphics, font, entry.stack(), entry.resolveMeta(), x, y, w, h, isHovered, entry.rotated(), isDragged);
        }

        // 悬浮 Tooltip (被拖拽抓取的条目不显示原地 Tooltip)
        if (hoveredEntry != null && hoveredEntry.entryId() != draggedEntryId) {
            List<Component> tooltip = ScavengerTooltipHelper.buildTooltipLines(hoveredEntry.stack(), true);
            graphics.setTooltipForNextFrame(font, tooltip, hoveredEntry.stack().getTooltipImage(), mouseX, mouseY);
        }
    }

    /**
     * 渲染野外搜刮容器中的脱敏网格快照（包含未搜索迷雾与逐刻雷达读条）。
     */
    public static void renderRedactedViews(
            GuiGraphicsExtractor graphics,
            Font font,
            int startX, int startY,
            Collection<RedactedGridEntryView> views,
            int mouseX, int mouseY,
            @Nullable String nameFilter
    ) {
        renderRedactedViews(graphics, font, startX, startY, views, mouseX, mouseY, nameFilter, -1);
    }

    public static void renderRedactedViews(
            GuiGraphicsExtractor graphics,
            Font font,
            int startX, int startY,
            Collection<RedactedGridEntryView> views,
            int mouseX, int mouseY,
            @Nullable String nameFilter,
            int draggedEntryId
    ) {
        RedactedGridEntryView hoveredView = null;

        for (RedactedGridEntryView view : views) {
            int x = startX + view.anchorX() * CELL_SIZE;
            int y = startY + view.anchorY() * CELL_SIZE;
            int w = view.effectiveWidth() * CELL_SIZE;
            int h = view.effectiveHeight() * CELL_SIZE;

            boolean isDragged = view.entryId() == draggedEntryId;
            boolean isHovered = !isDragged && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            if (isHovered) {
                hoveredView = view;
            }

            if (view.phase() == SearchEntryPhase.REVEALED) {
                // 已揭示
                ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(view.stack());
                // 若有过滤词且未命中，降低透明度
                boolean matchesFilter = nameFilter == null || nameFilter.isBlank()
                        || view.stack().getHoverName().getString().toLowerCase().contains(nameFilter.toLowerCase());
                renderItemEntryBox(graphics, font, view.stack(), meta, x, y, w, h, isHovered && matchesFilter, view.rotated(), isDragged);
            } else if (view.phase() == SearchEntryPhase.SEARCHING) {
                // 正在搜索中：雷达扫描与百分比环
                renderSearchingBox(graphics, font, x, y, w, h, view.progressFraction(), isHovered);
            } else {
                // 未搜索迷雾遮罩
                renderIdleFogBox(graphics, font, x, y, w, h, isHovered);
            }
        }

        // 已揭示条目 Tooltip
        if (hoveredView != null && hoveredView.phase() == SearchEntryPhase.REVEALED && !hoveredView.stack().isEmpty() && hoveredView.entryId() != draggedEntryId) {
            List<Component> tooltip = ScavengerTooltipHelper.buildTooltipLines(hoveredView.stack(), true);
            graphics.setTooltipForNextFrame(font, tooltip, hoveredView.stack().getTooltipImage(), mouseX, mouseY);
        }
    }

    /**
     * 绘制单个已揭示物品的品质底框、边框、扫光与图标。
     */
    private static void renderItemEntryBox(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            ScavengerItemMeta meta,
            int x, int y, int w, int h,
            boolean isHovered,
            boolean rotated,
            boolean isGhostOrigin
    ) {
        if (isGhostOrigin) {
            // 被抓起后的原位幽灵占位框：低透明度虚影
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x1A445566);
            drawBorder(graphics, x, y, w, h, 0x4488AABB);
            return;
        }

        ScavengerRarity rarity = meta != null ? meta.rarity() : ScavengerRarity.COMMON;
        int colorRgb = rarity.colorRgb() & 0x00FFFFFF;

        // 1. 半透明品质底色填充 (Hover 时稍微提亮)
        int bgAlpha = isHovered ? 0x55000000 : 0x30000000;
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgAlpha | colorRgb);

        // 2. 品质边框
        int borderAlpha = isHovered ? 0xFF000000 : 0xAA000000;
        int borderColor = borderAlpha | colorRgb;

        // 绝密 (MYTHIC) 呼吸红光动画
        if (rarity == ScavengerRarity.MYTHIC) {
            double pulse = (Math.sin(System.currentTimeMillis() / 180.0) + 1.0) / 2.0;
            int redAlpha = (int) (180 + 75 * pulse);
            borderColor = (redAlpha << 24) | 0x00FF3333;
        }

        drawBorder(graphics, x, y, w, h, borderColor);

        // 3. 传说 (LEGENDARY) 45° 金色扫光效果
        if (rarity == ScavengerRarity.LEGENDARY) {
            int sweepOffset = (int) ((System.currentTimeMillis() / 25) % (w + h));
            int sx = x + sweepOffset;
            int sy = y;
            if (sx < x + w && sy < y + h) {
                graphics.fill(Math.min(sx, x + w - 2), y + 1, Math.min(sx + 2, x + w - 1), y + h - 1, 0x50FFF275);
            }
        }

        // 4. 居中渲染物品图标
        int itemX = x + (w - 16) / 2;
        int itemY = y + (h - 16) / 2;
        graphics.item(stack, itemX, itemY);
        graphics.itemDecorations(font, stack, itemX, itemY);

        // 5. 旋转标记 (若处于旋转状态且非正方形)
        if (rotated && (meta == null || meta.width() != meta.height())) {
            graphics.text(font, "⟳", x + 2, y + 2, 0xFF00E5FF, true);
        }
    }

    /**
     * 绘制正在搜索中的雷达旋转动画与进度条。
     */
    private static void renderSearchingBox(
            GuiGraphicsExtractor graphics,
            Font font,
            int x, int y, int w, int h,
            float progress,
            boolean isHovered
    ) {
        // 深蓝扫描背景
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xDD0D1B2A);
        drawBorder(graphics, x, y, w, h, isHovered ? 0xFFFFFFFF : 0xFF00E5FF);

        // 扫描进度条 (底部 3px)
        int barW = Math.max(0, (int) ((w - 4) * Math.min(1.0f, progress)));
        graphics.fill(x + 2, y + h - 5, x + w - 2, y + h - 2, 0x80000000);
        graphics.fill(x + 2, y + h - 5, x + 2 + barW, y + h - 2, 0xFF00E5FF);

        // 居中百分比文字 (仅创造模式下显示辅助调试，生存/冒险模式下隐藏以增强硬核探索悬疑感)
        net.minecraft.client.player.LocalPlayer clientPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (clientPlayer != null && clientPlayer.isCreative()) {
            String pctStr = String.format("%.0f%%", progress * 100.0f);
            int textW = font.width(pctStr);
            graphics.text(font, pctStr, x + (w - textW) / 2, y + (h - 8) / 2 - 2, 0xFFE0FBFC, true);
        }

        // 旋转雷达点
        double angle = (System.currentTimeMillis() / 150.0) % (2 * Math.PI);
        int r = Math.min(w, h) / 3;
        int dotX = x + w / 2 + (int) (r * Math.cos(angle));
        int dotY = y + h / 2 + (int) (r * Math.sin(angle));
        graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF70EEFF);
    }

    /**
     * 绘制未搜索条目的斜纹迷雾遮罩。
     */
    private static void renderIdleFogBox(
            GuiGraphicsExtractor graphics,
            Font font,
            int x, int y, int w, int h,
            boolean isHovered
    ) {
        // 暗灰实心迷雾底
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xEE161D26);
        drawBorder(graphics, x, y, w, h, isHovered ? 0xFF00E5FF : 0xFF3D4856);

        // 居中问号与暗纹
        String qStr = "?";
        int textW = font.width(qStr);
        graphics.text(font, qStr, x + (w - textW) / 2, y + (h - 8) / 2, isHovered ? 0xFF00E5FF : 0xFF6C7A89, true);
    }

    /**
     * 绘制拖拽放置时的合法（绿）或非法（红）吸附投影。
     */
    public static void renderGhostProjection(
            GuiGraphicsExtractor graphics,
            int startX, int startY,
            int targetCol, int targetRow,
            int widthCells, int heightCells,
            boolean isValid
    ) {
        int x = startX + targetCol * CELL_SIZE;
        int y = startY + targetRow * CELL_SIZE;
        int w = widthCells * CELL_SIZE;
        int h = heightCells * CELL_SIZE;

        int fillColor = isValid ? 0x552ECC71 : 0x55E74C3C;
        int borderColor = isValid ? 0xFF2ECC71 : 0xFFE74C3C;

        graphics.fill(x, y, x + w, y + h, fillColor);
        drawBorder(graphics, x, y, w, h, borderColor);
    }

    /**
     * 检测目标矩形在 GridItemEntry 集合中是否存在碰撞重叠。
     *
     * @param targetCol      目标起始列
     * @param targetRow      目标起始行
     * @param widthCells     占用列宽
     * @param heightCells    占用行高
     * @param ignoredEntryId 忽略的条目 ID（如当前拖拽条目自身，无忽略传 -1）
     * @param entries        已有条目集合
     * @return 若发生碰撞返回 true，无碰撞返回 false
     */
    public static boolean hasCollision(
            int targetCol, int targetRow,
            int widthCells, int heightCells,
            int ignoredEntryId,
            Collection<GridItemEntry> entries
    ) {
        if (entries == null || entries.isEmpty()) return false;
        for (GridItemEntry entry : entries) {
            if (entry.entryId() == ignoredEntryId) continue;
            int eCol = entry.anchorX();
            int eRow = entry.anchorY();
            int eW = entry.effectiveWidth();
            int eH = entry.effectiveHeight();
            boolean overlaps = !(targetCol + widthCells <= eCol || targetCol >= eCol + eW ||
                    targetRow + heightCells <= eRow || targetRow >= eRow + eH);
            if (overlaps) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测目标矩形在 RedactedGridEntryView 集合中是否存在碰撞重叠（适用于搜刮容器与基地仓库）。
     */
    public static boolean hasRedactedCollision(
            int targetCol, int targetRow,
            int widthCells, int heightCells,
            int ignoredEntryId,
            Collection<RedactedGridEntryView> views
    ) {
        if (views == null || views.isEmpty()) return false;
        for (RedactedGridEntryView view : views) {
            if (view.entryId() == ignoredEntryId) continue;
            int eCol = view.anchorX();
            int eRow = view.anchorY();
            int eW = view.effectiveWidth();
            int eH = view.effectiveHeight();
            boolean overlaps = !(targetCol + widthCells <= eCol || targetCol >= eCol + eW ||
                    targetRow + heightCells <= eRow || targetRow >= eRow + eH);
            if (overlaps) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验当前落位是否合法（在网格范围内且不与任何已有物品发生空间重叠）。
     */
    public static boolean isPlacementValid(
            int targetCol, int targetRow,
            int widthCells, int heightCells,
            int totalCols, int totalRows,
            int ignoredEntryId,
            Collection<GridItemEntry> entries
    ) {
        boolean inBounds = targetCol >= 0 && targetCol + widthCells <= totalCols &&
                targetRow >= 0 && targetRow + heightCells <= totalRows;
        if (!inBounds) return false;
        return !hasCollision(targetCol, targetRow, widthCells, heightCells, ignoredEntryId, entries);
    }

    /**
     * 校验当前落位是否合法（适用于 RedactedGridEntryView 视图）。
     */
    public static boolean isRedactedPlacementValid(
            int targetCol, int targetRow,
            int widthCells, int heightCells,
            int totalCols, int totalRows,
            int ignoredEntryId,
            Collection<RedactedGridEntryView> views
    ) {
        boolean inBounds = targetCol >= 0 && targetCol + widthCells <= totalCols &&
                targetRow >= 0 && targetRow + heightCells <= totalRows;
        if (!inBounds) return false;
        return !hasRedactedCollision(targetCol, targetRow, widthCells, heightCells, ignoredEntryId, views);
    }

    public static @Nullable GridItemEntry findClickedEntry(
            int mouseX, int mouseY,
            int startX, int startY,
            List<GridItemEntry> entries
    ) {
        for (GridItemEntry entry : entries) {
            int x = startX + entry.anchorX() * CELL_SIZE;
            int y = startY + entry.anchorY() * CELL_SIZE;
            int w = entry.effectiveWidth() * CELL_SIZE;
            int h = entry.effectiveHeight() * CELL_SIZE;
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
                return entry;
            }
        }
        return null;
    }

    public static @Nullable RedactedGridEntryView findClickedRedactedView(
            int mouseX, int mouseY,
            int startX, int startY,
            Collection<RedactedGridEntryView> views
    ) {
        for (RedactedGridEntryView view : views) {
            int x = startX + view.anchorX() * CELL_SIZE;
            int y = startY + view.anchorY() * CELL_SIZE;
            int w = view.effectiveWidth() * CELL_SIZE;
            int h = view.effectiveHeight() * CELL_SIZE;
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
                return view;
            }
        }
        return null;
    }

    public static int @Nullable [] getCellCoordinates(
            int mouseX, int mouseY,
            int startX, int startY,
            int columns, int rows
    ) {
        if (mouseX < startX || mouseY < startY) return null;
        int col = (mouseX - startX) / CELL_SIZE;
        int row = (mouseY - startY) / CELL_SIZE;
        if (col >= 0 && col < columns && row >= 0 && row < rows) {
            return new int[]{col, row};
        }
        return null;
    }

    /**
     * 绘制跟随鼠标悬浮的手持物品预览（包含大件半透明外框、居中图标与数量角标）。
     */
    public static void renderFloatingDragItem(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            int effectiveWidthCells,
            int effectiveHeightCells,
            int grabOffsetX,
            int grabOffsetY,
            boolean rotated,
            int mouseX,
            int mouseY
    ) {
        if (stack == null || stack.isEmpty()) return;

        int wPx = effectiveWidthCells * CELL_SIZE;
        int hPx = effectiveHeightCells * CELL_SIZE;
        int floatX = mouseX - grabOffsetX * CELL_SIZE;
        int floatY = mouseY - grabOffsetY * CELL_SIZE;

        // 半透明科技蓝悬浮外框
        graphics.fill(floatX, floatY, floatX + wPx, floatY + hPx, 0x500A1C2A);
        drawBorder(graphics, floatX, floatY, wPx, hPx, 0xC000E5FF);

        // 居中图标
        int itemX = floatX + (wPx - 16) / 2;
        int itemY = floatY + (hPx - 16) / 2;
        graphics.item(stack, itemX, itemY);
        graphics.itemDecorations(font, stack, itemX, itemY);

        if (rotated) {
            graphics.text(font, "⟳", floatX + 2, floatY + 2, 0xFF00E5FF, true);
        }
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
