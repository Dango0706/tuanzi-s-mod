package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionContents.class)
public class PotionContentsMixin {

    @Inject(method = "onConsume", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$applyPharmacology(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (!level.isClientSide() && user instanceof ServerPlayer player && stack.is(Items.POTION)) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                var enchRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var pharmHolder = enchRegistry.getOrThrow(ModEnchantments.PHARMACOLOGY);
                int levelEnch = EnchantmentHelper.getItemEnchantmentLevel(pharmHolder, helmet);
                if (levelEnch > 0) {
                    ci.cancel();
                    float multiplier = 1.0f + 0.08f * levelEnch;
                    float baseScale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
                    PotionContents self = (PotionContents) (Object) this;
                    ServerLevel serverLevel = (ServerLevel) level;

                    self.forEachEffect(effect -> {
                        if (effect.getEffect().value().isInstantaneous()) {
                            effect.getEffect().value().applyInstantaneousEffect(serverLevel, player, player, user, effect.getAmplifier(), 1.0);
                        } else {
                            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                                int newDuration = Math.round(effect.getDuration() * multiplier);
                                MobEffectInstance scaledEffect = new MobEffectInstance(
                                        effect.getEffect(),
                                        newDuration,
                                        effect.getAmplifier(),
                                        effect.isAmbient(),
                                        effect.isVisible(),
                                        effect.showIcon()
                                );
                                user.addEffect(scaledEffect);
                                ModLog.debug(player, "【药理】正面药水效果 %s 持续时间由 %d 提升至 %d ticks (倍率: %.2f, 等级: %d)",
                                        effect.getEffect().value().getDescriptionId(), effect.getDuration(), newDuration, multiplier, levelEnch);
                            } else {
                                user.addEffect(effect);
                            }
                        }
                    }, baseScale);
                }
            }
        }
    }
}
