package me.tuanzi.gametest;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/**
 * 【永恒】附魔机制 GameTest 测试集
 */
public class EternalEnchantmentGameTest {

    private static ItemStack createEnchantedBook(Holder<Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(enchantment, level));
        return book;
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    @GameTest
    public void testAnvilApplyEternalAndExperienceCost(GameTestHelper helper) {
        var enchRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> eternal = enchRegistry.getOrThrow(ModEnchantments.ETERNAL);
        Holder<Enchantment> efficiency = enchRegistry.getOrThrow(Enchantments.EFFICIENCY);
        Holder<Enchantment> unbreaking = enchRegistry.getOrThrow(Enchantments.UNBREAKING);
        Holder<Enchantment> fortune = enchRegistry.getOrThrow(Enchantments.FORTUNE);

        Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        survivalPlayer.giveExperienceLevels(80);

        AnvilMenu anvil = new AnvilMenu(0, survivalPlayer.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        ItemStack eternalBook = createEnchantedBook(eternal, 1);

        // 1. 0 个原有附魔（带损伤的钻石镐）+ 永恒书 -> 消耗 8 级，获得 Unbreakable=true 且损伤归零
        ItemStack plainPick = new ItemStack(Items.DIAMOND_PICKAXE);
        plainPick.setDamageValue(120);
        anvil.getSlot(0).set(plainPick);
        anvil.getSlot(1).set(eternalBook.copy());
        assertTrue(helper, anvil.getCost() == 8, "0 个附魔物品附魔永恒时，经验消耗应为 8 级，实际: " + anvil.getCost());
        ItemStack plainResult = anvil.getSlot(2).getItem();
        assertTrue(helper, !plainResult.isEmpty(), "无附魔钻石镐应可附魔永恒");
        assertTrue(helper, plainResult.has(DataComponents.UNBREAKABLE), "附魔永恒后必须获得 Unbreakable 组件");
        assertTrue(helper, !plainResult.isDamageableItem(), "拥有 Unbreakable 后 isDamageableItem 应为 false");
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(eternal, plainResult) == 1, "输出物品应带有永恒 I");
        assertTrue(helper, anvil.getSlot(2).mayPickup(survivalPlayer), "8 级消耗时生存玩家应可取出");

        // 2. 3 个原有附魔（效率 III、耐久 III、时运 III）+ 永恒书 -> 消耗 24 级
        ItemStack threeEnchPick = new ItemStack(Items.DIAMOND_PICKAXE);
        threeEnchPick.enchant(efficiency, 3);
        threeEnchPick.enchant(unbreaking, 3);
        threeEnchPick.enchant(fortune, 3);
        anvil.getSlot(0).set(threeEnchPick);
        anvil.getSlot(1).set(eternalBook.copy());
        assertTrue(helper, anvil.getCost() == 24, "3 个附魔物品附魔永恒时，经验消耗应为 24 级，实际: " + anvil.getCost());
        ItemStack threeResult = anvil.getSlot(2).getItem();
        assertTrue(helper, threeResult.has(DataComponents.UNBREAKABLE), "3 附魔镐附魔永恒后应具有 Unbreakable");
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(efficiency, threeResult) == 3, "应保留原有效率 III");

        // 3. 8 个原有附魔 -> 消耗 64 级且生存玩家仍可取出（突破原版 40 级限制）
        ItemStack eightEnchArmor = new ItemStack(Items.DIAMOND_HELMET);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(Enchantments.PROTECTION), 4);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(Enchantments.RESPIRATION), 3);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(Enchantments.THORNS), 3);
        eightEnchArmor.enchant(unbreaking, 3);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(Enchantments.MENDING), 1);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(ModEnchantments.PHARMACOLOGY), 1);
        eightEnchArmor.enchant(enchRegistry.getOrThrow(ModEnchantments.PURITY), 1);
        anvil.getSlot(0).set(eightEnchArmor);
        anvil.getSlot(1).set(eternalBook.copy());
        assertTrue(helper, anvil.getCost() == 64, "8 个附魔物品附魔永恒时，经验消耗应为 64 级，实际: " + anvil.getCost());
        assertTrue(helper, !anvil.getSlot(2).getItem().isEmpty(), "8 个附魔物品附魔永恒不应被 40 级上限阻塞");
        assertTrue(helper, anvil.getSlot(2).mayPickup(survivalPlayer), "80 级生存玩家应可取出 64 级消耗的永恒输出");

        // 4. 非耐久物品（木棍、另一本附魔书）不能附魔永恒
        anvil.getSlot(0).set(new ItemStack(Items.STICK));
        anvil.getSlot(1).set(eternalBook.copy());
        assertTrue(helper, anvil.getSlot(2).getItem().isEmpty(), "木棍不可附魔永恒");

        anvil.getSlot(0).set(createEnchantedBook(efficiency, 3));
        anvil.getSlot(1).set(eternalBook.copy());
        assertTrue(helper, anvil.getSlot(2).getItem().isEmpty(), "附魔书不可在铁砧中附魔永恒");

        helper.succeed();
    }

    @GameTest
    public void testBlockFutureEnchantmentsAndUpgrades(GameTestHelper helper) {
        var enchRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> eternal = enchRegistry.getOrThrow(ModEnchantments.ETERNAL);
        Holder<Enchantment> efficiency = enchRegistry.getOrThrow(Enchantments.EFFICIENCY);
        Holder<Enchantment> unbreaking = enchRegistry.getOrThrow(Enchantments.UNBREAKING);
        Holder<Enchantment> tonghui = enchRegistry.getOrThrow(ModEnchantments.TONGHUI);

        Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        survivalPlayer.giveExperienceLevels(50);

        // 先通过 ItemStack#enchant 直接给钻石镐附魔效率 III 与永恒（同时验证 ItemStack#enchant 会触发 Unbreakable）
        ItemStack eternalPick = new ItemStack(Items.DIAMOND_PICKAXE);
        eternalPick.enchant(efficiency, 3);
        eternalPick.enchant(eternal, 1);
        assertTrue(helper, eternalPick.has(DataComponents.UNBREAKABLE), "通过 ItemStack#enchant 附魔永恒也必须赋予 Unbreakable");

        // 1. 铁砧中尝试升级已有附魔（效率 III + 效率 III -> 效率 IV）：必须被阻止
        AnvilMenu anvil = new AnvilMenu(0, survivalPlayer.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        anvil.getSlot(0).set(eternalPick.copy());
        anvil.getSlot(1).set(createEnchantedBook(efficiency, 3));
        assertTrue(helper, anvil.getSlot(2).getItem().isEmpty(), "已带永恒的物品禁止升级效率 III -> IV");

        // 2. 铁砧中尝试添加新附魔（耐久 III / 永恒 / 通惠）：必须全部被阻止
        anvil.getSlot(0).set(eternalPick.copy());
        anvil.getSlot(1).set(createEnchantedBook(unbreaking, 3));
        assertTrue(helper, anvil.getSlot(2).getItem().isEmpty(), "已带永恒的物品禁止添加新附魔耐久 III");

        anvil.getSlot(0).set(eternalPick.copy());
        anvil.getSlot(1).set(createEnchantedBook(eternal, 1));
        assertTrue(helper, anvil.getSlot(2).getItem().isEmpty(), "已带永恒的物品禁止再次附魔永恒");

        anvil.getSlot(0).set(eternalPick.copy());
        anvil.getSlot(1).set(createEnchantedBook(tonghui, 1));
        assertTrue(helper, anvil.getSlot(2).getItem().isEmpty(), "已带永恒的物品禁止再次附魔通惠");

        // 3. 铁砧中空副槽仅改名：允许保留永恒与 Unbreakable
        anvil.getSlot(0).set(eternalPick.copy());
        anvil.getSlot(1).set(ItemStack.EMPTY);
        anvil.setItemName("永恒神镐");
        ItemStack renamed = anvil.getSlot(2).getItem();
        assertTrue(helper, !renamed.isEmpty(), "已带永恒的物品应允许仅改名");
        assertTrue(helper, renamed.has(DataComponents.UNBREAKABLE), "改名后应保留 Unbreakable");
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(eternal, renamed) == 1, "改名后应保留永恒附魔");

        // 4. ItemStack#enchant 与 EnchantmentHelper.isEnchantmentCompatible 拦截
        ItemStack testCopy = eternalPick.copy();
        assertTrue(helper, !EnchantmentHelper.isEnchantmentCompatible(
                EnchantmentHelper.getEnchantmentsForCrafting(testCopy).keySet(), unbreaking),
                "已带永恒的物品对新附魔应判定为不兼容（拦截 /enchant）");
        testCopy.enchant(efficiency, 4);
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(efficiency, testCopy) == 3,
                "ItemStack#enchant 不应将效率 III 升级为 IV");
        testCopy.enchant(unbreaking, 3);
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(unbreaking, testCopy) == 0,
                "ItemStack#enchant 不应追加耐久 III");

        // 5. 实际调用 hurtAndBreak 不消耗耐久
        testCopy.hurtAndBreak(10, helper.getLevel(), null, item -> {});
        assertTrue(helper, testCopy.getDamageValue() == 0, "永恒物品调用 hurtAndBreak 后耐久损伤应仍为 0");

        helper.succeed();
    }

    @GameTest
    public void testGrindstoneTagsAndColor(GameTestHelper helper) {
        var enchRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> eternal = enchRegistry.getOrThrow(ModEnchantments.ETERNAL);
        Holder<Enchantment> efficiency = enchRegistry.getOrThrow(Enchantments.EFFICIENCY);

        // 1. 标签与无生存途径验证
        assertTrue(helper, eternal.is(EnchantmentTags.CURSE), "永恒应归属 CURSE 标签");
        assertTrue(helper, !eternal.is(EnchantmentTags.IN_ENCHANTING_TABLE), "永恒不应进入附魔台池");
        assertTrue(helper, !eternal.is(EnchantmentTags.ON_RANDOM_LOOT), "永恒不应进入随机战利品池");
        assertTrue(helper, !eternal.is(EnchantmentTags.TRADEABLE), "永恒不应进入村民交易池");
        assertTrue(helper, !eternal.is(EnchantmentTags.TREASURE), "永恒不应进入通用宝藏池");
        assertTrue(helper, !eternal.is(EnchantmentTags.NON_TREASURE), "永恒不应进入通用非宝藏池");
        assertTrue(helper, !eternal.value().isPrimaryItem(new ItemStack(Items.DIAMOND_PICKAXE)), "isPrimaryItem 应返回 false");

        // 2. 名称颜色为红色加粗 (&c&l = ChatFormatting.RED + bold)
        Component fullName = Enchantment.getFullname(eternal, 1);
        TextColor expectedRed = TextColor.fromLegacyFormat(ChatFormatting.RED);
        assertTrue(helper, expectedRed != null && expectedRed.equals(fullName.getStyle().getColor()),
                "永恒附魔名称颜色必须为红色 (&c / ChatFormatting.RED)，实际: " + fullName.getStyle().getColor());
        assertTrue(helper, fullName.getStyle().isBold(),
                "永恒附魔名称必须为加粗 (&l)");

        // 3. 砂轮单物品祛魔保留永恒与 Unbreakable，双物品合并阻断
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));

        ItemStack pickWithBoth = new ItemStack(Items.DIAMOND_PICKAXE);
        pickWithBoth.enchant(efficiency, 3);
        pickWithBoth.enchant(eternal, 1);

        grindstone.getSlot(0).set(pickWithBoth.copy());
        grindstone.getSlot(1).set(ItemStack.EMPTY);
        ItemStack disenchanted = grindstone.getSlot(2).getItem();
        assertTrue(helper, !disenchanted.isEmpty(), "砂轮单物品可洗去普通附魔");
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(efficiency, disenchanted) == 0, "砂轮应洗去普通附魔效率 III");
        assertTrue(helper, EnchantmentHelper.getItemEnchantmentLevel(eternal, disenchanted) == 1, "砂轮不可祛除永恒");
        assertTrue(helper, disenchanted.has(DataComponents.UNBREAKABLE), "砂轮后应仍保留 Unbreakable");

        ItemStack damagedNormalPick = new ItemStack(Items.DIAMOND_PICKAXE);
        damagedNormalPick.setDamageValue(50);
        grindstone.getSlot(0).set(damagedNormalPick);
        grindstone.getSlot(1).set(pickWithBoth.copy());
        assertTrue(helper, grindstone.getSlot(2).getItem().isEmpty(), "砂轮禁止合并带永恒的物品");

        helper.succeed();
    }
}
