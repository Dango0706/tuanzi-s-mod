package me.tuanzi;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.network.ChainMiningKeyPacket;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class Tuanzis_mod implements ModInitializer {
	public static final String MOD_ID = "tuanzis_mod";
	private static final Map<ServerPlayer, Boolean> playerKeyStates = Collections.synchronizedMap(new WeakHashMap<>());

	@Override
	public void onInitialize() {
		// 注册连锁采掘按键网络包
		PayloadTypeRegistry.serverboundPlay().register(ChainMiningKeyPacket.TYPE, ChainMiningKeyPacket.CODEC);

		// 注册 C2S 接收器
		ServerPlayNetworking.registerGlobalReceiver(ChainMiningKeyPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				playerKeyStates.put(context.player(), payload.holding());
			});
		});

		ModEnchantments.initialize();
		me.tuanzi.init.ModItems.initialize();
		me.tuanzi.init.ModItemGroups.initialize();
		me.tuanzi.init.ModStatusEffects.initialize();
		me.tuanzi.init.ModPotions.initialize();
		me.tuanzi.init.ModTrades.initialize();
		me.tuanzi.util.ModLootTableModifiers.initialize();
		ModLog.info("Hello Fabric world!");
		ModLog.debug("Mod initialization started in development mode!");
	}

	public static boolean isHoldingChainMiningKey(ServerPlayer player) {
		return playerKeyStates.getOrDefault(player, false);
	}
}
