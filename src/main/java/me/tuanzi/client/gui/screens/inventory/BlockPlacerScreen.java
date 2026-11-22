package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.block.entity.BlockPlacerBlockEntity;
import me.tuanzi.network.BlockPlacerActionPacket;
import me.tuanzi.world.inventory.BlockPlacerMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 方块放置器操作界面
 */
public class BlockPlacerScreen extends AbstractContainerScreen<BlockPlacerMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int LABEL_COLOR = 0xFF404040;

    private Button modeButton;
    private Button intervalButton;
    private Button sneakButton;
    private Button extractButton;

    public BlockPlacerScreen(BlockPlacerMenu menu, Inventory inventory, Component title) {
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

        this.modeButton = this.addRenderableWidget(
                Button.builder(getModeButtonText(), btn -> sendAction(BlockPlacerActionPacket.ACTION_CYCLE_MODE))
                        .bounds(x + 8, y + 38, 48, 14)
                        .tooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.block_placer.mode.tooltip")))
                        .build()
        );

        this.intervalButton = this.addRenderableWidget(
                Button.builder(getIntervalButtonText(), btn -> sendAction(BlockPlacerActionPacket.ACTION_CYCLE_INTERVAL))
                        .bounds(x + 58, y + 38, 48, 14)
                        .tooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.block_placer.interval.tooltip")))
                        .build()
        );

        this.sneakButton = this.addRenderableWidget(
                Button.builder(getSneakButtonText(), btn -> sendAction(BlockPlacerActionPacket.ACTION_TOGGLE_SNEAK))
                        .bounds(x + 8, y + 54, 48, 14)
                        .tooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.block_placer.sneak.tooltip")))
                        .build()
        );

        this.extractButton = this.addRenderableWidget(
                Button.builder(getExtractButtonText(), btn -> sendAction(BlockPlacerActionPacket.ACTION_TOGGLE_EXTRACT))
                        .bounds(x + 58, y + 54, 48, 14)
                        .tooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.block_placer.extract.tooltip")))
                        .build()
        );

        updateButtonStates();
    }

    private void sendAction(int action) {
        ClientPlayNetworking.send(new BlockPlacerActionPacket(this.menu.getBlockPos(), action));
    }

    private Component getModeButtonText() {
        return switch (this.menu.getRedstoneMode()) {
            case BlockPlacerBlockEntity.MODE_CONTINUOUS -> Component.translatable("gui.tuanzis_mod.block_placer.mode.continuous");
            case BlockPlacerBlockEntity.MODE_PULSE_LATCH -> Component.translatable("gui.tuanzis_mod.block_placer.mode.latch");
            default -> Component.translatable("gui.tuanzis_mod.block_placer.mode.single");
        };
    }

    private Component getIntervalButtonText() {
        return Component.translatable("gui.tuanzis_mod.block_placer.interval", this.menu.getContinuousInterval());
    }

    private Component getSneakButtonText() {
        return Component.translatable(this.menu.isSimulateSneak()
                ? "gui.tuanzis_mod.block_placer.sneak.on"
                : "gui.tuanzis_mod.block_placer.sneak.off");
    }

    private Component getExtractButtonText() {
        return Component.translatable(this.menu.isAllowBottomExtract()
                ? "gui.tuanzis_mod.block_placer.extract.on"
                : "gui.tuanzis_mod.block_placer.extract.off");
    }

    private void updateButtonStates() {
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeButtonText());
        }
        if (this.intervalButton != null) {
            this.intervalButton.setMessage(getIntervalButtonText());
            this.intervalButton.active = (this.menu.getRedstoneMode() == BlockPlacerBlockEntity.MODE_CONTINUOUS);
        }
        if (this.sneakButton != null) {
            this.sneakButton.setMessage(getSneakButtonText());
        }
        if (this.extractButton != null) {
            this.extractButton.setMessage(getExtractButtonText());
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;

        // 1. 绘制原版容器框体，并清除上半部默认 3x3 底纹避免错位
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        graphics.fill(x + 7, y + 15, x + 169, y + 71, 0xFFC6C6C6);

        // 2. 绘制过滤槽原版槽位底框 (12, 17 -> 11, 16)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 11, y + 16, 18, 18);

        // 3. 绘制 3x3 库存槽原版槽位底框 (114, 17 -> 113, 16)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 113 + col * 18, y + 16 + row * 18, 18, 18);
            }
        }

        // 4. 绘制状态指示灯小方块 (34, 27)
        int lampColor = switch (BlockPlacerBlockEntity.getLampForStatus(this.menu.getStatusCode())) {
            case GREEN -> 0xFF33D146;
            case YELLOW -> 0xFFF2C938;
            case RED -> 0xFFE63939;
        };
        graphics.fill(x + 33, y + 26, x + 41, y + 34, 0xFF373737);
        graphics.fill(x + 34, y + 27, x + 40, y + 33, lampColor);

        updateButtonStates();
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);

        // 过滤槽标注（位于标题下方、状态灯上方，避免与标题及指示灯重叠）
        graphics.text(this.font, Component.translatable("gui.tuanzis_mod.block_placer.filter_hint"), 33, 16, LABEL_COLOR, false);

        // 状态原因文本
        Component statusText = switch (this.menu.getStatusCode()) {
            case BlockPlacerBlockEntity.STATUS_READY -> Component.translatable("gui.tuanzis_mod.block_placer.status.ready");
            case BlockPlacerBlockEntity.STATUS_SUCCESS -> Component.translatable("gui.tuanzis_mod.block_placer.status.success");
            case BlockPlacerBlockEntity.STATUS_NO_ITEM -> Component.translatable("gui.tuanzis_mod.block_placer.status.no_item");
            case BlockPlacerBlockEntity.STATUS_FILTER_MISMATCH -> Component.translatable("gui.tuanzis_mod.block_placer.status.filter_mismatch");
            case BlockPlacerBlockEntity.STATUS_TARGET_BLOCKED -> Component.translatable("gui.tuanzis_mod.block_placer.status.target_blocked");
            case BlockPlacerBlockEntity.STATUS_COLLISION -> Component.translatable("gui.tuanzis_mod.block_placer.status.collision");
            case BlockPlacerBlockEntity.STATUS_PROTECTED -> Component.translatable("gui.tuanzis_mod.block_placer.status.protected");
            case BlockPlacerBlockEntity.STATUS_COOLDOWN -> Component.translatable("gui.tuanzis_mod.block_placer.status.cooldown");
            default -> Component.translatable("gui.tuanzis_mod.block_placer.status.ready");
        };
        graphics.text(this.font, statusText, 44, 26, 0xFF202020, false);
    }
}
