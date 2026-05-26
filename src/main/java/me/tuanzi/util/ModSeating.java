package me.tuanzi.util;

import me.tuanzi.entity.SeatEntity;
import me.tuanzi.init.ModEntities;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class ModSeating {
    public static void initialize() {
        UseBlockCallback.EVENT.register(ModSeating::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        // 限制只在主手右键交互时触发，防止双手重复触发
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // 必须是空手（主手和副手均不持有任何物品）
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        // 如果玩家处于潜行状态，不触发坐下（方便潜行放置方块或与其他方块交互）
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        // 已经骑乘在其他实体上时，不触发坐下
        if (player.isPassenger()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        // 必须是楼梯方块
        if (!(state.getBlock() instanceof StairBlock)) {
            return InteractionResult.PASS;
        }

        // 必须是正向楼梯 (Half.BOTTOM)
        if (state.getValue(StairBlock.HALF) != Half.BOTTOM) {
            return InteractionResult.PASS;
        }

        // 检查楼梯正上方一格，不能被实体碰撞方块阻挡（防止窒息/卡头）
        BlockPos abovePos = pos.above();
        if (!level.getBlockState(abovePos).getCollisionShape(level, abovePos).isEmpty()) {
            return InteractionResult.PASS;
        }

        // 避免多人重叠坐在同一个楼梯上，检查该楼梯 pos 区域是否已存在 SeatEntity
        AABB searchBox = new AABB(pos);
        List<SeatEntity> existingSeats = level.getEntitiesOfClass(SeatEntity.class, searchBox);
        if (!existingSeats.isEmpty()) {
            return InteractionResult.PASS;
        }

        // 执行坐下逻辑
        if (!level.isClientSide()) {
            double seatX = pos.getX() + 0.5;
            double seatY = pos.getY() + 0.3; // 设定适当的坐高
            double seatZ = pos.getZ() + 0.5;

            // 创建并生成不可见的 SeatEntity
            SeatEntity seatEntity = new SeatEntity(ModEntities.SEAT, level);
            seatEntity.setPos(seatX, seatY, seatZ);
            seatEntity.setYRot(player.getYRot());
            seatEntity.setXRot(0.0F);
            
            level.addFreshEntity(seatEntity);
            
            // 让玩家骑乘该实体
            player.startRiding(seatEntity);
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
