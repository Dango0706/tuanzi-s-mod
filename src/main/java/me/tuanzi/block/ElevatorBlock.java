package me.tuanzi.block;

import me.tuanzi.block.entity.ElevatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 电梯方块：支持 16 种原版染料颜色、同色上下传送（1 秒冷却）以及蹲下右键伪装成手持方块外观。
 */
public class ElevatorBlock extends Block implements EntityBlock {
    public static final BooleanProperty DISGUISED = BooleanProperty.create("disguised");

    private final DyeColor color;

    public ElevatorBlock(DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(DISGUISED, false));
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISGUISED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(DISGUISED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState disguise = getDisguiseState(state, level, pos);
        if (disguise != null) {
            return disguise.getShape(level, pos, context);
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState disguise = getDisguiseState(state, level, pos);
        if (disguise != null) {
            VoxelShape disguiseCollision = disguise.getCollisionShape(level, pos, context);
            // 若伪装目标本身无碰撞箱（如花草、地毯外饰等），兜底保持完整方块碰撞箱以免玩家坠落
            return disguiseCollision.isEmpty() ? Shapes.block() : disguiseCollision;
        }
        return Shapes.block();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState disguise = getDisguiseState(state, level, pos);
        if (disguise != null) {
            return disguise.getShadeBrightness(level, pos);
        }
        return 0.2F;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    private static BlockState getDisguiseState(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.getValue(DISGUISED)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ElevatorBlockEntity elevatorBe && elevatorBe.hasDisguise()) {
            BlockState disguise = elevatorBe.getDisguiseState();
            if (disguise != null && !(disguise.getBlock() instanceof ElevatorBlock)) {
                return disguise;
            }
        }
        return null;
    }
}
