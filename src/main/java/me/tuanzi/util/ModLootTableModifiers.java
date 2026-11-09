package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.Set;

public class ModLootTableModifiers {
    private static final Set<ResourceKey<LootTable>> TARGET_CHESTS = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/nether_fortress")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/desert_pyramid"))
    );

    private static final Set<ResourceKey<LootTable>> DUMMY_CHESTS = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/pillager_outpost")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_weaponsmith"))
    );

    private static final Set<ResourceKey<LootTable>> SATIATION_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/desert_pyramid")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/jungle_temple")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/buried_treasure")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("gameplay/fishing/treasure"))
    );

    private static final Set<ResourceKey<LootTable>> SHIPWRECK_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/shipwreck_treasure")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/shipwreck_supply"))
    );

    private static final Set<ResourceKey<LootTable>> IGLOO_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/igloo_chest"))
    );

    private static final Set<ResourceKey<LootTable>> END_CITY_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/end_city_treasure"))
    );

    private static final Set<ResourceKey<LootTable>> VILLAGE_OUTPOST_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/pillager_outpost")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_plains_house")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_desert_house"))
    );

    // 新增附魔专属战利品箱表
    private static final Set<ResourceKey<LootTable>> ECHOLOCATION_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/ancient_city")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_library"))
    );

    private static final Set<ResourceKey<LootTable>> VILLAGE_CHEST_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_plains_house")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_desert_house")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_savanna_house")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_snowy_house")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_taiga_house")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_weaponsmith")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_toolsmith")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_fletcher"))
    );

    private static final Set<ResourceKey<LootTable>> MOON_EMBRACE_LOOT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/igloo_chest")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/buried_treasure"))
    );

    private static final Set<ResourceKey<LootTable>> UNDERWATER_RUIN_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/underwater_ruin_big")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/underwater_ruin_small"))
    );

    private static final Set<ResourceKey<LootTable>> PILLAGER_OUTPOST_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/pillager_outpost"))
    );

    private static final Set<ResourceKey<LootTable>> MINESHAFT_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/abandoned_mineshaft"))
    );

    private static final Set<ResourceKey<LootTable>> DORMANT_REPAIR_CHEST_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/ancient_city")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_corridor")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_crossing")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_library")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/nether_bridge")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/nether_fortress"))
    );

    private static final Set<ResourceKey<LootTable>> DORMANT_REPAIR_FISHING_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("gameplay/fishing/treasure"))
    );

    private static final Set<ResourceKey<LootTable>> METABOLISM_CHEST_TABLES = Set.of(
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/ancient_city")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_corridor")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_crossing")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_library")),
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon"))
    );

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            var enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);

            if (TARGET_CHESTS.contains(key)) {
                var smeltingEnch = enchantmentRegistry.getOrThrow(ModEnchantments.SMELTING);
                addEnchantedBookPool(tableBuilder, smeltingEnch, 0.15f, 1);
            }

            if (DUMMY_CHESTS.contains(key)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                    .setRolls(ContextIntProviders.binomial(1, 0.20f))
                    .add(LootItem.lootTableItem(me.tuanzi.init.ModItems.TRIAL_DUMMY));
                tableBuilder.pool(poolBuilder.build());
            }

            if (SATIATION_LOOT_TABLES.contains(key)) {
                var satiationEnch = enchantmentRegistry.getOrThrow(ModEnchantments.SATIATION);
                addEnchantedBookPool(tableBuilder, satiationEnch, 0.20f, 3);
            }

            if (SHIPWRECK_LOOT_TABLES.contains(key)) {
                var bountyEnch = enchantmentRegistry.getOrThrow(ModEnchantments.BOUNTY_OF_THE_SEA);
                addEnchantedBookPool(tableBuilder, bountyEnch, 0.25f, 3);
            }

            if (IGLOO_LOOT_TABLES.contains(key)) {
                var snowStriderEnch = enchantmentRegistry.getOrThrow(ModEnchantments.SNOW_STRIDER);
                addEnchantedBookPool(tableBuilder, snowStriderEnch, 0.25f, 3);
            }

            if (END_CITY_LOOT_TABLES.contains(key)) {
                var voidAnchorEnch = enchantmentRegistry.getOrThrow(ModEnchantments.VOID_ANCHOR);
                addEnchantedBookPool(tableBuilder, voidAnchorEnch, 0.20f, 1);

                // 增效 (Boost I-III) 末地城战利品箱
                var boostEnch = enchantmentRegistry.getOrThrow(ModEnchantments.BOOST);
                addEnchantedBookPool(tableBuilder, boostEnch, 0.20f, 3);
            }

            if (VILLAGE_OUTPOST_LOOT_TABLES.contains(key)) {
                var tradingMasterEnch = enchantmentRegistry.getOrThrow(ModEnchantments.TRADING_MASTER);
                addEnchantedBookPool(tableBuilder, tradingMasterEnch, 0.15f, 3);
            }

            // 1. 回声定位 (Echolocation I-III) 远古城市、要塞图书馆
            if (ECHOLOCATION_LOOT_TABLES.contains(key)) {
                var echolocationEnch = enchantmentRegistry.getOrThrow(ModEnchantments.ECHOLOCATION);
                addEnchantedBookPool(tableBuilder, echolocationEnch, 0.15f, 3);
            }

            // 2. 抱朴 (Purity I-III) 与 花间 (Among Flowers I-III) 村庄战利品箱
            if (VILLAGE_CHEST_TABLES.contains(key)) {
                var purityEnch = enchantmentRegistry.getOrThrow(ModEnchantments.PURITY);
                addEnchantedBookPool(tableBuilder, purityEnch, 0.15f, 3);

                var amongFlowersEnch = enchantmentRegistry.getOrThrow(ModEnchantments.AMONG_FLOWERS);
                addEnchantedBookPool(tableBuilder, amongFlowersEnch, 0.15f, 3);
            }

            // 3. 衔月 (Moon Embrace I-III) 雪屋战利品箱、埋藏宝藏
            if (MOON_EMBRACE_LOOT_TABLES.contains(key)) {
                var moonEmbraceEnch = enchantmentRegistry.getOrThrow(ModEnchantments.MOON_EMBRACE);
                addEnchantedBookPool(tableBuilder, moonEmbraceEnch, 0.20f, 3);
            }

            // 4. 听涛 (Wave Listener I-III) 海底废墟战利品箱
            if (UNDERWATER_RUIN_TABLES.contains(key)) {
                var waveListenerEnch = enchantmentRegistry.getOrThrow(ModEnchantments.WAVE_LISTENER);
                addEnchantedBookPool(tableBuilder, waveListenerEnch, 0.20f, 3);
            }

            // 5. 归鸿 (Returning Swan I-III) 掠夺者前哨战利品箱
            if (PILLAGER_OUTPOST_TABLES.contains(key)) {
                var returningSwanEnch = enchantmentRegistry.getOrThrow(ModEnchantments.RETURNING_SWAN);
                addEnchantedBookPool(tableBuilder, returningSwanEnch, 0.20f, 3);
            }

            // 6. 石心 (Stone Heart I-III) 废弃矿井战利品箱
            if (MINESHAFT_TABLES.contains(key)) {
                var stoneHeartEnch = enchantmentRegistry.getOrThrow(ModEnchantments.STONE_HEART);
                addEnchantedBookPool(tableBuilder, stoneHeartEnch, 0.20f, 3);
            }

            // 7. 休眠修复 (Dormant Repair I-III)
            if (DORMANT_REPAIR_CHEST_TABLES.contains(key)) {
                var dormantEnch = enchantmentRegistry.getOrThrow(ModEnchantments.DORMANT_REPAIR);
                addEnchantedBookPool(tableBuilder, dormantEnch, 0.15f, 3);
            }
            if (DORMANT_REPAIR_FISHING_TABLES.contains(key)) {
                var dormantEnch = enchantmentRegistry.getOrThrow(ModEnchantments.DORMANT_REPAIR);
                addEnchantedBookPool(tableBuilder, dormantEnch, 0.05f, 3);
            }

            // 8. 代谢 (Metabolism I-III)
            if (METABOLISM_CHEST_TABLES.contains(key)) {
                var metabolismEnch = enchantmentRegistry.getOrThrow(ModEnchantments.METABOLISM);
                addEnchantedBookPool(tableBuilder, metabolismEnch, 0.15f, 3);
            }
        });
    }

    private static void addEnchantedBookPool(
        LootTable.Builder tableBuilder,
        Holder<Enchantment> enchantmentHolder,
        float dropChance,
        int maxLevel
    ) {
        LootPool.Builder poolBuilder = LootPool.lootPool()
            .setRolls(ContextIntProviders.binomial(1, dropChance));

        for (int lvl = 1; lvl <= maxLevel; lvl++) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            mutable.set(enchantmentHolder, lvl);
            int weight = maxLevel - lvl + 1; // 1级权重最高，3级稀有
            poolBuilder.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                .setWeight(weight)
                .apply(SetComponentsFunction.setComponent(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable())));
        }

        tableBuilder.pool(poolBuilder.build());
    }
}
