package me.tuanzi.client;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class SeekingTargetCache {
    private static long lastUpdatedTime = -1;
    private static Entity targetedEntity = null;

    public static Entity getTargetedEntity(Minecraft client) {
        if (client.player == null || client.level == null) {
            targetedEntity = null;
            return null;
        }

        long now = System.currentTimeMillis();
        if (now == lastUpdatedTime) {
            return targetedEntity;
        }
        lastUpdatedTime = now;

        ItemStack mainHand = client.player.getMainHandItem();
        ItemStack offHand = client.player.getOffhandItem();
        boolean hasSeeking = isSeekingCrossbow(client, mainHand) || isSeekingCrossbow(client, offHand);

        if (!hasSeeking) {
            targetedEntity = null;
            return null;
        }

        // 在客户端执行长距离射线检测 (64格)
        double maxDistance = 64.0;
        Vec3 eyePos = client.player.getEyePosition(1.0f);
        Vec3 viewVec = client.player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(viewVec.scale(maxDistance));
        
        AABB searchArea = client.player.getBoundingBox().expandTowards(viewVec.scale(maxDistance)).inflate(1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
            client.player,
            eyePos,
            endPos,
            searchArea,
            entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
            maxDistance * maxDistance
        );

        if (entityHitResult != null) {
            targetedEntity = entityHitResult.getEntity();
        } else {
            targetedEntity = null;
        }

        return targetedEntity;
    }

    private static boolean isSeekingCrossbow(Minecraft client, ItemStack stack) {
        if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack) && client.level != null) {
            var lookup = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var seekingArrow = lookup.getOrThrow(ModEnchantments.SEEKING_ARROW);
            return EnchantmentHelper.getItemEnchantmentLevel(seekingArrow, stack) > 0;
        }
        return false;
    }
}
