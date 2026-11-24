package me.tuanzi.mixin;

import me.tuanzi.block.entity.BlockBreakerBlockEntity;
import me.tuanzi.block.entity.BlockBreakerPistonManager;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private static boolean tuanzis_mod$skipSideEffectsOnTickSetBlock(Level level, BlockPos pos, BlockState state, int flags) {
        if (!level.isClientSide() && state.is(ModBlocks.BLOCK_BREAKER)) {
            return level.setBlock(pos, state, flags | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }
        return level.setBlock(pos, state, flags);
    }

    @Redirect(
            method = "finalTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean tuanzis_mod$skipSideEffectsOnFinalTickSetBlock(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide() && state.is(ModBlocks.BLOCK_BREAKER)) {
            return level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }
        return level.setBlockAndUpdate(pos, state);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void onTickTail(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity entity, CallbackInfo ci) {
        if (!level.isClientSide()) {
            if (level.getBlockState(pos).is(ModBlocks.BLOCK_BREAKER)) {
                if (level.getBlockEntity(pos) instanceof BlockBreakerBlockEntity breakerBe) {
                    CompoundTag tag = BlockBreakerPistonManager.consumeMigration(level.dimension(), pos);
                    if (tag != null) {
                        if (breakerBe.isEmpty()) {
                            breakerBe.loadFromTag(tag);
                        }
                        ModLog.debug("[方块破坏器活塞移动] 在目标位置 " + pos + " 兜底确认迁移数据并清理暂存");
                    }
                }
            }
        }
    }
}
