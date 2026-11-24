package me.tuanzi.gametest;

import me.tuanzi.init.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 裂虚之痕（Rift Scar）横扫攻击比率增强 GameTest 测试集
 */
public class RiftScarGameTest {

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    @GameTest
    public void testRiftScarSweepingAttributeAndEnchantability(GameTestHelper helper) {
        ItemStack riftScar = new ItemStack(ModItems.RIFT_SCAR);
        assertTrue(helper, riftScar.is(ItemTags.SWORDS), "裂虚之痕必须属于 ItemTags.SWORDS");
        assertTrue(helper, riftScar.is(ItemTags.SWEEPING_ENCHANTABLE), "裂虚之痕必须支持横扫之刃附魔");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.setPos(pos.x, pos.y, pos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, riftScar);
        for (int i = 0; i < 5; i++) {
            player.tick();
        }

        double ratio = player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);
        assertTrue(helper, Math.abs(ratio - 0.25) < 1e-5,
                "主手持握无附魔裂虚之痕时，横扫攻击比率应为 0.25，实际: " + ratio);

        var enchRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sweepingEdge = enchRegistry.getOrThrow(Enchantments.SWEEPING_EDGE);
        ItemStack enchanted = riftScar.copy();
        enchanted.enchant(sweepingEdge, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted);
        for (int i = 0; i < 5; i++) {
            player.tick();
        }

        double enchantedRatio = player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);
        assertTrue(helper, Math.abs(enchantedRatio - 1.0) < 1e-5,
                "裂虚之痕叠加横扫之刃 III 后，横扫攻击比率应达到上限 1.0，实际: " + enchantedRatio);

        helper.succeed();
    }

    @GameTest
    public void testRiftScarSweepAttackDamageInWorld(GameTestHelper helper) {
        // 铺设地面方块，保证玩家与猪处于同一平地
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }

        Pig primary = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(2.5, 1.0, 3.0));
        Pig secondary = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.1, 1.0, 3.0));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 playerPos = helper.absoluteVec(new Vec3(2.5, 1.0, 1.5));
        player.setPos(playerPos.x, playerPos.y, playerPos.z);
        player.setOnGround(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RIFT_SCAR));

        // 推进 25 ticks 同步装备属性并充满攻击冷却（2.0 攻速对应 10 ticks）
        for (int i = 0; i < 25; i++) {
            player.tick();
        }

        float initialSecondaryHealth = secondary.getHealth();
        player.attack(primary);

        // 原版横扫伤害 = 1.0 + 0.25 * 5.12 = 2.28；触发裂虚之痕 0 护甲虚无切割 (+75%) => 2.28 * 1.75 = 3.99
        float expectedSweepDamage = (1.0F + 0.25F * 5.12F) * 1.75F;
        float actualSweepDamage = initialSecondaryHealth - secondary.getHealth();
        assertTrue(helper, Math.abs(actualSweepDamage - expectedSweepDamage) < 0.02F,
                "次要 0 护甲目标受到的横扫伤害应约为 " + expectedSweepDamage + "，实际: " + actualSweepDamage);

        helper.succeed();
    }
}
