package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.client.ClientGhostManager;
import me.tuanzi.network.GhostTransformPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    public GhostControllerScreen() {
        super(Component.translatable("gui.tuanzis_mod.ghost_control.title"));
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - 60;
        int y = this.height / 2 - 50;

        // X平移 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjust(new BlockPos(-1, 0, 0), 0, false)).bounds(x, y + 10, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjust(new BlockPos(1, 0, 0), 0, false)).bounds(x + 100, y + 10, 20, 16).build());

        // Y平移 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjust(new BlockPos(0, -1, 0), 0, false)).bounds(x, y + 30, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjust(new BlockPos(0, 1, 0), 0, false)).bounds(x + 100, y + 30, 20, 16).build());

        // Z平移 [-] 与 [+]
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjust(new BlockPos(0, 0, -1), 0, false)).bounds(x, y + 50, 20, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjust(new BlockPos(0, 0, 1), 0, false)).bounds(x + 100, y + 50, 20, 16).build());

        // 旋转与镜像
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.ghost_control.rotate"), btn -> adjust(BlockPos.ZERO, 1, false)).bounds(x + 24, y + 70, 34, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.ghost_control.mirror"), btn -> adjust(BlockPos.ZERO, 0, true)).bounds(x + 62, y + 70, 34, 16).build());

        // 显示/隐藏虚影
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.ghost_control.toggle"), btn -> {
            ClientGhostManager.toggleVisibility();
        }).bounds(x + 10, y + 90, 100, 16).build());
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
                int rot = (tag.getIntOr("Rotation", 0) + rotDelta) % 4;
                boolean mir = tag.getBooleanOr("Mirrored", false);
                if (flip) mir = !mir;

                // 客户端先预览并发送 C2S 刷新服务端物品
                ClientPlayNetworking.send(new GhostTransformPacket(new BlockPos(ox, oy, oz), rot, mir));
            }
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = this.width / 2;
        int y = this.height / 2 - 50;

        // 绘制半透明黑色背景板 (WOW 视觉)
        graphics.fill(x - 70, y - 5, x + 70, y + 115, 0xAA000000);

        // 绘制标题与各项控制标签
        graphics.text(this.font, this.title, x - this.font.width(this.title) / 2, y, 0xFFFFFF, false);
        graphics.text(this.font, Component.literal("X 平移"), x - 20, y + 14, 0xCCCCCC, false);
        graphics.text(this.font, Component.literal("Y 平移"), x - 20, y + 34, 0xCCCCCC, false);
        graphics.text(this.font, Component.literal("Z 平移"), x - 20, y + 54, 0xCCCCCC, false);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        // 按下 G 或 ESC 键都可以关闭
        // GLFW_KEY_G 对应的键码为 71
        if (event.key() == 71 || event.key() == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
}
