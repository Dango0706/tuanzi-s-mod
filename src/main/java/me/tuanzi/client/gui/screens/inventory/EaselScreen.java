package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.network.canvas.EaselActionC2SPacket;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.inventory.canvas.EaselMenu;
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
import net.minecraft.world.item.ItemStack;

/**
 * 画板操作界面：放入画布、选择 2D/3D 类型、进入编辑空间、保存画布与设置访问权限。
 */
@Environment(EnvType.CLIENT)
public class EaselScreen extends AbstractContainerScreen<EaselMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int LABEL_COLOR = 0xFF404040;

    private Button select2dBtn;
    private Button select3dBtn;
    private Button enterEditBtn;
    private Button saveBtn;
    private Button permissionBtn;
    private Button resetBtn;
    private Button exportBtn;

    public EaselScreen(EaselMenu menu, Inventory inventory, Component title) {
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

        this.select2dBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.select_2d"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_SET_2D))
                        .bounds(x + 38, y + 20, 62, 16)
                        .build()
        );
        this.select3dBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.select_3d"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_SET_3D))
                        .bounds(x + 104, y + 20, 62, 16)
                        .build()
        );
        this.enterEditBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.enter_edit"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_ENTER_EDIT))
                        .bounds(x + 38, y + 40, 62, 16)
                        .build()
        );
        this.saveBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.save"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_SAVE_CANVAS))
                        .bounds(x + 104, y + 40, 62, 16)
                        .build()
        );
        this.permissionBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.permission"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_CYCLE_PERMISSION))
                        .bounds(x + 104, y + 60, 62, 16)
                        .build()
        );
        this.resetBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.reset"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_RESET_CANVAS))
                        .bounds(x + 38, y + 60, 30, 16)
                        .build()
        );
        this.exportBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.easel.export"),
                                btn -> sendAction(EaselActionC2SPacket.ACTION_EXPORT_CORRUPTED))
                        .bounds(x + 70, y + 60, 30, 16)
                        .build()
        );

        updateButtonStates();
    }

    private void sendAction(int action) {
        ClientPlayNetworking.send(new EaselActionC2SPacket(this.menu.getBlockPos(), action));
    }

    private void updateButtonStates() {
        ItemStack stack = this.menu.getCanvasStack();
        boolean hasCanvas = !stack.isEmpty();
        CanvasMetadata meta = hasCanvas ? CanvasMetadata.read(stack) : null;
        boolean unset = meta != null && meta.type() == CanvasType.UNSET;
        boolean corrupted = meta != null && meta.corrupted();

        if (select2dBtn != null) {
            select2dBtn.visible = hasCanvas && unset && !corrupted;
            select2dBtn.active = select2dBtn.visible;
        }
        if (select3dBtn != null) {
            select3dBtn.visible = hasCanvas && unset && !corrupted;
            select3dBtn.active = select3dBtn.visible && meta.size() <= CanvasType.MAX_SIZE_3D;
        }
        if (enterEditBtn != null) {
            enterEditBtn.active = hasCanvas && !unset;
        }
        if (saveBtn != null) {
            saveBtn.active = hasCanvas && !unset && !corrupted;
        }
        if (permissionBtn != null) {
            permissionBtn.active = hasCanvas;
            if (meta != null) {
                permissionBtn.setMessage(Component.translatable(meta.permission().getTranslationKey()));
            } else {
                permissionBtn.setMessage(Component.translatable("gui.tuanzis_mod.easel.permission"));
            }
        }
        if (resetBtn != null) {
            resetBtn.visible = hasCanvas && corrupted;
        }
        if (exportBtn != null) {
            exportBtn.visible = hasCanvas && corrupted;
        }
    }

    static void drawContainerPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        graphics.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }

    static void drawPlayerInventorySlots(GuiGraphicsExtractor graphics, int leftPos, int topPos, int invY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                        leftPos + 7 + col * 18, topPos + invY - 1 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    leftPos + 7 + col * 18, topPos + invY + 57, 18, 18);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;
        drawContainerPanel(graphics, x, y, this.imageWidth, this.imageHeight);
        // 画布槽位底框 (14, 24 -> 13, 23)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 13, y + 23, 18, 18);
        drawPlayerInventorySlots(graphics, x, y, 96);
        updateButtonStates();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        ItemStack stack = this.menu.getCanvasStack();
        if (stack.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.easel.insert_hint"), 38, 24, 0xFF555555, false);
            return;
        }
        CanvasMetadata meta = CanvasMetadata.read(stack);
        if (meta.type() != CanvasType.UNSET) {
            String sizeText = meta.type() == CanvasType.VOXEL_3D
                    ? (meta.size() + "³")
                    : (meta.size() + "×" + meta.size());
            graphics.text(this.font,
                    Component.translatable("gui.tuanzis_mod.easel.info_line",
                            Component.translatable(meta.type().getTranslationKey()), sizeText, meta.nonEmptyVoxels()),
                    38, 22, LABEL_COLOR, false);
        }
        if (this.menu.hasActiveDraft()) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.easel.unsaved_draft"), 8, 60, 0xFFB05500, false);
        } else if (!meta.ownerName().isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.easel.owner_short", meta.ownerName()), 8, 60, 0xFF555555, false);
        }
        if (this.menu.isOccupied()) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.easel.occupied"), 8, 72, 0xFF0077AA, false);
        }
    }
}
