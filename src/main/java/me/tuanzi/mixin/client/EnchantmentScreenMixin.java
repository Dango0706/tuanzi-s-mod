package me.tuanzi.mixin.client;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Unique
    private static final Identifier TUANZIS_REFRESH_ICON = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/gui/refresh_icon.png");

    @Unique
    private static final int CODEX_REFRESH_BUTTON_ID = 3;

    @Unique
    private Button tuanzis_mod$refreshButton;

    public EnchantmentScreenMixin(EnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tuanzis_mod$addRefreshButton(CallbackInfo ci) {
        int buttonX = this.leftPos + 35;
        int buttonY = this.topPos + 67;

        this.tuanzis_mod$refreshButton = new Button(buttonX, buttonY, 18, 16, Component.empty(), (btn) -> {
            if (this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.player != null) {
                if (this.menu.clickMenuButton(this.minecraft.player, CODEX_REFRESH_BUTTON_ID)) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CODEX_REFRESH_BUTTON_ID);
                }
            }
        }, java.util.function.Supplier::get) {
            @Override
            public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
                this.extractDefaultSprite(graphics);
                int iconColor = this.isActive() ? 0xFFFFFFFF : 0x77AAAAAA;
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TUANZIS_REFRESH_ICON,
                    this.getX() + (this.width - 16) / 2,
                    this.getY() + (this.height - 16) / 2,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16,
                    iconColor
                );
            }
        };

        this.addRenderableWidget(this.tuanzis_mod$refreshButton);
        this.tuanzis_mod$updateButtonState();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void tuanzis_mod$onTick(CallbackInfo ci) {
        this.tuanzis_mod$updateButtonState();
    }

    @Unique
    private void tuanzis_mod$updateButtonState() {
        if (this.tuanzis_mod$refreshButton == null || this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        boolean hasCodex = this.minecraft.player.getOffhandItem().is(ModItems.CODEX_OF_ENCHANTING);
        this.tuanzis_mod$refreshButton.visible = hasCodex;
        if (!hasCodex) {
            return;
        }

        ItemStack item = this.menu.getSlot(0).getItem();
        boolean canEnchant = !item.isEmpty() && item.isEnchantable();
        boolean hasExp = this.minecraft.player.experienceLevel >= 1 || this.minecraft.player.hasInfiniteMaterials();

        this.tuanzis_mod$refreshButton.active = canEnchant && hasExp;

        if (!canEnchant) {
            this.tuanzis_mod$refreshButton.setTooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.enchantment.refresh.no_item")));
        } else if (!hasExp) {
            this.tuanzis_mod$refreshButton.setTooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.enchantment.refresh.no_exp")));
        } else {
            this.tuanzis_mod$refreshButton.setTooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.enchantment.refresh.ready")));
        }
    }
}
