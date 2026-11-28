package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.block.entity.Printer3DBlockEntity;
import me.tuanzi.network.canvas.Printer3DActionC2SPacket;
import me.tuanzi.world.canvas.CanvasPrintCalculator;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.inventory.canvas.Printer3DMenu;
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

import java.util.List;

/**
 * 3D 打印机操作界面：放入画布与打印原料、选择缩放倍率、预览消耗并执行打印。
 */
@Environment(EnvType.CLIENT)
public class Printer3DScreen extends AbstractContainerScreen<Printer3DMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int[] SCALE_OPTIONS = {1, 2, 4, 8, 16};

    private final Button[] scaleButtons = new Button[SCALE_OPTIONS.length];
    private Button printBtn;

    public Printer3DScreen(Printer3DMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 178);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 84;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            final int scaleVal = SCALE_OPTIONS[i];
            scaleButtons[i] = this.addRenderableWidget(
                    Button.builder(Component.literal(scaleVal + "x"),
                                    btn -> ClientPlayNetworking.send(new Printer3DActionC2SPacket(
                                            this.menu.getBlockPos(), Printer3DActionC2SPacket.ACTION_SELECT_SCALE, scaleVal)))
                            .bounds(x + 8 + i * 24, y + 42, 22, 14)
                            .build()
            );
        }

        this.printBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.printer.print_btn"),
                                btn -> ClientPlayNetworking.send(new Printer3DActionC2SPacket(
                                        this.menu.getBlockPos(), Printer3DActionC2SPacket.ACTION_PRINT, this.menu.getSelectedScale())))
                        .bounds(x + 130, y + 42, 38, 14)
                        .build()
        );
        updateButtonStates();
    }

    private void updateButtonStates() {
        int size = this.menu.getCanvasSize();
        CanvasType type = this.menu.getCanvasType();
        int selected = this.menu.getSelectedScale();
        List<Integer> allowed = CanvasPrintCalculator.getAllowedScales(size, type);

        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            int val = SCALE_OPTIONS[i];
            Button btn = scaleButtons[i];
            if (btn != null) {
                boolean valid = allowed.contains(val);
                btn.active = valid && val != selected;
                btn.setMessage(Component.literal(val == selected ? ("[" + val + "x]") : (val + "x")));
            }
        }
        if (printBtn != null) {
            printBtn.active = this.menu.getStatusCode() == Printer3DBlockEntity.STATUS_READY;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;
        EaselScreen.drawContainerPanel(graphics, x, y, this.imageWidth, this.imageHeight);
        // 槽位底框：画布(16,22) 原料(52,22) 输出(140,22)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 15, y + 21, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 51, y + 21, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 139, y + 21, 18, 18);
        EaselScreen.drawPlayerInventorySlots(graphics, x, y, 96);
        updateButtonStates();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        graphics.text(this.font, "+", 40, 26, LABEL_COLOR, false);
        graphics.text(this.font, "→", 100, 26, LABEL_COLOR, false);

        int status = this.menu.getStatusCode();
        if (status != Printer3DBlockEntity.STATUS_NO_CANVAS) {
            Component costText = Component.translatable("gui.tuanzis_mod.printer.cost", this.menu.getCost());
            Component voxelText = Component.translatable("gui.tuanzis_mod.printer.voxels", this.menu.getNonEmptyVoxels(), this.menu.getVoxelLimit());
            graphics.text(this.font, costText, 8, 59, 0xFF1F5A1F, false);
            int voxelX = Math.max(8 + this.font.width(costText) + 6, 168 - this.font.width(voxelText));
            graphics.text(this.font, voxelText, voxelX, 59, LABEL_COLOR, false);
        }

        Component statusText = switch (status) {
            case Printer3DBlockEntity.STATUS_READY -> Component.translatable("gui.tuanzis_mod.printer.status.ready");
            case Printer3DBlockEntity.STATUS_NO_CANVAS -> Component.translatable("gui.tuanzis_mod.printer.status.no_canvas");
            case Printer3DBlockEntity.STATUS_UNSET_TYPE -> Component.translatable("gui.tuanzis_mod.printer.status.unset_type");
            case Printer3DBlockEntity.STATUS_EMPTY_CANVAS -> Component.translatable("gui.tuanzis_mod.printer.status.empty_canvas");
            case Printer3DBlockEntity.STATUS_EXCEEDS_LIMIT -> Component.translatable("gui.tuanzis_mod.printer.status.exceeds_limit");
            case Printer3DBlockEntity.STATUS_NO_MATERIAL -> Component.translatable("gui.tuanzis_mod.printer.status.no_material");
            case Printer3DBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("gui.tuanzis_mod.printer.status.output_full");
            case Printer3DBlockEntity.STATUS_CORRUPTED -> Component.translatable("gui.tuanzis_mod.printer.status.corrupted");
            default -> Component.empty();
        };
        int statusColor = (status == Printer3DBlockEntity.STATUS_READY) ? 0xFF1F7A1F : 0xFFB02020;
        graphics.text(this.font, statusText, 8, 71, statusColor, false);
    }
}
