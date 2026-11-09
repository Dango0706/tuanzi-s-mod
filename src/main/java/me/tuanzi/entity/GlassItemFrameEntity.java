package me.tuanzi.entity;

import me.tuanzi.init.ModEntities;
import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GlassItemFrameEntity extends ItemFrame {
    public GlassItemFrameEntity(final EntityType<? extends GlassItemFrameEntity> type, final Level level) {
        super(type, level);
    }

    public GlassItemFrameEntity(final Level level, final BlockPos pos, final Direction direction) {
        super(ModEntities.GLASS_ITEM_FRAME, level, pos, direction);
    }

    @Override
    public boolean isInvisible() {
        return !this.getItem().isEmpty() || super.isInvisible();
    }

    @Override
    public void setItem(ItemStack itemStack, boolean updateNeighbours) {
        super.setItem(itemStack, updateNeighbours);
        ModLog.debug("GlassItemFrameEntity [id=" + this.getId() + "] setItem: " + itemStack.getItem() + ", isInvisible: " + this.isInvisible());
    }

    @Override
    protected ItemStack getFrameItemStack() {
        return new ItemStack(ModItems.GLASS_ITEM_FRAME);
    }
}
