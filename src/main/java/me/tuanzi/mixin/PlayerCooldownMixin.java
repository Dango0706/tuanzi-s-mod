package me.tuanzi.mixin;

import me.tuanzi.util.TideCleaverPlayerTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerCooldownMixin implements TideCleaverPlayerTracker {

    @Unique
    private int tuanzis_mod$ticksSinceFullyCharged = 0;

    @Unique
    private float tuanzis_mod$lastAttackStrength = 0.0f;

    @Unique
    private int tuanzis_mod$lastAttackFullyChargedTicks = 0;

    @Override
    public float tuanzis_mod$getLastAttackStrength() {
        return this.tuanzis_mod$lastAttackStrength;
    }

    @Override
    public int tuanzis_mod$getLastAttackFullyChargedTicks() {
        return this.tuanzis_mod$lastAttackFullyChargedTicks;
    }

    @Override
    public int tuanzis_mod$getCurrentFullyChargedTicks() {
        return this.tuanzis_mod$ticksSinceFullyCharged;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickCooldown(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        // 检测完全充能状态
        if (player.getAttackStrengthScale(0.5f) >= 1.0f) {
            this.tuanzis_mod$ticksSinceFullyCharged++;
        } else {
            this.tuanzis_mod$ticksSinceFullyCharged = 0;
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void tuanzis_mod$beforeAttack(Entity entity, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        this.tuanzis_mod$lastAttackStrength = player.getAttackStrengthScale(0.5f);
        this.tuanzis_mod$lastAttackFullyChargedTicks = this.tuanzis_mod$ticksSinceFullyCharged;

        if (!player.level().isClientSide()) {
            me.tuanzi.util.ModLog.debug(player, entity, "【玩家攻击】攻击强度: " 
                + String.format("%.4f", this.tuanzis_mod$lastAttackStrength) 
                + ", 冷却已满刻数: " + this.tuanzis_mod$lastAttackFullyChargedTicks);
        }
    }
}
