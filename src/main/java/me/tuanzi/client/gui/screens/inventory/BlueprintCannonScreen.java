package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.network.BlueprintCannonActionPacket;
import me.tuanzi.world.inventory.BlueprintCannonMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BlueprintCannonScreen extends AbstractContainerScreen<BlueprintCannonMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    private Button startButton;
    private Button pauseButton;
    private Button terminateButton;
    private Button materialListButton;
    private Button settingsButton;

    public BlueprintCannonScreen(BlueprintCannonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186); // 增加高度以容纳控制区与进度条
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
          // 开始 / 继续按钮 (▶)
        this.startButton = this.addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
            sendAction("START");
        }).bounds(x + 8, y + 17, 16, 16).build());

        // 暂停按钮 (‖)
        this.pauseButton = this.addRenderableWidget(Button.builder(Component.literal("‖"), btn -> {
            sendAction("PAUSE");
        }).bounds(x + 26, y + 17, 16, 16).build());

        // 终止按钮 (■)
        this.terminateButton = this.addRenderableWidget(Button.builder(Component.literal("■"), btn -> {
            sendAction("TERMINATE");
        }).bounds(x + 44, y + 17, 16, 16).build());

        // 设置按钮 (切换销毁模式 / 精确匹配等)
        this.settingsButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_cannon.settings"), btn -> {
            boolean nextDestroy = !menu.isDestroyMode();
            sendAction(menu.getBuildState() == 1 ? "START" : (menu.getBuildState() == 2 ? "PAUSE" : "TERMINATE"), nextDestroy, menu.isExactMatch());
        }).bounds(x + 120, y + 17, 48, 16).build());

        // 材料清单入口
        this.materialListButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_cannon.material_list"), btn -> {
            BlockPos pos = menu.getCannonPos();
            ClientPlayNetworking.send(new me.tuanzi.network.BlueprintMaterialBookPacket(pos));
        }).bounds(x + 8, y + 35, 48, 16).build());
    }

    private void sendAction(String action) {
        sendAction(action, menu.isDestroyMode(), menu.isExactMatch());
    }

    private void sendAction(String action, boolean destroyMode, boolean exactMatch) {
        BlockPos cannonPos = menu.getCannonPos();
        ClientPlayNetworking.send(new BlueprintCannonActionPacket(cannonPos, action, destroyMode, exactMatch));
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;

        // 绘制原版 dispenser 框体
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, 176, 166, 256, 256);
        // 增高区域绘制灰色底板
        graphics.fill(x, y + 166, x + 176, y + 186, 0xFFC6C6C6);
        graphics.fill(x, y + 166, x + 1, y + 186, 0xFFFFFFFF); // 左边高亮线
        graphics.fill(x + 175, y + 166, x + 176, y + 186, 0xFF555555); // 右边阴影线

        // 覆盖非活动槽
        int[][] disabledSlots = {
            {62, 17}, {80, 17}, {98, 17},
            {80, 35},
            {62, 53},           {98, 53}
        };
        for (int[] ds : disabledSlots) {
            graphics.fill(x + ds[0] - 1, y + ds[1] - 1, x + ds[0] + 17, y + ds[1] + 17, 0xAA2E2E2E);
        }

        // 绘制浆料燃料格高亮 (98, 35)
        graphics.fill(x + 98 - 1, y + 35 - 1, x + 98 + 17, y + 35 + 17, 0x33FFAA00);

        // 按钮状态更新
        int state = menu.getBuildState();
        if (state == 1) {
            startButton.active = false;
            pauseButton.active = true;
            terminateButton.active = true;
        } else if (state == 2) {
            startButton.active = true;
            pauseButton.active = false;
            terminateButton.active = true;
        } else {
            startButton.active = !menu.getBlueprint().isEmpty();
            pauseButton.active = false;
            terminateButton.active = false;
        }

        // 设置按钮的文字显示
        settingsButton.setMessage(menu.isDestroyMode() ? 
            Component.translatable("gui.tuanzis_mod.blueprint_cannon.destroy") : 
            Component.translatable("gui.tuanzis_mod.blueprint_cannon.drop"));

        // 读取进度：currentIndex（已处理格子数）/ totalBlocks（蓝图 blocks 数组总长度）
        // 这样完成时 currentIndex == totalBlocks → 100%
        int currentIdx = menu.getCurrentIndex();
        int totalBlks = menu.getTotalBlocks();

        int pct;
        String pctText;
        if (totalBlks <= 0) {
            // 无蓝图或蓝图被取走：清零显示
            pct = 0;
            pctText = Component.translatable("gui.tuanzis_mod.blueprint_cannon.no_blueprint").getString();
        } else {
            pct = (int) ((currentIdx * 100L) / totalBlks);
            pct = Math.min(100, Math.max(0, pct));
            pctText = currentIdx + "/" + totalBlks + " (" + pct + "%)";
        }

        // 绘制主 GUI 标题与背包标题
        graphics.text(this.font, this.title, x + 8, y + 6, 4210752, false);
        graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_cannon.backpack"), x + 8, y + 72, 4210752, false);

        // 绘制浆料燃料量 (64点耐久)
        int fuelVal = menu.getSlurryDurability();
        Component fuelText = Component.translatable("gui.tuanzis_mod.blueprint_cannon.fuel_status", fuelVal);
        graphics.text(this.font, fuelText, x + 8, y + 74, 4210752, false);

        // 状态与异常信息
        int pauseReason = menu.getPauseReason();
        Component stateText;
        if (state == 2 && pauseReason == 1) {
            stateText = Component.translatable("gui.tuanzis_mod.blueprint_cannon.fuel_insufficient").withStyle(ChatFormatting.RED);
        } else if (state == 2 && pauseReason == 2) {
            stateText = Component.translatable("gui.tuanzis_mod.blueprint_cannon.block_missing").withStyle(ChatFormatting.RED);
        } else {
            Component st = switch (state) {
                case 1 -> Component.translatable("gui.tuanzis_mod.blueprint_cannon.running").withStyle(ChatFormatting.GREEN);
                case 2 -> Component.translatable("gui.tuanzis_mod.blueprint_cannon.paused").withStyle(ChatFormatting.GOLD);
                default -> Component.translatable("gui.tuanzis_mod.blueprint_cannon.terminated").withStyle(ChatFormatting.GRAY);
            };
            stateText = Component.translatable("gui.tuanzis_mod.blueprint_cannon.status_prefix", st);
        }
        graphics.text(this.font, stateText, x + 90, y + 74, 4210752, false);

        // 增高区绘制进度条与详细比率
        graphics.fill(x + 8, y + 170, x + 168, y + 174, 0xFF333333);
        int fillWidth = (int) (160.0F * pct / 100.0F);
        // 完成时（pct=100）绿色；暂停时橙色；运行中绿色；空闲/无蓝图灰色
        int barColor = (totalBlks <= 0 || state == 0) ? 0xFF555555
                     : (state == 2) ? 0xFFFF8800
                     : 0xFF00FF44;
        graphics.fill(x + 8, y + 170, x + 8 + fillWidth, y + 174, barColor);

        int textWidth = this.font.width(pctText);
        graphics.text(this.font, Component.literal(pctText), x + 88 - textWidth / 2, y + 176, 4210752, false);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        // 统一在 extractBackground 中以 100% 稳定的绝对坐标绘制标题与背包名称
    }
}
