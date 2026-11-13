package me.tuanzi.mixin;

import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerInfinityBowMixin {

    @Unique
    private static boolean tuanzis_mod$hasInfinity(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(Enchantments.INFINITY)) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void tuanzis_mod$provideVirtualArrowForInfinity(ItemStack heldWeapon, CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue().isEmpty() && !heldWeapon.isEmpty() && heldWeapon.getItem() instanceof BowItem) {
            if (tuanzis_mod$hasInfinity(heldWeapon)) {
                ModLog.debug("PlayerInfinityBow", "getProjectile", "检测到玩家手持具有无限附魔的弓且背包无箭，提供虚拟箭矢支持射击。");
                cir.setReturnValue(new ItemStack(Items.ARROW));
            }
        }
    }
}
