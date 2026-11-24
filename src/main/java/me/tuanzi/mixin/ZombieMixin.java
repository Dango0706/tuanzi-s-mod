package me.tuanzi.mixin;

import me.tuanzi.util.LeaderZombieHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin {

    @Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$leaderZombieExperienceReward(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        Zombie self = (Zombie) (Object) this;
        if (!LeaderZombieHelper.isLeaderZombie(self)) {
            return;
        }

        double maxHealth = Math.max(0.0D, self.getMaxHealth());
        double roll = self.getRandom().nextDouble();
        int reward = LeaderZombieHelper.calculateExperience(maxHealth, self.isBaby(), roll);

        ModLog.debug(self.getLastHurtByPlayer(), self,
                "【领队僵尸经验】击杀领队僵尸(%s, 幼年=%s, 最大生命=%.2f, 除数=%.2f)，掉落基础经验=%d",
                self.getType().getDescription().getString(), self.isBaby(), maxHealth, 2.5D + roll * 2.0D, reward);
        cir.setReturnValue(reward);
    }
}
