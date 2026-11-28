package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.network.canvas.PaperShredderActionC2SPacket;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasShredderHelper;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.inventory.canvas.PaperShredderMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 碎纸机操作界面：放入画布后点击“损毁”，进入强警告二次确认态（明确提示不返还画布内材料），
 * 确认后彻底损毁画布并返还画布合成材料。
 */
@Environment(EnvType.CLIENT)
public class PaperShredderScreen extends AbstractContainerScreen<PaperShredderMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int LABEL_COLOR = 0xFF404040;

    private Button shredBtn;
    private Button confirmBtn;
    private Button cancelBtn;

    private boolean awaitingConfirm = false;
    private ItemStack lastTrackedStack = ItemStack.EMPTY;

    public PaperShredderScreen(PaperShredderMenu menu, Inventory inventory, Component title) {
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

        this.shredBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.paper_shredder.shred_btn"),
                                btn -> {
                                    this.awaitingConfirm = true;
                                    updateButtonStates();
                                })
                        .bounds(x + 98, y + 60, 70, 16)
                        .build()
        );
        this.confirmBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.paper_shredder.confirm_btn"),
                                btn -> {
                                    ClientPlayNetworking.send(new PaperShredderActionC2SPacket(this.menu.getBlockPos(), true));
                                    this.awaitingConfirm = false;
                                    updateButtonStates();
                                })
                        .bounds(x + 38, y + 62, 62, 16)
                        .build()
        );
        this.cancelBtn = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.paper_shredder.cancel_btn"),
                                btn -> {
                                    this.awaitingConfirm = false;
                                    updateButtonStates();
                                })
                        .bounds(x + 106, y + 62, 62, 16)
                        .build()
        );

        updateButtonStates();
    }

    private void syncTrackedCanvas() {
        ItemStack current = this.menu.getCanvasStack();
        if (!ItemStack.isSameItemSameComponents(current, lastTrackedStack)) {
            this.lastTrackedStack = current.copy();
            this.awaitingConfirm = false;
        }
    }

    private boolean canCurrentPlayerShred(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        UUID playerUuid = mc.player != null ? mc.player.getUUID() : null;
        return CanvasShredderHelper.canPlayerShred(CanvasMetadata.read(stack), playerUuid);
    }

    private void updateButtonStates() {
        syncTrackedCanvas();
        ItemStack stack = this.menu.getCanvasStack();
        boolean hasCanvas = !stack.isEmpty();
        boolean isOwner = canCurrentPlayerShred(stack);
        boolean inUse = this.menu.isInUse();
        boolean canOperate = hasCanvas && isOwner && !inUse;

        if (!canOperate) {
            this.awaitingConfirm = false;
        }

        if (shredBtn != null) {
            shredBtn.visible = !awaitingConfirm;
            shredBtn.active = canOperate;
        }
        if (confirmBtn != null) {
            confirmBtn.visible = awaitingConfirm;
            confirmBtn.active = canOperate;
        }
        if (cancelBtn != null) {
            cancelBtn.visible = awaitingConfirm;
            cancelBtn.active = true;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;
        EaselScreen.drawContainerPanel(graphics, x, y, this.imageWidth, this.imageHeight);
        // 画布槽位底框 (14, 24 -> 13, 23)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 13, y + 23, 18, 18);
        if (this.awaitingConfirm) {
            // 二次确认强警示底框
            graphics.fill(x + 36, y + 18, x + 170, y + 59, 0xFF3A0B0B);
            graphics.fill(x + 37, y + 19, x + 169, y + 58, 0xFF5A1414);
        }
        EaselScreen.drawPlayerInventorySlots(graphics, x, y, 96);
        updateButtonStates();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        ItemStack stack = this.menu.getCanvasStack();
        if (stack.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.insert_hint"), 38, 28, 0xFF555555, false);
            return;
        }

        if (this.awaitingConfirm) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.warn_title"), 40, 22, 0xFFFF5555, false);
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.warn_line1"), 40, 34, 0xFFFF8888, false);
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.warn_line2"), 40, 45, 0xFFFFD055, false);
            return;
        }

        CanvasMetadata meta = CanvasMetadata.read(stack);
        String sizeText = meta.type() == CanvasType.VOXEL_3D
                ? (meta.size() + "³")
                : (meta.size() + "×" + meta.size());
        graphics.text(this.font,
                Component.translatable("gui.tuanzis_mod.easel.info_line",
                        Component.translatable(meta.type().getTranslationKey()), sizeText, meta.nonEmptyVoxels()),
                38, 21, LABEL_COLOR, false);

        int paperRefund = CanvasShredderHelper.calculatePaperRefundCount(meta);
        graphics.text(this.font,
                Component.translatable("gui.tuanzis_mod.paper_shredder.refund_info", paperRefund),
                38, 33, 0xFF1F5A1F, false);

        if (!canCurrentPlayerShred(stack)) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.not_owner"), 38, 46, 0xFFB02020, false);
        } else if (this.menu.isInUse()) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.in_use"), 38, 46, 0xFFB02020, false);
        } else {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.paper_shredder.no_block_refund_hint"), 8, 48, 0xFFAA2020, false);
        }
    }
}
