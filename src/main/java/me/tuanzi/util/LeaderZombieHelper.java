package me.tuanzi.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * 领队僵尸判定与经验掉落计算工具。
 */
public final class LeaderZombieHelper {

    public static final Identifier LEADER_ZOMBIE_BONUS_ID = Identifier.withDefaultNamespace("leader_zombie_bonus");

    private LeaderZombieHelper() {
    }

    public static boolean isLeaderZombie(Zombie zombie) {
        AttributeInstance reinforcements = zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null && reinforcements.hasModifier(LEADER_ZOMBIE_BONUS_ID)) {
            return true;
        }
        AttributeInstance maxHealth = zombie.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth != null && maxHealth.hasModifier(LEADER_ZOMBIE_BONUS_ID);
    }

    public static int calculateExperience(double maxHealth, boolean isBaby, double randomRoll01) {
        double clampedRoll = Math.max(0.0D, Math.min(1.0D, randomRoll01));
        double divisor = 2.5D + clampedRoll * 2.0D;
        double xp = Math.max(0.0D, maxHealth) / divisor;
        if (isBaby) {
            xp *= 2.0D;
        }
        return Math.max(1, (int) Math.round(xp));
    }
}
