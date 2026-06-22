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


public class TuanzisModClient implements ClientModInitializer {
    private static KeyMapping chainMiningKey;
    private static boolean lastHoldingState = false;
    private static KeyMapping.Category tuanzisModCategory;

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
        });
    }

    /**
     * 判断当前客户端玩家是否正在按住连锁采掘激活键
     */
    public static boolean isHoldingChainMiningKey() {
        return chainMiningKey != null && chainMiningKey.isDown();
    }
}
