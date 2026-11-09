package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.client.ClientGhostManager;
import me.tuanzi.network.GhostTransformPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class GhostControllerScreen extends Screen {

    private Button layerModeBtn;
    private Button smartAssistBtn;
    private Button visibilityBtn;

    public GhostControllerScreen() {
        super(Component.translatable("gui.tuanzis_mod.ghost_control.title"));
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - 75;
        int y = this.height / 2 - 78;

        // X平移 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjust(new BlockPos(-1, 0, 0), 0, false)).bounds(x, y + 16, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjust(new BlockPos(1, 0, 0), 0, false)).bounds(x + 130, y + 16, 20, 16).build());

        // Y平移 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjust(new BlockPos(0, -1, 0), 0, false)).bounds(x, y + 35, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjust(new BlockPos(0, 1, 0), 0, false)).bounds(x + 130, y + 35, 20, 16).build());

        // Z平移 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjust(new BlockPos(0, 0, -1), 0, false)).bounds(x, y + 54, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjust(new BlockPos(0, 0, 1), 0, false)).bounds(x + 130, y + 54, 20, 16).build());

        // 切层 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            ClientGhostManager.setLayerSlicing(ClientGhostManager.getMaxLayer() - 1, ClientGhostManager.isSingleLayerMode());
        }).bounds(x, y + 73, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            ClientGhostManager.setLayerSlicing(ClientGhostManager.getMaxLayer() + 1, ClientGhostManager.isSingleLayerMode());
        }).bounds(x + 130, y + 73, 20, 16).build());

        // 旋转与镜像
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.ghost_control.rotate"), btn -> adjust(BlockPos.ZERO, 1, false)).bounds(x, y + 94, 73, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.ghost_control.mirror"), btn -> adjust(BlockPos.ZERO, 0, true)).bounds(x + 77, y + 94, 73, 16).build());

        // 切层模式切换与智能纠错辅助开关
        this.layerModeBtn = this.addRenderableWidget(Button.builder(getLayerModeLabel(), btn -> {
            ClientGhostManager.setLayerSlicing(ClientGhostManager.getMaxLayer(), !ClientGhostManager.isSingleLayerMode());
            btn.setMessage(getLayerModeLabel());
        }).bounds(x, y + 114, 73, 16).build());

        this.smartAssistBtn = this.addRenderableWidget(Button.builder(getSmartAssistLabel(), btn -> {
            ClientGhostManager.setSmartAssistance(!ClientGhostManager.isSmartAssistance());
            btn.setMessage(getSmartAssistLabel());
        }).bounds(x + 77, y + 114, 73, 16).build());

        // 显示/隐藏虚影 与 清除虚影
        this.visibilityBtn = this.addRenderableWidget(Button.builder(getVisibilityLabel(), btn -> {
            ClientGhostManager.toggleVisibility();
            btn.setMessage(getVisibilityLabel());
        }).bounds(x, y + 134, 73, 16).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.inspector.clear_ghost"), btn -> {
            ClientGhostManager.clearGhosts();
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_cleared").withStyle(ChatFormatting.YELLOW));
            }
            this.onClose();
        }).bounds(x + 77, y + 134, 73, 16).build());
    }

    private Component getLayerModeLabel() {
        return Component.translatable(ClientGhostManager.isSingleLayerMode()
                ? "gui.tuanzis_mod.inspector.mode_single"
                : "gui.tuanzis_mod.inspector.mode_all");
    }

    private Component getSmartAssistLabel() {
        return Component.literal("纠错: " + (ClientGhostManager.isSmartAssistance() ? "开" : "关"))
                .withStyle(ClientGhostManager.isSmartAssistance() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private Component getVisibilityLabel() {
        return Component.literal(ClientGhostManager.isHidden() ? "虚影: 已隐藏" : "虚影: 显示中")
                .withStyle(ClientGhostManager.isHidden() ? ChatFormatting.YELLOW : ChatFormatting.AQUA);
    }

    private void adjust(BlockPos move, int rotDelta, boolean flip) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof me.tuanzi.item.StructureBlueprintItem)) {
            stack = mc.player.getOffhandItem();
        }

        if (stack.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

            if (tag.getBooleanOr("GhostPlaced", false)) {
                int ox = tag.getIntOr("OffsetX", 0) + move.getX();
                int oy = tag.getIntOr("OffsetY", 0) + move.getY();
                int oz = tag.getIntOr("OffsetZ", 0) + move.getZ();
                int rot = ((tag.getIntOr("Rotation", 0) + rotDelta) % 4 + 4) % 4;
                boolean mir = tag.getBooleanOr("Mirrored", false);
                if (flip) mir = !mir;

                BlockPos newOff = new BlockPos(ox, oy, oz);
                ClientGhostManager.updateTransform(newOff, rot, mir);
                ClientPlayNetworking.send(new GhostTransformPacket(newOff, rot, mir));
                return;
            }
        }

        // 未手持蓝图但世界中存在常驻虚影投影时，直接调整客户端虚影状态
        if (ClientGhostManager.hasActiveGhost()) {
            BlockPos cur = ClientGhostManager.getOffset();
            BlockPos newOff = cur.offset(move);
            int rot = ((ClientGhostManager.getRotationIdx() + rotDelta) % 4 + 4) % 4;
            boolean mir = ClientGhostManager.isMirrored();
            if (flip) mir = !mir;
            ClientGhostManager.updateTransform(newOff, rot, mir);
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        int cx = this.width / 2;
        int y = this.height / 2 - 78;
        // 在按钮层之前绘制半透明深色面板与边框，避免遮暗按钮文字
        graphics.fill(cx - 84, y - 6, cx + 84, y + 158, 0xCC14141C);
        graphics.outline(cx - 84, y - 6, 168, 164, 0xFF3A3A4E);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int y = this.height / 2 - 78;

        graphics.text(this.font, this.title, cx - this.font.width(this.title) / 2, y + 2, 0xFFFFFFFF, true);

        BlockPos off = ClientGhostManager.getOffset();
        String xLabel = Component.translatable("gui.tuanzis_mod.ghost_control.move_x").getString() + String.format(" (%+d)", off.getX());
        String yLabel = Component.translatable("gui.tuanzis_mod.ghost_control.move_y").getString() + String.format(" (%+d)", off.getY());
        String zLabel = Component.translatable("gui.tuanzis_mod.ghost_control.move_z").getString() + String.format(" (%+d)", off.getZ());
        int totalH = Math.max(1, ClientGhostManager.getStructureHeight());
        int curL = Math.clamp(ClientGhostManager.getMaxLayer() + 1, 1, totalH);
        String layerLabel = String.format("切层: %d / %d", curL, totalH);

        graphics.text(this.font, xLabel, cx - this.font.width(xLabel) / 2, y + 20, 0xFFDDDDDD, false);
        graphics.text(this.font, yLabel, cx - this.font.width(yLabel) / 2, y + 39, 0xFFDDDDDD, false);
        graphics.text(this.font, zLabel, cx - this.font.width(zLabel) / 2, y + 58, 0xFFDDDDDD, false);
        graphics.text(this.font, layerLabel, cx - this.font.width(layerLabel) / 2, y + 77, 0xFFFFDD55, false);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.isEscape() || me.tuanzi.client.TuanzisModClient.isOpenControllerKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
