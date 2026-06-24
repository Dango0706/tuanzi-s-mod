package me.tuanzi.mixin.client;

import me.tuanzi.item.WorldSculptorsPenItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphicsExtractor.class)
public abstract class WorldSculptorsPenRendererMixin {

    @Shadow @Final private Matrix3x2fStack pose;

    @Shadow public abstract void item(ItemStack itemStack, int x, int y);

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void tuanzis_mod$onItemDecorations(Font font, ItemStack itemStack, int x, int y, String countText, CallbackInfo ci) {
        if (itemStack.getItem() instanceof WorldSculptorsPenItem) {
            ItemStack selectedBlock = WorldSculptorsPenItem.getSelectedBlockStack(itemStack);
            if (!selectedBlock.isEmpty()) {
                // 保存渲染矩阵
                this.pose.pushMatrix();
                // 移到右下角偏移，并进行 0.45 倍缩放（使方块完美在笔贴图的下方右下角呈现）
                this.pose.translate(x + 9, y + 9);
                this.pose.scale(0.45f, 0.45f);
                // 绘制选定方块的 ItemStack
                this.item(selectedBlock, 0, 0);
                // 恢复渲染矩阵
                this.pose.popMatrix();
            }
        }
    }
}
