package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.block.entity.BlockBreakerBlockEntity;
import me.tuanzi.network.BlockBreakerToggleModePacket;
import me.tuanzi.world.inventory.BlockBreakerMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BlockBreakerScreen extends AbstractContainerScreen<BlockBreakerMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    private Button modeButton;

    public BlockBreakerScreen(BlockBreakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // 模式切换按钮 (单次脉冲 / 持续循环)
        this.modeButton = this.addRenderableWidget(Button.builder(getModeButtonText(), btn -> {
            ClientPlayNetworking.send(new BlockBreakerToggleModePacket(this.menu.getBlockPos()));
        }).bounds(x + 10, y + 55, 48, 16).build());
    }

    private Component getModeButtonText() {
        if (this.menu.isContinuousMode()) {
            return Component.literal("§b持续");
        } else {
            return Component.literal("§e单次");
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;

        // 1. 绘制原版 dispenser 框体背景
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // 2. 绘制工具槽装饰框 (X=25, Y=34，尺寸 18x18)
        graphics.fill(x + 25, y + 34, x + 43, y + 52, 0xFF8B8B8B);
        graphics.fill(x + 25, y + 34, x + 42, y + 35, 0xFF373737);
        graphics.fill(x + 25, y + 34, x + 26, y + 52, 0xFF373737);
        graphics.fill(x + 26, y + 51, x + 43, y + 52, 0xFFFFFFFF);
        graphics.fill(x + 42, y + 35, x + 43, y + 52, 0xFFFFFFFF);

        // 3. 动态更新按钮文字
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeButtonText());
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // 绘制状态文本
        Component statusText = switch (this.menu.getStatusCode()) {
            case BlockBreakerBlockEntity.STATUS_READY -> Component.literal("§a就绪");
            case BlockBreakerBlockEntity.STATUS_MINING -> Component.literal("§e挖掘: " + this.menu.getMiningProgress() + "%");
            case BlockBreakerBlockEntity.STATUS_NO_TOOL -> Component.literal("§c无工具");
            case BlockBreakerBlockEntity.STATUS_TOOL_BROKEN -> Component.literal("§c已损坏");
            case BlockBreakerBlockEntity.STATUS_BUFFER_FULL -> Component.literal("§6缓冲满");
            case BlockBreakerBlockEntity.STATUS_UNBREAKABLE -> Component.literal("§c不可破坏");
            case BlockBreakerBlockEntity.STATUS_WRONG_TOOL_LEVEL -> Component.literal("§c等级不足");
            case BlockBreakerBlockEntity.STATUS_COOLDOWN -> Component.literal("§7冷却中");
            default -> Component.literal("§7待命");
        };

        graphics.text(this.font, statusText, 10, 20, 0, false);
    }
}
