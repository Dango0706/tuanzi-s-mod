package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ImmortalTalismanPlayerMixin {

    @Unique
    private boolean tuanzis_mod$hasTalisman() {
        Player player = (Player) (Object) this;
        return player.getInventory().contains(stack -> stack.is(ModItems.IMMORTAL_TALISMAN));
    }

    /**
     * 拦截物品掉落。定义在 Player 类中。
     */
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onDropEquipment(ServerLevel level, CallbackInfo ci) {
        if (tuanzis_mod$hasTalisman()) {
            ci.cancel();
        }
    }

    /**
     * 拦截经验掉落。定义在 Player 类中。
     */
    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onGetBaseExperienceReward(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        if (tuanzis_mod$hasTalisman()) {
            cir.setReturnValue(0);
        }
    }
}
