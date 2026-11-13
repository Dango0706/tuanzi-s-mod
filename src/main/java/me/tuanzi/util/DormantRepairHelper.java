package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class DormantRepairHelper {
    public static final String DORMANT_TIMER_KEY = "tuanzis_mod:dormant_timer";

    /**
     * 重置物品的休眠计时器。在物品受到任何耐久损耗（挖掘、攻击、受伤、格挡等）时调用。
     */
    public static void resetTimer(ItemStack stack) {
        if (stack.isEmpty()) return;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(DORMANT_TIMER_KEY)) {
                tag.remove(DORMANT_TIMER_KEY);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    /**
     * 玩家每秒（20 ticks）调用一次，检查并为满足条件的休眠修复物品恢复耐久。
     */
    public static void tickRepair(ServerPlayer player) {
        var enchRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var dormantHolder = enchRegistry.getOrThrow(ModEnchantments.DORMANT_REPAIR);

        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.isDamaged()) continue;

            // 耐久为 0 时不修复
            if (stack.getDamageValue() >= stack.getMaxDamage()) continue;

            int level = EnchantmentHelper.getItemEnchantmentLevel(dormantHolder, stack);
            if (level <= 0) continue;

            int targetSeconds = switch (level) {
                case 1 -> 180;
                case 2 -> 150;
                default -> 120;
            };

            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            int currentTimer = tag.getInt(DORMANT_TIMER_KEY).orElse(0) + 1;

            if (currentTimer >= targetSeconds) {
                stack.setDamageValue(stack.getDamageValue() - 1);
                tag.remove(DORMANT_TIMER_KEY);
                ModLog.debug(player, "【休眠修复】物品 %s 恢复 1 点耐久 (当前损耗: %d/%d, 等级: %d)",
                        stack.getHoverName().getString(), stack.getDamageValue(), stack.getMaxDamage(), level);
            } else {
                tag.putInt(DORMANT_TIMER_KEY, currentTimer);
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
