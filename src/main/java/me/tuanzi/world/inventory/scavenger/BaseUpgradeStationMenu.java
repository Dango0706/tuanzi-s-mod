package me.tuanzi.world.inventory.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.network.scavenger.ScavengerServerPacketHandler;
import me.tuanzi.world.scavenger.base.BaseFacilityService;
import me.tuanzi.world.scavenger.loot.search.SearchSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 拾荒回廊基地整备升级台菜单（视图 D）。
 * 支持负重系统 (Lv.1~8) 与基地仓库扩容 (Lv.1~10) 的消耗预览与强化升级。
 */
public class BaseUpgradeStationMenu extends AbstractContainerMenu {

    private final Player player;
    private final ContainerData data;

    public BaseUpgradeStationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(5));
    }

    public BaseUpgradeStationMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.BASE_UPGRADE_STATION, containerId);
        this.player = playerInventory.player;
        this.data = data;
        this.addDataSlots(data);

        // 玩家快捷栏 9 格 (0..8) - 方便查看随身材料
        int hotbarY = 196;
        int hotbarX = 80;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, hotbarX + i * 18, hotbarY));
        }

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ScavengerServerPacketHandler.syncProfile(serverPlayer);
        }
    }

    public BlockPos getStationPos() {
        return new BlockPos(this.data.get(0), this.data.get(1), this.data.get(2));
    }

    public int getWeightLevel() {
        return this.data.get(3);
    }

    public int getStashLevel() {
        return this.data.get(4);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.isAlive() || player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return false;
        }
        BlockPos pos = getStationPos();
        if (pos.equals(BlockPos.ZERO)) {
            return true;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > SearchSessionManager.MAX_INTERACTION_DISTANCE_SQ) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return BaseFacilityService.isAuthorizedOwner(serverPlayer, pos);
        }
        return true;
    }
}
