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
            }
        }
    }
}
