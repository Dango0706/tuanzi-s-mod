package me.tuanzi.client.renderer.state;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jetbrains.annotations.Nullable;

/**
 * 电梯方块实体渲染状态：保存伪装方块的渲染快照。
 */
public class ElevatorRenderState extends BlockEntityRenderState {
    @Nullable
    public MovingBlockRenderState movingBlock;
}
