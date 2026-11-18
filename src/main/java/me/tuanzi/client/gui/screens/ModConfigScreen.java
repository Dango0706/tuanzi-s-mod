package me.tuanzi.client.gui.screens;

import me.tuanzi.config.CasinoRecipeControl;
import me.tuanzi.config.ModConfig;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * ModMenu 配置界面：支持修改并保存赌场核心合成配方开关。
 */
@Environment(EnvType.CLIENT)
public class ModConfigScreen extends Screen {
    private final Screen parent;
    private boolean enableCasinoRecipes;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("gui.tuanzis_mod.config.title"));
        this.parent = parent;
        ModConfig.load();
        this.enableCasinoRecipes = ModConfig.isCasinoRecipesEnabled();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        CycleButton<Boolean> casinoToggle = CycleButton.onOffBuilder(this.enableCasinoRecipes)
                .withTooltip(value -> Tooltip.create(Component.translatable("gui.tuanzis_mod.config.enable_casino_recipes.tooltip")))
                .create(
                        centerX - 140,
                        68,
                        280,
                        20,
                        Component.translatable("gui.tuanzis_mod.config.enable_casino_recipes"),
                        (button, value) -> this.enableCasinoRecipes = value
                );
        this.addRenderableWidget(casinoToggle);

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

    private void saveAndClose() {
        ModConfig.setCasinoRecipesEnabled(this.enableCasinoRecipes);
        if (this.minecraft != null && this.minecraft.getSingleplayerServer() != null) {
            var server = this.minecraft.getSingleplayerServer();
            server.execute(() -> CasinoRecipeControl.reloadServerResources(server));
            ModLog.debug("ModMenu 配置保存：单人模式已触发服务端配方热重载 (enableCasinoRecipes=" + this.enableCasinoRecipes + ")");
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, 24, 0xFFFFFFFF);

        List<FormattedCharSequence> scopeLines = this.font.split(
                Component.translatable("gui.tuanzis_mod.config.casino_scope_note"),
                320
        );
        int noteY = 104;
        for (FormattedCharSequence line : scopeLines) {
            graphics.centeredText(this.font, line, centerX, noteY, 0xFFBBBBBB);
            noteY += 12;
        }

        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.getSingleplayerServer() == null) {
            List<FormattedCharSequence> remoteLines = this.font.split(
                    Component.translatable("gui.tuanzis_mod.config.remote_server_note"),
                    320
            );
            int warnY = noteY + 10;
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
