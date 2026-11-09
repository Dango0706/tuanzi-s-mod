package me.tuanzi.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class VillagerMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        // 判定手持的是否为缚灵笼，且该缚灵笼尚未装有村民
        if (stack.is(me.tuanzi.init.ModItems.VILLAGER_CAGE) && !me.tuanzi.item.VillagerCageItem.isVillagerStored(stack)) {
            // 直接调用缚灵笼的 interactLivingEntity 并截获交互
            InteractionResult result = stack.interactLivingEntity(player, (Villager) (Object) this, hand);
            if (result.consumesAction()) {
                cir.setReturnValue(result);
            }
        }
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void tuanzis_mod$addMasterLibrarianTrade(net.minecraft.server.level.ServerLevel level, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        net.minecraft.world.entity.npc.villager.VillagerData data = villager.getVillagerData();
        if (data.level() == 5 && data.profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN)) {
            tuanzis_mod$tryAddMasterEnchantmentOffer(villager, level, me.tuanzi.init.ModEnchantments.SATIATION, "饱腹", 15);
            tuanzis_mod$tryAddMasterEnchantmentOffer(villager, level, me.tuanzi.init.ModEnchantments.ECHOLOCATION, "回声定位", 25);
            tuanzis_mod$tryAddMasterEnchantmentOffer(villager, level, me.tuanzi.init.ModEnchantments.MOON_EMBRACE, "衔月", 20);
        }
    }

    @Unique
    private void tuanzis_mod$tryAddMasterEnchantmentOffer(
        Villager villager,
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey,
        String enchName,
        int baseEmeraldCost
    ) {
        boolean alreadyHas = false;
        for (net.minecraft.world.item.trading.MerchantOffer offer : villager.getOffers()) {
            ItemStack result = offer.getResult();
            if (result.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
                net.minecraft.world.item.enchantment.ItemEnchantments enchantments = result.getOrDefault(
                    net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
                );
                for (var holder : enchantments.keySet()) {
                    if (holder.is(enchKey)) {
                        alreadyHas = true;
                        break;
                    }
                }
            }
            if (alreadyHas) break;
        }

        if (!alreadyHas) {
            var enchRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var holder = enchRegistry.getOrThrow(enchKey);
            int randomLevel = villager.getRandom().nextInt(3) + 1; // 1 to 3
            ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            net.minecraft.world.item.enchantment.ItemEnchantments.Mutable mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
            );
            mutable.set(holder, randomLevel);
            book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

            int emeraldCost = baseEmeraldCost + randomLevel * 10 + villager.getRandom().nextInt(5);
            net.minecraft.world.item.trading.MerchantOffer offer = new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD, emeraldCost),
                java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.BOOK, 1)),
                book,
                12, // maxUses
                30, // xp
                0.05f // priceMultiplier
            );
            villager.getOffers().add(offer);
            me.tuanzi.util.ModLog.debug(villager, null, "【" + enchName + "】大师级图书管理员已生成专属附魔书交易（等级: " + randomLevel + "，价格: " + emeraldCost + " 绿宝石 + 1 书）！");
        }
    }

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void tuanzis_mod$applyTradingMasterPriceDiscount(Player player, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (player == null) return;
        ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return;

        var enchRegistry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var tradingMasterHolder = enchRegistry.getOrThrow(me.tuanzi.init.ModEnchantments.TRADING_MASTER);
        int level = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(tradingMasterHolder, helmet);
        if (level > 0) {
            Villager villager = (Villager) (Object) this;
            float discountRate = level * 0.10f; // 10%, 20%, 30%
            for (net.minecraft.world.item.trading.MerchantOffer offer : villager.getOffers()) {
                int baseCount = offer.getBaseCostA().getCount();
                int discount = (int) Math.round(baseCount * discountRate);
                if (discount > 0) {
                    offer.addToSpecialPriceDiff(-discount);
                    me.tuanzi.util.ModLog.debug(player, "【交易大师】附魔生效（等级: %d）！价格降低 %d（折扣率: %.0f%%），基础数量: %d", level, discount, discountRate * 100, baseCount);
                }
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method = "rewardTradeXp",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"),
        index = 4
    )
    private int tuanzis_mod$boostTradeXp(int originalXp) {
        Villager villager = (Villager) (Object) this;
        Player player = villager.getTradingPlayer();
        if (player != null) {
            ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                var enchRegistry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var tradingMasterHolder = enchRegistry.getOrThrow(me.tuanzi.init.ModEnchantments.TRADING_MASTER);
                int level = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(tradingMasterHolder, helmet);
                if (level > 0) {
                    float boostRate = level * 0.20f; // 20%, 40%, 60%
                    int boosted = Math.round(originalXp * (1.0f + boostRate));
                    me.tuanzi.util.ModLog.debug(player, "【交易大师】附魔生效（等级: %d）！交易经验由 %d 提升至 %d（+%.0f%%）", level, originalXp, boosted, boostRate * 100);
                    return boosted;
                }
            }
        }
        return originalXp;
    }
}
