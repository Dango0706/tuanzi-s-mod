package me.tuanzi.mixin;

import me.tuanzi.item.BeeStingEchoItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerSweepMixin {
    @Inject(
        method = "isSweepAttack(ZZZ)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tuanzis_mod$disableBeeStingEchoSweep(boolean fullStrengthAttack, boolean criticalAttack, boolean knockbackAttack, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BeeStingEchoItem) {
            cir.setReturnValue(false);
        }
    }
}
