package fr.minevegas.casino;

import fr.minevegas.casino.compat.CasinoContraptions;
import fr.minevegas.casinodeco.CasinoDecoMod;
import fr.minevegas.casinodeco.CasinoFloorsAndWalls;
import fr.minevegas.casinosound.CasinoSoundMod;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public final class MineVegas {
   private static boolean initialized;

   private MineVegas() {
   }

   public static synchronized void init() {
      if (!initialized) {
         initialized = true;
         RouletteMod.init();
         CasinoDecoMod.init();
         CasinoSoundMod.init();
         CroupierHandItem.init();
      }
   }

   public static void functionalBlocks(Consumer<ItemLike> output) {
      RouletteMod.functionalBlocks(output);
      CasinoDecoMod.functionalBlocks(output);
      CasinoSoundMod.functionalBlocks(output);
   }

   public static void buildingBlocks(Consumer<ItemLike> output) {
      CasinoFloorsAndWalls.creative(output);
   }

   public static void ingredients(Consumer<ItemLike> output) {
      RouletteMod.ingredients(output);
   }

   public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
      RouletteMod.onChunkLoad(level, chunk);
      CasinoDecoMod.onChunkLoad(level, chunk);
   }

   public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity) {
      return CroupierHiring.onUse(player, level, hand, entity);
   }

   public static void onServerTick(MinecraftServer server) {
      CasinoContraptions.register();
      CroupierHiring.tickStreams();
      DerbyCredits.tick(server);
   }

   public static void onLevelTick(ServerLevel level) {
      CasinoSoundMod.onLevelTick(level);
   }

   public static void onServerStarted(MinecraftServer server) {
      CasinoBetRules.load(server);
      DerbyCredits.started(server);
   }

   public static void onServerStopped(MinecraftServer server) {
      CasinoBetRules.unload();
   }

   public static void onPlayerJoin(ServerPlayer player) {
      CasinoBetRules.sendTo(player);
      DerbyCredits.get(player.level().getServer()).pay(player, true);
   }

   public static void onBetRulesEdit(ServerPlayer player, CasinoBetRules.EditPayload edit) {
      CasinoBetRules.edit(player, edit);
   }

   public static void onBetRules(CasinoBetRules.RulesPayload rules) {
      CasinoBetRules.receive(rules);
   }
}
