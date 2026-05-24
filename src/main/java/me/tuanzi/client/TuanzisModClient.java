package me.tuanzi.client;

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
