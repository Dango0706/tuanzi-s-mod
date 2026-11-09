package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Raid.class)
public abstract class RaidMixin {

    @ModifyArg(
        method = "tick(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/triggers/PlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;)V"
        ),
        index = 0
    )
    private ServerPlayer tuanzis_mod$rewardTradingMasterOnRaidWin(ServerPlayer playerHero) {
        if (playerHero != null) {
            var enchRegistry = playerHero.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var tradingMaster = enchRegistry.getOrThrow(ModEnchantments.TRADING_MASTER);
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            mutable.set(tradingMaster, 1);
            book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

            if (!playerHero.getInventory().add(book)) {
                playerHero.spawnAtLocation((net.minecraft.server.level.ServerLevel) playerHero.level(), book);
            }
            ModLog.debug(playerHero, "【交易大师】玩家成功击退袭击并荣获村庄英雄，奖励一本【交易大师 I】附魔书！");
        }
        return playerHero;
    }
}
