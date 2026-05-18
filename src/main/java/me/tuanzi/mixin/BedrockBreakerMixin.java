package me.tuanzi.mixin;

import me.tuanzi.item.YurisRevengeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BedrockBreakerMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowBedrockBreaking(Player player, net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Float> cir) {
        BlockState state = (BlockState) (Object) this;
        
        // 如果玩家手持尤里的复仇
        if (player.getMainHandItem().getItem() instanceof YurisRevengeItem) {
            if (state.is(Blocks.BEDROCK)) {
                // 如果是基岩，计算挖掘进度（硬度模拟为 50）
                float speed = player.getMainHandItem().getDestroySpeed(state);
                cir.setReturnValue(speed / 50.0F / 30.0F);
            } else {
                // 如果不是基岩，强制无法破坏
                cir.setReturnValue(0.0F);
            }
        }
    }
}
