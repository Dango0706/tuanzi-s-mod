package me.tuanzi.world.inventory.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.network.scavenger.ScavengerItemRevealedS2CPacket;
import me.tuanzi.network.scavenger.ScavengerSearchProgressS2CPacket;
import me.tuanzi.network.scavenger.ScavengerServerPacketHandler;
import me.tuanzi.world.scavenger.loot.container.IScavengerLootContainer;
import me.tuanzi.world.scavenger.loot.meta.ScavengerRarity;
import me.tuanzi.world.scavenger.loot.search.SearchSessionManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import org.jspecify.annotations.Nullable;

/**
 * 拾荒回廊野外双栏搜刮菜单（视图 B）。
 * 左侧展示玩家随身背包、安全箱与快捷栏；右侧展示搜刮容器二维迷雾网格。
 */
public class ScavengerTacticalMenu extends AbstractContainerMenu {

    private final Player player;
    private final ContainerData data;
    private final @Nullable IScavengerLootContainer container;

    // 客户端构造器
    public ScavengerTacticalMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(5));
    }

    // 服务端构造器
    public ScavengerTacticalMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable IScavengerLootContainer container,
            ContainerData data
    ) {
        super(ModMenuTypes.SCAVENGER_TACTICAL, containerId);
        this.player = playerInventory.player;
        this.container = container;
        this.data = data;
        this.addDataSlots(data);

        // 玩家快捷栏 9 格 (0..8) - 放置在界面左下角
        int hotbarY = 196;
        int hotbarX = 12;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, hotbarX + i * 18, hotbarY));
        }

        // 服务端开启逐刻搜刮会话
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer && container != null) {
            SearchSessionManager.openSession(serverPlayer, container, new SearchSessionManager.SessionEventListener() {
                @Override
                public void onSearchTick(ServerPlayer p, int entryId, int elapsedTicks, int totalTicks) {
                    ServerPlayNetworking.send(p, new ScavengerSearchProgressS2CPacket(
                            container.containerWorldPos(), entryId, elapsedTicks, totalTicks, 0f, ScavengerRarity.COMMON
                    ));
                }

                @Override
                public void onStagePulse(ServerPlayer p, int entryId, float threshold, ScavengerRarity rarity) {
                    ServerPlayNetworking.send(p, new ScavengerSearchProgressS2CPacket(
                            container.containerWorldPos(), entryId, 0, 0, threshold, rarity
                    ));
                }

                @Override
                public void onItemRevealed(ServerPlayer p, int entryId, ItemStack revealedStack) {
                    ServerPlayNetworking.send(p, new ScavengerItemRevealedS2CPacket(
                            container.containerWorldPos(), entryId, revealedStack
                    ));
                    ScavengerServerPacketHandler.syncContainer(p, container);
                }

                @Override
                public void onSessionClosed(ServerPlayer p, String reason) {
                }
            });
            ScavengerServerPacketHandler.syncContainer(serverPlayer, container);
            ScavengerServerPacketHandler.syncProfile(serverPlayer);
        }
    }

    public BlockPos getContainerPos() {
        return new BlockPos(this.data.get(0), this.data.get(1), this.data.get(2));
    }

    public int getColumns() {
        return this.data.get(3);
    }

    public int getRows() {
        return this.data.get(4);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer) {
            SearchSessionManager.closeSession(player.getUUID(), "玩家关闭搜刮界面");
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player.level().isClientSide()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            ItemStack result = slotStack.copy();

            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                me.tuanzi.world.scavenger.profile.PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
                boolean inserted = false;

                // 优先存入随身战术背囊
                if (profile.hasBackpack() && profile.backpackGrid() != null) {
                    me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.backpackGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                    if (fit.isSuccess()) {
                        slotStack.shrink(1);
                        inserted = true;
                    }
                }

                // 若背囊满，存入安全箱
                if (!inserted && profile.hasSafebox() && profile.safeboxGrid() != null) {
                    me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.safeboxGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                    if (fit.isSuccess()) {
                        slotStack.shrink(1);
                        inserted = true;
                    }
                }

                // 若安全箱也满，存入搜刮容器（玩家放入的自身物资立即揭示无需重新搜索）
                if (!inserted && this.container != null) {
                    me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = this.container.scavengerLootState().grid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                    if (fit.isSuccess() && fit.placedEntry() != null) {
                        slotStack.shrink(1);
                        this.container.scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), serverPlayer.getUUID());
                        this.container.markContainerChanged();
                        ScavengerServerPacketHandler.syncContainer(serverPlayer, this.container);
                        inserted = true;
                    }
                }

                if (inserted) {
                    profile.markDirty();
                    ScavengerServerPacketHandler.syncProfile(serverPlayer);
                    if (slotStack.isEmpty()) {
                        slot.setByPlayer(ItemStack.EMPTY);
                    } else {
                        slot.setChanged();
                    }
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.isAlive() || player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return false;
        }
        BlockPos pos = getContainerPos();
        if (pos.equals(BlockPos.ZERO)) {
            return true;
        }
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= SearchSessionManager.MAX_INTERACTION_DISTANCE_SQ;
    }
}
