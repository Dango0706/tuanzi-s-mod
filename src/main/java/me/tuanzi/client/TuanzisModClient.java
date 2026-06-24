package me.tuanzi.client;

import me.tuanzi.client.update.UpdateChecker;
import me.tuanzi.network.ChainMiningKeyPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

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

        tuanzisModCategory = KeyMapping.Category.register(Identifier.withDefaultNamespace("tuanzis_mod"));

        chainMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.chain_mining",
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            tuanzisModCategory
        ));

        toggleGhostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_toggle_ghost",
            GLFW.GLFW_KEY_V,
            tuanzisModCategory
        ));

        openControllerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_open_controller",
            GLFW.GLFW_KEY_G,
            tuanzisModCategory
        ));

        rotateGhostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_rotate",
            GLFW.GLFW_KEY_R,
            tuanzisModCategory
        ));

        mirrorGhostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_mirror",
            GLFW.GLFW_KEY_F,
            tuanzisModCategory
        ));

        moveNorthKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_move_north",
            GLFW.GLFW_KEY_UP,
            tuanzisModCategory
        ));

        moveSouthKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_move_south",
            GLFW.GLFW_KEY_DOWN,
            tuanzisModCategory
        ));

        moveWestKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_move_west",
            GLFW.GLFW_KEY_LEFT,
            tuanzisModCategory
        ));

        moveEastKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_move_east",
            GLFW.GLFW_KEY_RIGHT,
            tuanzisModCategory
        ));

        moveUpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_move_up",
            GLFW.GLFW_KEY_PAGE_UP,
            tuanzisModCategory
        ));

        moveDownKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tuanzis_mod.blueprint_move_down",
            GLFW.GLFW_KEY_PAGE_DOWN,
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

        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION_BLOCK_ENTITY,
            me.tuanzi.client.renderer.SoulMerchantStationBlockEntityRenderer::new
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

        // 重写客户端的 DecoyEntity 构造器，实例化 ClientDecoyEntity
        me.tuanzi.entity.DecoyEntity.constructor = (type, level) -> 
            new me.tuanzi.client.entity.ClientDecoyEntity(level, me.tuanzi.client.renderer.DecoyRenderer.skinRenderCache);

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

            // 手持结构蓝图且无界面时的快捷键控制
            if (client.gui.screen() == null) {
                ItemStack mainHand = client.player.getMainHandItem();
                ItemStack offHand = client.player.getOffhandItem();
                ItemStack targetStack = ItemStack.EMPTY;

                if (mainHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
                    targetStack = mainHand;
                } else if (offHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
                    targetStack = offHand;
                }

                boolean vPressed = toggleGhostKey.isDown();
                boolean gPressed = openControllerKey.isDown();
                boolean rPressed = rotateGhostKey.isDown();
                boolean fPressed = mirrorGhostKey.isDown();
                boolean upPressed = moveNorthKey.isDown();
                boolean downPressed = moveSouthKey.isDown();
                boolean leftPressed = moveWestKey.isDown();
                boolean rightPressed = moveEastKey.isDown();
                boolean pgUpPressed = moveUpKey.isDown();
                boolean pgDnPressed = moveDownKey.isDown();
                long window = client.getWindow().handle();
                boolean ctrlPressed = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS ||
                                      org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

                if (!targetStack.isEmpty()) {
                    net.minecraft.world.item.component.CustomData customData = targetStack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        if (tag.getBooleanOr("GhostPlaced", false)) {
                            int ox = tag.getIntOr("OffsetX", 0);
                            int oy = tag.getIntOr("OffsetY", 0);
                            int oz = tag.getIntOr("OffsetZ", 0);
                            int rot = tag.getIntOr("Rotation", 0);
                            boolean mir = tag.getBooleanOr("Mirrored", false);

                            boolean changed = false;

                            if (vPressed && !lastVState) {
                                ClientGhostManager.toggleVisibility();
                            }
                            if (gPressed && !lastGState) {
                                client.setScreenAndShow(new me.tuanzi.client.gui.screens.inventory.GhostControllerScreen());
                            }
                            if (rPressed && !lastRState) {
                                rot = (rot + 1) % 4;
                                changed = true;
                            }
                            if (fPressed && !lastFState) {
                                mir = !mir;
                                changed = true;
                            }
                            if (ctrlPressed) {
                                if (upPressed && !lastUpState) {
                                    oz -= 1;
                                    changed = true;
                                }
                                if (downPressed && !lastDownState) {
                                    oz += 1;
                                    changed = true;
                                }
                                if (leftPressed && !lastLeftState) {
                                    ox -= 1;
                                    changed = true;
                                }
                                if (rightPressed && !lastRightState) {
                                    ox += 1;
                                    changed = true;
                                }
                                if (pgUpPressed && !lastPgUpState) {
                                    oy += 1;
                                    changed = true;
                                }
                                if (pgDnPressed && !lastPgDnState) {
                                    oy -= 1;
                                    changed = true;
                                }
                            }

                            if (changed) {
                                ClientPlayNetworking.send(new me.tuanzi.network.GhostTransformPacket(new BlockPos(ox, oy, oz), rot, mir));
                            }
                        }
                    }
                }

                lastVState = vPressed;
                lastGState = gPressed;
                lastRState = rPressed;
                lastFState = fPressed;
                lastUpState = upPressed;
                lastDownState = downPressed;
                lastLeftState = leftPressed;
                lastRightState = rightPressed;
                lastPgUpState = pgUpPressed;
                lastPgDnState = pgDnPressed;
            }
        });

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
    }

    /**
     * 判断当前客户端玩家是否正在按住连锁采掘激活键
     */
    public static boolean isHoldingChainMiningKey() {
        return chainMiningKey != null && chainMiningKey.isDown();
    }
}
