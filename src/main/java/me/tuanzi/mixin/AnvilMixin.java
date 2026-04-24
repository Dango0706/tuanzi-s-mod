package me.tuanzi.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMixin {
    @Shadow @Final private DataSlot cost;

    /**
     * 移除铁砧 40 级上限的判断逻辑。
     * 将代码中出现的常数 40 替换为一个极大的值（Integer.MAX_VALUE）。
     */
    @ModifyConstant(method = "createResult", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 40))
    private int tuanzis_mod$removeLevelLimit(int constant) {
        return Integer.MAX_VALUE;
    }

    /**
     * 确保玩家可以取出结果，即便等级非常高。
     */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$alwaysMayPickup(Player player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        // 如果玩家不是创造模式，则只需检查经验等级是否足够支付当前花费
        if (!player.getAbilities().instabuild) {
            cir.setReturnValue(player.experienceLevel >= this.cost.get());
        }
    }
}
