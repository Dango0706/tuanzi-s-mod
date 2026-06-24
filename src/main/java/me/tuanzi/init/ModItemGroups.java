package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ModItemGroups {
    public static final CreativeModeTab TUANZIS_MOD_TAB = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "tuanzis_mod_tab"),
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.tuanzis_mod.tuanzis_mod_tab"))
            .icon(() -> new ItemStack(Items.ENCHANTED_BOOK)) 
            .displayItems((displayContext, entries) -> {
                var enchantmentRegistry = displayContext.holders().lookupOrThrow(Registries.ENCHANTMENT);
                var soulbound = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);
                var experience = enchantmentRegistry.getOrThrow(ModEnchantments.EXPERIENCE);
                var smelting = enchantmentRegistry.getOrThrow(ModEnchantments.SMELTING);
                var chainMining = enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_MINING);
                
                ItemStack soulboundBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(soulboundBook, mutable -> mutable.set(soulbound, 1));
                entries.accept(soulboundBook);

                ItemStack experienceBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(experienceBook, mutable -> mutable.set(experience, 4));
                entries.accept(experienceBook);

                ItemStack smeltingBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(smeltingBook, mutable -> mutable.set(smelting, 1));
                entries.accept(smeltingBook);

                ItemStack chainMiningBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(chainMiningBook, mutable -> mutable.set(chainMining, 4));
                entries.accept(chainMiningBook);

                var bloodRage = enchantmentRegistry.getOrThrow(ModEnchantments.BLOOD_RAGE);
                ItemStack bloodRageBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(bloodRageBook, mutable -> mutable.set(bloodRage, 1));
                entries.accept(bloodRageBook);

                var berserker = enchantmentRegistry.getOrThrow(ModEnchantments.BERSERKER);
                ItemStack berserkerBook2 = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(berserkerBook2, mutable -> mutable.set(berserker, 2));
                entries.accept(berserkerBook2);

                var execute = enchantmentRegistry.getOrThrow(ModEnchantments.EXECUTE);
                ItemStack executeBook3 = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(executeBook3, mutable -> mutable.set(execute, 3));
                entries.accept(executeBook3);

                var chainPain = enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_PAIN);
                ItemStack chainPainBook1 = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(chainPainBook1, mutable -> mutable.set(chainPain, 1));
                entries.accept(chainPainBook1);

                var seekingArrow = enchantmentRegistry.getOrThrow(ModEnchantments.SEEKING_ARROW);
                ItemStack seekingArrowBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(seekingArrowBook, mutable -> mutable.set(seekingArrow, 1));
                entries.accept(seekingArrowBook);

                var buzzingRhythm = enchantmentRegistry.getOrThrow(ModEnchantments.BUZZING_RHYTHM);
                ItemStack buzzingRhythmBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(buzzingRhythmBook, mutable -> mutable.set(buzzingRhythm, 4));
                entries.accept(buzzingRhythmBook);

                var resonancePulse = enchantmentRegistry.getOrThrow(ModEnchantments.RESONANCE_PULSE);
                ItemStack resonancePulseBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(resonancePulseBook, mutable -> mutable.set(resonancePulse, 3));
                entries.accept(resonancePulseBook);

                var abyssalRhythm = enchantmentRegistry.getOrThrow(ModEnchantments.ABYSSAL_RHYTHM);
                ItemStack abyssalRhythmBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(abyssalRhythmBook, mutable -> mutable.set(abyssalRhythm, 4));
                entries.accept(abyssalRhythmBook);

                var voidResonance = enchantmentRegistry.getOrThrow(ModEnchantments.VOID_RESONANCE);
                ItemStack voidResonanceBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(voidResonanceBook, mutable -> mutable.set(voidResonance, 5));
                entries.accept(voidResonanceBook);

                var steelShieldGift = enchantmentRegistry.getOrThrow(ModEnchantments.STEEL_SHIELD_GIFT);
                ItemStack steelShieldGiftBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(steelShieldGiftBook, mutable -> mutable.set(steelShieldGift, 4));
                entries.accept(steelShieldGiftBook);

                // 加入彩虹海绵
                entries.accept(ModItems.RAINBOW_SPONGE);
                // 加入尤里的复仇
                entries.accept(ModItems.YURIS_REVENGE);
                // 加入不朽圣符
                entries.accept(ModItems.IMMORTAL_TALISMAN);
                // 加入狂暴护符
                entries.accept(ModItems.BERSERK_CHARM);
                // 加入战狼护符
                entries.accept(ModItems.WOLF_COMMAND);
                // 加入坚守者的心脏
                entries.accept(ModItems.WARDEN_HEART);
                // 加入回响破障者
                entries.accept(ModItems.ECHO_BREAKER);
                // 加入蜂刺余响
                entries.accept(ModItems.BEE_STING_ECHO);
                // 加入钢御壁垒
                entries.accept(ModItems.STEEL_BARRIER);
                // 加入裂虚之痕
                entries.accept(ModItems.RIFT_SCAR);
                // 加入幽匿裂片
                entries.accept(ModItems.SCULLY_SHARD);
                // 加入潮汐切割者
                entries.accept(ModItems.TIDE_CLEAVER);
                // 加入潮汐织靴
                entries.accept(ModItems.TIDAL_WEAVE_BOOTS);
                // 加入稻草人与假目标
                entries.accept(ModItems.SCARECROW);
                entries.accept(ModItems.TRIAL_DUMMY);
                entries.accept(ModItems.DECOY_TOTEM);
                // 加入缚灵笼
                entries.accept(ModItems.VILLAGER_CAGE);
                // 加入灵笼贸易站
                entries.accept(ModBlocks.SOUL_MERCHANT_STATION);

                // 加入旅者手札、传送纸与道标符石
                entries.accept(ModItems.TRAVELERS_NOTEBOOK);
                entries.accept(ModItems.CRAFTSMAN_CHARM);
                entries.accept(ModItems.TELEPORTATION_PAPER);
                entries.accept(ModItems.SIGNPOST_RUNE);
                entries.accept(ModItems.SHURIKEN);
                entries.accept(ModItems.NETHER_STEW);
                entries.accept(ModItems.WORLD_SCULPTORS_PEN);
                entries.accept(ModItems.VOID_INK_INGOT);

                // 加入抽卡道具
                entries.accept(ModItems.STAR_TRAVEL_CARD_PACK);
                entries.accept(ModItems.STAR_TRAVEL_CARD_CHEST);
                entries.accept(ModItems.SAKURA_FESTIVAL_CARD_PACK);
                entries.accept(ModItems.SAKURA_FESTIVAL_CARD_CHEST);

                // 加入飞行药水及其变体
                addPotionVariants(entries, ModPotions.FLIGHT_POTION);
                addPotionVariants(entries, ModPotions.LONG_FLIGHT_POTION);

                // 加入不死药水及其变体
                addPotionVariants(entries, ModPotions.UNDYING_POTION);
                addPotionVariants(entries, ModPotions.LONG_UNDYING_POTION);

                // 加入肾上腺素药水及其变体
                addPotionVariants(entries, ModPotions.ADRENALINE_POTION);
                addPotionVariants(entries, ModPotions.ADRENALINE_POTION_II);
                addPotionVariants(entries, ModPotions.LONG_ADRENALINE_POTION);
            })
            .build()
    );

    private static void addPotionVariants(CreativeModeTab.Output entries, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.SPLASH_POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.LINGERING_POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.TIPPED_ARROW, potion));
    }

    public static void initialize() {
    }
}
