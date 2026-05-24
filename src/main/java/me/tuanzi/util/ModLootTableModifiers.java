package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;

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

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (TARGET_CHESTS.contains(key)) {
                var enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
                var smeltingEnch = enchantmentRegistry.getOrThrow(ModEnchantments.SMELTING);

                ItemEnchantments.Mutable mutableEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutableEnchantments.set(smeltingEnch, 1);
                ItemEnchantments smeltingEnchantmentInstance = mutableEnchantments.toImmutable();

                // 添加一个新的 LootPool，有一定概率掉落熔炼附魔书
                LootPool.Builder poolBuilder = LootPool.lootPool()
                    // 15% 概率掉落 (1次尝试，0.15 成功率)
                    .setRolls(BinomialDistributionGenerator.binomial(1, 0.15f))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .apply(SetComponentsFunction.setComponent(DataComponents.STORED_ENCHANTMENTS, smeltingEnchantmentInstance))
                    );

                tableBuilder.pool(poolBuilder.build());
            }

            if (DUMMY_CHESTS.contains(key)) {
                // 20% 的极大概率在战利品箱中掉落试炼假人物品
                LootPool.Builder poolBuilder = LootPool.lootPool()
                    .setRolls(BinomialDistributionGenerator.binomial(1, 0.20f))
                    .add(LootItem.lootTableItem(me.tuanzi.init.ModItems.TRIAL_DUMMY));
                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
