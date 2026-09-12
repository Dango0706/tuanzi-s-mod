package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Unique
    private static final Style TUANZIS_MOD$ANCIENT_SCROLL_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x7F00FF));

    @Inject(method = "getFullname", at = @At("HEAD"), cancellable = true)
    private static void tuanzis_mod$renderAncientScrollColor(Holder<Enchantment> enchantment, int level, CallbackInfoReturnable<Component> cir) {
        if (enchantment.is(ModEnchantments.ANCIENT_SCROLL)) {
            MutableComponent result = enchantment.value().description().copy();
            result = ComponentUtils.mergeStyles(result, TUANZIS_MOD$ANCIENT_SCROLL_STYLE);

            if (level != 1 || enchantment.value().getMaxLevel() != 1) {
                result.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level).withStyle(TUANZIS_MOD$ANCIENT_SCROLL_STYLE));
            }

            cir.setReturnValue(result);
        }
    }
}
