package me.tuanzi.mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    /**
     * 移除客户端铁砧界面 40 级“过于昂贵”的红色警告显示限制。
     */
    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
    private int tuanzis_mod$removeTooExpensiveLabel(int constant) {
        return Integer.MAX_VALUE;
    }
}
