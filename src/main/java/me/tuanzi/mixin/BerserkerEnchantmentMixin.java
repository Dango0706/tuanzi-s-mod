package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BerserkerEnchantmentMixin {

    @Inject(method = "getArmorValue", at = @At("RETURN"), cancellable = true)
    private void tuanzis_mod$applyBerserkerArmorReduction(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.isEmpty()) return;

        var registry = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var berserkerEnch = registry.getOrThrow(ModEnchantments.BERSERKER);
        
        int level = EnchantmentHelper.getItemEnchantmentLevel(berserkerEnch, chestStack);
        if (level > 0) {
            int originalArmor = cir.getReturnValue();
            if (originalArmor <= 0) return;

            // 获取该胸甲对 Attributes.ARMOR 的基础属性修改值
            ItemAttributeModifiers modifiers = chestStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            double baseChestArmor = 0.0;
            if (modifiers != null) {
                baseChestArmor = modifiers.compute(Attributes.ARMOR, 0.0, EquipmentSlot.CHEST);
            }

            if (baseChestArmor > 0.0) {
                // 计算扣减值：I级扣减35%，II级扣减30%
                double reductionRate = level == 1 ? 0.35 : 0.30;
                double reductionAmount = baseChestArmor * reductionRate;
                
                // 四舍五入，并做 clamp 以防过度扣减
                int intReduction = (int) Math.round(reductionAmount);
                int finalArmor = Math.max(0, originalArmor - intReduction);
                cir.setReturnValue(finalArmor);
            }
        }
    }
}
