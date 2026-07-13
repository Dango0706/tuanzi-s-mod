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
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

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
		// 注册油漆桶颜色修改包
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.PaintBucketColorPacket.TYPE, me.tuanzi.network.PaintBucketColorPacket.CODEC);
		// 注册伤害飘字网络同步包
		PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.TrialDummyDamagePacket.TYPE, me.tuanzi.network.TrialDummyDamagePacket.CODEC);
		// 注册手札书本及传送请求网络包
		PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.OpenBookScreenS2CPacket.TYPE, me.tuanzi.network.OpenBookScreenS2CPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.TeleportRequestPacket.TYPE, me.tuanzi.network.TeleportRequestPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.WorldSculptorsPenModePacket.TYPE, me.tuanzi.network.WorldSculptorsPenModePacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.GhostTransformPacket.TYPE, me.tuanzi.network.GhostTransformPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlueprintCannonActionPacket.TYPE, me.tuanzi.network.BlueprintCannonActionPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlueprintTableImportPacket.TYPE, me.tuanzi.network.BlueprintTableImportPacket.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlueprintMaterialBookPacket.TYPE, me.tuanzi.network.BlueprintMaterialBookPacket.CODEC);

		// 注册 C2S 接收器
		ServerPlayNetworking.registerGlobalReceiver(ChainMiningKeyPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				playerKeyStates.put(context.player(), payload.holding());
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.PaintBucketColorPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				net.minecraft.world.InteractionHand hand = payload.isMainHand() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
				ItemStack stack = player.getItemInHand(hand);
				if (stack.is(me.tuanzi.init.ModItems.PAINT_BUCKET)) {
					stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(payload.rgb()));
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.DYE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
					me.tuanzi.util.ModLog.debug(player, null, "油漆桶颜色修改成功: " + String.format("#%06X", payload.rgb()));
				}
			});
		});

		// 注册自定义合成配方序列化器
		net.minecraft.core.Registry.register(
			net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER,
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "color_block_dye"),
			me.tuanzi.world.item.crafting.ColorBlockDyeRecipe.SERIALIZER
		);
		net.minecraft.core.Registry.register(
			net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER,
			net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "color_block_shaped"),
			me.tuanzi.world.item.crafting.ColorBlockShapedRecipe.SERIALIZER
		);

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.TeleportRequestPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				me.tuanzi.world.teleport.TeleportManager.handleTeleportRequest(context.player(), payload.slotIdx(), payload.isMainHand());
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.WorldSculptorsPenModePacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
				if (!stack.is(me.tuanzi.init.ModItems.WORLD_SCULPTORS_PEN)) {
					stack = player.getOffhandItem();
				}
				if (stack.is(me.tuanzi.init.ModItems.WORLD_SCULPTORS_PEN)) {
					me.tuanzi.item.WorldSculptorsPenItem.toggleMode(stack, player);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.GhostTransformPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
				if (!stack.is(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT)) {
					stack = player.getOffhandItem();
				}
				if (stack.is(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT)) {
					net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
					net.minecraft.nbt.CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
					tag.putInt("OffsetX", payload.offset().getX());
					tag.putInt("OffsetY", payload.offset().getY());
					tag.putInt("OffsetZ", payload.offset().getZ());
					tag.putInt("Rotation", payload.rotation());
					tag.putBoolean("Mirrored", payload.mirrored());
					stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
					me.tuanzi.util.ModLog.debug(player, null, "收到虚影变换：offset=" + payload.offset() + ", rotation=" + payload.rotation() + ", mirrored=" + payload.mirrored());
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.BlueprintCannonActionPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(payload.pos());
				if (be instanceof me.tuanzi.block.entity.BlueprintCannonBlockEntity cannonBe) {
					cannonBe.handleAction(payload.action(), payload.destroyMode(), payload.exactMatch());
					me.tuanzi.util.ModLog.debug(player, null, "收到大炮控制：action=" + payload.action() + ", destroyMode=" + payload.destroyMode() + ", exactMatch=" + payload.exactMatch());
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.BlueprintTableImportPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(payload.tablePos());
				if (be instanceof me.tuanzi.block.entity.BlueprintTableBlockEntity tableBe) {
					net.minecraft.world.item.ItemStack inputStack = tableBe.getItems().get(1);
					if (inputStack.is(me.tuanzi.init.ModItems.BLANK_BLUEPRINT)) {
						inputStack.shrink(1);
						ItemStack result = new ItemStack(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT);
						result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(payload.blueprintData()));
						
						ItemStack slot0 = tableBe.getItems().get(0);
						if (slot0.isEmpty()) {
							tableBe.getItems().set(0, result);
						} else {
							if (!player.getInventory().add(result)) {
								player.drop(result, false);
							}
						}
						tableBe.setChanged();
						me.tuanzi.util.ModLog.debug(player, null, "成功导入结构数据写入结构蓝图！");
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.BlueprintMaterialBookPacket.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(payload.pos());
				if (be instanceof me.tuanzi.block.entity.BlueprintCannonBlockEntity cannonBe) {
					cannonBe.writeMaterialBook(player.level(), payload.pos());
				}
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
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			me.tuanzi.gacha.PlayerGachaManager.shutdown();
			me.tuanzi.gacha.PoolManager.shutdown(server.registryAccess());
		});

		// 注册抽卡系统指令
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			me.tuanzi.gacha.GachaCommands.register(dispatcher);
			me.tuanzi.command.BlueprintCommand.register(dispatcher);
		});

		ModLog.debug("Mod initialization started in development mode!");
	}

	public static boolean isHoldingChainMiningKey(ServerPlayer player) {
		return playerKeyStates.getOrDefault(player, false);
	}
}
