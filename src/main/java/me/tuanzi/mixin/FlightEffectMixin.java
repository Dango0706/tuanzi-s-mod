package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FlightEffectMixin {
    @Unique
    private boolean tuanzis_mod$hadFlightEffect = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tuanzis_mod$tickFlightAbility(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            MobEffectInstance effect = player.getEffect(ModStatusEffects.FLIGHT);
            boolean hasEffect = effect != null;
            
            if (hasEffect) {
                int duration = effect.getDuration();
                // 15秒提醒 (300 tick)
                if (duration == 300) {
                    tuanzis_mod$sendFlightAlert(player, 15);
                }
                // 10秒提醒 (200 tick)
                else if (duration == 200) {
                    tuanzis_mod$sendFlightAlert(player, 10);
                }
                // 5秒内倒计时 (每秒提醒)
                else if (duration <= 100 && duration > 0 && duration % 20 == 0) {
                    tuanzis_mod$sendFlightAlert(player, duration / 20);
                }
            }

            // 状态切换逻辑
            if (hasEffect && !tuanzis_mod$hadFlightEffect) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                tuanzis_mod$hadFlightEffect = true;
            } else if (!hasEffect && tuanzis_mod$hadFlightEffect) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                tuanzis_mod$hadFlightEffect = false;
            }
        }
    }

    @Unique
    private void tuanzis_mod$sendFlightAlert(Player player, int seconds) {
        // 在 1.21.2+ 中，Actionbar 消息使用 sendOverlayMessage
        player.sendOverlayMessage(Component.translatable("message.tuanzis_mod.flight_expiring", seconds));
        
        // 播放音效
        float pitch = seconds <= 5 ? 1.5f : 1.0f;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, pitch);
    }
}
