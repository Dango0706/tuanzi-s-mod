package me.tuanzi;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.network.ChainMiningKeyPacket;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class Tuanzis_mod implements ModInitializer {
	private static final Map<ServerPlayer, Boolean> playerKeyStates = Collections.synchronizedMap(new WeakHashMap<>());
	public static final String MOD_ID = "tuanzis_mod";

	@Override
	public void onInitialize() {
		// 注册连锁采掘按键网络包
		PayloadTypeRegistry.serverboundPlay().register(ChainMiningKeyPacket.TYPE, ChainMiningKeyPacket.CODEC);
		// 注册伤害飘字网络同步包
		PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.TrialDummyDamagePacket.TYPE, me.tuanzi.network.TrialDummyDamagePacket.CODEC);
		// 注册手札书本及传送请求网络包
		PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.OpenBookScreenS2CPacket.TYPE, me.tuanzi.network.OpenBookScreenS2CPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.TeleportRequestPacket.TYPE, me.tuanzi.network.TeleportRequestPacket.CODEC);

		// 注册 C2S 接收器
		ServerPlayNetworking.registerGlobalReceiver(ChainMiningKeyPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				playerKeyStates.put(context.player(), payload.holding());
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.TeleportRequestPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				me.tuanzi.world.teleport.TeleportManager.handleTeleportRequest(context.player(), payload.slotIdx(), payload.isMainHand());
			});
		});

		ModEnchantments.initialize();

		// 注册击杀生物获得血怒状态效果的监听
		net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killedEntity, damageSource) -> {
			if (killer instanceof Player player) {
				var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
				var enchantmentHolder = registry.getOrThrow(ModEnchantments.BLOOD_RAGE);
				int level = EnchantmentHelper.getItemEnchantmentLevel(enchantmentHolder, player.getMainHandItem());
				if (level > 0) {
					// 获得或刷新 7 秒的“血怒”状态效果 (140 ticks)
					player.addEffect(new net.minecraft.world.effect.MobEffectInstance(me.tuanzi.init.ModStatusEffects.BLOOD_RAGE, 140, 0));
					me.tuanzi.util.ModLog.debug(player, killedEntity, "杀死生物获得血怒效果！击杀目标: " + killedEntity.getName().getString() + "，获得或刷新 7 秒的血怒效果。");
				}
			}
		});
		me.tuanzi.init.ModBlocks.initialize();
		me.tuanzi.init.ModEntities.initialize();
		me.tuanzi.init.ModItems.initialize();
		me.tuanzi.init.ModMenuTypes.initialize();
		me.tuanzi.world.teleport.TeleportManager.initialize();
		me.tuanzi.init.ModItemGroups.initialize();
		me.tuanzi.init.ModStatusEffects.initialize();
		me.tuanzi.init.ModPotions.initialize();
		me.tuanzi.init.ModTrades.initialize();
		me.tuanzi.util.ModLootTableModifiers.initialize();
		me.tuanzi.util.ModSeating.initialize();

		// 初始化抽卡保底与卡池系统生命周期事件
		me.tuanzi.gacha.PlayerGachaManager.initialize();
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			me.tuanzi.gacha.PlayerGachaManager.onPlayerDisconnect(handler.getPlayer().getUUID());
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			me.tuanzi.gacha.PoolManager.loadAllPools(server.registryAccess());
		});

		// 注册抽卡系统指令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			me.tuanzi.gacha.GachaCommands.register(dispatcher);
		});

		ModLog.debug("Mod initialization started in development mode!");
	}

	public static boolean isHoldingChainMiningKey(ServerPlayer player) {
		return playerKeyStates.getOrDefault(player, false);
	}
}
