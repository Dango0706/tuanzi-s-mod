package me.tuanzi.mixin;

import me.tuanzi.entity.TrialDummyEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackDurabilityMixin {
    
    @Inject(
        method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$preventDummyAttackDamage(int amount, net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.EquipmentSlot slot, CallbackInfo ci) {
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
    private void tuanzis_mod$preventDummyAttackDamageServer(int amount, net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, java.util.function.Consumer<?> onBreak, CallbackInfo ci) {
        if (player != null) {
            if (TrialDummyEntity.ATTACKING_PLAYERS.contains(player.getUUID())) {
                ci.cancel();
            }
        }
    }
}
