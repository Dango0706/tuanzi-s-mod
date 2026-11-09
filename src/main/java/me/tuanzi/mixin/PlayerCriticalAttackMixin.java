package me.tuanzi.mixin;

import me.tuanzi.entity.CinderSovereignEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家对处于【日冕破城杵 · 炽核坠碾】卡地虚弱窗口的日冕核心造成近战攻击时必定触发暴击
 */
@Mixin(Player.class)
public class PlayerCriticalAttackMixin {
    @Inject(method = "canCriticalAttack", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$guaranteeCritOnCrucibleVulnerable(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof CinderSovereignEntity boss && boss.isCrucibleVulnerable()) {
            cir.setReturnValue(true);
        }
    }
}
