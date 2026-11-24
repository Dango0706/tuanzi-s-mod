package me.tuanzi.client.gui.screens;

import me.tuanzi.item.CreativeCoreItem;
import me.tuanzi.network.CreativeCoreConfigPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

@Environment(EnvType.CLIENT)
public class CreativeCoreScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 145;

    private final InteractionHand hand;
    private int intervalTicks;
    private int shotsPerCycle;

    private EditBox intervalInput;
    private EditBox shotsInput;
    private boolean syncingInputs = false;

    public CreativeCoreScreen(InteractionHand hand, int intervalTicks, int shotsPerCycle) {
        super(Component.translatable("gui.tuanzis_mod.creative_core.title"));
        this.hand = hand;
        this.intervalTicks = CreativeCoreItem.clampIntervalTicks(intervalTicks);
        this.shotsPerCycle = CreativeCoreItem.clampShotsPerCycle(shotsPerCycle);
    }

    @Override
    protected void init() {
        super.init();
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int controlX = panelX + 106;

        // 第一行：发射间隔 X ticks (1 ~ 1200)
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> setIntervalTicks(this.intervalTicks - 1))
                .bounds(controlX, panelY + 30, 18, 18)
                .build());

        this.intervalInput = new EditBox(this.font, controlX + 21, panelY + 30, 56, 18,
                Component.translatable("gui.tuanzis_mod.creative_core.interval_label"));
        this.intervalInput.setMaxLength(4);
        this.intervalInput.setValue(String.valueOf(this.intervalTicks));
        this.intervalInput.setResponder(text -> {
            if (this.syncingInputs) {
                return;
            }
            Integer parsed = parsePositiveInt(text);
            if (parsed != null && parsed >= CreativeCoreItem.MIN_INTERVAL_TICKS && parsed <= CreativeCoreItem.MAX_INTERVAL_TICKS) {
                this.intervalTicks = parsed;
                this.intervalInput.setTextColor(0xFFE0E0E0);
            } else {
                this.intervalInput.setTextColor(0xFFFF5555);
            }
        });
        this.addRenderableWidget(this.intervalInput);

        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> setIntervalTicks(this.intervalTicks + 1))
                .bounds(controlX + 80, panelY + 30, 18, 18)
                .build());

        // 第二行：单次发射 Y 次 (1 ~ 64)
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> setShotsPerCycle(this.shotsPerCycle - 1))
                .bounds(controlX, panelY + 58, 18, 18)
                .build());

        this.shotsInput = new EditBox(this.font, controlX + 21, panelY + 58, 56, 18,
                Component.translatable("gui.tuanzis_mod.creative_core.shots_label"));
        this.shotsInput.setMaxLength(2);
        this.shotsInput.setValue(String.valueOf(this.shotsPerCycle));
        this.shotsInput.setResponder(text -> {
            if (this.syncingInputs) {
                return;
            }
            Integer parsed = parsePositiveInt(text);
            if (parsed != null && parsed >= CreativeCoreItem.MIN_SHOTS_PER_CYCLE && parsed <= CreativeCoreItem.MAX_SHOTS_PER_CYCLE) {
                this.shotsPerCycle = parsed;
                this.shotsInput.setTextColor(0xFFE0E0E0);
            } else {
                this.shotsInput.setTextColor(0xFFFF5555);
            }
        });
        this.addRenderableWidget(this.shotsInput);

        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> setShotsPerCycle(this.shotsPerCycle + 1))
                .bounds(controlX + 80, panelY + 58, 18, 18)
                .build());

        // 底部按钮行：确定 / 重置默认 / 取消
        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.creative_core.confirm"), btn -> {
            applyInputsAndClamp();
            ClientPlayNetworking.send(new CreativeCoreConfigPacket(
                    this.intervalTicks,
                    this.shotsPerCycle,
                    this.hand == InteractionHand.MAIN_HAND
            ));
            this.onClose();
        }).bounds(panelX + 12, panelY + 112, 60, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.creative_core.reset"), btn -> {
            setIntervalTicks(CreativeCoreItem.DEFAULT_INTERVAL_TICKS);
            setShotsPerCycle(CreativeCoreItem.DEFAULT_SHOTS_PER_CYCLE);
        }).bounds(panelX + 80, panelY + 112, 60, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.creative_core.cancel"), btn -> this.onClose())
                .bounds(panelX + 148, panelY + 112, 60, 20).build());
    }

    private void setIntervalTicks(int ticks) {
        this.intervalTicks = CreativeCoreItem.clampIntervalTicks(ticks);
        if (this.intervalInput != null) {
            this.syncingInputs = true;
            this.intervalInput.setValue(String.valueOf(this.intervalTicks));
            this.intervalInput.setTextColor(0xFFE0E0E0);
            this.syncingInputs = false;
        }
    }

    private void setShotsPerCycle(int shots) {
        this.shotsPerCycle = CreativeCoreItem.clampShotsPerCycle(shots);
        if (this.shotsInput != null) {
            this.syncingInputs = true;
            this.shotsInput.setValue(String.valueOf(this.shotsPerCycle));
            this.shotsInput.setTextColor(0xFFE0E0E0);
            this.syncingInputs = false;
        }
    }

    private static Integer parsePositiveInt(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void applyInputsAndClamp() {
        if (this.intervalInput != null) {
            Integer parsed = parsePositiveInt(this.intervalInput.getValue());
            if (parsed != null) {
                this.intervalTicks = CreativeCoreItem.clampIntervalTicks(parsed);
            }
        }
        if (this.shotsInput != null) {
            Integer parsed = parsePositiveInt(this.shotsInput.getValue());
            if (parsed != null) {
                this.shotsPerCycle = CreativeCoreItem.clampShotsPerCycle(parsed);
            }
        }
        setIntervalTicks(this.intervalTicks);
        setShotsPerCycle(this.shotsPerCycle);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC14141C);
        graphics.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF3A3A4E);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, panelY + 10, 0xFFFFFFFF, true);

        Component intervalLabel = Component.translatable("gui.tuanzis_mod.creative_core.interval_label");
        graphics.text(this.font, intervalLabel, panelX + 12, panelY + 35, 0xFFDDDDDD, false);

        Component shotsLabel = Component.translatable("gui.tuanzis_mod.creative_core.shots_label");
        graphics.text(this.font, shotsLabel, panelX + 12, panelY + 63, 0xFFDDDDDD, false);

        Component preview = Component.translatable("gui.tuanzis_mod.creative_core.preview", this.intervalTicks, this.shotsPerCycle)
                .withStyle(ChatFormatting.AQUA);
        graphics.text(this.font, preview, centerX - this.font.width(preview) / 2, panelY + 89, 0xFFFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
