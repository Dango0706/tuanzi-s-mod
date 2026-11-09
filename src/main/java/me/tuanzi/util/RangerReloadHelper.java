package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class RangerReloadHelper {
    public static boolean hasRangerReload(Level level, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof CrossbowItem && level != null) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var ench = lookup.getOrThrow(ModEnchantments.RANGER_RELOAD);
            return EnchantmentHelper.getItemEnchantmentLevel(ench, stack) > 0;
        }
        return false;
    }
}
