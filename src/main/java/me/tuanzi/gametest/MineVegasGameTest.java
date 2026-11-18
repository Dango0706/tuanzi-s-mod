package me.tuanzi.gametest;

import fr.minevegas.casino.CasinoBets;
import fr.minevegas.casino.CroupierOffer;
import fr.minevegas.casino.RouletteBlock;
import fr.minevegas.casino.RouletteBlockEntity;
import fr.minevegas.casino.RouletteMod;
import fr.minevegas.casino.SlotMachineBlock;
import fr.minevegas.casino.SlotMachineBlockEntity;
import fr.minevegas.casino.SlotMachinePart;
import fr.minevegas.casinosound.CasinoSoundMod;
import fr.minevegas.casinosound.SpeakerBlockEntity;
import me.tuanzi.config.ModConfig;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MineVegasGameTest {

    @GameTest
    public void testChipsAcceptedAndCoinsRejectedByCasinoBets(GameTestHelper helper) {
        if (!CasinoBets.accepts(new ItemStack(Items.DIAMOND))) {
            helper.fail(Component.literal("CasinoBets should accept minecraft:diamond"));
            return;
        }
        if (CasinoBets.accepts(new ItemStack(Items.DIRT))) {
            helper.fail(Component.literal("CasinoBets must reject minecraft:dirt"));
            return;
        }

        for (Item coin : ModItems.COINS) {
            if (CasinoBets.accepts(new ItemStack(coin))) {
                helper.fail(Component.literal("CasinoBets must reject coin by default: " + coin));
                return;
            }
        }
        for (Block chip : ModBlocks.CHIPS) {
            if (!CasinoBets.accepts(new ItemStack(chip))) {
                helper.fail(Component.literal("CasinoBets must accept chip: " + chip));
                return;
            }
        }
        helper.succeed();
    }

    @GameTest
    public void testCasinoBlocksAndEntitiesPlacement(GameTestHelper helper) {
        // 放置地面支撑方块
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }

        // 放置轮盘桌根部并检验方块实体
        BlockPos roulettePos = new BlockPos(1, 1, 1);
        helper.setBlock(roulettePos, RouletteMod.TABLE.defaultBlockState().setValue(RouletteBlock.PART, 0));
        BlockEntity rouletteBe = helper.getBlockEntity(roulettePos, RouletteBlockEntity.class);
        if (rouletteBe == null) {
            helper.fail(Component.literal("RouletteBlockEntity missing"));
            return;
        }

        // 放置老虎机三格结构并检验方块实体
        BlockPos slotPos = new BlockPos(4, 1, 1);
        BlockState slotBase = RouletteMod.SLOT_MACHINE.defaultBlockState().setValue(SlotMachineBlock.FACING, Direction.SOUTH);
        helper.setBlock(slotPos, slotBase.setValue(SlotMachineBlock.HALF, SlotMachinePart.LOWER));
        helper.setBlock(slotPos.above(), slotBase.setValue(SlotMachineBlock.HALF, SlotMachinePart.UPPER));
        helper.setBlock(slotPos.above(2), slotBase.setValue(SlotMachineBlock.HALF, SlotMachinePart.TOP));
        BlockEntity slotBe = helper.getBlockEntity(slotPos, SlotMachineBlockEntity.class);
        if (slotBe == null) {
            helper.fail(Component.literal("SlotMachineBlockEntity missing"));
            return;
        }

        // 放置荷官柜台与赌场音箱
        BlockPos stationPos = new BlockPos(1, 1, 4);
        helper.setBlock(stationPos, RouletteMod.DEALER_STATION.defaultBlockState());
        helper.assertBlockPresent(RouletteMod.DEALER_STATION, stationPos);

        BlockPos speakerPos = new BlockPos(4, 1, 4);
        helper.setBlock(speakerPos, CasinoSoundMod.SPEAKER.defaultBlockState());
        BlockEntity speakerBe = helper.getBlockEntity(speakerPos, SpeakerBlockEntity.class);
        if (speakerBe == null) {
            helper.fail(Component.literal("SpeakerBlockEntity missing"));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void testCasinoRecipesConfigToggleAndCroupierOffer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean originalEnabled = ModConfig.isCasinoRecipesEnabled();

        CraftingInput rouletteInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.WOOL.red()), new ItemStack(Items.WOOL.green()), new ItemStack(Items.WOOL.black()),
                new ItemStack(Items.GOLD_BLOCK), new ItemStack(RouletteMod.MECHANISM), new ItemStack(Items.GOLD_BLOCK),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.REDSTONE), new ItemStack(Items.OAK_PLANKS)
        ));
        CraftingInput chairInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.WOOL.red()), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.WOOL.red()), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.GOLD_NUGGET), ItemStack.EMPTY, new ItemStack(Items.GOLD_NUGGET)
        ));

        try {
            // 1. 测试关闭赌场配方开关
            ModConfig.setCasinoRecipesEnabled(false);
            RecipeManager disabledManager = new RecipeManager(server.reloadableRegistries().lookup());

            if (disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("roulettecasino", "roulette_table"))).isPresent()
                    || disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("roulettecasino", "blackjack_table"))).isPresent()
                    || disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("roulettecasino", "dealer_station"))).isPresent()
                    || disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("roulettecasino", "casino_guide"))).isPresent()) {
                helper.fail(Component.literal("Core casino recipes must be filtered out when enableCasinoRecipes=false"));
                return;
            }
            if (disabledManager.getRecipeFor(RecipeType.CRAFTING, rouletteInput, level).isPresent()) {
                helper.fail(Component.literal("Roulette table must not be craftable when enableCasinoRecipes=false"));
                return;
            }

            // 装饰/座椅/音箱/筹码不受影响
            if (disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("roulettecasino", "casino_chair"))).isEmpty()
                    || disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("casinodeco", "casino_sofa"))).isEmpty()
                    || disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("casinosound", "casino_speaker"))).isEmpty()
                    || disabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("tuanzis_mod", "chip_50_from_coin"))).isEmpty()) {
                helper.fail(Component.literal("Decorative and unaffected recipes must remain available when enableCasinoRecipes=false"));
                return;
            }
            if (disabledManager.getRecipeFor(RecipeType.CRAFTING, chairInput, level).isEmpty()) {
                helper.fail(Component.literal("Casino chair must remain craftable when enableCasinoRecipes=false"));
                return;
            }

            // 验证荷官村民交易在关闭时移除雇佣卡
            Villager croupier = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            if (croupier == null) {
                helper.fail(Component.literal("Failed to create test Villager"));
                return;
            }
            var croupierHolder = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(RouletteMod.CROUPIER_KEY);
            croupier.setVillagerData(croupier.getVillagerData().withProfession(croupierHolder));
            croupier.getOffers().add(CroupierOffer.create());
            CroupierOffer.keep(croupier);
            boolean hasHiringOfferWhenDisabled = croupier.getOffers().stream()
                    .anyMatch(o -> o.getResult().is(RouletteMod.HIRING_CARD));
            if (hasHiringOfferWhenDisabled) {
                helper.fail(Component.literal("Croupier hiring card trade must be removed when enableCasinoRecipes=false"));
                return;
            }

            // 2. 测试重新开启赌场配方开关
            ModConfig.setCasinoRecipesEnabled(true);
            RecipeManager enabledManager = new RecipeManager(server.reloadableRegistries().lookup());
            if (enabledManager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("roulettecasino", "roulette_table"))).isEmpty()
                    || enabledManager.getRecipeFor(RecipeType.CRAFTING, rouletteInput, level).isEmpty()) {
                helper.fail(Component.literal("Roulette table must be craftable when enableCasinoRecipes=true"));
                return;
            }
            CroupierOffer.keep(croupier);
            boolean hasHiringOfferWhenEnabled = croupier.getOffers().stream()
                    .anyMatch(o -> o.getResult().is(RouletteMod.HIRING_CARD));
            if (!hasHiringOfferWhenEnabled) {
                helper.fail(Component.literal("Croupier hiring card trade must be restored when enableCasinoRecipes=true"));
                return;
            }

            croupier.discard();
            helper.succeed();
        } finally {
            ModConfig.setCasinoRecipesEnabled(originalEnabled);
        }
    }
}
