package me.tuanzi.client;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class EcholocationClientTracker {
    private static final Map<Integer, Long> GLOWING_ENTITIES = new ConcurrentHashMap<>();
    private static int pulseTimer = 0;

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            pulseTimer = 0;
            GLOWING_ENTITIES.clear();
            return;
        }

        // 定期清理已过期的实体
        long currentTime = client.level.getGameTime();
        GLOWING_ENTITIES.values().removeIf(expireTime -> currentTime > expireTime);

        ItemStack helmet = client.player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            pulseTimer = 0;
            return;
        }

        var enchRegistry = client.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var echolocationHolder = enchRegistry.getOrThrow(ModEnchantments.ECHOLOCATION);
        int level = EnchantmentHelper.getItemEnchantmentLevel(echolocationHolder, helmet);
        if (level <= 0) {
            pulseTimer = 0;
            return;
        }

        // 脉冲周期：12/9/6 秒 -> (15 - 3 * level) * 20 ticks
        int intervalTicks = (15 - 3 * level) * 20;
        pulseTimer++;
        if (pulseTimer >= intervalTicks) {
            pulseTimer = 0;

            // 范围：半径 10/14/18 格，潜行时降低 50% (5/7/9 格)
            float radius = (6 + 4 * level);
            if (client.player.isCrouching()) {
                radius *= 0.5f;
            }

            final float finalRadius = radius;
            List<LivingEntity> targets = client.level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(finalRadius),
                e -> e != client.player && e.isAlive() && e.distanceTo(client.player) <= finalRadius
            );

            long expireTime = currentTime + 100L; // 发光 5 秒 (100 ticks)
            for (LivingEntity target : targets) {
                GLOWING_ENTITIES.put(target.getId(), expireTime);
            }

            // 播放专属回声声效并输出调试日志
            client.level.playSound(
                client.player,
                client.player.getX(), client.player.getY(), client.player.getZ(),
                SoundEvents.SCULK_CLICKING,
                SoundSource.PLAYERS,
                0.8F,
                1.4F
            );

            // 播放回声脉冲环状粒子
            double px = client.player.getX();
            double py = client.player.getY() + 0.5;
            double pz = client.player.getZ();
            for (int i = 0; i < 16; i++) {
                double angle = i * (Math.PI * 2.0 / 16.0);
                double vx = Math.cos(angle) * 0.2;
                double vz = Math.sin(angle) * 0.2;
                client.level.addParticle(ParticleTypes.SCULK_CHARGE_POP, px, py, pz, vx, 0.05, vz);
            }

            ModLog.debug(client.player, "【回声定位】脉冲触发（等级: %d，范围: %.1f 格）！已探测并标记 %d 个生物发光 5 秒",
                level, finalRadius, targets.size());
        }
    }

    public static boolean isGlowing(Entity entity) {
        if (entity == null || entity.level() == null) return false;
        Long expireTime = GLOWING_ENTITIES.get(entity.getId());
        if (expireTime == null) return false;
        if (entity.level().getGameTime() <= expireTime) {
            return true;
        } else {
            GLOWING_ENTITIES.remove(entity.getId());
            return false;
        }
    }
}
