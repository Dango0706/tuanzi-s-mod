package me.tuanzi.mixin;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public class AbstractVillagerMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void onStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        AbstractVillager villager = (AbstractVillager) (Object) this;
        
        // 只要是无 AI 的村民（在灵笼贸易站中托管的虚拟村民必为无 AI）
        if (villager.isNoAi()) {
            double distSqr = player.distanceToSqr(villager.getX(), villager.getY(), villager.getZ());
            System.out.println("[TuanziMod DEBUG] stillValid Mixin Intercepted!");
            System.out.println(" - Villager class: " + villager.getClass().getName());
            System.out.println(" - tradingPlayer: " + villager.getTradingPlayer());
            System.out.println(" - current player: " + player);
            System.out.println(" - isAlive: " + villager.isAlive());
            System.out.println(" - distanceToSqr: " + distSqr);
            
            // 只要离贸易站物理距离在 8 格内 (distSqr <= 64.0)，且村民依然存活，即强制认为交易有效，100% 避免因在世界中无实体跟踪导致闪退
            boolean valid = villager.isAlive() && distSqr <= 64.0;
            System.out.println(" - Forced return value: " + valid);
            cir.setReturnValue(valid);
        }
    }
}
