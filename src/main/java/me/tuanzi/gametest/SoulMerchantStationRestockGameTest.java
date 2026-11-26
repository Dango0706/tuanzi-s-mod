package me.tuanzi.gametest;

import me.tuanzi.block.entity.SoulMerchantStationBlockEntity;
import me.tuanzi.entity.StationVillager;
import me.tuanzi.init.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Optional;

/**
 * 灵笼贸易站独占工作站绑定、存量 POTENTIAL_JOB_SITE 自愈与自动补货 GameTest。
 */
public class SoulMerchantStationRestockGameTest {

    private static CompoundTag createLockedLibrarianTag(ServerLevel level, GlobalPos potentialJobSite) {
        Villager villager = new Villager(EntityTypes.VILLAGER, level);
        villager.setVillagerData(
                villager.getVillagerData()
                        .withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN)
                        .withLevel(3)
        );
        villager.setVillagerXp(76);

        MerchantOffers offers = new MerchantOffers();
        MerchantOffer offer = new MerchantOffer(
                new ItemCost(Items.EMERALD, 9),
                Optional.empty(),
                new ItemStack(Items.BOOKSHELF, 1),
                12,
                12,
                5,
                0.05f
        );
        offers.add(offer);
        villager.setOffers(offers);

        if (potentialJobSite != null) {
            villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, potentialJobSite);
        }

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        villager.save(output);
        return output.buildResult();
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(Component.literal(message));
        }
    }

    @GameTest(maxTicks = 40)
    public void testLegacyPotentialJobSiteSelfHealsAndRestocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lecternRel = new BlockPos(1, 1, 1);
        BlockPos stationRel = new BlockPos(1, 2, 1);
        BlockPos lecternAbs = helper.absolutePos(lecternRel);

        helper.setBlock(lecternRel, Blocks.LECTERN);
        helper.setBlock(stationRel, ModBlocks.SOUL_MERCHANT_STATION);

        // 等待 2 tick 让 ServerLevel.updatePOIOnBlockStateChange 将讲台 POI 注册进 PoiManager
        helper.runAfterDelay(2, () -> {
            PoiManager poiManager = level.getPoiManager();
            // 模拟用户存档现场：讲台 POI 票已被占用（freeTickets=0），且贸易站内村民携带指向该讲台的 POTENTIAL_JOB_SITE
            Optional<BlockPos> preTaken = poiManager.take(
                    poiType -> poiType.is(PoiTypes.LIBRARIAN),
                    (poiType, poiPos) -> poiPos.equals(lecternAbs),
                    lecternAbs,
                    0
            );
            assertTrue(helper, preTaken.isPresent(), "测试前置失败：未能预先占用讲台 POI " + lecternAbs);
            assertTrue(
                    helper,
                    poiManager.getInRange(p -> p.is(PoiTypes.LIBRARIAN), lecternAbs, 0, PoiManager.Occupancy.HAS_SPACE).findAny().isEmpty(),
                    "测试前置失败：讲台 POI 应处于无空闲票状态"
            );

            SoulMerchantStationBlockEntity station = helper.getBlockEntity(stationRel, SoulMerchantStationBlockEntity.class);
            CompoundTag villagerTag = createLockedLibrarianTag(level, GlobalPos.of(level.dimension(), lecternAbs));
            station.importVillager(villagerTag);
            station.setRestockTimer(SoulMerchantStationBlockEntity.RESTOCK_INTERVAL - 5);

            helper.runAfterDelay(10, () -> {
                Villager villager = station.getOrCreateVillager();
                assertTrue(helper, villager != null, "贸易站应存在已加载村民");
                assertTrue(
                        helper,
                        villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty(),
                        "存量 POTENTIAL_JOB_SITE 记忆应在自愈后被擦除"
                );
                Optional<GlobalPos> boundJobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                assertTrue(
                        helper,
                        boundJobSite.isPresent() && boundJobSite.get().pos().equals(lecternAbs),
                        "自愈后专属 JOB_SITE 应指向正下方讲台 " + lecternAbs + "，实际: " + boundJobSite
                );
                assertTrue(
                        helper,
                        !villager.getOffers().isEmpty() && villager.getOffers().getFirst().getUses() == 0,
                        "达到 8000 ticks 后锁定交易 uses 应重置为 0，实际: "
                                + (villager.getOffers().isEmpty() ? "empty" : villager.getOffers().getFirst().getUses())
                );
                assertTrue(
                        helper,
                        poiManager.getInRange(p -> p.is(PoiTypes.LIBRARIAN), lecternAbs, 0, PoiManager.Occupancy.HAS_SPACE).findAny().isEmpty(),
                        "自愈接管后讲台 POI 应继续保持独占（无空闲票）"
                );
                helper.succeed();
            });
        });
    }

    @GameTest(maxTicks = 60)
    public void testExclusiveJobSitePreventsSharingAndReleasesOnExport(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lecternRel = new BlockPos(2, 1, 2);
        BlockPos station1Rel = new BlockPos(1, 1, 2);
        BlockPos station2Rel = new BlockPos(3, 1, 2);
        BlockPos lecternAbs = helper.absolutePos(lecternRel);

        helper.setBlock(lecternRel, Blocks.LECTERN);
        helper.setBlock(station1Rel, ModBlocks.SOUL_MERCHANT_STATION);
        helper.setBlock(station2Rel, ModBlocks.SOUL_MERCHANT_STATION);

        helper.runAfterDelay(2, () -> {
            SoulMerchantStationBlockEntity station1 = helper.getBlockEntity(station1Rel, SoulMerchantStationBlockEntity.class);
            SoulMerchantStationBlockEntity station2 = helper.getBlockEntity(station2Rel, SoulMerchantStationBlockEntity.class);

            // 先导入第一台并立即令其绑定独占讲台
            station1.importVillager(createLockedLibrarianTag(level, null));
            station1.setRestockTimer(SoulMerchantStationBlockEntity.RESTOCK_INTERVAL - 4);

            helper.runAfterDelay(6, () -> {
                Villager v1 = station1.getOrCreateVillager();
                assertTrue(helper, v1 != null && v1.getOffers().getFirst().getUses() == 0, "第一台贸易站应独占讲台并完成补货");
                assertTrue(
                        helper,
                        level.getPoiManager().getInRange(p -> p.is(PoiTypes.LIBRARIAN), lecternAbs, 0, PoiManager.Occupancy.HAS_SPACE).findAny().isEmpty(),
                        "第一台绑定后讲台 POI 不应再有空闲票"
                );

                // 再导入第二台紧邻同一讲台：不应共用，满 8000 后仍保持锁定
                station2.importVillager(createLockedLibrarianTag(level, null));
                station2.setRestockTimer(SoulMerchantStationBlockEntity.RESTOCK_INTERVAL - 4);

                helper.runAfterDelay(6, () -> {
                    Villager v2 = station2.getOrCreateVillager();
                    assertTrue(helper, v2 != null && v2.getOffers().getFirst().getUses() == 12, "第二台贸易站不应共用已被占用的讲台，交易应保持锁定");

                    // 取出第一台村民后，讲台 POI 应立即释放，随后第二台应自动接管并成功补货
                    station1.exportVillager();
                    assertTrue(
                            helper,
                            level.getPoiManager().getInRange(p -> p.is(PoiTypes.LIBRARIAN), lecternAbs, 0, PoiManager.Occupancy.HAS_SPACE).findAny().isPresent(),
                            "第一台取出村民后，讲台 POI 票应立即归还为 HAS_SPACE"
                    );

                    station2.setRestockTimer(SoulMerchantStationBlockEntity.RESTOCK_INTERVAL - 4);
                    helper.runAfterDelay(6, () -> {
                        Villager v2After = station2.getOrCreateVillager();
                        assertTrue(helper, v2After != null && v2After.getOffers().getFirst().getUses() == 0, "第一台释放后，第二台应接管讲台并成功补货");
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(maxTicks = 60)
    public void testPreRemoveReleasesPoiAndRebindNewAdjacentJobSite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lectern1Rel = new BlockPos(1, 1, 2);
        BlockPos stationRel = new BlockPos(2, 1, 2);
        BlockPos lectern2Rel = new BlockPos(3, 1, 2);
        BlockPos lectern1Abs = helper.absolutePos(lectern1Rel);
        BlockPos lectern2Abs = helper.absolutePos(lectern2Rel);

        helper.setBlock(lectern1Rel, Blocks.LECTERN);
        helper.setBlock(stationRel, ModBlocks.SOUL_MERCHANT_STATION);

        helper.runAfterDelay(2, () -> {
            SoulMerchantStationBlockEntity station = helper.getBlockEntity(stationRel, SoulMerchantStationBlockEntity.class);
            station.importVillager(createLockedLibrarianTag(level, null));
            station.setRestockTimer(SoulMerchantStationBlockEntity.RESTOCK_INTERVAL - 4);

            helper.runAfterDelay(6, () -> {
                Villager v = station.getOrCreateVillager();
                assertTrue(
                        helper,
                        v != null && v.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(g -> g.pos().equals(lectern1Abs)).orElse(false),
                        "贸易站应先绑定西侧讲台"
                );

                // 挖掉西侧讲台，改在东侧放置新讲台
                helper.setBlock(lectern1Rel, Blocks.AIR);
                helper.setBlock(lectern2Rel, Blocks.LECTERN);
                v.getOffers().getFirst().setToOutOfStock();
                station.syncVillagerToNbt();

                helper.runAfterDelay(3, () -> {
                    station.setRestockTimer(SoulMerchantStationBlockEntity.RESTOCK_INTERVAL - 4);
                    helper.runAfterDelay(6, () -> {
                        Villager vRebound = station.getOrCreateVillager();
                        assertTrue(
                                helper,
                                vRebound != null && vRebound.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(g -> g.pos().equals(lectern2Abs)).orElse(false),
                                "原讲台破坏后贸易站应重新绑定东侧新讲台"
                        );
                        assertTrue(helper, vRebound.getOffers().getFirst().getUses() == 0, "重新绑定新讲台后应正常补货");

                        // 使用 flag 2 | 256 模拟 /setblock replace 跳过 preRemoveSideEffects，验证 setRemoved 同样归还 POI 票
                        level.setBlock(helper.absolutePos(stationRel), Blocks.AIR.defaultBlockState(), 2 | 256);
                        helper.runAfterDelay(2, () -> {
                            assertTrue(
                                    helper,
                                    level.getPoiManager().getInRange(p -> p.is(PoiTypes.LIBRARIAN), lectern2Abs, 0, PoiManager.Occupancy.HAS_SPACE).findAny().isPresent(),
                                    "使用 /setblock replace 移除贸易站后，setRemoved 必须释放东侧讲台的 POI 票"
                            );
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    @GameTest(maxTicks = 40)
    public void testYieldJobSiteIgnoresStationAndNoAiVillager(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lecternRel = new BlockPos(2, 1, 2);
        BlockPos lecternAbs = helper.absolutePos(lecternRel);
        helper.setBlock(lecternRel, Blocks.LECTERN);

        helper.runAfterDelay(2, () -> {
            StationVillager stationVillager = new StationVillager(EntityTypes.VILLAGER, level);
            stationVillager.setVillagerData(
                    stationVillager.getVillagerData()
                            .withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN)
                            .withLevel(2)
            );
            stationVillager.setNoAi(true);
            stationVillager.snapTo(lecternAbs.getX() + 0.5, lecternAbs.getY() + 1.33, lecternAbs.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(stationVillager);

            // 生成一只紧邻讲台、已获得讲台 POTENTIAL_JOB_SITE 的无业自由村民，令其运行 AI
            Villager unemployed = new Villager(EntityTypes.VILLAGER, level);
            unemployed.setVillagerData(unemployed.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
            unemployed.snapTo(lecternAbs.getX() + 1.5, lecternAbs.getY() + 1.0, lecternAbs.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(unemployed);
            unemployed.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(level.dimension(), lecternAbs));

            helper.runAfterDelay(25, () -> {
                assertTrue(
                        helper,
                        stationVillager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty(),
                        "YieldJobSiteMixin 应阻止外部无业村民向 StationVillager/NoAI 村民移交 POTENTIAL_JOB_SITE"
                );
                // 校验 BuiltInRegistries 引用以确保编译期兼容
                assertTrue(helper, BuiltInRegistries.VILLAGER_PROFESSION.getOptional(VillagerProfession.LIBRARIAN).isPresent(), "图书管理员职业已注册");
                stationVillager.discard();
                unemployed.discard();
                helper.succeed();
            });
        });
    }
}
