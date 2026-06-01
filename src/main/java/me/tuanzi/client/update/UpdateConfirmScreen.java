package me.tuanzi.client.update;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class UpdateConfirmScreen extends Screen {
    private final Screen parent;
    private List<FormattedCharSequence> changelogLines = new ArrayList<>();
    private double scrollAmount = 0.0;
    private double maxScrollAmount = 0.0;
    private boolean isDownloading = false;
    private boolean isDownloaded = false;
    private boolean downloadFailed = false;
    private String statusText = "";

    public UpdateConfirmScreen(Screen parent) {
        super(Component.literal("模组更新提示"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int panelWidth = 320;
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        if (this.isDownloading) {
            // 下载状态不显示常规按钮，只显示文字
            return;
        }

        if (this.isDownloaded) {
            // 下载完成后提示立即重启或稍后重启
            this.addRenderableWidget(
                Button.builder(Component.literal("立即重启"), button -> this.minecraft.stop())
                    .bounds(this.width / 2 - 110, panelY + panelHeight - 35, 100, 20)
                    .build()
            );
            this.addRenderableWidget(
                Button.builder(Component.literal("稍后重启"), button -> this.minecraft.setScreen(this.parent))
                    .bounds(this.width / 2 + 10, panelY + panelHeight - 35, 100, 20)
                    .build()
            );
            return;
        }

        // 常规状态下的升级面板按钮
        int btnWidth = 80;
        int btnY = panelY + panelHeight - 35;
        
        // 自动更新
        this.addRenderableWidget(
            Button.builder(Component.literal("自动更新"), button -> {
                this.isDownloading = true;
                this.statusText = "正在下载更新包，请稍候...";
                this.init(); // 重新加载界面按钮
                UpdateChecker.downloadAndInstall().thenAccept(success -> {
                    this.minecraft.execute(() -> {
                        this.isDownloading = false;
                        if (success) {
                            this.isDownloaded = true;
                            this.statusText = "更新成功！重启游戏后生效。";
                        } else {
                            this.downloadFailed = true;
                            this.statusText = "下载更新失败，请检查网络后再试！";
                        }
                        this.init();
                    });
                });
            })
            .bounds(panelX + 25, btnY, btnWidth, 20)
            .build()
        );

        // 忽略更新一天
        this.addRenderableWidget(
            Button.builder(Component.literal("忽略更新一天"), button -> {
                UpdateChecker.saveIgnoreTime();
                UpdateChecker.setAlreadyNotified(true); // 确保当次关闭阻断
                this.minecraft.setScreen(this.parent);
            })
            .bounds(panelX + 120, btnY, btnWidth, 20)
            .build()
        );

        // 暂不更新
        this.addRenderableWidget(
            Button.builder(Component.literal("暂不更新"), button -> {
                UpdateChecker.setAlreadyNotified(true); // 本次游戏不再弹窗
                this.minecraft.setScreen(this.parent);
            })
            .bounds(panelX + 215, btnY, btnWidth, 20)
            .build()
        );

        // 预先使用自定义 Markdown 解析并切分折行，缓存至内存中，避免 extractRenderState 产生高频 GC
        String rawChangelog = UpdateChecker.getChangelog();
        Component parsedChangelog = SimpleMarkdownParser.parse(rawChangelog);
        this.changelogLines = this.font.split(parsedChangelog, panelWidth - 40);

        int contentHeight = this.changelogLines.size() * 12;
        int viewHeight = 120; // 55 到 175 像素区间，高度为 120 像素
        this.maxScrollAmount = Math.max(0, contentHeight - viewHeight);
        this.scrollAmount = 0.0; // 重设初始滚动量
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScrollAmount > 0) {
            // 支持滚轮上下拖拽，速度为每格 12 像素
            this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 12.0, 0.0, this.maxScrollAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // 先渲染父界面，营造背景毛玻璃/遮罩的高端质感 (传入 -999, -999 以屏蔽原版主界面的按钮悬停 Hover，防止穿透)
        if (this.parent != null) {
            this.parent.extractRenderState(graphics, -999, -999, a);
        }

        // 渐变全屏墨黑遮罩
        graphics.fill(0, 0, this.width, this.height, ARGB.color(180, 10, 10, 15));

        int panelWidth = 320;
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        // 绘制质感磨砂卡片背景 (深蓝色系微渐变边框，彰显高级感)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, ARGB.color(230, 20, 20, 30));
        graphics.outline(panelX, panelY, panelWidth, panelHeight, ARGB.color(255, 66, 133, 244)); // 优雅蔚蓝描边

        // 渲染主标题
        graphics.centeredText(this.font, "发现团子模组更新！", this.width / 2, panelY + 15, ARGB.color(255, 255, 220, 100));

        // 渲染版本对比信息
        String currentVersion = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("tuanzis_mod")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.5");
        String versionCompareStr = "当前版本: v" + currentVersion + "  ->  最新版本: v" + UpdateChecker.getLatestVersion();
        graphics.centeredText(this.font, versionCompareStr, this.width / 2, panelY + 35, ARGB.color(255, 180, 220, 255));

        int scrollAreaY0 = panelY + 55;
        int scrollAreaY1 = panelY + 175;
        int viewHeight = 120;

        if (this.isDownloading || this.isDownloaded || this.downloadFailed) {
            // 下载/成功/失败状态绘制提示文字
            graphics.centeredText(this.font, this.statusText, this.width / 2, panelY + 100, ARGB.color(255, 255, 255, 255));
        } else {
            // 1. 裁剪区域限制绘制：仅渲染可视纵轴 55px 至 175px 之间的文字段落
            graphics.enableScissor(panelX + 15, scrollAreaY0, panelX + panelWidth - 15, scrollAreaY1);

            int startY = panelY + 58 - (int) this.scrollAmount;
            for (int j = 0; j < this.changelogLines.size(); j++) {
                int lineY = startY + j * 12;
                // 仅对位于可见范围之内的单行进行绘制调用，达成高频率渲染裁剪优化
                if (lineY + 9 >= scrollAreaY0 && lineY <= scrollAreaY1) {
                    graphics.text(this.font, this.changelogLines.get(j), panelX + 20, lineY, ARGB.color(255, 255, 255, 255));
                }
            }

            graphics.disableScissor();

            // 2. 如果存在超出视口高度的内容，绘制极富工业科技感的定制蔚蓝色滚动条
            if (this.maxScrollAmount > 0) {
                int scrollbarX = panelX + panelWidth - 9;
                int scrollbarBgColor = ARGB.color(60, 255, 255, 255); // 极其柔和的磨砂玻璃白
                int scrollerColor = ARGB.color(200, 66, 133, 244);    // 尊贵蔚蓝色滑块
                
                int scrollerHeight = Math.max(15, (int) ((float) (viewHeight * viewHeight) / (this.changelogLines.size() * 12)));
                int scrollerY = scrollAreaY0 + (int) (this.scrollAmount * (viewHeight - scrollerHeight) / this.maxScrollAmount);
                
                // 绘制滚动条滑道背景
                graphics.fill(scrollbarX, scrollAreaY0, scrollbarX + 2, scrollAreaY1, scrollbarBgColor);
                // 绘制可移动的蔚蓝色滑块
                graphics.fill(scrollbarX - 1, scrollerY, scrollbarX + 3, scrollerY + scrollerHeight, scrollerColor);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.isDownloading && !this.isDownloaded;
    }
}
