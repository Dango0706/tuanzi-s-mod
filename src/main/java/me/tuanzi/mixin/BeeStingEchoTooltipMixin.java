package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public class BeeStingEchoTooltipMixin {
    @ModifyVariable(
        method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> tuanzis_mod$wrapConsumer(
        TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> originalConsumer,
        EquipmentSlotGroup slot
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.is(ModItems.BEE_STING_ECHO) && slot == EquipmentSlotGroup.MAINHAND) {
            var enchantments = stack.getEnchantments();
            if (enchantments != null && !enchantments.isEmpty()) {
                float sharpnessDamage = 0.0f;
                for (var entry : enchantments.entrySet()) {
                    Holder<Enchantment> enchantmentHolder = entry.getKey();
                    int level = entry.getIntValue();
                    if (level <= 0) continue;
                    if (enchantmentHolder.is(Enchantments.SHARPNESS)) {
                        sharpnessDamage += 1.0f + 0.5f * (level - 1);
                    }
                }
                if (sharpnessDamage > 0.0f) {
                    final float extra = sharpnessDamage;
                    return (attribute, modifier, display) -> {
                        if (attribute.is(Attributes.ATTACK_DAMAGE) && modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID)) {
                            AttributeModifier newModifier = new AttributeModifier(
                                modifier.id(),
                                modifier.amount() + extra,
                                modifier.operation()
                            );
                            originalConsumer.accept(attribute, newModifier, display);
                        } else {
                            originalConsumer.accept(attribute, modifier, display);
                        }
                    };
                }
            }
        }
        return originalConsumer;
    }
}
