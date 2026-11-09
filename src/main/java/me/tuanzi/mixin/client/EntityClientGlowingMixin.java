package me.tuanzi.mixin.client;

import me.tuanzi.client.EcholocationClientTracker;
import me.tuanzi.client.ScavengerTeamClient;
import me.tuanzi.client.SeekingTargetCache;
import me.tuanzi.init.ModStatusEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityClientGlowingMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            // 拾荒回廊队友高亮透视
            if (ScavengerTeamClient.shouldGlow(client.player, entity)) {
                cir.setReturnValue(true);
                return;
            }
            // 追踪箭锁定高亮
            if (SeekingTargetCache.getTargetedEntity(client) == entity) {
                cir.setReturnValue(true);
                return;
            }
            // 回声定位高亮（仅佩戴者可见）
            if (EcholocationClientTracker.isGlowing(entity)) {
                cir.setReturnValue(true);
                return;
            }
            // 归鸿“鸿痕”标记高亮
            if (entity instanceof LivingEntity living && living.hasEffect(ModStatusEffects.HONG_HEN)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        // 归鸿“鸿痕”标记发光为鲜红色 (Pure Red 0xFF0000)
        if (entity instanceof LivingEntity living && living.hasEffect(ModStatusEffects.HONG_HEN)) {
            cir.setReturnValue(0xFF0000);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            int teammateColor = ScavengerTeamClient.getTeammateOutlineColor(client.player, entity);
            if (teammateColor != -1) {
                cir.setReturnValue(teammateColor);
            }
        }
    }
}
