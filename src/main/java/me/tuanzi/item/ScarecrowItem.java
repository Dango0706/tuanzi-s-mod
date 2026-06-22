package me.tuanzi.item;

import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.tuanzi.entity.ScarecrowEntity;
import me.tuanzi.init.ModEntities;

public class ScarecrowItem extends Item {
    public ScarecrowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level level = context.getLevel();
            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            BlockPos blockPos = placeContext.getClickedPos();
            ItemStack itemStack = context.getItemInHand();
            Vec3 pos = Vec3.atBottomCenterOf(blockPos);
            AABB box = ModEntities.SCARECROW.getDimensions().makeBoundingBox(pos.x(), pos.y(), pos.z());
            if (level.noCollision(null, box) && level.getEntities(null, box).isEmpty()) {
                if (level instanceof ServerLevel serverLevel) {
                    PostSpawnProcessor<ScarecrowEntity> entityConfig = EntityType.createDefaultStackConfig(serverLevel, itemStack, context.getPlayer());
                    ScarecrowEntity entity = ModEntities.SCARECROW.create(serverLevel, entityConfig, blockPos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
                    if (entity == null) {
                        return InteractionResult.FAIL;
                    }

                    float yRot = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), yRot, 0.0F);
                    serverLevel.addFreshEntityWithPassengers(entity);
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
                    entity.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
                }

                itemStack.shrink(1);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        }
    }
}
