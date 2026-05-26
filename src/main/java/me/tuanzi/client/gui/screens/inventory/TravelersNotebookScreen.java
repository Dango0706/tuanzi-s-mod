package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.world.inventory.TravelersNotebookMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class TravelersNotebookScreen extends AbstractContainerScreen<TravelersNotebookMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/shulker_box.png");

    public TravelersNotebookScreen(TravelersNotebookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        
        // 突出燃料格 (Slot 0)
        // Slot 0 位置是 8, 18。我们加上淡橙色高亮。
        graphics.fill(x + 7, y + 17, x + 7 + 18, y + 17 + 18, 0x55FFAA00);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
}
