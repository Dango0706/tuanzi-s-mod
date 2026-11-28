package me.tuanzi.block;

import me.tuanzi.block.entity.PrintedModelBlockEntity;
import me.tuanzi.block.entity.PrintedModelPartBlockEntity;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 打印模型主方块：
 * 显示由画布烘焙出的静态 2D/3D 作品，碰撞箱自动合并，破坏仅掉落模型自身（不返还画布）。
 */
public class PrintedModelBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<PrintedModelMountMode> MOUNT_MODE =
            EnumProperty.create("mount_mode", PrintedModelMountMode.class);

    public PrintedModelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MOUNT_MODE, PrintedModelMountMode.FLOOR));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrintedModelBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        CanvasMetadata meta = CanvasMetadata.read(context.getItemInHand());
        Direction clickedFace = context.getClickedFace();
        PrintedModelMountMode mode = PrintedModelMountMode.FLOOR;
        Direction facing = context.getHorizontalDirection().getOpposite();

        if (meta.type() != CanvasType.VOXEL_3D) {
            if (clickedFace == Direction.DOWN) {
                mode = PrintedModelMountMode.CEILING;
            } else if (clickedFace.getAxis().isHorizontal()) {
                mode = PrintedModelMountMode.WALL;
                facing = clickedFace;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(MOUNT_MODE, mode);
    }

    public static int[] computeExtentForPlacement(BlockState state, CanvasMetadata meta) {
        int scale = Math.max(1, meta.printScale());
        boolean is2d = meta.type() != CanvasType.VOXEL_3D;
        PrintedModelMountMode mode = state.getValue(MOUNT_MODE);
        Direction facing = state.getValue(FACING);
        if (!is2d || mode != PrintedModelMountMode.WALL) {
            return new int[]{scale, is2d ? 1 : scale, scale};
        }
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            return new int[]{scale, scale, 1};
        }
        return new int[]{1, scale, scale};
    }

    public static boolean canPlaceMultiCell(Level level, BlockPos originPos, BlockState state, CanvasMetadata meta, BlockPlaceContext context) {
        int[] ext = computeExtentForPlacement(state, meta);
        for (int dy = 0; dy < ext[1]; dy++) {
            for (int dz = 0; dz < ext[2]; dz++) {
                for (int dx = 0; dx < ext[0]; dx++) {
                    BlockPos p = originPos.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(p)) {
                        return false;
                    }
                    if (!level.getBlockState(p).canBeReplaced(context)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrintedModelBlockEntity modelBe) {
            return modelBe.getCellShape(state, 0, 0, 0);
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrintedModelBlockEntity modelBe) {
            return modelBe.getCellShape(state, 0, 0, 0);
        }
        return Shapes.block();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PrintedModelBlockEntity modelBe) {
            CanvasMetadata.write(stack, modelBe.getMetadata());
        }
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrintedModelBlockEntity modelBe) {
            CanvasMetadata.write(stack, modelBe.getMetadata());
        }
        return stack;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            cleanupParts(sl, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        cleanupParts(level, pos, state);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    public static void cleanupParts(ServerLevel level, BlockPos masterPos, BlockState state) {
        BlockEntity be = level.getBlockEntity(masterPos);
        if (!(be instanceof PrintedModelBlockEntity modelBe)) {
            return;
        }
        if (modelBe.isBreakingMasterChain()) {
            return;
        }
        modelBe.setBreakingMasterChain(true);
        int[] ext = modelBe.getWorldDimensions(state);
        for (int dy = 0; dy < ext[1]; dy++) {
            for (int dz = 0; dz < ext[2]; dz++) {
                for (int dx = 0; dx < ext[0]; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos partPos = masterPos.offset(dx, dy, dz);
                    BlockState partState = level.getBlockState(partPos);
                    if (partState.is(ModBlocks.PRINTED_MODEL_PART)) {
                        BlockEntity partBe = level.getBlockEntity(partPos);
                        if (partBe instanceof PrintedModelPartBlockEntity pbe && masterPos.equals(pbe.getMasterPos())) {
                            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MOUNT_MODE);
    }
}
