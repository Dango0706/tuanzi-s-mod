package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阻止客户端预测放置及常规 BlockItem 放置在主世界配置建筑高度上限之外生效。
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$checkOverworldBuildLimit(
            BlockPlaceContext context,
            BlockState stateForPlacement,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!OverworldBuildHeightControl.isWithinBuildLimit(context.getLevel(), context.getClickedPos())) {
            cir.setReturnValue(false);
            return;
        }
        if (ModDimensions.isCanvasSpaceLevel(context.getLevel())
                && !CanvasSpaceManager.canPlaceAt(context.getPlayer(), context.getLevel(), context.getClickedPos(), stateForPlacement)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$checkOverworldBuildLimitOnPlaceBlock(
            BlockPlaceContext context,
            BlockState placementState,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!OverworldBuildHeightControl.isWithinBuildLimit(context.getLevel(), context.getClickedPos())) {
            cir.setReturnValue(false);
            return;
        }
        if (ModDimensions.isCanvasSpaceLevel(context.getLevel())
                && !CanvasSpaceManager.canPlaceAt(context.getPlayer(), context.getLevel(), context.getClickedPos(), placementState)) {
            cir.setReturnValue(false);
        }
    }
}
