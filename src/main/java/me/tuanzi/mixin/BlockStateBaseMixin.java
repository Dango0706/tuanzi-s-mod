package me.tuanzi.mixin;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import me.tuanzi.block.entity.ColorBlockBlockEntity;
import me.tuanzi.block.entity.ColorSlabBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getMapColor", at = @At("HEAD"), cancellable = true)
    private void onGetMapColor(BlockGetter level, BlockPos pos, CallbackInfoReturnable<MapColor> cir) {
        BlockBehaviour.BlockStateBase state = (BlockBehaviour.BlockStateBase) (Object) this;

        if (state.getBlock() instanceof ColorBlock || state.getBlock() instanceof ColorStairs) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ColorBlockBlockEntity colorBe) {
                cir.setReturnValue(ColorBlock.getClosestMapColor(colorBe.getColor()));
            }
        } else if (state.getBlock() instanceof ColorSlab) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ColorSlabBlockEntity slabBe) {
                SlabType type = state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);
                int color = (type == SlabType.BOTTOM) ? slabBe.getBottomColor() : slabBe.getTopColor();
                cir.setReturnValue(ColorBlock.getClosestMapColor(color));
            }
        }
    }
}
