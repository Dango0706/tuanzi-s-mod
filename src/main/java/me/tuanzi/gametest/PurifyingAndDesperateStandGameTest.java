package me.tuanzi.gametest;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.DesperateStandHelper;
import me.tuanzi.util.DesperateStandTracker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 「净化脉冲 / 净化药水」与「背水一战 / 背水药水」Minecraft GameTest 机制测试集
 */
public class PurifyingAndDesperateStandGameTest {

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    @GameTest
    public void testPurifyingPulseInstantPeriodicAndDurationReduction(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // 1. 预先施加：中毒(负面)、虚弱(负面)、力量(正面)、肾上腺素透支(牛奶不可清除负面)
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0));
        player.addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE_OVERDRAW, 300, 0));

        // 2. 施加净化脉冲 (45s = 900 ticks) -> 瞬间清除牛奶可清除的负面，保留正面与透支
        player.addEffect(new MobEffectInstance(ModStatusEffects.PURIFYING_PULSE, 900, 0));
        assertTrue(helper, !player.hasEffect(MobEffects.POISON), "饮用瞬间应清除中毒效果");
        assertTrue(helper, !player.hasEffect(MobEffects.WEAKNESS), "饮用瞬间应清除虚弱效果");
        assertTrue(helper, player.hasEffect(MobEffects.STRENGTH), "净化脉冲必须保留正面状态(力量)");
        assertTrue(helper, player.hasEffect(ModStatusEffects.ADRENALINE_OVERDRAW), "净化脉冲不能清除肾上腺素透支");

        // 3. 持续期间新获得负面状态持续时间 -50%，而肾上腺素透支不受影响
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0));
        MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
        assertTrue(helper, slow != null && slow.getDuration() == 100,
                "持续期间新获得缓慢 200t 应减半为 100t，实际: " + (slow == null ? -1 : slow.getDuration()));

        player.addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE_OVERDRAW, 350, 0));
        MobEffectInstance overdraw = player.getEffect(ModStatusEffects.ADRENALINE_OVERDRAW);
        assertTrue(helper, overdraw != null && overdraw.getDuration() == 350,
                "持续期间刷新肾上腺素透支 350t 不应被减半，实际: " + (overdraw == null ? -1 : overdraw.getDuration()));

        // 再加第 2 个负面状态用于验证每 5 秒清除 1 个（总共 2 个负面：缓慢 + 饥饿）
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 0)); // 减半为 150t
        assertTrue(helper, player.hasEffect(MobEffects.SLOWNESS) && player.hasEffect(MobEffects.HUNGER),
                "应同时存在缓慢与饥饿两个负面状态");

        // 首次 tick (duration=900 % 100 == 0) 会触发 1 次，先走过 1 tick 看看还剩几个，再走 100 ticks 清除剩余
        player.tick();
        int remainingAfterFirstTick = (player.hasEffect(MobEffects.SLOWNESS) ? 1 : 0)
                + (player.hasEffect(MobEffects.HUNGER) ? 1 : 0);
        assertTrue(helper, remainingAfterFirstTick == 1,
                "一次周期净化应只清除 1 个负面状态，实际剩余: " + remainingAfterFirstTick);

        for (int i = 0; i < 100; i++) {
            player.tick();
        }
        assertTrue(helper, !player.hasEffect(MobEffects.SLOWNESS) && !player.hasEffect(MobEffects.HUNGER),
                "经过 5 秒（100 ticks）后第 2 个负面状态也应被自动清除");
        assertTrue(helper, player.hasEffect(MobEffects.STRENGTH), "周期净化不得误删正面状态");
        assertTrue(helper, player.hasEffect(ModStatusEffects.ADRENALINE_OVERDRAW), "周期净化不得误删肾上腺素透支");

        helper.succeed();
    }

    @GameTest
    public void testDesperateStandMeleeBonusDeferralKillReductionAndSettlement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 playerPos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.setPos(playerPos.x, playerPos.y, playerPos.z);
        player.setHealth(20.0f);
        // 穿戴钻石胸甲并 tick 刷新装备属性，测试护甲减伤后再记账，结算时无视该胸甲
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.tick();

        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(2.5, 1.0, 2.5));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(2.5, 1.0, 1.5));

        // 1. 施加背水一战 (100 ticks = 5s)
        player.addEffect(new MobEffectInstance(ModStatusEffects.DESPERATE_STAND, 100, 0));

        // 验证近战增伤 +10%（投射物不增伤）
        float meleeDmg = DamageCalculator.calculateDamage(10.0f, level.damageSources().playerAttack(player), pig);
        assertTrue(helper, Math.abs(meleeDmg - 11.0f) < 1e-4f, "背水一战近战伤害应 +10% (10 -> 11)，实际: " + meleeDmg);
        float rangedDmg = DamageCalculator.calculateDamage(10.0f, level.damageSources().magic(), pig);
        assertTrue(helper, Math.abs(rangedDmg - 10.0f) < 1e-4f, "背水一战非近战伤害不应加成，实际: " + rangedDmg);

        // 2. 玩家受击：不立刻扣血，而是将经护甲减免后的实际伤害记入创伤值
        boolean hurt = player.hurtServer(level, level.damageSources().mobAttack(zombie), 10.0f);
        assertTrue(helper, hurt, "背水一战期间受击判定应正常执行");
        assertTrue(helper, Math.abs(player.getHealth() - 20.0f) < 1e-4f,
                "背水一战期间受到伤害不应立即扣血，实际血量: " + player.getHealth());
        DesperateStandTracker tracker = (DesperateStandTracker) player;
        float traumaAfterHit = tracker.tuanzis_mod$getDesperateStandTrauma();
        assertTrue(helper, traumaAfterHit > 0.0f && traumaAfterHit < 10.0f,
                "创伤值应记录穿戴护甲后的实际应扣血量 (0 < trauma < 10)，实际: " + traumaAfterHit);

        // 3. 击杀敌对生物（僵尸最大生命值 20 -> 消减 2.0 创伤值）
        zombie.setHealth(1.0f);
        zombie.hurtServer(level, level.damageSources().playerAttack(player), 10.0f);
        float expectedAfterKill = Math.max(0.0f, traumaAfterHit - 2.0f);
        assertTrue(helper, Math.abs(tracker.tuanzis_mod$getDesperateStandTrauma() - expectedAfterKill) < 1e-4f,
                "击杀最大血量 20 的僵尸应减少 2.0 创伤，期望: " + expectedAfterKill + "，实际: " + tracker.tuanzis_mod$getDesperateStandTrauma());

        // 4. 补记 4.0 直接伤害并加上抗性提升 IV 与伤害吸收，验证结算时完全无视抗性与吸收！
        DesperateStandHelper.tryDeferDirectDamage(player, 4.0f);
        float expectedFinalTrauma = expectedAfterKill + 4.0f;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 3));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1)); // 提供 8.0 点伤害吸收 (MAX_ABSORPTION=8)
        float absorptionBefore = player.getAbsorptionAmount();
        assertTrue(helper, absorptionBefore > 0.0f, "前置伤害吸收应大于 0，实际: " + absorptionBefore);

        // 推进 100 ticks，效果恰好到期移除并预约 1 tick 后结算
        for (int i = 0; i < 100; i++) {
            player.tick();
        }
        assertTrue(helper, !player.hasEffect(ModStatusEffects.DESPERATE_STAND), "100 ticks 后背水一战状态应已结束");
        assertTrue(helper, Math.abs(player.getHealth() - 20.0f) < 1e-4f,
                "效果结束当 tick 尚未结算（应延迟到结束后的 1 tick），实际血量: " + player.getHealth());

        // 再推进 1 tick（效果结束后的第 1 tick）-> 统一结算真实伤害！
        player.tick();
        float expectedHealth = 20.0f - expectedFinalTrauma;
        assertTrue(helper, Math.abs(player.getHealth() - expectedHealth) < 1e-3f,
                "效果结束后 1 tick 应无视护甲/抗性/吸收扣除精确创伤值，期望血量: " + expectedHealth + "，实际: " + player.getHealth());
        assertTrue(helper, Math.abs(player.getAbsorptionAmount() - absorptionBefore) < 1e-4f,
                "真实伤害结算不应消耗或被伤害吸收抵挡，期望吸收值: " + absorptionBefore + "，剩余吸收值: " + player.getAbsorptionAmount());
        assertTrue(helper, tracker.tuanzis_mod$getDesperateStandTrauma() == 0.0f, "结算后创伤值应归零");

        helper.succeed();
    }

    @GameTest
    public void testDesperateStandEarlyMilkRemovalAndTotemDeathProtection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 1. 喝牛奶（removeAllEffects）提前清除背水一战时仍会在 1 tick 后结算创伤，且触发致死时可被不死图腾抵挡
        Player totemPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        totemPlayer.setHealth(20.0f);
        totemPlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        totemPlayer.addEffect(new MobEffectInstance(ModStatusEffects.DESPERATE_STAND, 600, 0));
        DesperateStandHelper.tryDeferDirectDamage(totemPlayer, 25.0f);

        totemPlayer.removeAllEffects();
        assertTrue(helper, !totemPlayer.hasEffect(ModStatusEffects.DESPERATE_STAND), "牛奶应移除背水一战状态");
        totemPlayer.tick(); // 1 tick 后结算 25 点真实致死伤害
        assertTrue(helper, totemPlayer.isAlive(), "致死创伤结算时副手不死图腾应成功触发免死");
        assertTrue(helper, totemPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty(), "不死图腾应被消耗");
        assertTrue(helper, totemPlayer.hasEffect(MobEffects.REGENERATION) && totemPlayer.getHealth() >= 1.0f,
                "图腾免死后应获得生命恢复且血量 >= 1.0，实际: " + totemPlayer.getHealth());

        // 2. 无图腾与无不死效果时，致死创伤应直接致死
        Player doomedPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        doomedPlayer.setHealth(20.0f);
        doomedPlayer.addEffect(new MobEffectInstance(ModStatusEffects.DESPERATE_STAND, 20, 0));
        DesperateStandHelper.tryDeferDirectDamage(doomedPlayer, 25.0f);
        for (int i = 0; i < 21; i++) {
            doomedPlayer.tick();
        }
        assertTrue(helper, doomedPlayer.isDeadOrDying(), "无免死手段时剩余创伤 >= 当前血量必须致死");

        helper.succeed();
    }
}
