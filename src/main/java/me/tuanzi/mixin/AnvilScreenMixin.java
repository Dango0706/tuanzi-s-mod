package me.tuanzi.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @Shadow private EditBox name;

    /**
     * 移除客户端铁砧界面 40 级“过于昂贵”的红色警告显示限制。
     */
    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
    private int tuanzis_mod$removeTooExpensiveLabel(int constant) {
        return Integer.MAX_VALUE;
    }

    /**
     * 移除 50 字符的重命名限制
     */
    @Inject(method = "subInit", at = @At("TAIL"))
    private void tuanzis_mod$removeRenameLimit(CallbackInfo ci) {
        if (this.name != null) {
            this.name.setMaxLength(Integer.MAX_VALUE);
        }
    }
}
