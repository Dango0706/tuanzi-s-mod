package me.tuanzi.mixin;

import me.tuanzi.util.EternalEnchantmentHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    /**
     * 已拥有【永恒】附魔的物品无法再与任何附魔兼容（拦截 /enchant 指令等）
     */
    @Inject(method = "isEnchantmentCompatible", at = @At("HEAD"), cancellable = true)
    private static void tuanzis_mod$blockCompatibleIfEternal(
            Collection<Holder<Enchantment>> enchants,
            Holder<Enchantment> target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (EternalEnchantmentHelper.containsEternal(enchants)) {
            ModLog.debug("EnchantmentHelper", "isEnchantmentCompatible",
                    "物品已拥有【永恒】附魔，拒绝继续兼容/追加附魔: " + target.getRegisteredName());
            cir.setReturnValue(false);
        }
    }
}
