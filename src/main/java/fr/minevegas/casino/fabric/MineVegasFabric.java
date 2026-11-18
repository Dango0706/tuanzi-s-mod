package fr.minevegas.casino.fabric;

import fr.minevegas.casino.CasinoBetRules;
import fr.minevegas.casino.CasinoSettings;
import fr.minevegas.casino.DerbyLobby;
import fr.minevegas.casino.MineVegas;
import java.util.Objects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.CreativeModeTabs;

public final class MineVegasFabric implements ModInitializer {
   public void onInitialize() {
      MineVegas.init();
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register((CreativeModeTabEvents.ModifyOutput)(output) -> {
         Objects.requireNonNull(output);
         MineVegas.functionalBlocks(output::accept);
      });
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register((CreativeModeTabEvents.ModifyOutput)(output) -> {
         Objects.requireNonNull(output);
         MineVegas.ingredients(output::accept);
      });
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register((CreativeModeTabEvents.ModifyOutput)(output) -> {
         Objects.requireNonNull(output);
         MineVegas.buildingBlocks(output::accept);
      });
      ServerChunkEvents.CHUNK_LOAD.register((ServerChunkEvents.Load)(level, chunk, newlyGenerated) -> MineVegas.onChunkLoad(level, chunk));
      UseEntityCallback.EVENT.register((UseEntityCallback)(player, level, hand, entity, hit) -> MineVegas.onUseEntity(player, level, hand, entity));
      ServerTickEvents.END_SERVER_TICK.register(MineVegas::onServerTick);
      ServerTickEvents.END_LEVEL_TICK.register(MineVegas::onLevelTick);
      PayloadTypeRegistry.clientboundPlay().register(CasinoSettings.Payload.TYPE, CasinoSettings.Payload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(CasinoSettings.EditPayload.TYPE, CasinoSettings.EditPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(CasinoSettings.EditPayload.TYPE, (edit, context) -> CasinoSettings.edit(context.player(), edit));
      PayloadTypeRegistry.clientboundPlay().register(DerbyLobby.Payload.TYPE, DerbyLobby.Payload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(CasinoBetRules.RulesPayload.TYPE, CasinoBetRules.RulesPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(CasinoBetRules.EditPayload.TYPE, CasinoBetRules.EditPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(CasinoBetRules.EditPayload.TYPE, (edit, context) -> MineVegas.onBetRulesEdit(context.player(), edit));
      ServerLifecycleEvents.SERVER_STARTED.register(MineVegas::onServerStarted);
      ServerLifecycleEvents.SERVER_STOPPED.register(MineVegas::onServerStopped);
      ServerPlayConnectionEvents.JOIN.register((ServerPlayConnectionEvents.Join)(listener, sender, server) -> MineVegas.onPlayerJoin(listener.getPlayer()));
   }
}
