package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class SoulboundMixin {
    // 临时存储，用于在同一个实例的死亡周期内转移物品
    private final List<ItemStack> tuanzis_mod$soulboundItems = new ArrayList<>();

    /**
     * 拦截死亡：将带有灵魂绑定的物品暂时取出，防止其进入掉落列表。
     * 注入到 die 方法头部。
     */
    @Inject(method = "die", at = @At("HEAD"))
    private void tuanzis_mod$captureSoulboundItems(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        // 如果开启了死亡掉落保护，则不需要处理
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) return;

        Inventory inventory = player.getInventory();
        var enchantmentRegistry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var soulboundHolder = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(soulboundHolder, stack) > 0) {
                tuanzis_mod$soulboundItems.add(stack.copy());
                // 设置为 count 0 或空，防止掉落
                stack.setCount(0);
            }
        }
    }

    /**
     * 死亡逻辑完成后（掉落物已生成），将物品放回旧玩家的背包中（用于 restoreFrom 读取）。
     */
    @Inject(method = "die", at = @At("TAIL"))
    private void tuanzis_mod$putBackSoulboundItems(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Inventory inventory = player.getInventory();
        for (ItemStack stack : tuanzis_mod$soulboundItems) {
            inventory.add(stack);
        }
        tuanzis_mod$soulboundItems.clear();
    }

    /**
     * 在玩家重生（实例切换）时，将物品从旧实例拷贝到新实例。
     */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void tuanzis_mod$copySoulboundItemsOnRespawn(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        if (!alive) {
            ServerPlayer newPlayer = (ServerPlayer) (Object) this;
            if (!newPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
                Inventory oldInventory = oldPlayer.getInventory();
                Inventory newInventory = newPlayer.getInventory();
                
                var enchantmentRegistry = newPlayer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var soulboundHolder = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);

                for (int i = 0; i < oldInventory.getContainerSize(); i++) {
                    ItemStack stack = oldInventory.getItem(i);
                    if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(soulboundHolder, stack) > 0) {
                        newInventory.add(stack.copy());
                    }
                }
            }
        }
    }
}

