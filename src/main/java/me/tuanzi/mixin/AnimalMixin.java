package me.tuanzi.mixin;

import me.tuanzi.util.HusbandryHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Animal.class)
public class AnimalMixin {

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void tuanzis_mod$applyHusbandryCooldown(ServerLevel level, Animal partner, @Nullable AgeableMob offspring, CallbackInfo ci) {
        Animal self = (Animal) (Object) this;
        if (!HusbandryHelper.isApplicableAnimal(self)) return;

        ServerPlayer cause = Optional.ofNullable(self.getLoveCause()).or(() -> Optional.ofNullable(partner.getLoveCause())).orElse(null);
        int husbandryLevel = HusbandryHelper.getHusbandryLevel(cause);
        if (husbandryLevel > 0) {
            float reduction = 0.20f * husbandryLevel; // 20%, 40%, 60%
            int cooldown = Math.round(6000 * (1.0f - reduction));
            self.setAge(cooldown);
            partner.setAge(cooldown);
            ModLog.debug(cause, "【牧养】动物繁殖成功，繁殖冷却缩短为 %d ticks (-%.0f%%, 等级: %d)",
                cooldown, reduction * 100f, husbandryLevel);
        }
    }
}
