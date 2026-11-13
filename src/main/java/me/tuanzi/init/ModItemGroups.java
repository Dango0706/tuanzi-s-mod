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
                        // 统一注册附魔书：仅保留 1 级与最高等级
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.EXPERIENCE), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.SMELTING), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_MINING), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.BLOOD_RAGE), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.BERSERKER), 2);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.EXECUTE), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_PAIN), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.SEEKING_ARROW), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.BUZZING_RHYTHM), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.RESONANCE_PULSE), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ABYSSAL_RHYTHM), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.VOID_RESONANCE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.STEEL_SHIELD_GIFT), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.OVERLOAD_PROTOCOL), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.PERVADING_VIBRATION), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_BLOOD_LEECH), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_METAL_CUTTER), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_LEATHER_PIERCER), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_RESERVED_CHAMBER), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_STANDING_FIRM), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ANCIENT_SCROLL_SEASONED), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.RAIN_BLADE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.FROST_BLADE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.THUNDER_BLADE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.SUN_BLADE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.DAY_BLADE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.NIGHT_BLADE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.RANGER_RELOAD), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.FLOW_STATE), 4);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.FINALITY), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.DOUBLE_JUMP), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.EXTENSION), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.RANGED_STRIKE), 5);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.WAYFARER), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.STEP_UP), 2);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.SATIATION), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.BOUNTIFUL_HARVEST), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.EXPERIENCE_AFFINITY), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.BOUNTY_OF_THE_SEA), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.SNOW_STRIDER), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.VOID_ANCHOR), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.TRADING_MASTER), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.BOOST), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.ECHOLOCATION), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.PURITY), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.MOON_EMBRACE), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.WAVE_LISTENER), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.RETURNING_SWAN), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.AMONG_FLOWERS), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.STONE_HEART), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.TONGHUI), 1);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.PHARMACOLOGY), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.DORMANT_REPAIR), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.HUSBANDRY), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.METABOLISM), 3);
                        addEnchantmentBooks(entries, enchantmentRegistry.getOrThrow(ModEnchantments.GAP_VERDICT), 4);

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
                        // 加入脉冲谐振剑
                        entries.accept(ModItems.PULSE_RESONANCE_SWORD);
                        // 加入星隙裁刃
                        entries.accept(ModItems.STAR_RIFT_BLADE);
                        // 加入地脉定桩锤
                        entries.accept(ModItems.EARTH_PILE_HAMMER);
                        // 加入缠皮长握柄与连击斩马刀系列
                        entries.accept(ModItems.REINFORCED_LONG_HILT);
                        entries.accept(ModItems.WOODEN_NODACHI);
                        entries.accept(ModItems.STONE_NODACHI);
                        entries.accept(ModItems.IRON_NODACHI);
                        entries.accept(ModItems.GOLDEN_NODACHI);
                        entries.accept(ModItems.DIAMOND_NODACHI);
                        entries.accept(ModItems.NETHERITE_NODACHI);
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

                        // 加入蓝图构筑系统
                        entries.accept(ModItems.COMPRESSED_BUILD_SLURRY);
                        entries.accept(ModItems.BLANK_BLUEPRINT);
                        entries.accept(ModItems.STRUCTURE_BLUEPRINT);
                        entries.accept(ModBlocks.BLUEPRINT_CANNON);
                        entries.accept(ModBlocks.BLUEPRINT_TABLE);
                        entries.accept(ModBlocks.PLAYER_SIMULATOR);
                        entries.accept(ModBlocks.BLOCK_BREAKER);
                        entries.accept(ModBlocks.BLOCK_PLACER);
                        entries.accept(ModItems.LOGIC_CORE);
                        entries.accept(ModItems.PLAYER_CONTROL_CORE);
                        entries.accept(ModItems.CREATIVE_CORE);

                        // 加入旅者手札、传送纸与道标符石
                        entries.accept(ModItems.TRAVELERS_NOTEBOOK);
                        entries.accept(ModItems.CRAFTSMAN_CHARM);
                        entries.accept(ModItems.TELEPORTATION_PAPER);
                        entries.accept(ModItems.SIGNPOST_RUNE);
                        entries.accept(ModItems.SHURIKEN);
                        entries.accept(ModItems.NETHER_STEW);
                        entries.accept(ModItems.WORLD_SCULPTORS_PEN);
                        entries.accept(ModItems.VOID_INK_INGOT);
                        entries.accept(ModBlocks.COLOR_BLOCK);
                        entries.accept(ModBlocks.COLOR_SLAB);
                        entries.accept(ModBlocks.COLOR_STAIRS);
                        entries.accept(ModItems.PAINT_BUCKET);
                        entries.accept(ModItems.CHROMATIC_SKULL);
                        entries.accept(ModItems.CODEX_OF_ENCHANTING);
                        entries.accept(ModItems.GLASS_ITEM_FRAME);
                        entries.accept(ModItems.GOLDEN_POCKET);
                        entries.accept(ModItems.CINDER_SOVEREIGN_SPAWN_EGG);

                        // 加入抽卡道具
                        entries.accept(ModItems.STAR_TRAVEL_CARD_PACK);
                        entries.accept(ModItems.STAR_TRAVEL_CARD_CHEST);
                        entries.accept(ModItems.SAKURA_FESTIVAL_CARD_PACK);
                        entries.accept(ModItems.SAKURA_FESTIVAL_CARD_CHEST);
                        entries.accept(ModItems.NATIONAL_DAY_CARD_PACK);
                        entries.accept(ModItems.NATIONAL_DAY_CARD_CHEST);
                        entries.accept(ModItems.HALLOWEEN_CARD_PACK);
                        entries.accept(ModItems.HALLOWEEN_CARD_CHEST);
                        entries.accept(ModItems.CHRISTMAS_CARD_PACK);
                        entries.accept(ModItems.CHRISTMAS_CARD_CHEST);
                        entries.accept(ModItems.NEW_YEAR_CARD_PACK);
                        entries.accept(ModItems.NEW_YEAR_CARD_CHEST);
                        entries.accept(ModItems.PRIME_MINISTER_CARD_PACK);
                        entries.accept(ModItems.PRIME_MINISTER_CARD_CHEST);

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

    public static final CreativeModeTab SCAVENGER_CORRIDOR_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_corridor_tab"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                    .title(Component.translatable("itemGroup.tuanzis_mod.scavenger_corridor_tab"))
                    .icon(() -> new ItemStack(ModItems.HALL_RETURN_BEACON))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(ModItems.HALL_RETURN_BEACON);
                        entries.accept(ModItems.ROOM_RETURN_BEACON);
                        entries.accept(ModItems.MARKET_RETURN_BEACON);
                        entries.accept(ModBlocks.BLACKBOX_CORE_CASE);
                        entries.accept(ModBlocks.BASE_STASH_CABINET);
                        entries.accept(ModBlocks.BASE_UPGRADE_STATION);

                        // 6 阶示范搜刮物
                        entries.accept(ModItems.SCAVENGER_BANDAGE_ROLL);
                        entries.accept(ModItems.STASIS_GEL_INJECTOR);
                        entries.accept(ModItems.PHASE_RELAY_CHIP);
                        entries.accept(ModItems.HYDRAULIC_SERVO_ARM);
                        entries.accept(ModItems.SECTOR_07_CIPHER_CARD);
                        entries.accept(ModItems.ABYSSAL_PRISM_CORE);

                        // 4 阶战术背囊
                        entries.accept(ModItems.FIELD_SLING_PACK);
                        entries.accept(ModItems.SCOUT_TACTICAL_PACK);
                        entries.accept(ModItems.TACTICAL_ASSAULT_PACK);
                        entries.accept(ModItems.EXPEDITION_HEAVY_RIG);

                        // 4 阶黑匣安全箱
                        entries.accept(ModItems.PROTO_LOCKBOX_1X2);
                        entries.accept(ModItems.REINFORCED_SAFEBOX_2X2);
                        entries.accept(ModItems.CIPHER_SAFEBOX_2X3);
                        entries.accept(ModItems.AEGIS_CORE_VAULT_3X3);
                    })
                    .build()
    );

    private static void addPotionVariants(CreativeModeTab.Output entries, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.SPLASH_POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.LINGERING_POTION, potion));
        entries.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.TIPPED_ARROW, potion));
    }

    private static void addEnchantmentBooks(CreativeModeTab.Output entries, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, int maxLevel) {
        ItemStack book1 = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book1, mutable -> mutable.set(enchantment, 1));
        entries.accept(book1);

        if (maxLevel > 1) {
            ItemStack bookMax = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.updateEnchantments(bookMax, mutable -> mutable.set(enchantment, maxLevel));
            entries.accept(bookMax);
        }
    }

    public static void initialize() {

    }
}
