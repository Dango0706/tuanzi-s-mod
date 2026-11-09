package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Locale;

/**
 * 古卷附魔-经验老辣 (Ancient Scroll - Seasoned) 计算辅助类
 * 仅能附魔在头盔上，最大等级 4。
 * 效果：当玩家汲取经验球时，提升获得的经验数量。
 * 提升公式：5% * level + max(0, level - 3) * 5%
 * 实际理论经验值：base * (1 + 5% * level + max(0, level - 3) * 5%)
 */
public final class ExperiencedEnchantmentHelper {

    private ExperiencedEnchantmentHelper() {
    }

    /**
     * 计算经验老辣的经验提升率
     * 公式: 5% * level + max(0, level - 3) * 5%
     *
     * @param level 附魔等级
     * @return 提升比例 (如 0.05F, 0.10F, 0.15F, 0.25F)
     */
    public static float getBonusRate(int level) {
        if (level <= 0) {
            return 0.0f;
        }
        return 0.05f * level + Math.max(0, level - 3) * 0.05f;
    }

    /**
     * 计算理论经验值 (浮点值，未离散化)
     *
     * @param baseAmount 经验球原始经验数量
     * @param level      附魔等级
     * @return 理论经验值
     */
    public static double calculateTheoreticalExperience(int baseAmount, int level) {
        if (baseAmount <= 0 || level <= 0) {
            return baseAmount;
        }
        return baseAmount * (1.0 + (double) getBonusRate(level));
    }

    /**
     * 根据基础经验、附魔等级和随机发生器计算最终获得的经验值。
     * 采用期望守恒的概率进位（Stochastic Rounding），使得小数值在离散化后长期数学期望严格等于理论值，杜绝小额经验球由于截断丢失增益。
     *
     * @param baseAmount 经验球原始经验数量
     * @param level      附魔等级
     * @param random     随机数发生器
     * @return 最终发放的经验值
     */
    public static int calculateBoostedExperience(int baseAmount, int level, RandomSource random) {
        if (baseAmount <= 0 || level <= 0) {
            return baseAmount;
        }
        float bonusRate = getBonusRate(level);
        float extra = baseAmount * bonusRate;
        int integerBonus = (int) extra;
        float fraction = extra - integerBonus;
        if (fraction > 0.0f && random != null && random.nextFloat() < fraction) {
            integerBonus++;
        }
        return baseAmount + integerBonus;
    }

    /**
     * 为汲取经验球的服务端玩家结算经验老辣附魔加成
     *
     * @param player     触碰经验球的玩家
     * @param baseAmount 经验球基础数值
     * @return 增幅后的经验数值
     */
    public static int calculateExperience(ServerPlayer player, int baseAmount) {
        if (player == null || baseAmount <= 0) {
            return baseAmount;
        }

        // 仅检测头盔槽位 (EquipmentSlot.HEAD)
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.isEmpty()) {
            return baseAmount;
        }

        var enchantmentLookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var seasonedEnchantment = enchantmentLookup.getOrThrow(ModEnchantments.ANCIENT_SCROLL_SEASONED);
        int level = EnchantmentHelper.getItemEnchantmentLevel(seasonedEnchantment, headStack);

        if (level <= 0) {
            return baseAmount;
        }

        float bonusRate = getBonusRate(level);
        double theoretical = calculateTheoreticalExperience(baseAmount, level);
        int finalAmount = calculateBoostedExperience(baseAmount, level, player.getRandom());

        ModLog.debug(player, null, "【古卷附魔-经验老辣】汲取经验球生效！原始经验: " + baseAmount
                + "，头盔附魔等级: " + level
                + " 级，提升比例: " + String.format(Locale.ROOT, "%.1f%%", bonusRate * 100.0f)
                + "，理论经验值: " + String.format(Locale.ROOT, "%.2f", theoretical)
                + "，实际结算经验: " + finalAmount);

        return finalAmount;
    }
}
