package me.tuanzi.mixin;

import me.tuanzi.block.entity.BlockBreakerBlockEntity;
import me.tuanzi.block.entity.BlockBreakerPistonManager;
import me.tuanzi.config.OverworldBuildHeightControl;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.inventory.BlockBreakerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    @Redirect(
            method = "isPushable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMaxY()I")
    )
    private static int tuanzis_mod$getEffectiveMaxY(Level level) {
        return OverworldBuildHeightControl.getEffectiveMaxY(level);
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void onIsPushable(BlockState state, Level level, BlockPos pos, Direction direction, boolean allowDestroyable, Direction connectionDirection, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ModBlocks.BLOCK_BREAKER)) {
            int maxY = OverworldBuildHeightControl.getEffectiveMaxY(level);
            // 方块破坏器支持被活塞推拉
            if (pos.getY() < level.getMinY() || pos.getY() > maxY || !level.getWorldBorder().isWithinBounds(pos)) {
                cir.setReturnValue(false);
                return;
            }
            if (direction == Direction.DOWN && pos.getY() == level.getMinY()) {
                cir.setReturnValue(false);
                return;
            }
            if (direction == Direction.UP && pos.getY() == maxY) {
                cir.setReturnValue(false);
                return;
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z", shift = At.Shift.AFTER))
    private void onMoveBlocks(Level level, BlockPos pistonPos, Direction direction, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) {
            return;
        }

        PistonStructureResolver resolver = new PistonStructureResolver(level, pistonPos, direction, extending);
        if (!resolver.resolve()) {
            return;
        }

        List<BlockPos> toPush = resolver.getToPush();
        Direction pushDirection = extending ? direction : direction.getOpposite();

        for (BlockPos pos : toPush) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.BLOCK_BREAKER)) {
                if (level.getBlockEntity(pos) instanceof BlockBreakerBlockEntity breakerBe) {
                    BlockPos targetPos = pos.relative(pushDirection);
                    ModLog.debug("[方块破坏器活塞移动] 开始从 " + pos + " 迁移至 " + targetPos);

                    // 1. 仅关闭正在查看该被推动破坏器 GUI 的玩家界面
                    for (net.minecraft.world.entity.player.Player player : level.players()) {
                        if (player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof BlockBreakerMenu menu) {
                            if (menu.getContainer() == breakerBe) {
                                serverPlayer.closeContainer();
                            }
                        }
                    }

                    // 2. 暂停工作，标记正在移动
                    breakerBe.onPistonMoveStart();

                    // 3. 完整保存所有数据至迁移管理器
                    BlockBreakerPistonManager.recordMigration(level.dimension(), targetPos, breakerBe.saveToTag());
                }
            }
        }
    }
}
