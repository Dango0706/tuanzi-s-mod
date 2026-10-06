package me.tuanzi.mixin.client;

import me.tuanzi.item.NodachiItem;
import me.tuanzi.network.NodachiMissPacket;
import me.tuanzi.util.NodachiPlayerTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public HitResult hitResult;
    @Shadow private int missTime;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null) {
            if (this.missTime > 0 || mc.player.isHandsBusy()) {
                return;
            }

            ItemStack stack = mc.player.getMainHandItem();
            if (stack.is(me.tuanzi.init.ModItems.WORLD_SCULPTORS_PEN)) {
                if (mc.player.isSecondaryUseActive()) { // Shift + 左键
                    // 向服务器发送网络包切换模式
                    ClientPlayNetworking.send(
                        new me.tuanzi.network.WorldSculptorsPenModePacket()
                    );
                    cir.setReturnValue(true); // 拦截并返回 true
                    return;
                }
            }

            // 野太刀脱力软僵直判定与节拍器攻击响应
            if (stack.getItem() instanceof NodachiItem && mc.player instanceof NodachiPlayerTracker tracker) {
                if (tracker.tuanzis_mod$getNodachiExhaustionTicks() > 0) {
                    cir.setReturnValue(false);
                    return;
                }

                // 检测本次出刀是否正处于准星节拍器的高亮 QTE 窗口内（且冷却已满 100%）
                boolean onBeat = me.tuanzi.client.NodachiMetronome.isCurrentAttackInRhythm(mc.player);
                ClientPlayNetworking.send(new me.tuanzi.network.NodachiBeatPacket(onBeat));

                // 攻击触发：节拍器响应出刀
                me.tuanzi.client.NodachiMetronome.onAttack();

                // 挥空惩罚判定：玩家手持野太刀挥砍且未命中任何实体或方块（对准空气）
                if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.MISS) {
                    tracker.tuanzis_mod$triggerNodachiExhaustion("挥空攻击 (对准空气未命中实体或方块)");
                    ClientPlayNetworking.send(new NodachiMissPacket());
                }
            }
        }
    }
}
