package me.tuanzi.mixin;

import me.tuanzi.block.entity.BlockBreakerBlockEntity;
import me.tuanzi.block.entity.BlockBreakerPistonManager;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private static void onTickTail(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity entity, CallbackInfo ci) {
        if (!level.isClientSide()) {
            if (level.getBlockState(pos).is(ModBlocks.BLOCK_BREAKER)) {
                if (level.getBlockEntity(pos) instanceof BlockBreakerBlockEntity breakerBe) {
                    CompoundTag tag = BlockBreakerPistonManager.getMigration(level.dimension(), pos);
                    if (tag != null) {
                        breakerBe.loadFromTag(tag);
                        BlockBreakerPistonManager.confirmMigration(level.dimension(), pos);
                        ModLog.debug("[方块破坏器活塞移动] 在目标位置 " + pos + " 成功还原并恢复全部方块实体状态与数据");
                    }
                }
            }
        }
    }
}
