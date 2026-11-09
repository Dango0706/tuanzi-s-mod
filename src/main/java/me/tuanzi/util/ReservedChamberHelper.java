package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ReservedChamberHelper {

    /**
     * 获取指定弩物品上的“预备弹仓”附魔等级。
     *
     * @param level 当前所在世界（可为 null）
     * @param stack 弩物品堆
     * @return 预备弹仓附魔等级（0 表示未附魔）
     */
    public static int getReservedChamberLevel(@Nullable Level level, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof CrossbowItem)) {
            return 0;
        }

        if (level != null) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var ench = lookup.getOrThrow(ModEnchantments.RESERVED_CHAMBER);
            return EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        }

        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey().is(ModEnchantments.RESERVED_CHAMBER)) {
                    return entry.getIntValue();
                }
            }
        }

        return 0;
    }

    /**
     * 判断弩是否附有“预备弹仓”附魔。
     */
    public static boolean hasReservedChamber(@Nullable Level level, ItemStack stack) {
        return getReservedChamberLevel(level, stack) > 0;
    }
}
