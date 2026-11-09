package me.tuanzi.block;

import me.tuanzi.block.entity.BaseStashCabinetBlockEntity;
import me.tuanzi.world.scavenger.base.BaseFacilityService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 拾荒回廊玩家基地独立仓库柜方块。
 */
public class BaseStashCabinetBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0)
    );

    public BaseStashCabinetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BaseStashCabinetBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (!BaseFacilityService.isAuthorizedOwner(serverPlayer, pos)) {
                    serverPlayer.sendSystemMessage(Component.literal("§c你不是该私人基地设施的所有者！"));
                    return InteractionResult.CONSUME;
                }
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BaseStashCabinetBlockEntity cabinetBe) {
                player.openMenu(cabinetBe);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
