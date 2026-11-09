package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.world.inventory.scavenger.BaseUpgradeStationMenu;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
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
 * 拾荒回廊玩家私人基地整备升级台方块实体。
 */
public class BaseUpgradeStationBlockEntity extends BlockEntity implements MenuProvider {

    public BaseUpgradeStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BASE_UPGRADE_STATION_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tuanzis_mod.base_upgrade_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        SimpleContainerData data = new SimpleContainerData(5);
        data.set(0, this.worldPosition.getX());
        data.set(1, this.worldPosition.getY());
        data.set(2, this.worldPosition.getZ());
        int weightLv = 1;
        int stashLv = 1;
        if (player instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
            weightLv = profile.weightLevel();
            stashLv = profile.stashLevel();
        }
        data.set(3, weightLv);
        data.set(4, stashLv);
        return new BaseUpgradeStationMenu(containerId, playerInventory, data);
    }
}
