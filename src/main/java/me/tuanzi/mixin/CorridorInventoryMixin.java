package me.tuanzi.mixin;

import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.profile.CorridorInventoryRules;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在拾荒回廊维度内接管 {@link Inventory#add(int, ItemStack)}：
 * 封锁原版隐藏主物品栏（9..35），并将自动拾取路由至空背包槽、快捷栏(0..8)或已装备战术背包二维网格。
 */
@Mixin(Inventory.class)
public abstract class CorridorInventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$routeCorridorAdd(int slot, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!CorridorInventoryRules.shouldInterceptInventory(this.player)) {
            return;
        }
        if (this.player.level().isClientSide()) {
            // 客户端侧严禁向 9..35 写入；地面拾取由服务端权威驱动同步
            if (CorridorInventoryRules.isBlockedVanillaMainSlot(slot)) {
                cir.setReturnValue(false);
            }
            return;
        }

        PlayerCorridorProfile profile = (this.player instanceof ScavengerPlayerTracker tracker)
                ? tracker.tuanzis_mod$getCorridorProfile()
                : null;

        boolean added = CorridorInventoryRules.routeItemAddition(
                slot,
                itemStack,
                idx -> this.items.get(idx),
                (idx, stack) -> this.items.set(idx, stack),
                profile,
                this.player.registryAccess(),
                this.player.getRandom()
        );
        cir.setReturnValue(added);
    }
}
