package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.world.inventory.scavenger.BaseStashMenu;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 拾荒回廊玩家私人基地独立大容量仓库柜方块实体。
 */
public class BaseStashCabinetBlockEntity extends BlockEntity implements MenuProvider {

    public BaseStashCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BASE_STASH_CABINET_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tuanzis_mod.base_stash_cabinet");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        SimpleContainerData data = new SimpleContainerData(4);
        data.set(0, this.worldPosition.getX());
        data.set(1, this.worldPosition.getY());
        data.set(2, this.worldPosition.getZ());
        int stashLv = 1;
        if (player instanceof ScavengerPlayerTracker tracker) {
            stashLv = tracker.tuanzis_mod$getCorridorProfile().stashLevel();
        }
        data.set(3, stashLv);
        return new BaseStashMenu(containerId, playerInventory, data);
    }
}
