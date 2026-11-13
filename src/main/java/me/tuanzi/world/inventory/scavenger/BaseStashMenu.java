package me.tuanzi.world.inventory.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.network.scavenger.ScavengerServerPacketHandler;
import me.tuanzi.world.scavenger.base.BaseFacilityService;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.search.SearchSessionManager;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 拾荒回廊玩家基地独立仓库柜菜单（视图 C）。
 * 右侧展示 9 列宽的大容量独立仓库（Lv.1~10，带滚动条），左侧展示玩家单兵装备（4 护甲+副手+背囊+安全箱）与随身网格。
 */
public class BaseStashMenu extends AbstractContainerMenu {

    private final Player player;
    private final ContainerData data;
    private final ScavengerEquipmentContainer equipmentContainer;

    public BaseStashMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(4));
    }

    public BaseStashMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.BASE_STASH, containerId);
        this.player = playerInventory.player;
        this.data = data;
        this.addDataSlots(data);
        this.equipmentContainer = new ScavengerEquipmentContainer(this.player);

        // 1. 盔甲 4 槽位 (头部 39, 胸甲 38, 护腿 37, 护靴 36) - 放置在界面左侧装备区第 1 列 (x=10)
        EquipmentSlot[] armorSlots = new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < 4; i++) {
            this.addSlot(new ArmorSlot(playerInventory, this.player, armorSlots[i], 39 - i, 10, 20 + i * 18, null));
        }

        // 2. 副手槽位 (索引 40) - 放置在第 2 列 (x=28, y=56)
        this.addSlot(new Slot(playerInventory, 40, 28, 56));

        // 3. 战术背囊槽位 (索引 5) 与 黑匣安全箱槽位 (索引 6) - 放置在第 2 列 (x=28, y=20 / y=38)
        this.addSlot(new ScavengerEquipmentSlot(this.equipmentContainer, 0, 28, 20, EquipmentGridSpec.SlotType.BACKPACK, this.player));
        this.addSlot(new ScavengerEquipmentSlot(this.equipmentContainer, 1, 28, 38, EquipmentGridSpec.SlotType.SAFEBOX, this.player));

        // 4. 玩家快捷栏 9 格 (索引 7..15, 对应 playerInventory 0..8) - 界面底部 (x=10, y=196)
        int hotbarY = 196;
        int hotbarX = 10;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, hotbarX + i * 18, hotbarY));
        }

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ScavengerServerPacketHandler.syncBaseStash(serverPlayer, getCabinetPos());
            ScavengerServerPacketHandler.syncProfile(serverPlayer);
        }
    }

    public ScavengerEquipmentContainer getEquipmentContainer() {
        return this.equipmentContainer;
    }

    public BlockPos getCabinetPos() {
        return new BlockPos(this.data.get(0), this.data.get(1), this.data.get(2));
    }

    public int getStashLevel() {
        return this.data.get(3);
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

            int hotbarStart = 7; // 4 armor + 1 offhand + 1 backpack + 1 safebox = 7
            int hotbarEnd = 16;  // 7 + 9 = 16

            if (slotIndex < hotbarStart) {
                // 安全箱不可被取出！
                if (slotIndex == 6) {
                    return ItemStack.EMPTY;
                }

                // 从装备槽脱下：优先存入随身背包网格或基地仓库网格（支持 2x2/2x3 等大尺寸装备放入）
                if (player instanceof ServerPlayer serverPlayer
                        && serverPlayer instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                    PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();

                    // 若脱下的是背囊本身 (slotIndex == 5)，必须背囊网格为空
                    if (slotIndex == 5) {
                        if (profile.hasBackpack() && !profile.backpackGrid().isEmpty()) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // 优先存入随身背包网格
                        if (profile.hasBackpack() && profile.backpackGrid() != null) {
                            me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.backpackGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                            if (fit.isSuccess()) {
                                slotStack.shrink(1);
                                profile.markDirty();
                                ScavengerServerPacketHandler.syncBaseStash(serverPlayer, getCabinetPos());
                                ScavengerServerPacketHandler.syncProfile(serverPlayer);
                                slot.setByPlayer(ItemStack.EMPTY);
                                return result;
                            }
                        }
                        // 次之存入基地仓库网格
                        if (profile.baseStashGrid() != null) {
                            me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.baseStashGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                            if (fit.isSuccess()) {
                                slotStack.shrink(1);
                                profile.markDirty();
                                ScavengerServerPacketHandler.syncBaseStash(serverPlayer, getCabinetPos());
                                ScavengerServerPacketHandler.syncProfile(serverPlayer);
                                slot.setByPlayer(ItemStack.EMPTY);
                                return result;
                            }
                        }
                    }
                }

                // 若网格未放入，再尝试移动到快捷栏 (限 1x1)
                me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta meta = me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.resolveItemMeta(slotStack);
                if (meta.isAllowedInHotbar(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, hotbarStart, hotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从快捷栏 Shift 点击：
                // A. 若为背包装备且背包槽为空 -> 装备到背包槽
                if (PlayerCorridorProfile.resolveEquipmentSpec(slotStack, EquipmentGridSpec.SlotType.BACKPACK).isPresent()
                        && !this.slots.get(5).hasItem()) {
                    if (this.moveItemStackTo(slotStack, 5, 6, false)) {
                        slot.setChanged();
                        return result;
                    }
                }
                // B. 若为安全箱装备且安全箱槽为空 -> 装备到安全箱槽
                if (PlayerCorridorProfile.resolveEquipmentSpec(slotStack, EquipmentGridSpec.SlotType.SAFEBOX).isPresent()
                        && !this.slots.get(6).hasItem()) {
                    if (this.moveItemStackTo(slotStack, 6, 7, false)) {
                        slot.setChanged();
                        return result;
                    }
                }
                // C. 尝试放入护甲槽 (0..4)
                if (this.moveItemStackTo(slotStack, 0, 4, false)) {
                    slot.setChanged();
                    return result;
                }

                // D. 存入基地仓库与背囊
                if (player instanceof ServerPlayer serverPlayer
                        && serverPlayer instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                    PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
                    boolean inserted = false;

                    // 优先存入基地独立仓库网格
                    if (profile.baseStashGrid() != null) {
                        me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.baseStashGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                        if (fit.isSuccess()) {
                            slotStack.shrink(1);
                            inserted = true;
                        }
                    }

                    // 若仓库满，存入随身背囊网格
                    if (!inserted && profile.hasBackpack() && profile.backpackGrid() != null) {
                        me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.backpackGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                        if (fit.isSuccess()) {
                            slotStack.shrink(1);
                            inserted = true;
                        }
                    }

                    if (inserted) {
                        profile.markDirty();
                        ScavengerServerPacketHandler.syncBaseStash(serverPlayer, getCabinetPos());
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

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.isAlive() || player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return false;
        }
        BlockPos pos = getCabinetPos();
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

