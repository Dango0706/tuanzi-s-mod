package me.tuanzi.client.gui.screens;

import me.tuanzi.network.PaintBucketColorPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.EditBox;

@Environment(EnvType.CLIENT)
public class PaintBucketScreen extends Screen {
    private final InteractionHand hand;
    private final int initialColor;

    private int r;
    private int g;
    private int b;

    private RGBSlider rSlider;
    private RGBSlider gSlider;
    private RGBSlider bSlider;
    private EditBox hexInput;

    // 16 种原版染料的经典 RGB 值
    private static final int[] PRESET_COLORS = new int[]{
        0xF9FFFE, // 0: 白色
        0xF9801D, // 1: 橙色
        0xC74EBD, // 2: 品红色
        0x3AB3DA, // 3: 淡蓝色
        0xFED83D, // 4: 黄色
        0x80C71F, // 5: 黄绿色
        0xF38BAA, // 6: 粉红色
        0x474F52, // 7: 灰色
        0x9D9D97, // 8: 淡灰色
        0x169C9C, // 9: 青色
        0x893293, // 10: 紫色
        0x3C44AA, // 11: 蓝色
        0x835432, // 12: 棕色
        0x5E7C16, // 13: 绿色
        0xB02E26, // 14: 红色
        0x1D1D21  // 15: 黑色
    };

    private int presetX;
    private int presetY;

    public PaintBucketScreen(InteractionHand hand, int initialColor) {
        super(Component.translatable("gui.tuanzis_mod.paint_bucket.title"));
        this.hand = hand;
        this.initialColor = initialColor;

        this.r = (initialColor >> 16) & 0xFF;
        this.g = (initialColor >> 8) & 0xFF;
        this.b = initialColor & 0xFF;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int bgStartX = centerX - 150;
        int bgStartY = centerY - 75;

        int startX = bgStartX + 10;
        int startY = bgStartY;

        // 初始化 RGB 3 个滑块
        this.rSlider = new RGBSlider(startX, startY + 20, 120, 20, "R", this.r / 255.0, val -> {
            this.r = val;
            this.updateHexInputFromRGB();
        });
        this.gSlider = new RGBSlider(startX, startY + 44, 120, 20, "G", this.g / 255.0, val -> {
            this.g = val;
            this.updateHexInputFromRGB();
        });
        this.bSlider = new RGBSlider(startX, startY + 68, 120, 20, "B", this.b / 255.0, val -> {
            this.b = val;
            this.updateHexInputFromRGB();
        });

        this.addRenderableWidget(this.rSlider);
        this.addRenderableWidget(this.gSlider);
        this.addRenderableWidget(this.bSlider);

        // 初始化 Hex 输入框
        this.hexInput = new EditBox(this.font, startX, startY + 92, 120, 20, Component.literal("Hex"));
        this.hexInput.setMaxLength(7);
        this.hexInput.setValue(String.format("%02X%02X%02X", this.r, this.g, this.b));
        this.hexInput.setResponder(val -> {
            String hex = val.trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() == 6) {
                try {
                    int parsedColor = Integer.parseInt(hex, 16);
                    int nr = (parsedColor >> 16) & 0xFF;
                    int ng = (parsedColor >> 8) & 0xFF;
                    int nb = parsedColor & 0xFF;
                    
                    if (nr != this.r || ng != this.g || nb != this.b) {
                        this.r = nr;
                        this.g = ng;
                        this.b = nb;
                        
                        this.rSlider.setValueExternal(this.r / 255.0);
                        this.gSlider.setValueExternal(this.g / 255.0);
                        this.bSlider.setValueExternal(this.b / 255.0);
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        });
        this.addRenderableWidget(this.hexInput);

        // 确定与取消按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.paint_bucket.confirm"), btn -> {
            int finalColor = (this.r << 16) | (this.g << 8) | this.b;
            ClientPlayNetworking.send(new PaintBucketColorPacket(finalColor, this.hand == InteractionHand.MAIN_HAND));
            this.onClose();
        }).bounds(startX, startY + 120, 58, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.paint_bucket.cancel"), btn -> {
            this.onClose();
        }).bounds(startX + 62, startY + 120, 58, 20).build());

        // 计算 16 个颜色预设方块区域起始位置 (大方块预览的宽度是 110，预设 8 个的跨度也是 110，右移 5 像素以防数值标签遮挡)
        this.presetX = centerX + 20;
        this.presetY = startY + 92;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int startX = centerX - 150;
        int startY = centerY - 75;

        // 1. 绘制半透明黑色背景板 (WOW 视觉，扩大为 300 x 150)
        graphics.fill(startX, startY, startX + 300, startY + 150, 0xDD000000);

        // 2. 绘制标题
        graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, startY + 5, 0xFFFFFFFF, false);

        // 3. 绘制右侧当前颜色大方块预览 (扩大为 110 x 60，右移 5 像素以防遮挡)
        int previewX = centerX + 20;
        int previewY = startY + 20;
        int previewW = 110;
        int previewH = 60;
        int previewColor = (255 << 24) | (this.r << 16) | (this.g << 8) | this.b;
        
        // 预览颜色大方块
        graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, previewColor);
        // 外围黑色立体边框
        graphics.fill(previewX - 1, previewY - 1, previewX + previewW + 1, previewY, 0xFF555555);
        graphics.fill(previewX - 1, previewY + previewH, previewX + previewW + 1, previewY + previewH + 1, 0xFF555555);
        graphics.fill(previewX - 1, previewY - 1, previewX, previewY + previewH + 1, 0xFF555555);
        graphics.fill(previewX + previewW, previewY - 1, previewX + previewW + 1, previewY + previewH + 1, 0xFF555555);

        // 4. 绘制 16 种原版染料的预设颜色小按钮 (每个 12x12，间距 2 像素)
        for (int i = 0; i < 16; i++) {
            int row = i / 8;
            int col = i % 8;
            int bx = this.presetX + col * 14;
            int by = this.presetY + row * 14;

            int colorVal = (255 << 24) | PRESET_COLORS[i];
            graphics.fill(bx, by, bx + 12, by + 12, colorVal);

            // 悬停或者常规画边框
            boolean isHovered = mouseX >= bx && mouseX < bx + 12 && mouseY >= by && mouseY < by + 12;
            int borderCol = isHovered ? 0xFFFFFFFF : 0xFF333333;
            graphics.fill(bx, by, bx + 12, by + 1, borderCol);
            graphics.fill(bx, by + 11, bx + 12, by + 12, borderCol);
            graphics.fill(bx, by, bx + 1, by + 12, borderCol);
            graphics.fill(bx + 11, by, bx + 12, by + 12, borderCol);
        }

        // 5. 绘制滑块及输入框旁边的 RGB/Hex 精确数值显示标签 (对齐 Y 轴垂直居中)
        graphics.text(this.font, "R: " + this.r, startX + 134, startY + 26, 0xFFFF5555, false);
        graphics.text(this.font, "G: " + this.g, startX + 134, startY + 50, 0xFF55FF55, false);
        graphics.text(this.font, "B: " + this.b, startX + 134, startY + 74, 0xFF5555FF, false);
        graphics.text(this.font, "Hex", startX + 134, startY + 98, 0xFFCCCCCC, false);

        // 6. 提取所有组件的渲染状态，使其在上层正常渲染
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void updateHexInputFromRGB() {
        if (this.hexInput != null) {
            String currentHex = this.hexInput.getValue().trim();
            if (currentHex.startsWith("#")) {
                currentHex = currentHex.substring(1);
            }
            String newHex = String.format("%02X%02X%02X", this.r, this.g, this.b);
            if (!currentHex.equalsIgnoreCase(newHex)) {
                this.hexInput.setValue(newHex);
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        // 判定是否点击了 16 预设颜色方块 (调整点击框范围为 12x12，间距 2 像素)
        for (int i = 0; i < 16; i++) {
            int row = i / 8;
            int col = i % 8;
            int bx = this.presetX + col * 14;
            int by = this.presetY + row * 14;

            if (mouseX >= bx && mouseX < bx + 12 && mouseY >= by && mouseY < by + 12) {
                int color = PRESET_COLORS[i];
                this.r = (color >> 16) & 0xFF;
                this.g = (color >> 8) & 0xFF;
                this.b = color & 0xFF;

                this.rSlider.setValueExternal(this.r / 255.0);
                this.gSlider.setValueExternal(this.g / 255.0);
                this.bSlider.setValueExternal(this.b / 255.0);
                this.updateHexInputFromRGB();

                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 自定义滑块组件
     */
    private static class RGBSlider extends net.minecraft.client.gui.components.AbstractSliderButton {
        private final java.util.function.Consumer<Integer> updateCallback;
        private final String prefix;

        public RGBSlider(int x, int y, int width, int height, String prefix, double initialValue, java.util.function.Consumer<Integer> updateCallback) {
            super(x, y, width, height, Component.literal(prefix + ": " + (int)(initialValue * 255)), initialValue);
            this.prefix = prefix;
            this.updateCallback = updateCallback;
        }

        public void setValueExternal(double val) {
            this.value = val;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(prefix + ": " + (int)(this.value * 255)));
        }

        @Override
        protected void applyValue() {
            this.updateCallback.accept((int)(this.value * 255));
        }
    }
}
