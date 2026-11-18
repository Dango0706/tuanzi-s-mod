package me.tuanzi.mixin;

import me.tuanzi.util.EternalEnchantmentHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {

    /**
     * 禁止在砂轮中将带有【永恒】的物品与另一件物品合并，防止【永恒】脱离无法破坏属性被转移。
     */
    @Inject(method = "mergeItems", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$preventMergingEternalItems(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
        if (EternalEnchantmentHelper.hasEternal(input) || EternalEnchantmentHelper.hasEternal(additional)) {
            ModLog.debug("GrindstoneMenu", "mergeItems", "检测到【永恒】物品参与双物品砂轮合并，已阻断输出。");
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
