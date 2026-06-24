package me.tuanzi.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player != null) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.is(me.tuanzi.init.ModItems.WORLD_SCULPTORS_PEN)) {
                if (mc.player.isSecondaryUseActive()) { // Shift + 左键
                    // 向服务器发送网络包切换模式
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new me.tuanzi.network.WorldSculptorsPenModePacket()
                    );
                    cir.setReturnValue(true); // 拦截并返回 true
                }
            }
        }
    }
}
