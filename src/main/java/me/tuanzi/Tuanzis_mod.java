package me.tuanzi;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.network.ChainMiningKeyPacket;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class Tuanzis_mod implements ModInitializer {
    public static final String MOD_ID = "tuanzis_mod";
    private static final Map<ServerPlayer, Boolean> playerKeyStates = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile net.minecraft.server.MinecraftServer activeServer;
    private static me.tuanzi.config.ModConfigWatcher configWatcher;

    public static boolean isHoldingChainMiningKey(ServerPlayer player) {
        return playerKeyStates.getOrDefault(player, false);
    }

    @Override
    public void onInitialize() {
        me.tuanzi.config.ModConfig.load();
        configWatcher = new me.tuanzi.config.ModConfigWatcher(me.tuanzi.config.ModConfig.getConfigPath(), path -> {
            boolean previousCasinoEnabled = me.tuanzi.config.ModConfig.isCasinoRecipesEnabled();
            me.tuanzi.config.ModConfig.load(path);
            me.tuanzi.config.OverworldBuildHeightControl.handleHotReload(activeServer, previousCasinoEnabled);
        });
        configWatcher.start();
        // 注册主世界建筑高度热更新同步网络包
        PayloadTypeRegistry.clientboundPlay().register(
                me.tuanzi.network.OverworldBuildHeightSyncS2CPacket.TYPE,
                me.tuanzi.network.OverworldBuildHeightSyncS2CPacket.CODEC
        );
        // 注册二段跳网络包
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.DoubleJumpC2SPacket.TYPE, me.tuanzi.network.DoubleJumpC2SPacket.CODEC);
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
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlockBreakerToggleModePacket.TYPE, me.tuanzi.network.BlockBreakerToggleModePacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlockPlacerActionPacket.TYPE, me.tuanzi.network.BlockPlacerActionPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.GhostTransformPacket.TYPE, me.tuanzi.network.GhostTransformPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.GhostPlaceStructurePacket.TYPE, me.tuanzi.network.GhostPlaceStructurePacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlueprintCannonActionPacket.TYPE, me.tuanzi.network.BlueprintCannonActionPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.BlueprintCannonShootS2CPacket.TYPE, me.tuanzi.network.BlueprintCannonShootS2CPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().registerLarge(
                me.tuanzi.network.BlueprintTableImportPacket.TYPE,
                me.tuanzi.network.BlueprintTableImportPacket.CODEC,
                me.tuanzi.network.BlueprintTableImportPacket.MAX_COMPRESSED_BYTES
        );
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.BlueprintMaterialBookPacket.TYPE, me.tuanzi.network.BlueprintMaterialBookPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.ChromaticSkullRequestPacket.TYPE, me.tuanzi.network.ChromaticSkullRequestPacket.CODEC);
        // 注册野太刀势槽同步与挥空网络包
        PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.NodachiSyncPacket.TYPE, me.tuanzi.network.NodachiSyncPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.NodachiMissPacket.TYPE, me.tuanzi.network.NodachiMissPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.NodachiBeatPacket.TYPE, me.tuanzi.network.NodachiBeatPacket.CODEC);
        // 注册日冕过热状态同步网络包
        PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.CinderHeatSyncPacket.TYPE, me.tuanzi.network.CinderHeatSyncPacket.CODEC);
        // 注册背水一战创伤值同步网络包
        PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.DesperateStandSyncPacket.TYPE, me.tuanzi.network.DesperateStandSyncPacket.CODEC);
        // 注册拾荒回廊队伍状态同步网络包
        PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.ScavengerTeamSyncPacket.TYPE, me.tuanzi.network.ScavengerTeamSyncPacket.CODEC);
        // 注册拾荒回廊专属饰品槽同步与信标激活网络包
        PayloadTypeRegistry.clientboundPlay().register(me.tuanzi.network.CorridorAccessorySyncPacket.TYPE, me.tuanzi.network.CorridorAccessorySyncPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(me.tuanzi.network.CorridorBeaconUsePacket.TYPE, me.tuanzi.network.CorridorBeaconUsePacket.CODEC);
        // 注册拾荒回廊搜刮元数据热重载全量同步 S2C 网络包
        PayloadTypeRegistry.clientboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerDataSyncS2CPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerDataSyncS2CPacket.CODEC
        );
        // 注册拾荒回廊战术网格与搜索交互 C2S / S2C 网络包
        PayloadTypeRegistry.serverboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerPrioritizeSearchC2SPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerPrioritizeSearchC2SPacket.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerBaseUpgradeC2SPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerBaseUpgradeC2SPacket.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                me.tuanzi.network.scavenger.OpenCorridorLoadoutC2SPacket.TYPE,
                me.tuanzi.network.scavenger.OpenCorridorLoadoutC2SPacket.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerContainerSyncS2CPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerContainerSyncS2CPacket.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerSearchProgressS2CPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerSearchProgressS2CPacket.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerItemRevealedS2CPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerItemRevealedS2CPacket.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                me.tuanzi.network.scavenger.ScavengerProfileSyncS2CPacket.TYPE,
                me.tuanzi.network.scavenger.ScavengerProfileSyncS2CPacket.CODEC
        );

        // 注册拾荒回廊战术网格与升级 C2S 接收器
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> me.tuanzi.network.scavenger.ScavengerServerPacketHandler.handleGridAction(context.player(), payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerPrioritizeSearchC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> me.tuanzi.network.scavenger.ScavengerServerPacketHandler.handlePrioritizeSearch(context.player(), payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerBaseUpgradeC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> me.tuanzi.network.scavenger.ScavengerServerPacketHandler.handleBaseUpgrade(context.player(), payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.OpenCorridorLoadoutC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.level().dimension() == me.tuanzi.init.ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
                    player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (containerId, playerInv, p) -> new me.tuanzi.world.inventory.scavenger.CorridorTacticalLoadoutMenu(containerId, playerInv),
                            net.minecraft.network.chat.Component.translatable("gui.tuanzis_mod.scavenger_loadout")
                    ));
                    me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);
                }
            });
        });

        // 注册回廊信标使用 C2S 接收器
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.CorridorBeaconUsePacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                me.tuanzi.world.scavenger.ScavengerManager.handleBeaconUsePacket(context.player(), payload.slot());
            });
        });

        // 注册 C2S 接收器
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.NodachiBeatPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.getMainHandItem().getItem() instanceof me.tuanzi.item.NodachiItem) {
                    if (player instanceof me.tuanzi.util.NodachiPlayerTracker tracker) {
                        if (payload.onBeat()) {
                            // 给予 6 ticks (约 0.3 秒) 宽限期，覆盖本次攻击命中结算
                            tracker.tuanzis_mod$setNodachiRhythmGraceTicks(6);
                            me.tuanzi.util.ModLog.debug(player, null, "【野太刀·QTE网络包】收到客户端节奏窗口合拍信号，赋予 6 刻宽限期。");
                        } else {
                            // 不在节奏窗口内，显式清空宽限期
                            tracker.tuanzis_mod$setNodachiRhythmGraceTicks(0);
                            me.tuanzi.util.ModLog.debug(player, null, "【野太刀·QTE网络包】收到客户端非节奏窗口出刀信号，清空宽限期。");
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.NodachiMissPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.getMainHandItem().getItem() instanceof me.tuanzi.item.NodachiItem) {
                    if (player instanceof me.tuanzi.util.NodachiPlayerTracker tracker) {
                        tracker.tuanzis_mod$triggerNodachiExhaustion("客户端挥空同步 (未命中任何实体或方块)");
                    }
                }
            });
        });

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

        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.ChromaticSkullRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                String name = payload.playerName().trim();
                if (name.length() < 3 || name.length() > 16 || !name.matches("^[a-zA-Z0-9_]+$")) {
                    me.tuanzi.util.ModLog.debug(player, null, "彩色变化头颅输入玩家名非法: " + name);
                    return;
                }
                net.minecraft.world.InteractionHand hand = payload.isMainHand() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
                ItemStack heldStack = player.getItemInHand(hand);
                if (heldStack.is(me.tuanzi.init.ModItems.CHROMATIC_SKULL)) {
                    heldStack.shrink(1);
                    ItemStack playerHead = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
                    playerHead.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(name));
                    if (!player.getInventory().add(playerHead)) {
                        player.drop(playerHead, false, Prediction.SERVER_ONLY);
                    }
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
                    me.tuanzi.util.ModLog.debug(player, null, "彩色变化头颅转换成功，获得正版玩家头颅: " + name);
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
        net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "crafting_axe_strip"),
                me.tuanzi.world.item.crafting.AxeStripRecipe.SERIALIZER
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

        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.BlockBreakerToggleModePacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) <= 64.0) {
                    if (player.level().getBlockEntity(payload.pos()) instanceof me.tuanzi.block.entity.BlockBreakerBlockEntity breakerBe) {
                        breakerBe.toggleMode();
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.BlockPlacerActionPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) <= 64.0) {
                    if (player.level().getBlockEntity(payload.pos()) instanceof me.tuanzi.block.entity.BlockPlacerBlockEntity placerBe) {
                        placerBe.handleGuiAction(payload.action());
                    }
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
                    player.containerMenu.broadcastChanges();
                    me.tuanzi.util.ModLog.debug(player, null, "收到虚影变换：offset=" + payload.offset() + ", rotation=" + payload.rotation() + ", mirrored=" + payload.mirrored());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.GhostPlaceStructurePacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (!player.canUseGameMasterBlocks()) {
                    ModLog.warn("玩家 " + player.getName().getString() + " 尝试无权限直接放置蓝图建筑，已拦截");
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.blueprint.no_permission")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }

                CompoundTag blueprintTag = null;
                if (payload.useHeldBlueprint()) {
                    ItemStack stack = player.getMainHandItem();
                    if (!stack.is(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT)) {
                        stack = player.getOffhandItem();
                    }
                    if (stack.is(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT)) {
                        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                        if (customData != null) {
                            blueprintTag = customData.copyTag();
                        }
                    }
                } else if (payload.blueprintData() != null && !payload.blueprintData().isEmpty()) {
                    blueprintTag = payload.blueprintData();
                }

                if (blueprintTag == null || !blueprintTag.contains("Width")) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.blueprint.invalid")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }

                int w = blueprintTag.getIntOr("Width", 0);
                int h = blueprintTag.getIntOr("Height", 0);
                int l = blueprintTag.getIntOr("Length", 0);
                long volume = (long) w * h * l;
                if (w <= 0 || h <= 0 || l <= 0 || volume > 524288L) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.blueprint.invalid")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }

                me.tuanzi.block.cannon.BlueprintDirectPlacer.PlaceResult result =
                        me.tuanzi.block.cannon.BlueprintDirectPlacer.placeBlueprint(
                                player.level(),
                                blueprintTag,
                                payload.basePos(),
                                payload.offset(),
                                payload.rotation(),
                                payload.mirrored()
                        );
                ModLog.debug(player, null, "直接放置蓝图建筑完成：basePos=" + payload.basePos()
                        + ", offset=" + payload.offset()
                        + ", rot=" + payload.rotation()
                        + ", mir=" + payload.mirrored()
                        + ", placedBlocks=" + result.placedBlocks()
                        + ", clearedAir=" + result.clearedAirBlocks()
                        + ", spawnedEntities=" + result.spawnedEntities());
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.tuanzis_mod.blueprint.direct_placed",
                        result.placedBlocks(),
                        result.spawnedEntities()
                ).withStyle(net.minecraft.ChatFormatting.GREEN));
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
                me.tuanzi.block.entity.BlueprintTableBlockEntity tableBe = null;
                if (player.containerMenu instanceof me.tuanzi.world.inventory.BlueprintTableMenu tableMenu && tableMenu.stillValid(player)) {
                    if (tableMenu.getContainer() instanceof me.tuanzi.block.entity.BlueprintTableBlockEntity menuBe) {
                        tableBe = menuBe;
                    }
                }
                if (tableBe == null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(payload.tablePos());
                    if (be instanceof me.tuanzi.block.entity.BlueprintTableBlockEntity posBe && posBe.stillValid(player)) {
                        tableBe = posBe;
                    }
                }
                if (tableBe == null) {
                    me.tuanzi.util.ModLog.debug(player, null, "蓝图桌导入被忽略：未找到有效的蓝图桌容器, packetPos=" + payload.tablePos());
                    return;
                }
                CompoundTag blueprintData = payload.blueprintData();
                if (blueprintData == null || !blueprintData.contains("Width")) {
                    me.tuanzi.util.ModLog.debug(player, null, "蓝图桌导入/更新失败：蓝图数据无效");
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.blueprint_table.parse_fail").withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
                if (payload.updateExportSlot()) {
                    ItemStack slot0 = tableBe.getItems().get(0);
                    if (slot0.is(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT)) {
                        slot0.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(blueprintData.copy()));
                        tableBe.setChanged();
                        player.containerMenu.broadcastChanges();
                        me.tuanzi.util.ModLog.debug(player, null, "成功同步更新蓝图桌槽位 0 结构蓝图数据！");
                    } else {
                        me.tuanzi.util.ModLog.debug(player, null, "蓝图桌槽位 0 更新被忽略：槽位 0 不是结构蓝图");
                    }
                    return;
                }
                net.minecraft.world.item.ItemStack inputStack = tableBe.getItems().get(1);
                if (inputStack.is(me.tuanzi.init.ModItems.BLANK_BLUEPRINT)) {
                    inputStack.shrink(1);
                    ItemStack result = new ItemStack(me.tuanzi.init.ModItems.STRUCTURE_BLUEPRINT);
                    result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(blueprintData));

                    ItemStack slot0 = tableBe.getItems().get(0);
                    if (slot0.isEmpty()) {
                        tableBe.getItems().set(0, result);
                    } else {
                        if (!player.getInventory().add(result)) {
                            player.drop(result, false, Prediction.SERVER_ONLY);
                        }
                    }
                    tableBe.setChanged();
                    player.containerMenu.broadcastChanges();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.blueprint_table.import_success").withStyle(net.minecraft.ChatFormatting.GREEN));
                    me.tuanzi.util.ModLog.debug(player, null, "成功导入结构数据写入结构蓝图！");
                } else {
                    me.tuanzi.util.ModLog.debug(player, null, "蓝图桌导入被忽略：槽位 1 不是空白蓝图");
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

        // 注册二段跳网络包接收器
        ServerPlayNetworking.registerGlobalReceiver(me.tuanzi.network.DoubleJumpC2SPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                me.tuanzi.util.DoubleJumpHandler.handleServerDoubleJump(context.player());
            });
        });

        ModEnchantments.initialize();

        // 允许鞘翅在附魔台中直接附魔（添加 ENCHANTABLE 数据组件）
        net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(net.minecraft.world.item.Items.ELYTRA, builder -> {
                builder.set(net.minecraft.core.component.DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(1));
            });
        });

        // 注册击杀生物获得血怒状态效果的监听与背水一战创伤消减
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killedEntity, damageSource) -> {
            if (killer instanceof Player player) {
                me.tuanzi.util.DesperateStandHelper.onPlayerKilledEntity(player, killedEntity);
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
        me.tuanzi.util.TempBlockManager.initialize();
        me.tuanzi.init.ModSounds.initialize();

        // 初始化抽卡保底与卡池系统生命周期事件
        me.tuanzi.gacha.PlayerGachaManager.initialize();
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            me.tuanzi.gacha.PlayerGachaManager.onPlayerDisconnect(handler.getPlayer().getUUID());
            me.tuanzi.world.scavenger.loot.search.SearchSessionManager.closeSession(handler.getPlayer().getUUID(), "玩家断开连接");
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeServer = server;
            me.tuanzi.gacha.PoolManager.loadAllPools(server.registryAccess());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            me.tuanzi.gacha.PlayerGachaManager.shutdown();
            me.tuanzi.gacha.PoolManager.shutdown(server.registryAccess());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (activeServer == server) {
                activeServer = null;
            }
        });

        // 初始化拾荒回廊搜刮元数据加载、/reload 热更新广播与进服同步
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server;
            me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.reloadServerData(server.getResourceManager());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.reloadServerData(resourceManager);
                me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.syncToAllPlayers(server);
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    if (online instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                        tracker.tuanzis_mod$getCorridorProfile().syncEquipmentAndStashDimensionsWithConfig();
                    }
                }
            }
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            me.tuanzi.config.OverworldBuildHeightControl.syncToPlayer(handler.getPlayer());
            me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.syncToPlayer(handler.getPlayer());
            if (handler.getPlayer() instanceof me.tuanzi.util.DesperateStandTracker tracker) {
                me.tuanzi.util.DesperateStandHelper.syncToClient(handler.getPlayer(), tracker.tuanzis_mod$getDesperateStandTrauma());
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            me.tuanzi.world.scavenger.loot.search.SearchSessionManager.tickAllSessions(server);
        });

        // 注册抽卡系统指令
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            me.tuanzi.gacha.GachaCommands.register(dispatcher);
            me.tuanzi.command.BlueprintCommand.register(dispatcher);
            me.tuanzi.command.CorridorCommand.register(dispatcher);
            me.tuanzi.command.WitherKillsCommand.register(dispatcher);
            me.tuanzi.command.ModConfigCommand.register(dispatcher);
        });

        // 初始化并入的 MineVegas Casino 模块（游戏机器、赌场装饰与赌场音响）
        new fr.minevegas.casino.fabric.MineVegasFabric().onInitialize();
        ModLog.debug("MineVegas Casino module initialized.");

        ModLog.debug("Mod initialization started in development mode!");
    }
}
