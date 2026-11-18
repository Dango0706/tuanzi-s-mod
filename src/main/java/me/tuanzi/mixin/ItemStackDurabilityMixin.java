package me.tuanzi.mixin;

import me.tuanzi.entity.TrialDummyEntity;
import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.EternalEnchantmentHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackDurabilityMixin {

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tuanzis_mod$preventDummyAttackDamage(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            if (TrialDummyEntity.ATTACKING_PLAYERS.contains(player.getUUID())) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tuanzis_mod$preventDummyAttackDamageServer(int amount, ServerLevel level, ServerPlayer player, Consumer<?> onBreak, CallbackInfo ci) {
        if (player != null) {
            if (TrialDummyEntity.ATTACKING_PLAYERS.contains(player.getUUID())) {
                ci.cancel();
                return;
            }
        }
        if (amount > 0) {
            ItemStack self = (ItemStack) (Object) this;
            me.tuanzi.util.DormantRepairHelper.resetTimer(self);
        }
    }

    @Inject(method = "enchant", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$blockEnchantIfEternal(Holder<Enchantment> enchantment, int level, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (EternalEnchantmentHelper.hasEternal(self)) {
            ModLog.debug("ItemStack", "enchant",
                    "物品已拥有【永恒】附魔，拦截再次附魔/升级: 物品=" + self.getHoverName().getString()
                            + ", 目标附魔=" + enchantment.getRegisteredName() + " Lv." + level);
            ci.cancel();
        }
    }

    @Inject(method = "enchant", at = @At("TAIL"))
    private void tuanzis_mod$applyUnbreakableOnEternalEnchant(Holder<Enchantment> enchantment, int level, CallbackInfo ci) {
        if (enchantment.is(ModEnchantments.ETERNAL)) {
            ItemStack self = (ItemStack) (Object) this;
            EternalEnchantmentHelper.applyUnbreakable(self);
            ModLog.debug("ItemStack", "enchant",
                    "通过 ItemStack#enchant 应用【永恒】附魔，已同步设置 Unbreakable=true: " + self.getHoverName().getString());
        }
    }

    @Inject(method = "getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true)
    private void tuanzis_mod$applyPurityEatingSpeed(LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if (original <= 0 || user == null) return;

        ItemStack self = (ItemStack) (Object) this;
        // 判定是否为进食行为（食品组件或进食动作）
        if (self.has(DataComponents.FOOD) || self.getUseAnimation() == ItemUseAnimation.EAT) {
            ItemStack helmet = user.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                var enchRegistry = user.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var purityHolder = enchRegistry.getOrThrow(ModEnchantments.PURITY);
                int level = EnchantmentHelper.getItemEnchantmentLevel(purityHolder, helmet);
                if (level > 0) {
                    float reduction = 0.10f * level; // 10%, 20%, 30%
                    int reduced = Math.max(1, Math.round(original * (1.0f - reduction)));
                    cir.setReturnValue(reduced);
                    if (!user.level().isClientSide()) {
                        ModLog.debug(user, "【抱朴】进食时间缩短: 原始耗时 %d ticks -> 现耗时 %d ticks (等级: %d, -%.0f%%)",
                                original, reduced, level, reduction * 100f);
                    }
                }
            }
        }
    }
}
