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
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int LABEL_COLOR = 0xFF404040;

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
        }).bounds(x + 10, y + 55, 48, 14).build());
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

        // 1. 绘制原版容器框体，并清除上半部默认居中的 3x3 底纹，避免与实际槽位错位
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        graphics.fill(x + 7, y + 15, x + 169, y + 71, 0xFFC6C6C6);

        // 2. 绘制工具槽原版槽位底框 (对应 Menu 槽位 26, 35)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 25, y + 34, 18, 18);

        // 3. 绘制 3x3 输出缓冲原版槽位底框 (对应 Menu 槽位 98 + col * 18, 17 + row * 18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 97 + col * 18, y + 16 + row * 18, 18, 18);
            }
        }

        // 4. 动态更新按钮文字
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeButtonText());
        }
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

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

        graphics.text(this.font, statusText, 10, 20, 0xFFFFFFFF, true);
    }
}
