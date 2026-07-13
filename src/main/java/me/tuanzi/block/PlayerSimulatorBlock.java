package me.tuanzi.block;

import me.tuanzi.block.entity.PlayerSimulatorBlockEntity;
import me.tuanzi.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class PlayerSimulatorBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<PlayerSimulatorBlock> CODEC = simpleCodec(PlayerSimulatorBlock::new);
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public PlayerSimulatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerSimulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.PLAYER_SIMULATOR_BLOCK_ENTITY, PlayerSimulatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerSimulatorBlockEntity simulatorBe) {
                player.openMenu(simulatorBe);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean shouldTrigger = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean isTriggered = state.getValue(TRIGGERED);
        
        if (shouldTrigger && !isTriggered) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerSimulatorBlockEntity simulatorBe) {
                simulatorBe.trigger();
            }
            level.setBlock(pos, state.setValue(TRIGGERED, true), 2);
        } else if (!shouldTrigger && isTriggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        net.minecraft.world.item.ItemStack stack = context.getItemInHand();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains("OwnerUUID")) {
                try {
                    java.util.UUID ownerUuid = java.util.UUID.fromString(tag.getString("OwnerUUID").orElse(""));
                    Player player = context.getPlayer();
                    if (player != null && !player.getUUID().equals(ownerUuid)) {
                        if (!context.getLevel().isClientSide()) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你不是该玩家模拟器的拥有者，无法放置！"));
                        }
                        return null; // 直接返回 null 拦截放置行为
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerSimulatorBlockEntity simulatorBe) {
                if (placer instanceof Player player) {
                    simulatorBe.setOwner(player.getUUID(), player.getName().getString());
                } else {
                    net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                        if (tag.contains("OwnerUUID")) {
                            try {
                                java.util.UUID ownerUuid = java.util.UUID.fromString(tag.getString("OwnerUUID").orElse(""));
                                simulatorBe.setOwner(ownerUuid, tag.getString("OwnerName").orElse(""));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlayerSimulatorBlockEntity simulatorBe) {
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(this);
                if (simulatorBe.hasOwner()) {
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    tag.putString("OwnerUUID", simulatorBe.getOwnerUuid().toString());
                    tag.putString("OwnerName", simulatorBe.getOwnerName());
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

                    net.minecraft.network.chat.Component loreComponent = net.minecraft.network.chat.Component.literal("§7拥有者: §e" + simulatorBe.getOwnerName());
                    net.minecraft.world.item.component.ItemLore lore = new net.minecraft.world.item.component.ItemLore(java.util.List.of(loreComponent));
                    stack.set(net.minecraft.core.component.DataComponents.LORE, lore);
                }
                Block.popResource(level, pos, stack);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlayerSimulatorBlockEntity simulatorBe) {
            Containers.dropContents(level, pos, simulatorBe);
            level.updateNeighborsAt(pos, this);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        builder.add(FACING, TRIGGERED);
    }
}
