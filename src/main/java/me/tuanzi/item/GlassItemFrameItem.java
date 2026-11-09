package me.tuanzi.item;

import me.tuanzi.entity.GlassItemFrameEntity;
import me.tuanzi.init.ModEntities;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class GlassItemFrameItem extends ItemFrameItem {
    public GlassItemFrameItem(final Item.Properties properties) {
        super(ModEntities.GLASS_ITEM_FRAME, properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos blockPos = pos.relative(clickedFace);
        Player player = context.getPlayer();
        ItemStack itemInHand = context.getItemInHand();
        if (player != null && !this.mayPlace(player, clickedFace, itemInHand, blockPos)) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        GlassItemFrameEntity entity = new GlassItemFrameEntity(level, blockPos, clickedFace);

        EntityType.<GlassItemFrameEntity>createDefaultStackConfig(level, itemInHand, player).apply(entity);
        if (entity.survives()) {
            if (!level.isClientSide()) {
                entity.playPlacementSound();
                level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
                level.addFreshEntity(entity);
                ModLog.debug("Placed GlassItemFrame at " + blockPos + " facing " + clickedFace);
            }

            itemInHand.shrink(1);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.CONSUME;
        }
    }
}
