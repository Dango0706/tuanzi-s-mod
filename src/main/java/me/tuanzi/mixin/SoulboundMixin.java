package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(ServerPlayer.class)
public abstract class SoulboundMixin {
    @Unique
    private final Map<Integer, ItemStack> tuanzis_mod$soulboundItems = new HashMap<>();

    @Unique
    private boolean tuanzis_mod$hasImmortalTalisman(ServerPlayer player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.IMMORTAL_TALISMAN));
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void tuanzis_mod$captureSoulboundItems(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        if (tuanzis_mod$hasImmortalTalisman(player)) return;

        Inventory inventory = player.getInventory();
        var enchantmentRegistry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var soulboundHolder = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(soulboundHolder, stack) > 0) {
                tuanzis_mod$soulboundItems.put(i, stack.copy());
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void tuanzis_mod$putBackSoulboundItems(DamageSource source, CallbackInfo ci) {
        if (tuanzis_mod$soulboundItems.isEmpty()) return;
        
        ServerPlayer player = (ServerPlayer) (Object) this;
        Inventory inventory = player.getInventory();
        tuanzis_mod$soulboundItems.forEach(inventory::setItem);
        tuanzis_mod$soulboundItems.clear();
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void tuanzis_mod$copySoulboundItemsOnRespawn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        if (!keepEverything && !tuanzis_mod$hasImmortalTalisman(oldPlayer)) {
            ServerPlayer newPlayer = (ServerPlayer) (Object) this;
            if (!newPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
                Inventory oldInventory = oldPlayer.getInventory();
                Inventory newInventory = newPlayer.getInventory();
                
                var enchantmentRegistry = newPlayer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var soulboundHolder = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);

                for (int i = 0; i < oldInventory.getContainerSize(); i++) {
                    ItemStack stack = oldInventory.getItem(i);
                    if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(soulboundHolder, stack) > 0) {
                        if (newInventory.getItem(i).isEmpty()) {
                            newInventory.setItem(i, stack.copy());
                        } else {
                            newInventory.add(stack.copy());
                        }
                    }
                }
            }
        }
    }
}
