package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class ExperienceEnchantmentMixin {

    @Inject(method = "processMobExperience", at = @At("RETURN"), cancellable = true)
    private static void tuanzis_mod$applyExperienceEnchantment(ServerLevel serverLevel, Entity killer, Entity killed, int amount, CallbackInfoReturnable<Integer> cir) {
        if (killer instanceof Player player) {
            // 除击杀玩家外
            if (killed instanceof Player) return;

            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty()) return;

            var registry = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var enchantmentHolder = registry.getOrThrow(ModEnchantments.EXPERIENCE);

            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantmentHolder, mainHand);
            if (level > 0) {
                int currentAmount = cir.getReturnValue();
                // 每一级增加25%(向上取整)
                float multiplier = 1.0f + (level * 0.25f);
                int newExp = (int) Math.ceil(currentAmount * multiplier);
                cir.setReturnValue(newExp);
                me.tuanzi.util.ModLog.debug(player, killed, "阅历附魔触发成功：将击杀生物掉落经验由 " + currentAmount + " 提升至 " + newExp + "（阅历等级: " + level + " 级）。");
            }

            var affinityHolder = registry.getOrThrow(ModEnchantments.EXPERIENCE_AFFINITY);
            int affinityLevel = EnchantmentHelper.getItemEnchantmentLevel(affinityHolder, mainHand);
            if (affinityLevel > 0) {
                int currentAmount = cir.getReturnValue();
                // 每一级增加8%
                float multiplier = 1.0f + (affinityLevel * 0.08f);
                int newExp = Math.round(currentAmount * multiplier);
                cir.setReturnValue(newExp);
                me.tuanzi.util.ModLog.debug(player, killed, String.format(java.util.Locale.ROOT, "【经验亲和】附魔生效（等级: %d）！击杀生物掉落经验由 %d 提升至 %d（+%.0f%%）", affinityLevel, currentAmount, newExp, affinityLevel * 8.0f));
            }
        }
    }

    @Inject(method = "processBlockExperience", at = @At("RETURN"), cancellable = true)
    private static void tuanzis_mod$applyExperienceAffinityBlock(ServerLevel serverLevel, ItemStack itemStack, int amount, CallbackInfoReturnable<Integer> cir) {
        if (itemStack.isEmpty()) return;
        int currentAmount = cir.getReturnValue();
        if (currentAmount <= 0) return;

        var registry = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var affinityHolder = registry.getOrThrow(ModEnchantments.EXPERIENCE_AFFINITY);
        int level = EnchantmentHelper.getItemEnchantmentLevel(affinityHolder, itemStack);
        if (level > 0) {
            float multiplier = 1.0f + (level * 0.08f);
            int newExp = Math.round(currentAmount * multiplier);
            cir.setReturnValue(newExp);
            me.tuanzi.util.ModLog.debug("【经验亲和】附魔生效（等级: %d）！挖掘矿石掉落经验由 %d 提升至 %d（+%.0f%%）", level, currentAmount, newExp, level * 8.0f);
        }
    }
}
