package me.tuanzi.effect;

import me.tuanzi.network.DesperateStandSyncPacket;
import me.tuanzi.util.DesperateStandTracker;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 状态效果「背水一战」：
 * - 持续期间近战伤害 +10%；
 * - 受到的伤害延迟记入创伤值，击杀敌对生物按目标最大生命值 10% 消减创伤；
 * - 效果结束后的 1 tick 结算剩余创伤值真实伤害（无视护甲、抗性、伤害吸收，可致死）。
 */
public class DesperateStandStatusEffect extends MobEffect {
    public DesperateStandStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xDCE6F2); // 银白色
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof DesperateStandTracker tracker) {
            tracker.tuanzis_mod$setDesperateStandSettleTicks(0);
            float trauma = tracker.tuanzis_mod$getDesperateStandTrauma();
            if (entity instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new DesperateStandSyncPacket(trauma));
            }
            ModLog.debug(entity, null, "【背水一战】状态激活！当前累计创伤值: " + String.format("%.1f", trauma));
        }
    }
}
