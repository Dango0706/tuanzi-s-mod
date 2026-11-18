package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Collection;

/**
 * 【永恒】附魔核心逻辑工具类
 */
public final class EternalEnchantmentHelper {
    public static final int LEVELS_PER_ENCHANTMENT = 8;
    public static final int MIN_ANVIL_COST = 8;

    private EternalEnchantmentHelper() {
    }

    /**
     * 判断物品（含附魔书或装备/工具）是否拥有【永恒】附魔
     */
    public static boolean hasEternal(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        return containsEternal(enchantments.keySet());
    }

    /**
     * 判断附魔集合中是否包含【永恒】附魔
     */
    public static boolean containsEternal(Collection<Holder<Enchantment>> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return false;
        }
        for (Holder<Enchantment> holder : enchantments) {
            if (holder.is(ModEnchantments.ETERNAL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算使用铁砧附魔【永恒】时所需的固定经验等级：
     * 附魔前物品原有附魔数量 × 8 级，最低消耗 8 级。
     */
    public static int calculateAnvilCost(int existingEnchantmentCount) {
        int clampedCount = Math.max(1, existingEnchantmentCount);
        int cost = clampedCount * LEVELS_PER_ENCHANTMENT;
        ModLog.debug("EternalEnchantmentHelper", "calculateAnvilCost",
                "计算【永恒】铁砧附魔消耗: 原有附魔数=" + existingEnchantmentCount + ", 最终消耗等级=" + cost);
        return cost;
    }

    /**
     * 为物品赋予无法破坏属性（DataComponents.UNBREAKABLE），并清除残余耐久损伤。
     */
    public static void applyUnbreakable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        if (stack.has(DataComponents.DAMAGE) && stack.getDamageValue() > 0) {
            stack.setDamageValue(0);
        }
        ModLog.debug("EternalEnchantmentHelper", "applyUnbreakable",
                "已为物品赋予无法破坏(Unbreakable=true)属性: " + stack.getHoverName().getString());
    }
}
