package me.tuanzi.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {

    public AnvilScreenMixin(AnvilMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title, net.minecraft.resources.Identifier texture) {
        super(menu, playerInventory, title, texture);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void tuanzis_mod$renderIncompatibleText(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        ItemStack result = this.menu.getSlot(2).getItem();
        if (!result.isEmpty()) {
            CustomData customData = result.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.copyTag().getBooleanOr("ink_incompatible", false)) {
                Component line = Component.translatable("tooltip.tuanzis_mod.void_ink_ingot.incompatible");
                int color = 0xFFFF5555; // 红色
                int tx = this.imageWidth - 8 - this.font.width(line) - 2;
                graphics.fill(tx - 2, 67, this.imageWidth - 8, 79, 1325400064);
                graphics.text(this.font, line, tx, 69, color);
            }
        }
    }
}
