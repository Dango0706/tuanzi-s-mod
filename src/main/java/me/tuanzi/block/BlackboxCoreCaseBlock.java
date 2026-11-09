package me.tuanzi.block;

import me.tuanzi.block.entity.BlackboxCoreCaseBlockEntity;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlackboxCoreCaseBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(
        Block.box(0.0, 0.0, 2.5, 16.0, 13.0, 13.5),
        Block.box(7.0, 13.0, 7.0, 9.0, 16.5, 9.0)
    );

    private static final VoxelShape SHAPE_EAST_WEST = Shapes.or(
        Block.box(2.5, 0.0, 0.0, 13.5, 13.0, 16.0),
        Block.box(7.0, 13.0, 7.0, 9.0, 16.5, 9.0)
    );

    public BlackboxCoreCaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        if (direction.getAxis() == Direction.Axis.X) {
            return SHAPE_EAST_WEST;
        }
        return SHAPE_NORTH_SOUTH;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlackboxCoreCaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, me.tuanzi.init.ModBlocks.BLACKBOX_CORE_CASE_BLOCK_ENTITY, BlackboxCoreCaseBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BlackboxCoreCaseBlockEntity caseBlockEntity) {
                boolean wasOpened = caseBlockEntity.isOpened();
                caseBlockEntity.openCase();
                player.openMenu(caseBlockEntity);

                if (!wasOpened) {
                    // 1. 气压泄出声（长嘶声）
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9f, 0.65f);
                    level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.75f);

                    // 2. 面板滑动机械声（三层嵌套滑开机构）
                    level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8f, 1.25f);
                    level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 0.85f, 1.1f);
                    level.playSound(null, pos, SoundEvents.COPPER_DOOR_OPEN, SoundSource.BLOCKS, 0.65f, 1.3f);

                    // 3. 核心启动的低频上扬音
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.35f);
                    level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 0.85f, 1.15f);
                } else {
                    level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.4f, 1.4f);
                }

                ModLog.debug(player, null, "【拾荒回廊】玩家成功打开黑匣子数据核心箱：" + pos.toShortString() + ", 首次开箱: " + !wasOpened);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // 细微硬盘转动声与电流杂音
        if (random.nextInt(8) == 0) {
            level.playLocalSound(centerX, pos.getY() + 0.5, centerZ, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.15f, 1.95f, false);
        }
        if (random.nextInt(14) == 0) {
            level.playLocalSound(centerX, pos.getY() + 0.5, centerZ, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 0.18f, 2.0f, false);
        }

        // 天线顶端脉动的悬浮蓝色光点 (Y 轴约 1.15 格高)
        double antennaTipY = pos.getY() + 1.15;
        level.addParticle(
            ParticleTypes.ELECTRIC_SPARK,
            centerX + (random.nextDouble() - 0.5) * 0.04,
            antennaTipY + (random.nextDouble() - 0.5) * 0.04,
            centerZ + (random.nextDouble() - 0.5) * 0.04,
            0.0, 0.005, 0.0
        );

        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.GLOW, centerX, antennaTipY, centerZ, 0.0, 0.002, 0.0);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
