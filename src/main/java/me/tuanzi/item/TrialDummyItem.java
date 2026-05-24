package me.tuanzi.item;

import me.tuanzi.entity.TrialDummyEntity;
import me.tuanzi.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class TrialDummyItem extends Item {
    public TrialDummyItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        Direction clickedFace = context.getClickedFace();
        // 只能放置在方块顶部表面
        if (clickedFace != Direction.UP) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        
        // 验证底座必须是固体方块
        if (!clickedState.isCollisionShapeFullBlock(level, clickedPos)) {
            return InteractionResult.FAIL;
        }

        BlockPos spawnPos = clickedPos.above();
        BlockPos headPos = spawnPos.above();
        
        // 检查假人占用的2格空间是否允许放置（必须是空气或可替换方块）
        if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(headPos).isAir()) {
            return InteractionResult.FAIL;
        }

        ItemStack itemStack = context.getItemInHand();
        UUID ownerUuid = player.getUUID();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // 1. 扫描半径 10 格内由该玩家放置的旧假人，并进行瞬间无损回收（不扣减耐久度）
            AABB searchBox = new AABB(spawnPos).inflate(10.0);
            List<TrialDummyEntity> oldDummies = serverLevel.getEntitiesOfClass(TrialDummyEntity.class, searchBox,
                dummy -> dummy.getOwnerUuid() != null && dummy.getOwnerUuid().equals(ownerUuid));
            
            for (TrialDummyEntity oldDummy : oldDummies) {
                // safelyRecycled = true, deductDurability = false (无损瞬间回收)
                oldDummy.trialDummyDestroyed(serverLevel, player.damageSources().playerAttack(player), true, false);
            }

            // 2. 在目标位置生成全新的试炼假人实体
            TrialDummyEntity entity = ModEntities.TRIAL_DUMMY.create(serverLevel, null, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
            if (entity == null) {
                return InteractionResult.FAIL;
            }

            // 绑定放置玩家 UUID 以及当前物品的剩余耐久度数据
            entity.setOwnerUuid(ownerUuid);
            int currentDamage = itemStack.getDamageValue();
            entity.setDurability(32 - currentDamage);

            // 根据玩家视线旋转角度
            float yRot = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
            entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), yRot, 0.0F);
            entity.yBodyRot = yRot;
            entity.yHeadRot = yRot;

            serverLevel.addFreshEntityWithPassengers(entity);
            
            // 播放清脆的盔甲架放置声音
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
        }

        // 扣除手持物品
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
