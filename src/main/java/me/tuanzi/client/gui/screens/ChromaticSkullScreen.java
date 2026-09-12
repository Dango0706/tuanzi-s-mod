package me.tuanzi.client.gui.screens;

import me.tuanzi.network.ChromaticSkullRequestPacket;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.lwjgl.glfw.GLFW;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChromaticSkullScreen extends Screen {
    private final InteractionHand hand;
    private EditBox nameInput;
    private Button confirmButton;
    private Button cancelButton;

    private @Nullable String confirmedName;
    private @Nullable ResolvableProfile previewProfile;
    private ItemStack previewItemStack = ItemStack.EMPTY;

    public ChromaticSkullScreen(InteractionHand hand) {
        super(Component.translatable("gui.tuanzis_mod.chromatic_skull.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelWidth = 290;
        int panelHeight = 130;
        int startX = centerX - panelWidth / 2;
        int startY = centerY - panelHeight / 2;

        String previousText = this.nameInput != null ? this.nameInput.getValue() : "";

        // 玩家名称输入框 (左栏)
        this.nameInput = new EditBox(this.font, startX + 18, startY + 48, 168, 20, Component.translatable("gui.tuanzis_mod.chromatic_skull.instruction"));
        this.nameInput.setMaxLength(16);
        this.nameInput.setHint(Component.translatable("gui.tuanzis_mod.chromatic_skull.hint"));
        this.nameInput.setValue(previousText);
        this.nameInput.setResponder(this::onInputChanged);
        this.addRenderableWidget(this.nameInput);
        this.setInitialFocus(this.nameInput);

        // 确认 / 获取该头颅 按钮
        Component btnText = isConfirmed()
            ? Component.translatable("gui.tuanzis_mod.chromatic_skull.claim")
            : Component.translatable("gui.tuanzis_mod.chromatic_skull.confirm");
        this.confirmButton = Button.builder(btnText, btn -> onConfirmOrClaim())
            .bounds(startX + 18, startY + 94, 80, 20)
            .build();
        this.confirmButton.active = isValidPlayerName(this.nameInput.getValue().trim());
        this.addRenderableWidget(this.confirmButton);

        // 取消按钮
        this.cancelButton = Button.builder(Component.translatable("gui.tuanzis_mod.chromatic_skull.cancel"), btn -> this.onClose())
            .bounds(startX + 106, startY + 94, 80, 20)
            .build();
        this.addRenderableWidget(this.cancelButton);
    }

    private void onInputChanged(String text) {
        String trimmed = text.trim();
        if (this.confirmedName != null && !this.confirmedName.equals(trimmed)) {
            this.confirmedName = null;
            this.previewProfile = null;
            this.previewItemStack = ItemStack.EMPTY;
            if (this.confirmButton != null) {
                this.confirmButton.setMessage(Component.translatable("gui.tuanzis_mod.chromatic_skull.confirm"));
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                ModLog.debug(this.minecraft.player, null, "彩色变化头颅GUI输入变更，重置确认状态。当前输入: " + trimmed);
            }
        }
        if (this.confirmButton != null) {
            this.confirmButton.active = isValidPlayerName(trimmed);
        }
    }

    private boolean isConfirmed() {
        if (this.confirmedName == null || this.nameInput == null) {
            return false;
        }
        return this.confirmedName.equals(this.nameInput.getValue().trim());
    }

    private boolean isValidPlayerName(String name) {
        return name != null && name.length() >= 3 && name.length() <= 16 && name.matches("^[a-zA-Z0-9_]+$");
    }

    private void onConfirmOrClaim() {
        String currentName = this.nameInput.getValue().trim();
        if (!isValidPlayerName(currentName)) {
            return;
        }

        if (isConfirmed()) {
            // 已确认，执行获取该头颅操作
            if (this.minecraft != null && this.minecraft.player != null) {
                ModLog.debug(this.minecraft.player, null, "彩色变化头颅GUI确认获取头颅: " + this.confirmedName);
            }
            ClientPlayNetworking.send(new ChromaticSkullRequestPacket(this.confirmedName, this.hand == InteractionHand.MAIN_HAND));
            this.onClose();
        } else {
            // 未确认，执行第一步：展示可视化头颅并改变按钮为“获取该头颅”
            this.confirmedName = currentName;
            this.previewProfile = ResolvableProfile.createUnresolved(this.confirmedName);
            this.previewItemStack = new ItemStack(Items.PLAYER_HEAD);
            this.previewItemStack.set(DataComponents.PROFILE, this.previewProfile);
            this.confirmButton.setMessage(Component.translatable("gui.tuanzis_mod.chromatic_skull.claim"));
            if (this.minecraft != null && this.minecraft.player != null) {
                ModLog.debug(this.minecraft.player, null, "彩色变化头颅GUI确认预览玩家头颅: " + this.confirmedName);
            }
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.confirmButton != null && this.confirmButton.active) {
                this.onConfirmOrClaim();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelWidth = 290;
        int panelHeight = 130;
        int startX = centerX - panelWidth / 2;
        int startY = centerY - panelHeight / 2;

        // 1. 绘制半透明深色背景板与外框
        graphics.fill(startX, startY, startX + panelWidth, startY + panelHeight, 0xEE1E1E24);
        graphics.fill(startX - 1, startY - 1, startX + panelWidth + 1, startY, 0xFF666677);
        graphics.fill(startX - 1, startY + panelHeight, startX + panelWidth + 1, startY + panelHeight + 1, 0xFF666677);
        graphics.fill(startX - 1, startY - 1, startX, startY + panelHeight + 1, 0xFF666677);
        graphics.fill(startX + panelWidth, startY - 1, startX + panelWidth + 1, startY + panelHeight + 1, 0xFF666677);

        // 2. 左栏：绘制标题与说明
        graphics.text(this.font, this.title, startX + 18, startY + 14, 0xFFFFCC00, false);
        Component instruction = Component.translatable("gui.tuanzis_mod.chromatic_skull.instruction");
        graphics.text(this.font, instruction, startX + 18, startY + 32, 0xFFCCCCCC, false);

        // 3. 右栏：头颅预览区域
        int previewX = startX + 198;
        int previewY = startY + 14;
        int previewWidth = 74;

        // 预览标题
        Component previewTitle = Component.translatable("gui.tuanzis_mod.chromatic_skull.preview_title");
        int titleWidth = this.font.width(previewTitle);
        graphics.text(this.font, previewTitle, previewX + (previewWidth - titleWidth) / 2, previewY, 0xFFE0E0E0, false);

        // 预览框尺寸与位置
        int boxSize = 64;
        int boxX = previewX + (previewWidth - boxSize) / 2;
        int boxY = previewY + 14;

        // 预览框深色背景与细边框
        graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF121216);
        graphics.fill(boxX - 1, boxY - 1, boxX + boxSize + 1, boxY, 0xFF4A4A58);
        graphics.fill(boxX - 1, boxY + boxSize, boxX + boxSize + 1, boxY + boxSize + 1, 0xFF4A4A58);
        graphics.fill(boxX - 1, boxY, boxX, boxY + boxSize, 0xFF4A4A58);
        graphics.fill(boxX + boxSize, boxY, boxX + boxSize + 1, boxY + boxSize + 1, 0xFF4A4A58);

        // 4. 预览内容渲染
        if (isConfirmed() && this.previewProfile != null) {
            // 绘制 48x48 高保真玩家面部 (含 Hat 外层)
            int faceSize = 48;
            int faceX = boxX + (boxSize - faceSize) / 2;
            int faceY = boxY + (boxSize - faceSize) / 2;
            PlayerFaceExtractor.extractRenderState(graphics, this.previewProfile, faceX, faceY, faceSize);

            // 右下角叠加 16x16 3D 原版玩家头颅物品微章
            if (!this.previewItemStack.isEmpty()) {
                graphics.item(this.previewItemStack, boxX + boxSize - 18, boxY + boxSize - 18);
            }

            // 悬停提示 Tooltip
            if (mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= boxY && mouseY <= boxY + boxSize) {
                graphics.setTooltipForNextFrame(this.font, Component.literal(this.confirmedName), mouseX, mouseY);
            }

            // 框下方展示玩家名（若过长则缩放显示）
            String displayName = this.confirmedName;
            int nameW = this.font.width(displayName);
            if (nameW > previewWidth) {
                float scale = (float) (previewWidth - 4) / nameW;
                graphics.pose().pushMatrix();
                graphics.pose().translate(previewX + 2, boxY + boxSize + 5);
                graphics.pose().scale(scale, scale);
                graphics.text(this.font, displayName, 0, 0, 0xFF88FF88, false);
                graphics.pose().popMatrix();
            } else {
                graphics.text(this.font, displayName, previewX + (previewWidth - nameW) / 2, boxY + boxSize + 5, 0xFF88FF88, false);
            }
        } else {
            // 未确认状态提示
            Component unconfirmed = Component.translatable("gui.tuanzis_mod.chromatic_skull.unconfirmed");
            int textWidth = this.font.width(unconfirmed);
            graphics.text(this.font, unconfirmed, boxX + (boxSize - textWidth) / 2, boxY + (boxSize - 8) / 2, 0xFF666677, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
