package me.tuanzi.block;

import me.tuanzi.block.entity.PrintedModelBlockEntity;
import me.tuanzi.block.entity.PrintedModelPartBlockEntity;
import me.tuanzi.world.canvas.CanvasMetadata;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 多格打印模型碰撞占位方块：代理对应单元碰撞箱并在被破坏时联动清理整个模型。
 */
public class PrintedModelPartBlock extends BaseEntityBlock {
    public PrintedModelPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrintedModelPartBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return resolveCellShape(level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return resolveCellShape(level, pos);
    }

    private static VoxelShape resolveCellShape(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrintedModelPartBlockEntity partBe) {
            PrintedModelBlockEntity master = partBe.getMasterEntity();
            if (master != null) {
                BlockState masterState = level.getBlockState(partBe.getMasterPos());
                return master.getCellShape(masterState, partBe.getCellX(), partBe.getCellY(), partBe.getCellZ());
            }
        }
        return Shapes.empty();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrintedModelPartBlockEntity partBe) {
            PrintedModelBlockEntity master = partBe.getMasterEntity();
            if (master != null) {
                BlockState masterState = level.getBlockState(partBe.getMasterPos());
                ItemStack stack = new ItemStack(masterState.getBlock());
                CanvasMetadata.write(stack, master.getMetadata());
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            destroyMasterFromPart(sl, pos, !player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        destroyMasterFromPart(level, pos, true);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    private static void destroyMasterFromPart(ServerLevel level, BlockPos partPos, boolean dropMasterItem) {
        BlockEntity be = level.getBlockEntity(partPos);
        if (!(be instanceof PrintedModelPartBlockEntity partBe)) {
            return;
        }
        BlockPos masterPos = partBe.getMasterPos();
        BlockState masterState = level.getBlockState(masterPos);
        BlockEntity masterBe = level.getBlockEntity(masterPos);
        if (masterBe instanceof PrintedModelBlockEntity modelBe && !modelBe.isBreakingMasterChain()) {
            if (dropMasterItem) {
                ItemStack drop = new ItemStack(masterState.getBlock());
                CanvasMetadata.write(drop, modelBe.getMetadata());
                Block.popResource(level, masterPos, drop);
            }
            PrintedModelBlock.cleanupParts(level, masterPos, masterState);
            level.setBlock(masterPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
