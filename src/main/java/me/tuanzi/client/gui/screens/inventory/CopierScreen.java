package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.block.entity.CopierBlockEntity;
import me.tuanzi.network.canvas.CopierActionC2SPacket;
import me.tuanzi.world.inventory.canvas.CopierMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 复印机操作界面：放入母版打印模型与打印原料，消耗原料生成复制品。
 */
@Environment(EnvType.CLIENT)
public class CopierScreen extends AbstractContainerScreen<CopierMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int LABEL_COLOR = 0xFF404040;

    private Button copyBtn;

    public CopierScreen(CopierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 178);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 85;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        this.copyBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.copier.copy_btn"),
                                btn -> ClientPlayNetworking.send(new CopierActionC2SPacket(
                                        this.menu.getBlockPos(), CopierActionC2SPacket.ACTION_COPY)))
                        .bounds(x + 112, y + 50, 56, 16)
                        .build()
        );
        updateButtonState();
    }

    private void updateButtonState() {
        if (copyBtn != null) {
            copyBtn.active = this.menu.getStatusCode() == CopierBlockEntity.STATUS_READY;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;
        EaselScreen.drawContainerPanel(graphics, x, y, this.imageWidth, this.imageHeight);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 15, y + 25, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 51, y + 25, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 139, y + 25, 18, 18);
        EaselScreen.drawPlayerInventorySlots(graphics, x, y, 96);
        updateButtonState();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        graphics.text(this.font, "+", 40, 30, LABEL_COLOR, false);
        graphics.text(this.font, "→", 100, 30, LABEL_COLOR, false);

        int status = this.menu.getStatusCode();
        if (status != CopierBlockEntity.STATUS_NO_MODEL) {
            graphics.text(this.font,
                    Component.translatable("gui.tuanzis_mod.printer.cost", this.menu.getCost()),
                    8, 54, 0xFF1F5A1F, false);
        }

        Component statusText = switch (status) {
            case CopierBlockEntity.STATUS_READY -> Component.translatable("gui.tuanzis_mod.copier.status.ready");
            case CopierBlockEntity.STATUS_NO_MODEL -> Component.translatable("gui.tuanzis_mod.copier.status.no_model");
            case CopierBlockEntity.STATUS_NO_MATERIAL -> Component.translatable("gui.tuanzis_mod.printer.status.no_material");
            case CopierBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("gui.tuanzis_mod.printer.status.output_full");
            case CopierBlockEntity.STATUS_DISABLED -> Component.translatable("gui.tuanzis_mod.copier.status.disabled");
            case CopierBlockEntity.STATUS_CORRUPTED -> Component.translatable("gui.tuanzis_mod.printer.status.corrupted");
            default -> Component.empty();
        };
        int color = (status == CopierBlockEntity.STATUS_READY) ? 0xFF1F7A1F : 0xFFB02020;
        graphics.text(this.font, statusText, 8, 69, color, false);
    }
}
