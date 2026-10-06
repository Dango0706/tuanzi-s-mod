package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Shadow
    public abstract Component description();

    @Unique
    private static final Style TUANZIS_MOD$ANCIENT_SCROLL_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x7F00FF));

    @Unique
    private boolean tuanzis_mod$isPowerEnchantment() {
        if (this.description().getContents() instanceof TranslatableContents tc) {
            return "enchantment.minecraft.power".equals(tc.getKey());
        }
        return this.description().getString().contains("enchantment.minecraft.power");
    }

    @Unique
    private static boolean tuanzis_mod$isCrossbow(ItemStack stack) {
        return stack.is(Items.CROSSBOW) || stack.getItem() instanceof CrossbowItem || stack.is(ItemTags.CROSSBOW_ENCHANTABLE);
    }

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

    /**
     * 铁砧与指令兼容：允许弩附魔力量
     */
    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowCrossbowPowerCanEnchant(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (tuanzis_mod$isPowerEnchantment() && tuanzis_mod$isCrossbow(itemStack)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 物品支持检测兼容：允许弩附魔力量
     */
    @Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowCrossbowPowerIsSupported(ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (tuanzis_mod$isPowerEnchantment() && tuanzis_mod$isCrossbow(item)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 附魔台候选池兼容：允许弩在附魔台刷出力量
     */
    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowCrossbowPowerIsPrimary(ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (tuanzis_mod$isPowerEnchantment() && tuanzis_mod$isCrossbow(item)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 伤害结算修改：当使用弩射出箭矢命中时，力量附魔增伤效果为弓的一半
     */
    @Inject(method = "modifyDamage", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$modifyCrossbowPowerDamage(
            ServerLevel serverLevel,
            int enchantmentLevel,
            ItemStack itemStack,
            Entity victim,
            DamageSource damageSource,
            MutableFloat amount,
            CallbackInfo ci
    ) {
        if (tuanzis_mod$isPowerEnchantment() && tuanzis_mod$isCrossbow(itemStack)) {
            // 力量附魔仅在射出箭矢时生效（排除直接近战敲击）
            Entity directAttacker = damageSource.getDirectEntity();
            boolean isArrow = directAttacker instanceof AbstractArrow
                    || (directAttacker != null && directAttacker.is(EntityTypeTags.ARROWS));
            if (isArrow) {
                float halfBonus = DamageCalculator.getCrossbowPowerBonus(enchantmentLevel);
                amount.add(halfBonus);
                ModLog.debug(damageSource.getEntity(), victim, "【弩-力量附魔】附魔生效！等级: " + enchantmentLevel
                        + "，弓力量基准增伤: " + String.format("%.2f", halfBonus * 2.0f)
                        + "，弩减半增伤: " + String.format("%.2f", halfBonus)
                        + "，累加后当前箭矢基础伤害: " + String.format("%.2f", amount.floatValue()));
            }
            ci.cancel();
        }
    }
}

