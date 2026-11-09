package me.tuanzi.mixin.client;

import me.tuanzi.client.CorridorAccessoryClientState;
import me.tuanzi.init.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    public InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$redirectCorridorInventory(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null
                && mc.level.dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL
                && !mc.player.isCreative()) {
            ci.cancel();
            mc.gui.setScreen(null);
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new me.tuanzi.network.scavenger.OpenCorridorLoadoutC2SPacket()
            );
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tuanzis_mod$renderCorridorAccessorySlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return;
        }

        int xo = this.leftPos - 26;
        int yo = this.topPos + 8;

        ItemStack hallStack = CorridorAccessoryClientState.getHallStack();
        ItemStack roomStack = CorridorAccessoryClientState.getRoomStack();
        ItemStack marketStack = CorridorAccessoryClientState.getMarketStack();

        // 槽位 0 (归厅信标)
        boolean hover0 = mouseX >= xo && mouseX < xo + 22 && mouseY >= yo && mouseY < yo + 22;
        graphics.fill(xo, yo, xo + 22, yo + 22, 0xD010141C);
        int borderCol0 = hover0 ? 0xFFFFFFFF : 0xFF3892A0;
        graphics.fill(xo, yo, xo + 22, yo + 1, borderCol0);
        graphics.fill(xo, yo + 21, xo + 22, yo + 22, borderCol0);
        graphics.fill(xo, yo, xo + 1, yo + 22, borderCol0);
        graphics.fill(xo + 21, yo, xo + 22, yo + 22, borderCol0);
        if (!hallStack.isEmpty()) {
            graphics.item(hallStack, xo + 3, yo + 3);
            graphics.itemDecorations(this.font, hallStack, xo + 3, yo + 3);
        }

        // 槽位 1 (归室信标)
        int yo1 = yo + 26;
        boolean hover1 = mouseX >= xo && mouseX < xo + 22 && mouseY >= yo1 && mouseY < yo1 + 22;
        graphics.fill(xo, yo1, xo + 22, yo1 + 22, 0xD01C1410);
        int borderCol1 = hover1 ? 0xFFFFFFFF : 0xFFC88A20;
        graphics.fill(xo, yo1, xo + 22, yo1 + 1, borderCol1);
        graphics.fill(xo, yo1 + 21, xo + 22, yo1 + 22, borderCol1);
        graphics.fill(xo, yo1, xo + 1, yo1 + 22, borderCol1);
        graphics.fill(xo + 21, yo1, xo + 22, yo1 + 22, borderCol1);
        if (!roomStack.isEmpty()) {
            graphics.item(roomStack, xo + 3, yo1 + 3);
            graphics.itemDecorations(this.font, roomStack, xo + 3, yo1 + 3);
        }

        // 槽位 2 (归市信标)
        int yo2 = yo + 52;
        boolean hover2 = mouseX >= xo && mouseX < xo + 22 && mouseY >= yo2 && mouseY < yo2 + 22;
        graphics.fill(xo, yo2, xo + 22, yo2 + 22, 0xD0101C14);
        int borderCol2 = hover2 ? 0xFFFFFFFF : 0xFF00C853;
        graphics.fill(xo, yo2, xo + 22, yo2 + 1, borderCol2);
        graphics.fill(xo, yo2 + 21, xo + 22, yo2 + 22, borderCol2);
        graphics.fill(xo, yo2, xo + 1, yo2 + 22, borderCol2);
        graphics.fill(xo + 21, yo2, xo + 22, yo2 + 22, borderCol2);
        if (!marketStack.isEmpty()) {
            graphics.item(marketStack, xo + 3, yo2 + 3);
            graphics.itemDecorations(this.font, marketStack, xo + 3, yo2 + 3);
        }

        // Tooltip 悬停显示
        if (hover0 && !hallStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hallStack, mouseX, mouseY);
        } else if (hover1 && !roomStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, roomStack, mouseX, mouseY);
        } else if (hover2 && !marketStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, marketStack, mouseX, mouseY);
        }
    }
}
