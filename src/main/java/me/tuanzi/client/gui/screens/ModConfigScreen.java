package me.tuanzi.client.gui.screens;

import me.tuanzi.config.CasinoRecipeControl;
import me.tuanzi.config.ModConfig;
import me.tuanzi.config.OverworldBuildHeightControl;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * ModMenu 配置界面：支持修改并保存赌场核心合成配方开关、HUD 位置及主世界最大建筑高度。
 */
@Environment(EnvType.CLIENT)
public class ModConfigScreen extends Screen {
    private final Screen parent;
    private boolean enableCasinoRecipes;
    private ModConfig.TraumaHudConflictMode traumaHudConflictMode;
    private int overworldMaxBuildHeight;
    private EditBox overworldHeightInput;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("gui.tuanzis_mod.config.title"));
        this.parent = parent;
        ModConfig.load();
        this.enableCasinoRecipes = ModConfig.isCasinoRecipesEnabled();
        this.traumaHudConflictMode = ModConfig.getTraumaHudConflictMode();
        this.overworldMaxBuildHeight = ModConfig.getOverworldMaxBuildHeight();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        CycleButton<Boolean> casinoToggle = CycleButton.onOffBuilder(this.enableCasinoRecipes)
                .withTooltip(value -> Tooltip.create(Component.translatable("gui.tuanzis_mod.config.enable_casino_recipes.tooltip")))
                .create(
                        centerX - 140,
                        56,
                        280,
                        20,
                        Component.translatable("gui.tuanzis_mod.config.enable_casino_recipes"),
                        (button, value) -> this.enableCasinoRecipes = value
                );
        this.addRenderableWidget(casinoToggle);

        CycleButton<ModConfig.TraumaHudConflictMode> traumaHudToggle = CycleButton
                .<ModConfig.TraumaHudConflictMode>builder(
                        mode -> Component.translatable(mode == ModConfig.TraumaHudConflictMode.RIGHT_CENTER
                                ? "gui.tuanzis_mod.config.trauma_hud_conflict_mode.right_center"
                                : "gui.tuanzis_mod.config.trauma_hud_conflict_mode.above_conflict"),
                        () -> this.traumaHudConflictMode
                )
                .withValues(ModConfig.TraumaHudConflictMode.values())
                .withTooltip(value -> Tooltip.create(Component.translatable("gui.tuanzis_mod.config.trauma_hud_conflict_mode.tooltip")))
                .create(
                        centerX - 140,
                        84,
                        280,
                        20,
                        Component.translatable("gui.tuanzis_mod.config.trauma_hud_conflict_mode"),
                        (button, value) -> this.traumaHudConflictMode = value
                );
        this.addRenderableWidget(traumaHudToggle);

        this.overworldHeightInput = new EditBox(
                this.font,
                centerX - 140,
                124,
                156,
                20,
                Component.translatable("gui.tuanzis_mod.config.overworld_max_build_height")
        );
        this.overworldHeightInput.setMaxLength(4);
        this.overworldHeightInput.setValue(Integer.toString(this.overworldMaxBuildHeight));
        this.overworldHeightInput.setTooltip(Tooltip.create(Component.translatable(
                "gui.tuanzis_mod.config.overworld_max_build_height.tooltip",
                ModConfig.MIN_OVERWORLD_MAX_BUILD_HEIGHT,
                ModConfig.MAX_OVERWORLD_MAX_BUILD_HEIGHT
        )));
        this.overworldHeightInput.setResponder(text -> {
            Integer parsed = parseHeight(text);
            if (parsed != null
                    && parsed >= ModConfig.MIN_OVERWORLD_MAX_BUILD_HEIGHT
                    && parsed <= ModConfig.MAX_OVERWORLD_MAX_BUILD_HEIGHT) {
                this.overworldMaxBuildHeight = parsed;
                this.overworldHeightInput.setTextColor(0xFFE0E0E0);
            } else {
                this.overworldHeightInput.setTextColor(0xFFFF5555);
            }
        });
        this.addRenderableWidget(this.overworldHeightInput);

        this.addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.tuanzis_mod.config.preset_vanilla", ModConfig.MIN_OVERWORLD_MAX_BUILD_HEIGHT),
                                button -> setHeightPreset(ModConfig.MIN_OVERWORLD_MAX_BUILD_HEIGHT)
                        )
                        .bounds(centerX + 22, 124, 56, 20)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.tuanzis_mod.config.preset_max", ModConfig.MAX_OVERWORLD_MAX_BUILD_HEIGHT),
                                button -> setHeightPreset(ModConfig.MAX_OVERWORLD_MAX_BUILD_HEIGHT)
                        )
                        .bounds(centerX + 82, 124, 58, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.tuanzis_mod.config.save"), button -> saveAndClose())
                        .bounds(centerX - 104, this.height - 36, 100, 20)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                        .bounds(centerX + 4, this.height - 36, 100, 20)
                        .build()
        );
    }

    private void setHeightPreset(int height) {
        this.overworldMaxBuildHeight = ModConfig.clampOverworldMaxBuildHeight(height);
        if (this.overworldHeightInput != null) {
            this.overworldHeightInput.setValue(Integer.toString(this.overworldMaxBuildHeight));
            this.overworldHeightInput.setTextColor(0xFFE0E0E0);
        }
    }

    private static Integer parseHeight(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void saveAndClose() {
        boolean previousCasinoRecipes = ModConfig.isCasinoRecipesEnabled();
        Integer parsed = this.overworldHeightInput != null ? parseHeight(this.overworldHeightInput.getValue()) : null;
        int finalHeight = ModConfig.clampOverworldMaxBuildHeight(
                parsed != null ? parsed : this.overworldMaxBuildHeight
        );

        ModConfig.setCasinoRecipesEnabled(this.enableCasinoRecipes);
        ModConfig.setTraumaHudConflictMode(this.traumaHudConflictMode);
        ModConfig.setOverworldMaxBuildHeight(finalHeight);
        if (this.minecraft != null && this.minecraft.getSingleplayerServer() != null) {
            var server = this.minecraft.getSingleplayerServer();
            server.execute(() -> {
                OverworldBuildHeightControl.syncToAllPlayers(server);
                if (previousCasinoRecipes != this.enableCasinoRecipes) {
                    CasinoRecipeControl.reloadServerResources(server);
                }
            });
            ModLog.debug("ModMenu 配置保存：单人模式已触发服务端热重载 (enableCasinoRecipes=" + this.enableCasinoRecipes
                    + ", traumaHudConflictMode=" + this.traumaHudConflictMode
                    + ", overworldMaxBuildHeight=" + finalHeight + ")");
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, 24, 0xFFFFFFFF);
        graphics.text(
                this.font,
                Component.translatable(
                        "gui.tuanzis_mod.config.overworld_max_build_height.label",
                        ModConfig.MIN_OVERWORLD_MAX_BUILD_HEIGHT,
                        ModConfig.MAX_OVERWORLD_MAX_BUILD_HEIGHT
                ),
                centerX - 140,
                112,
                0xFFE0E0E0
        );

        List<FormattedCharSequence> scopeLines = this.font.split(
                Component.translatable("gui.tuanzis_mod.config.casino_scope_note"),
                320
        );
        int noteY = 154;
        for (FormattedCharSequence line : scopeLines) {
            graphics.centeredText(this.font, line, centerX, noteY, 0xFFBBBBBB);
            noteY += 12;
        }

        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.getSingleplayerServer() == null) {
            List<FormattedCharSequence> remoteLines = this.font.split(
                    Component.translatable("gui.tuanzis_mod.config.remote_server_note"),
                    320
            );
            int warnY = noteY + 8;
            for (FormattedCharSequence line : remoteLines) {
                graphics.centeredText(this.font, line, centerX, warnY, 0xFFFFCC55);
                warnY += 12;
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}
