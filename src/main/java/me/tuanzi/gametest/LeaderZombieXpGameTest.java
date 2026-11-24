package me.tuanzi.gametest;

import me.tuanzi.util.LeaderZombieHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * 领队僵尸击杀经验掉落 GameTest 测试集
 */
public class LeaderZombieXpGameTest {

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    private static void makeLeader(Zombie zombie, double healthMultiplierBonus) {
        zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
                .addOrReplacePermanentModifier(new AttributeModifier(
                        LeaderZombieHelper.LEADER_ZOMBIE_BONUS_ID, 0.6D, AttributeModifier.Operation.ADD_VALUE));
        zombie.getAttribute(Attributes.MAX_HEALTH)
                .addOrReplacePermanentModifier(new AttributeModifier(
                        LeaderZombieHelper.LEADER_ZOMBIE_BONUS_ID, healthMultiplierBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        zombie.setHealth(zombie.getMaxHealth());
    }

    @GameTest
    public void testLeaderZombieAndBabyLeaderZombieXpReward(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // 1. 普通成年僵尸保持原版 5 点基础经验
        Zombie normal = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(1.5, 1.0, 1.5));
        assertTrue(helper, !LeaderZombieHelper.isLeaderZombie(normal), "普通僵尸不应判定为领队");
        int normalXp = normal.getExperienceReward(level, player);
        assertTrue(helper, normalXp == 5, "普通成年僵尸应掉落 5 点经验，实际: " + normalXp);

        // 2. 成年领队僵尸（+2.0 倍率 => 最大生命 60）：掉落经验应在 [13, 24] 区间
        Zombie adultLeader = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(2.5, 1.0, 1.5));
        makeLeader(adultLeader, 2.0D);
        assertTrue(helper, LeaderZombieHelper.isLeaderZombie(adultLeader), "带 leader_zombie_bonus 的僵尸应判定为领队");
        assertTrue(helper, Math.abs(adultLeader.getMaxHealth() - 60.0f) < 1e-3f,
                "成年领队最大生命应为 60，实际: " + adultLeader.getMaxHealth());
        for (int i = 0; i < 20; i++) {
            int xp = adultLeader.getExperienceReward(level, player);
            assertTrue(helper, xp >= 13 && xp <= 24,
                    "60血成年领队僵尸经验应在 [13, 24] 范围内，实际: " + xp);
        }

        // 3. 幼年领队僵尸（最大生命 60，幼年翻倍）：掉落经验应在 [27, 48] 区间
        Zombie babyLeader = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(3.5, 1.0, 1.5));
        babyLeader.setBaby(true);
        makeLeader(babyLeader, 2.0D);
        assertTrue(helper, babyLeader.isBaby() && LeaderZombieHelper.isLeaderZombie(babyLeader), "应为幼年领队僵尸");
        for (int i = 0; i < 20; i++) {
            int xp = babyLeader.getExperienceReward(level, player);
            assertTrue(helper, xp >= 27 && xp <= 48,
                    "60血幼年领队僵尸经验应在 [27, 48] 范围内，实际: " + xp);
        }

        // 4. 僵尸变种（尸壳领队，最大生命 80）：掉落经验应在 [18, 32] 区间
        Husk huskLeader = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(4.5, 1.0, 1.5));
        makeLeader(huskLeader, 3.0D);
        assertTrue(helper, LeaderZombieHelper.isLeaderZombie(huskLeader), "尸壳领队应正确识别");
        for (int i = 0; i < 20; i++) {
            int xp = huskLeader.getExperienceReward(level, player);
            assertTrue(helper, xp >= 18 && xp <= 32,
                    "80血尸壳领队经验应在 [18, 32] 范围内，实际: " + xp);
        }

        helper.succeed();
    }
}
