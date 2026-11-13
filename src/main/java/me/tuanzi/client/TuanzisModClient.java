package me.tuanzi.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.tuanzi.client.update.UpdateChecker;
import me.tuanzi.network.ChainMiningKeyPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class TuanzisModClient implements ClientModInitializer {
    private static KeyMapping chainMiningKey;
    private static boolean lastHoldingState = false;
    private static KeyMapping.Category tuanzisModCategory;

    private static KeyMapping toggleGhostKey;
    private static KeyMapping openControllerKey;
    private static KeyMapping rotateGhostKey;
    private static KeyMapping mirrorGhostKey;
    private static KeyMapping moveNorthKey;
    private static KeyMapping moveSouthKey;
    private static KeyMapping moveWestKey;
    private static KeyMapping moveEastKey;
    private static KeyMapping moveUpKey;
    private static KeyMapping moveDownKey;
    private static KeyMapping useHallBeaconKey;
    private static KeyMapping useRoomBeaconKey;
    private static KeyMapping useMarketBeaconKey;

    private static boolean lastVState = false;
    private static boolean lastGState = false;
    private static boolean lastRState = false;
    private static boolean lastFState = false;
    private static boolean lastUpState = false;
    private static boolean lastDownState = false;
    private static boolean lastLeftState = false;
    private static boolean lastRightState = false;
    private static boolean lastPgUpState = false;
    private static boolean lastPgDnState = false;

    /**
     * 判断当前客户端玩家是否正在按住连锁采掘激活键
     */
    public static boolean isHoldingChainMiningKey() {
        return chainMiningKey != null && chainMiningKey.isDown();
    }

    public static boolean isOpenControllerKey(net.minecraft.client.input.KeyEvent event) {
        return openControllerKey != null && openControllerKey.matches(event);
    }

    private static boolean consumeKeyTrigger(KeyMapping keyMapping, boolean prevDown) {
        if (keyMapping == null) return false;
        boolean clicked = false;
        while (keyMapping.consumeClick()) {
            clicked = true;
        }
        return clicked || (keyMapping.isDown() && !prevDown);
    }

    private static void drainClicks(KeyMapping keyMapping) {
        if (keyMapping == null) return;
        while (keyMapping.consumeClick()) {
            // 清空屏幕开启期间残留的点击计数
        }
    }

    @Override
    public void onInitializeClient() {
        // 启动异步更新检查
        UpdateChecker.checkUpdate();

        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
                me.tuanzi.client.renderer.ScarecrowModel.SCARECROW_LAYER,
                me.tuanzi.client.renderer.ScarecrowModel::createBodyLayer
        );

        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
                me.tuanzi.client.renderer.TrialDummyModel.TRIAL_DUMMY_LAYER,
                me.tuanzi.client.renderer.TrialDummyModel::createBodyLayer
        );

        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
                me.tuanzi.client.renderer.model.BlueprintCannonModel.LAYER_LOCATION,
                me.tuanzi.client.renderer.model.BlueprintCannonModel::createBodyLayer
        );

        tuanzisModCategory = KeyMapping.Category.register(Identifier.withDefaultNamespace("tuanzis_mod"));

        chainMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.chain_mining",
                InputConstants.KEY_GRAVE,
                tuanzisModCategory
        ));

        toggleGhostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_toggle_ghost",
                InputConstants.KEY_V,
                tuanzisModCategory
        ));

        openControllerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_open_controller",
                InputConstants.KEY_G,
                tuanzisModCategory
        ));

        rotateGhostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_rotate",
                InputConstants.KEY_R,
                tuanzisModCategory
        ));

        mirrorGhostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_mirror",
                InputConstants.KEY_F,
                tuanzisModCategory
        ));

        moveNorthKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_move_north",
                InputConstants.KEY_UP,
                tuanzisModCategory
        ));

        moveSouthKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_move_south",
                InputConstants.KEY_DOWN,
                tuanzisModCategory
        ));

        moveWestKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_move_west",
                InputConstants.KEY_LEFT,
                tuanzisModCategory
        ));

        moveEastKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_move_east",
                InputConstants.KEY_RIGHT,
                tuanzisModCategory
        ));

        moveUpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_move_up",
                InputConstants.KEY_PAGEUP,
                tuanzisModCategory
        ));

        moveDownKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.blueprint_move_down",
                InputConstants.KEY_PAGEDOWN,
                tuanzisModCategory
        ));

        useHallBeaconKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.use_hall_beacon",
                InputConstants.KEY_H,
                tuanzisModCategory
        ));

        useRoomBeaconKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.use_room_beacon",
                InputConstants.KEY_R,
                tuanzisModCategory
        ));

        useMarketBeaconKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuanzis_mod.use_market_beacon",
                InputConstants.KEY_M,
                tuanzisModCategory
        ));

        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.DECOY,
                context -> (net.minecraft.client.renderer.entity.EntityRenderer) new me.tuanzi.client.renderer.DecoyRenderer(context)
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.SCARECROW,
                context -> new me.tuanzi.client.renderer.ScarecrowRenderer(context)
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.TRIAL_DUMMY,
                context -> new me.tuanzi.client.renderer.TrialDummyRenderer(context)
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.SEAT,
                net.minecraft.client.renderer.entity.NoopRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.SHURIKEN,
                context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context)
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.GLASS_ITEM_FRAME,
                net.minecraft.client.renderer.entity.ItemFrameRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.CRESCENT_AURA,
                net.minecraft.client.renderer.entity.NoopRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.CINDER_SHIELD_PART,
                me.tuanzi.client.renderer.CinderShieldRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.CINDER_BLAZE_ROD_PART,
                me.tuanzi.client.renderer.CinderBlazeRodRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.CINDER_FIREBALL,
                me.tuanzi.client.renderer.CinderFireballRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.CINDER_METEOR,
                me.tuanzi.client.renderer.CinderMeteorRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                me.tuanzi.init.ModEntities.CINDER_SOVEREIGN,
                me.tuanzi.client.renderer.CinderSovereignRenderer::new
        );

        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION_BLOCK_ENTITY,
                me.tuanzi.client.renderer.SoulMerchantStationBlockEntityRenderer::new
        );

        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                me.tuanzi.init.ModBlocks.BLACKBOX_CORE_CASE_BLOCK_ENTITY,
                me.tuanzi.client.renderer.BlackboxCoreCaseRenderer::new
        );

        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                me.tuanzi.init.ModBlocks.BLUEPRINT_CANNON_BLOCK_ENTITY,
                me.tuanzi.client.renderer.BlueprintCannonRenderer::new
        );

        // 注册旅者手札存储界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.TRAVELERS_NOTEBOOK,
                me.tuanzi.client.gui.screens.inventory.TravelersNotebookScreen::new
        );

        // 注册工匠护符界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.CRAFTSMAN_CHARM,
                me.tuanzi.client.gui.screens.inventory.CraftsmanCharmScreen::new
        );

        // 注册蓝图大炮界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.BLUEPRINT_CANNON,
                me.tuanzi.client.gui.screens.inventory.BlueprintCannonScreen::new
        );

        // 注册蓝图桌界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.BLUEPRINT_TABLE,
                me.tuanzi.client.gui.screens.inventory.BlueprintTableScreen::new
        );

        // 注册玩家模拟器界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.PLAYER_SIMULATOR,
                me.tuanzi.client.gui.screens.inventory.PlayerSimulatorScreen::new
        );

        // 注册方块破坏器界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.BLOCK_BREAKER,
                me.tuanzi.client.gui.screens.inventory.BlockBreakerScreen::new
        );

        // 注册方块放置器界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.BLOCK_PLACER,
                me.tuanzi.client.gui.screens.inventory.BlockPlacerScreen::new
        );

        // 注册黄金口袋界面 Screen
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.GOLDEN_POCKET,
                me.tuanzi.client.gui.screens.inventory.GoldenPocketScreen::new
        );

        // 注册拾荒回廊战术界面 Screens
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.CORRIDOR_TACTICAL_LOADOUT,
                me.tuanzi.client.gui.screens.scavenger.CorridorTacticalLoadoutScreen::new
        );
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.SCAVENGER_TACTICAL,
                me.tuanzi.client.gui.screens.scavenger.ScavengerTacticalScreen::new
        );
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.BASE_STASH,
                me.tuanzi.client.gui.screens.scavenger.BaseStashScreen::new
        );
        net.minecraft.client.gui.screens.MenuScreens.register(
                me.tuanzi.init.ModMenuTypes.BASE_UPGRADE_STATION,
                me.tuanzi.client.gui.screens.scavenger.BaseUpgradeStationScreen::new
        );

        // 注册蜂刺余响自定义 TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "bee_sting_echo"),
                me.tuanzi.client.BeeStingEchoTintSource.MAP_CODEC
        );

        // 注册裂虚之痕自定义 TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "rift_scar"),
                me.tuanzi.client.RiftScarTintSource.MAP_CODEC
        );

        // 注册幽匿裂片自定义 TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "scully_shard"),
                me.tuanzi.client.ScullyShardTintSource.MAP_CODEC
        );

        // 注册钢御壁垒自定义 TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "steel_barrier"),
                me.tuanzi.client.SteelBarrierTintSource.MAP_CODEC
        );

        // 注册潮汐切割者自定义 Wear TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "tide_cleaver_wear"),
                me.tuanzi.client.TideCleaverWearTintSource.MAP_CODEC
        );

        // 注册潮汐切割者自定义 Energy TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "tide_cleaver_energy"),
                me.tuanzi.client.TideCleaverEnergyTintSource.MAP_CODEC
        );

        // 注册脉冲谐振剑自定义 TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "pulse_resonance_sword"),
                me.tuanzi.client.PulseResonanceSwordTintSource.MAP_CODEC
        );

        // 注册星隙裁刃自定义 TintSource
        net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("tuanzis_mod", "star_rift_blade"),
                me.tuanzi.client.StarRiftBladeTintSource.MAP_CODEC
        );

        // 为附魔了“过载协议”的普通武器添加脉冲层数 Tooltip 显示
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (!(stack.getItem() instanceof me.tuanzi.item.PulseResonanceSwordItem)) {
                var enchantments = stack.getEnchantments();
                if (enchantments != null && !enchantments.isEmpty()) {
                    boolean hasOverload = false;
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(me.tuanzi.init.ModEnchantments.OVERLOAD_PROTOCOL)) {
                            hasOverload = true;
                            break;
                        }
                    }
                    if (hasOverload) {
                        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                        int pulse = 0;
                        if (customData != null) {
                            pulse = customData.copyTag().getIntOr("PulseLevel", 0);
                        }
                        String color = pulse >= 3 ? "§c" : "§b";
                        lines.add(net.minecraft.network.chat.Component.translatable("item.tuanzis_mod.pulse_resonance_sword.pulse_info", color + pulse));
                    }
                }
            }
        });

        // 为附魔了“古卷附魔-屹立不倒”的装备添加高级提示框 (F3+H) 动态 Lore 显示
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (tooltipType.isAdvanced()) {
                int standingFirmLevel = 0;
                var enchantments = stack.getEnchantments();
                if (enchantments != null && !enchantments.isEmpty()) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(me.tuanzi.init.ModEnchantments.ANCIENT_SCROLL_STANDING_FIRM)) {
                            standingFirmLevel = entry.getIntValue();
                            break;
                        }
                    }
                }
                if (standingFirmLevel <= 0) {
                    net.minecraft.world.item.enchantment.ItemEnchantments stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
                    if (stored != null && !stored.isEmpty()) {
                        for (var entry : stored.entrySet()) {
                            if (entry.getKey().is(me.tuanzi.init.ModEnchantments.ANCIENT_SCROLL_STANDING_FIRM)) {
                                standingFirmLevel = entry.getIntValue();
                                break;
                            }
                        }
                    }
                }

                if (standingFirmLevel > 0) {
                    float chance = Math.min(100.0f, standingFirmLevel * 20.0f);
                    lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.ancient_scroll_standing_firm.advanced_header"));
                    lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.ancient_scroll_standing_firm.block_chance", String.format("%.1f", chance)));

                    var clientPlayer = net.minecraft.client.Minecraft.getInstance().player;
                    int totalExp = clientPlayer != null ? me.tuanzi.util.DamageCalculator.getTotalExperiencePoints(clientPlayer) : 0;
                    float maxBlock = me.tuanzi.util.DamageCalculator.calcMaxBlockableDamage(totalExp, standingFirmLevel);
                    if (maxBlock > 0.0f) {
                        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.ancient_scroll_standing_firm.block_damage", String.format("%.2f", maxBlock)));
                    } else {
                        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.ancient_scroll_standing_firm.insufficient_xp"));
                    }
                }

                int seasonedLevel = 0;
                if (enchantments != null && !enchantments.isEmpty()) {
                    for (var entry : enchantments.entrySet()) {
                        if (entry.getKey().is(me.tuanzi.init.ModEnchantments.ANCIENT_SCROLL_SEASONED)) {
                            seasonedLevel = entry.getIntValue();
                            break;
                        }
                    }
                }
                if (seasonedLevel <= 0) {
                    net.minecraft.world.item.enchantment.ItemEnchantments stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
                    if (stored != null && !stored.isEmpty()) {
                        for (var entry : stored.entrySet()) {
                            if (entry.getKey().is(me.tuanzi.init.ModEnchantments.ANCIENT_SCROLL_SEASONED)) {
                                seasonedLevel = entry.getIntValue();
                                break;
                            }
                        }
                    }
                }

                if (seasonedLevel > 0) {
                    float bonusPercent = me.tuanzi.util.ExperiencedEnchantmentHelper.getBonusRate(seasonedLevel) * 100.0f;
                    lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.ancient_scroll_seasoned.advanced_header"));
                    lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tuanzis_mod.ancient_scroll_seasoned.boost", String.format("%.1f", bonusPercent)));
                }
            }
        });

        // 注册 S2C 打开手札书本界面接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.OpenBookScreenS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                net.minecraft.world.InteractionHand hand = payload.isMainHand() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
                context.client().setScreenAndShow(new me.tuanzi.client.gui.screens.TravelersNotebookBookScreen(hand));
            });
        });

        // 在 Minecraft 1.26.1 中，方块半透明/透明渲染类型已完全交由模型 JSON 中的 "render_type" 属性原生声明，无需在 Java 代码中手动注册渲染层映射，此处保持纯净。

        // 注册 S2C 伤害数字同步接收器


        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.TrialDummyDamagePacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level != null) {
                    net.minecraft.world.entity.Entity entity = context.client().level.getEntity(payload.entityId());
                    if (entity instanceof me.tuanzi.entity.TrialDummyEntity dummy) {
                        dummy.clientDamageTexts.add(new me.tuanzi.entity.DamageText(payload.damage(), System.currentTimeMillis()));
                    }
                }
            });
        });

        // 注册野太刀势槽同步包接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.NodachiSyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player instanceof me.tuanzi.util.NodachiPlayerTracker tracker) {
                    tracker.tuanzis_mod$setNodachiMomentum(payload.momentum());
                    tracker.tuanzis_mod$setNodachiExhaustionTicks(payload.exhaustionTicks());
                    tracker.tuanzis_mod$setNodachiInactiveTicks(0);
                }
            });
        });

        // 注册日冕过热状态同步接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.CinderHeatSyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                me.tuanzi.client.gui.CinderHeatHud.update(payload.heat(), payload.inArena());
            });
        });

        // 注册拾荒回廊队伍状态同步接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.ScavengerTeamSyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                me.tuanzi.client.ScavengerTeamClient.updateTeam(payload.inTeam(), payload.colorRgb(), payload.memberUuids());
            });
        });

        // 注册拾荒回廊专属饰品槽同步接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.CorridorAccessorySyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                me.tuanzi.client.CorridorAccessoryClientState.setHallStack(payload.hallStack());
                me.tuanzi.client.CorridorAccessoryClientState.setRoomStack(payload.roomStack());
                me.tuanzi.client.CorridorAccessoryClientState.setMarketStack(payload.marketStack());
            });
        });

        // 注册拾荒回廊搜刮元数据热重载 S2C 接收器与断线清理、Tooltip 注入
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerDataSyncS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.applyClientSync(payload);
            });
        });

        // 注册拾荒回廊装备档案、容器快照、搜索进度与条目揭示 S2C 接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerProfileSyncS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> me.tuanzi.client.scavenger.ScavengerClientState.updateProfile(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerContainerSyncS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> me.tuanzi.client.scavenger.ScavengerClientState.updateContainer(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerSearchProgressS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> me.tuanzi.client.scavenger.ScavengerClientState.updateSearchProgress(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.scavenger.ScavengerItemRevealedS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> me.tuanzi.client.scavenger.ScavengerClientState.updateItemRevealed(payload));
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.clearClientSync();
            me.tuanzi.client.scavenger.ScavengerClientState.clearContainer();
        });
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            var clientPlayer = Minecraft.getInstance().player;
            boolean inCorridor = clientPlayer != null
                    && clientPlayer.level().dimension().equals(me.tuanzi.init.ModDimensions.SCAVENGER_CORRIDOR_LEVEL);
            me.tuanzi.world.scavenger.loot.data.ScavengerTooltipHelper.appendTooltipLines(stack, inCorridor, lines);
        });

        // 注册蓝图大炮开火发射 S2C 接收器
        ClientPlayNetworking.registerGlobalReceiver(me.tuanzi.network.BlueprintCannonShootS2CPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level == null) return;
                net.minecraft.world.level.block.entity.BlockEntity be = context.client().level.getBlockEntity(payload.cannonPos());
                if (be instanceof me.tuanzi.block.entity.BlueprintCannonBlockEntity cannonBe) {
                    cannonBe.previousTarget = cannonBe.currentTarget;
                    cannonBe.currentTarget = payload.targetPos();
                    net.minecraft.world.level.block.state.BlockState state = net.minecraft.world.level.block.Block.stateById(payload.blockStateId());
                    cannonBe.flyingBlocks.add(new me.tuanzi.block.cannon.LaunchedItem.ForBlockState(
                            payload.cannonPos(), payload.targetPos(), net.minecraft.world.item.ItemStack.EMPTY, state, null, payload.isDestroy()
                    ));

                    // 精准从模型炮口出膛截面喷涌推进浓烟与火花粒子
                    double[] angles = me.tuanzi.client.renderer.BlueprintCannonRenderer.getCannonAngles(cannonBe, payload.cannonPos(), 0.0f);
                    double yaw = angles[0];
                    double pitch = angles[1];
                    net.minecraft.world.phys.Vec3 muzzle = me.tuanzi.client.renderer.BlueprintCannonRenderer.getMuzzlePosition(payload.cannonPos(), yaw, pitch, 6.0);
                    net.minecraft.world.phys.Vec3 dir = me.tuanzi.client.renderer.BlueprintCannonRenderer.getMuzzleDirection(yaw, pitch);

                    net.minecraft.util.RandomSource random = context.client().level.getRandom();
                    for (int i = 0; i < 16; i++) {
                        double speed = 0.15 + random.nextDouble() * 0.12;
                        double vx = dir.x * speed + (random.nextDouble() - 0.5) * 0.06;
                        double vy = dir.y * speed + (random.nextDouble() - 0.5) * 0.06;
                        double vz = dir.z * speed + (random.nextDouble() - 0.5) * 0.06;

                        // 浓厚推进白云烟雾
                        context.client().level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, vx, vy, vz);
                        // 膛口爆燃火花
                        if (i < 5) {
                            context.client().level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, vx * 0.6, vy * 0.6, vz * 0.6);
                        }
                        // 扩散黑烟
                        context.client().level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, vx * 0.4, vy * 0.4, vz * 0.4);
                    }
                }
            });
        });

        // 重写客户端的 DecoyEntity 构造器，实例化 ClientDecoyEntity
        me.tuanzi.entity.DecoyEntity.constructor = (type, level) ->
                new me.tuanzi.client.entity.ClientDecoyEntity(level, me.tuanzi.client.renderer.DecoyRenderer.skinRenderCache);

        // 注册彩色方块、半砖、楼梯的 BLOCK 颜色处理器
        // 注册方块和楼梯的第 0 层颜色
        net.minecraft.client.color.block.BlockTintSource defaultColorSource = new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
                if (level != null && pos != null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    me.tuanzi.util.ModLog.debug("[defaultColorSource] colorInWorld called at " + pos + ", be: " + (be == null ? "null" : be.getClass().getName()));
                    if (be instanceof me.tuanzi.block.entity.ColorBlockBlockEntity colorBe) {
                        int col = colorBe.getColor();
                        me.tuanzi.util.ModLog.debug("[defaultColorSource] Returning color: " + String.format("#%06X", col));
                        return col;
                    }
                }
                return 0xFFFFFF;
            }

            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                me.tuanzi.util.ModLog.debug("[defaultColorSource] color(state) called");
                return 0xFFFFFF;
            }
        };
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(
                java.util.List.of(defaultColorSource),
                me.tuanzi.init.ModBlocks.COLOR_BLOCK,
                me.tuanzi.init.ModBlocks.COLOR_STAIRS
        );

        // 注册半砖的第 0 层和第 1 层颜色
        net.minecraft.client.color.block.BlockTintSource slabBottomSource = new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
                if (level != null && pos != null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    me.tuanzi.util.ModLog.debug("[slabBottomSource] colorInWorld called at " + pos + ", be: " + (be == null ? "null" : be.getClass().getName()));
                    if (be instanceof me.tuanzi.block.entity.ColorSlabBlockEntity slabBe) {
                        int col = slabBe.getBottomColor();
                        me.tuanzi.util.ModLog.debug("[slabBottomSource] Returning color: " + String.format("#%06X", col));
                        return col;
                    }
                }
                return 0xFFFFFF;
            }

            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                me.tuanzi.util.ModLog.debug("[slabBottomSource] color(state) called");
                return 0xFFFFFF;
            }
        };

        net.minecraft.client.color.block.BlockTintSource slabTopSource = new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
                if (level != null && pos != null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    me.tuanzi.util.ModLog.debug("[slabTopSource] colorInWorld called at " + pos + ", be: " + (be == null ? "null" : be.getClass().getName()));
                    if (be instanceof me.tuanzi.block.entity.ColorSlabBlockEntity slabBe) {
                        int col = slabBe.getTopColor();
                        me.tuanzi.util.ModLog.debug("[slabTopSource] Returning color: " + String.format("#%06X", col));
                        return col;
                    }
                }
                return 0xFFFFFF;
            }

            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                me.tuanzi.util.ModLog.debug("[slabTopSource] color(state) called");
                return 0xFFFFFF;
            }
        };

        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(
                java.util.List.of(slabBottomSource, slabTopSource),
                me.tuanzi.init.ModBlocks.COLOR_SLAB
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                lastHoldingState = false;
                return;
            }
            boolean isHolding = chainMiningKey.isDown();
            if (isHolding != lastHoldingState) {
                lastHoldingState = isHolding;
                ClientPlayNetworking.send(new ChainMiningKeyPacket(isHolding));
            }

            // 执行虚影管理器 Tick
            ClientGhostManager.tick(client);
            EcholocationClientTracker.tick(client);

            // 拾荒回廊专属信标快捷键
            if (client.gui.screen() == null && client.player.level().dimension() == me.tuanzi.init.ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
                while (useHallBeaconKey.consumeClick()) {
                    ClientPlayNetworking.send(new me.tuanzi.network.CorridorBeaconUsePacket(me.tuanzi.world.scavenger.CorridorAccessoryContainer.SLOT_HALL));
                }
                while (useRoomBeaconKey.consumeClick()) {
                    ClientPlayNetworking.send(new me.tuanzi.network.CorridorBeaconUsePacket(me.tuanzi.world.scavenger.CorridorAccessoryContainer.SLOT_ROOM));
                }
                while (useMarketBeaconKey.consumeClick()) {
                    ClientPlayNetworking.send(new me.tuanzi.network.CorridorBeaconUsePacket(me.tuanzi.world.scavenger.CorridorAccessoryContainer.SLOT_MARKET));
                }
            }

            // 手持结构蓝图或存在激活虚影且无界面时的快捷键控制
            if (client.gui.screen() == null) {
                ItemStack mainHand = client.player.getMainHandItem();
                ItemStack offHand = client.player.getOffhandItem();
                ItemStack targetStack = ItemStack.EMPTY;

                if (mainHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
                    targetStack = mainHand;
                } else if (offHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
                    targetStack = offHand;
                }

                boolean vTriggered = consumeKeyTrigger(toggleGhostKey, lastVState);
                boolean gTriggered = consumeKeyTrigger(openControllerKey, lastGState);
                boolean rTriggered = consumeKeyTrigger(rotateGhostKey, lastRState);
                boolean fTriggered = consumeKeyTrigger(mirrorGhostKey, lastFState);
                boolean upTriggered = consumeKeyTrigger(moveNorthKey, lastUpState);
                boolean downTriggered = consumeKeyTrigger(moveSouthKey, lastDownState);
                boolean leftTriggered = consumeKeyTrigger(moveWestKey, lastLeftState);
                boolean rightTriggered = consumeKeyTrigger(moveEastKey, lastRightState);
                boolean pgUpTriggered = consumeKeyTrigger(moveUpKey, lastPgUpState);
                boolean pgDnTriggered = consumeKeyTrigger(moveDownKey, lastPgDnState);
                boolean ctrlPressed = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) ||
                        InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);

                boolean handledByHeldBlueprint = false;
                if (!targetStack.isEmpty()) {
                    net.minecraft.world.item.component.CustomData customData = targetStack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        if (tag.getBooleanOr("GhostPlaced", false)) {
                            handledByHeldBlueprint = true;
                            int ox = tag.getIntOr("OffsetX", 0);
                            int oy = tag.getIntOr("OffsetY", 0);
                            int oz = tag.getIntOr("OffsetZ", 0);
                            int rot = tag.getIntOr("Rotation", 0);
                            boolean mir = tag.getBooleanOr("Mirrored", false);

                            boolean changed = false;

                            if (vTriggered) {
                                ClientGhostManager.toggleVisibility();
                            }
                            if (gTriggered) {
                                client.setScreenAndShow(new me.tuanzi.client.gui.screens.inventory.GhostControllerScreen());
                            }
                            if (rTriggered) {
                                rot = (rot + 1) % 4;
                                changed = true;
                            }
                            if (fTriggered) {
                                mir = !mir;
                                changed = true;
                            }
                            if (ctrlPressed) {
                                if (upTriggered) {
                                    oz -= 1;
                                    changed = true;
                                }
                                if (downTriggered) {
                                    oz += 1;
                                    changed = true;
                                }
                                if (leftTriggered) {
                                    ox -= 1;
                                    changed = true;
                                }
                                if (rightTriggered) {
                                    ox += 1;
                                    changed = true;
                                }
                                if (pgUpTriggered) {
                                    oy += 1;
                                    changed = true;
                                }
                                if (pgDnTriggered) {
                                    oy -= 1;
                                    changed = true;
                                }
                            }

                            if (changed) {
                                BlockPos updatedOffset = new BlockPos(ox, oy, oz);
                                ClientGhostManager.updateTransform(updatedOffset, rot, mir);
                                ClientPlayNetworking.send(new me.tuanzi.network.GhostTransformPacket(updatedOffset, rot, mir));
                            }
                        }
                    }
                }
                if (!handledByHeldBlueprint && ClientGhostManager.hasActiveGhost()) {
                    // 未手持已放置蓝图但世界中常驻有激活的虚影投影时，支持全套快捷键开关、控制器呼出与姿态微调
                    if (vTriggered) {
                        ClientGhostManager.toggleVisibility();
                    }
                    if (gTriggered) {
                        client.setScreenAndShow(new me.tuanzi.client.gui.screens.inventory.GhostControllerScreen());
                    }

                    BlockPos curOff = ClientGhostManager.getOffset();
                    int ox = curOff.getX();
                    int oy = curOff.getY();
                    int oz = curOff.getZ();
                    int rot = ClientGhostManager.getRotationIdx();
                    boolean mir = ClientGhostManager.isMirrored();
                    boolean changed = false;

                    if (rTriggered) {
                        rot = (rot + 1) % 4;
                        changed = true;
                    }
                    if (fTriggered) {
                        mir = !mir;
                        changed = true;
                    }
                    if (ctrlPressed) {
                        if (upTriggered) {
                            oz -= 1;
                            changed = true;
                        }
                        if (downTriggered) {
                            oz += 1;
                            changed = true;
                        }
                        if (leftTriggered) {
                            ox -= 1;
                            changed = true;
                        }
                        if (rightTriggered) {
                            ox += 1;
                            changed = true;
                        }
                        if (pgUpTriggered) {
                            oy += 1;
                            changed = true;
                        }
                        if (pgDnTriggered) {
                            oy -= 1;
                            changed = true;
                        }
                    }
                    if (changed) {
                        ClientGhostManager.updateTransform(new BlockPos(ox, oy, oz), rot, mir);
                    }
                }

                lastVState = toggleGhostKey.isDown();
                lastGState = openControllerKey.isDown();
                lastRState = rotateGhostKey.isDown();
                lastFState = mirrorGhostKey.isDown();
                lastUpState = moveNorthKey.isDown();
                lastDownState = moveSouthKey.isDown();
                lastLeftState = moveWestKey.isDown();
                lastRightState = moveEastKey.isDown();
                lastPgUpState = moveUpKey.isDown();
                lastPgDnState = moveDownKey.isDown();
            } else {
                drainClicks(toggleGhostKey);
                drainClicks(openControllerKey);
                drainClicks(rotateGhostKey);
                drainClicks(mirrorGhostKey);
                drainClicks(moveNorthKey);
                drainClicks(moveSouthKey);
                drainClicks(moveWestKey);
                drainClicks(moveEastKey);
                drainClicks(moveUpKey);
                drainClicks(moveDownKey);
                lastVState = false;
                lastGState = false;
                lastRState = false;
                lastFState = false;
                lastUpState = false;
                lastDownState = false;
                lastLeftState = false;
                lastRightState = false;
                lastPgUpState = false;
                lastPgDnState = false;
            }
        });

        // 注册蓝图结构虚影半透明世界渲染
        LevelRenderEvents.COLLECT_SUBMITS.register(me.tuanzi.client.ClientGhostManager::renderWorldGhosts);

        // 注册塑世之笔起点红色边缘高亮渲染
        LevelRenderEvents.BEFORE_GIZMOS.register((LevelRenderContext context) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            // 遍历主副手寻找塑世之笔
            ItemStack penStack = ItemStack.EMPTY;
            if (mc.player.getMainHandItem().is(me.tuanzi.init.ModItems.WORLD_SCULPTORS_PEN)) {
                penStack = mc.player.getMainHandItem();
            } else if (mc.player.getOffhandItem().is(me.tuanzi.init.ModItems.WORLD_SCULPTORS_PEN)) {
                penStack = mc.player.getOffhandItem();
            }

            if (penStack.isEmpty()) return;

            // 读取起点数据
            CustomData customData = penStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return;

            CompoundTag tag = customData.copyTag();
            if (!tag.getInt("pos1_x").isPresent()) return;

            // 维度匹配校验
            String dimStr = tag.getStringOr("pos1_dim", "");
            if (!dimStr.equals(mc.level.dimension().identifier().toString())) return;

            int x = tag.getIntOr("pos1_x", 0);
            int y = tag.getIntOr("pos1_y", 0);
            int z = tag.getIntOr("pos1_z", 0);
            BlockPos pos1 = new BlockPos(x, y, z);

            // 直接使用 Mojang 官方 Gizmos API 提交外框线，支持 setAlwaysOnTop 完美实现透视高亮线框
            try {
                Gizmos.cuboid(
                        pos1,
                        0.002F,
                        GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F))
                ).setAlwaysOnTop();
            } catch (IllegalStateException e) {
                // 回退以防极端情况下没有 GizmoCollector
            }
        });

        // 注册原生背包中回廊专属饰品槽位的鼠标点击事件
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen invScreen) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.allowMouseClick(invScreen).register((scr, event) -> {
                    double mouseX = event.x();
                    double mouseY = event.y();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.player.level().dimension() != me.tuanzi.init.ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
                        return true;
                    }

                    int leftPos = (invScreen.width - 176) / 2;
                    int topPos = (invScreen.height - 166) / 2;
                    int xo = leftPos - 26;
                    int yo = topPos + 8;

                    if (mouseX >= xo && mouseX < xo + 22 && mouseY >= yo && mouseY < yo + 22) {
                        ClientPlayNetworking.send(new me.tuanzi.network.CorridorBeaconUsePacket(me.tuanzi.world.scavenger.CorridorAccessoryContainer.SLOT_HALL));
                        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return false;
                    }

                    int yo1 = yo + 26;
                    if (mouseX >= xo && mouseX < xo + 22 && mouseY >= yo1 && mouseY < yo1 + 22) {
                        ClientPlayNetworking.send(new me.tuanzi.network.CorridorBeaconUsePacket(me.tuanzi.world.scavenger.CorridorAccessoryContainer.SLOT_ROOM));
                        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return false;
                    }

                    int yo2 = yo + 52;
                    if (mouseX >= xo && mouseX < xo + 22 && mouseY >= yo2 && mouseY < yo2 + 22) {
                        ClientPlayNetworking.send(new me.tuanzi.network.CorridorBeaconUsePacket(me.tuanzi.world.scavenger.CorridorAccessoryContainer.SLOT_MARKET));
                        mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return false;
                    }
                    return true;
                });
            }
        });
    }
}
