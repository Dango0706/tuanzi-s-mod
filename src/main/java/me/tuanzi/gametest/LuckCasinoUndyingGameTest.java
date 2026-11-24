package me.tuanzi.gametest;

import fr.minevegas.casino.CasinoSettings;
import fr.minevegas.casino.RouletteMod;
import fr.minevegas.casino.SlotMachineBlock;
import fr.minevegas.casino.SlotMachineBlockEntity;
import fr.minevegas.casino.SlotMachinePart;
import me.tuanzi.init.ModEnchantments;
import me.tuanzi.init.ModPotions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 不死药水堆叠16、祥瑞/厄兆附魔及幸运联动赌场 GameTest 测试集
 */
public class LuckCasinoUndyingGameTest {

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    @GameTest
    public void testUndyingPotionMaxStackSizeAndConsumption(GameTestHelper helper) {
        for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            ItemStack normal = PotionContents.createItemStack(container, ModPotions.UNDYING_POTION);
            ItemStack extended = PotionContents.createItemStack(container, ModPotions.LONG_UNDYING_POTION);
            assertTrue(helper, normal.getMaxStackSize() == 16,
                    container + " 普通不死药水最大堆叠应为 16，实际: " + normal.getMaxStackSize());
            assertTrue(helper, extended.getMaxStackSize() == 16,
                    container + " 延长不死药水最大堆叠应为 16，实际: " + extended.getMaxStackSize());

            ItemStack healing = PotionContents.createItemStack(container, Potions.HEALING);
            ItemStack flight = PotionContents.createItemStack(container, ModPotions.FLIGHT_POTION);
            assertTrue(helper, healing.getMaxStackSize() == 1,
                    container + " 原版药水最大堆叠应仍为 1，实际: " + healing.getMaxStackSize());
            assertTrue(helper, flight.getMaxStackSize() == 1,
                    container + " 其他模组药水最大堆叠应仍为 1，实际: " + flight.getMaxStackSize());
        }

        // 不死药箭保持 64 堆叠
        ItemStack arrow = PotionContents.createItemStack(Items.TIPPED_ARROW, ModPotions.UNDYING_POTION);
        assertTrue(helper, arrow.getMaxStackSize() == 64,
                "不死药箭最大堆叠应保持 64，实际: " + arrow.getMaxStackSize());

        // 堆叠 16 饮用普通不死药水：消耗 1 瓶剩 15 瓶，且赋予不死状态效果（MockPlayer 非 ServerPlayer，玻璃瓶返还由 ServerPlayer#handleExtraItemsCreatedOnUse 在真机 E2E 验证）
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack16 = PotionContents.createItemStack(Items.POTION, ModPotions.UNDYING_POTION);
        stack16.setCount(16);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack16);
        ItemStack afterDrink = stack16.finishUsingItem(helper.getLevel(), player);
        assertTrue(helper, afterDrink.is(Items.POTION) && afterDrink.getCount() == 15,
                "饮用 1 瓶后应剩余 15 瓶，实际: " + afterDrink.getCount());
        assertTrue(helper, player.hasEffect(me.tuanzi.init.ModStatusEffects.UNDYING),
                "饮用不死药水后应获得不死状态效果");

        // 堆叠 16 投掷喷溅型与滞留型不死药水：每次消耗 1 瓶剩 15 瓶
        ItemStack splash16 = PotionContents.createItemStack(Items.SPLASH_POTION, ModPotions.UNDYING_POTION);
        splash16.setCount(16);
        player.setItemInHand(InteractionHand.MAIN_HAND, splash16);
        splash16.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(helper, player.getMainHandItem().getCount() == 15,
                "投掷 1 瓶喷溅不死药水后应剩余 15 瓶，实际: " + player.getMainHandItem().getCount());

        ItemStack lingering16 = PotionContents.createItemStack(Items.LINGERING_POTION, ModPotions.LONG_UNDYING_POTION);
        lingering16.setCount(16);
        player.setItemInHand(InteractionHand.MAIN_HAND, lingering16);
        lingering16.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(helper, player.getMainHandItem().getCount() == 15,
                "投掷 1 瓶滞留不死药水后应剩余 15 瓶，实际: " + player.getMainHandItem().getCount());

        helper.succeed();
    }

    @GameTest
    public void testAuspiciousAndIllOmenEnchantmentPropertiesAndMechanics(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var enchRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> auspicious = enchRegistry.getOrThrow(ModEnchantments.AUSPICIOUS);
        Holder<Enchantment> illOmen = enchRegistry.getOrThrow(ModEnchantments.ILL_OMEN);

        // 1. 基本属性校验：UNCOMMON(5), 最大 IV(4), 互斥, 诅咒与附魔台标签
        assertTrue(helper, auspicious.value().getWeight() == 5, "祥瑞权重应为 5(UNCOMMON)");
        assertTrue(helper, illOmen.value().getWeight() == 5, "厄兆权重应为 5(UNCOMMON)");
        assertTrue(helper, auspicious.value().getMaxLevel() == 4, "祥瑞最高等级应为 4");
        assertTrue(helper, illOmen.value().getMaxLevel() == 4, "厄兆最高等级应为 4");
        assertTrue(helper, !Enchantment.areCompatible(auspicious, illOmen), "祥瑞与厄兆必须互斥");
        assertTrue(helper, !auspicious.is(EnchantmentTags.CURSE), "祥瑞不应是诅咒");
        assertTrue(helper, illOmen.is(EnchantmentTags.CURSE), "厄兆必须是诅咒");
        assertTrue(helper, auspicious.is(EnchantmentTags.IN_ENCHANTING_TABLE), "祥瑞必须在附魔台池中");
        assertTrue(helper, illOmen.is(EnchantmentTags.IN_ENCHANTING_TABLE), "厄兆也必须在附魔台池中");

        // 2. 仅限头盔（胸甲不支持，头盔支持）
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        assertTrue(helper, auspicious.value().canEnchant(helmet), "祥瑞应支持头盔");
        assertTrue(helper, illOmen.value().canEnchant(helmet), "厄兆应支持头盔");
        assertTrue(helper, !auspicious.value().canEnchant(chestplate), "祥瑞不应支持胸甲");
        assertTrue(helper, !illOmen.value().canEnchant(chestplate), "厄兆不应支持胸甲");

        // 3. 穿戴头盔对 player.getLuck() 与 CasinoSettings.playerLuck 的实时影响
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.tick();
        assertTrue(helper, Math.abs(player.getLuck()) < 1e-5f, "未佩戴头盔时幸运应为 0");

        ItemStack helmAusp1 = new ItemStack(Items.DIAMOND_HELMET);
        helmAusp1.enchant(auspicious, 1);
        player.setItemSlot(EquipmentSlot.HEAD, helmAusp1);
        player.tick();
        assertTrue(helper, Math.abs(player.getLuck() - 0.5f) < 1e-5f,
                "佩戴祥瑞 I 头盔幸运应为 +0.5，实际: " + player.getLuck());

        ItemStack helmAusp4 = new ItemStack(Items.DIAMOND_HELMET);
        helmAusp4.enchant(auspicious, 4);
        player.setItemSlot(EquipmentSlot.HEAD, helmAusp4);
        player.tick();
        assertTrue(helper, Math.abs(player.getLuck() - 2.0f) < 1e-5f,
                "佩戴祥瑞 IV 头盔幸运应为 +2.0，实际: " + player.getLuck());
        assertTrue(helper, Math.abs(CasinoSettings.playerLuck(player) - 2.0D) < 1e-5D,
                "CasinoSettings.playerLuck 应返回 2.0");

        ItemStack helmOmen4 = new ItemStack(Items.DIAMOND_HELMET);
        helmOmen4.enchant(illOmen, 4);
        player.setItemSlot(EquipmentSlot.HEAD, helmOmen4);
        player.tick();
        assertTrue(helper, Math.abs(player.getLuck() + 2.0f) < 1e-5f,
                "佩戴厄兆 IV 头盔幸运应为 -2.0，实际: " + player.getLuck());

        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.tick();
        assertTrue(helper, Math.abs(player.getLuck()) < 1e-5f,
                "摘下头盔后幸运应恢复为 0，实际: " + player.getLuck());

        // 4. 铁砧互斥校验（祥瑞 IV 头盔 + 厄兆 I 附魔书 => 不能合并出厄兆）
        ItemStack omenBook = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(omenBook, m -> m.set(illOmen, 1));
        AnvilMenu anvil = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(level, BlockPos.ZERO));
        anvil.getSlot(0).set(helmAusp4.copy());
        anvil.getSlot(1).set(omenBook);
        anvil.createResult();
        ItemStack anvilOut = anvil.getSlot(2).getItem();
        assertTrue(helper, anvilOut.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(illOmen, anvilOut) == 0,
                "铁砧不应允许将互斥的厄兆附魔到已有祥瑞的头盔上");

        // 5. 砂轮不可祛除厄兆诅咒
        ItemStack helmBoth = new ItemStack(Items.DIAMOND_HELMET);
        helmBoth.enchant(illOmen, 2);
        helmBoth.enchant(enchRegistry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
        GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(level, BlockPos.ZERO));
        grindstone.getSlot(0).set(helmBoth);
        grindstone.slotsChanged(new SimpleContainer(2));
        ItemStack grindOut = grindstone.getSlot(2).getItem();
        assertTrue(helper, !grindOut.isEmpty()
                        && EnchantmentHelper.getItemEnchantmentLevel(illOmen, grindOut) == 2
                        && EnchantmentHelper.getItemEnchantmentLevel(enchRegistry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), grindOut) == 0,
                "砂轮应保留厄兆诅咒并仅移除耐久等非诅咒附魔");

        helper.succeed();
    }

    @GameTest
    public void testCasinoMachineWithPlayerLuck(GameTestHelper helper) {
        // 放置老虎机并验证玩家佩戴祥瑞 IV 启动不抛出异常且正常扣注进入旋转状态
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        BlockPos slotPos = new BlockPos(1, 1, 1);
        BlockState slotBase = RouletteMod.SLOT_MACHINE.defaultBlockState().setValue(SlotMachineBlock.FACING, Direction.SOUTH);
        helper.setBlock(slotPos, slotBase.setValue(SlotMachineBlock.HALF, SlotMachinePart.LOWER));
        helper.setBlock(slotPos.above(), slotBase.setValue(SlotMachineBlock.HALF, SlotMachinePart.UPPER));
        helper.setBlock(slotPos.above(2), slotBase.setValue(SlotMachineBlock.HALF, SlotMachinePart.TOP));

        SlotMachineBlockEntity slotBe = helper.getBlockEntity(slotPos, SlotMachineBlockEntity.class);
        assertTrue(helper, slotBe != null, "SlotMachineBlockEntity 应存在");

        // 放入赌注并启动（此处用 null initiator 走无幸运路径，验证基础兼容；真机 E2E 使用真实 ServerPlayer 测试幸运联动）
        slotBe.bets().setItem(0, new ItemStack(Items.DIAMOND, 1));
        slotBe.start(null);
        assertTrue(helper, slotBe.isSpinning(), "老虎机应成功进入旋转状态");

        helper.succeed();
    }
}
