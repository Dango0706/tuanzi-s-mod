package me.tuanzi.world.inventory.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 拾荒回廊角色整备菜单（视图 A）。
 * 当玩家在回廊维度按下 E 键时打开，包含盔甲 4 槽、副手槽、战术背囊槽、安全箱槽以及随身快捷栏 1~9。
 */
public class CorridorTacticalLoadoutMenu extends AbstractContainerMenu {

    private final Player player;
    private final Inventory playerInventory;
    private final ScavengerEquipmentContainer equipmentContainer;

    public CorridorTacticalLoadoutMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.CORRIDOR_TACTICAL_LOADOUT, containerId);
        this.player = playerInventory.player;
        this.playerInventory = playerInventory;
        this.equipmentContainer = new ScavengerEquipmentContainer(this.player);

        // 1. 盔甲 4 槽位 (头部 39, 胸甲 38, 护腿 37, 护靴 36) - 放置在整备界面左侧装备区第 1 列 (x=12)
        EquipmentSlot[] armorSlots = new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < 4; i++) {
            this.addSlot(new ArmorSlot(playerInventory, this.player, armorSlots[i], 39 - i, 12, 20 + i * 18, null));
        }

        // 2. 副手槽位 (索引 40) - 放置在装备区第 2 列底部 (x=34, y=74)
        this.addSlot(new Slot(playerInventory, 40, 34, 74));

        // 3. 战术背囊槽位 (索引 5) 与 黑匣安全箱槽位 (索引 6) - 放置在装备区第 2 列 (x=34, y=20 / y=38)
        this.addSlot(new ScavengerEquipmentSlot(this.equipmentContainer, 0, 34, 20, EquipmentGridSpec.SlotType.BACKPACK, this.player));
        this.addSlot(new ScavengerEquipmentSlot(this.equipmentContainer, 1, 34, 38, EquipmentGridSpec.SlotType.SAFEBOX, this.player));

        // 4. 玩家随身快捷栏 9 槽位 (索引 7..15, 对应 playerInventory 0..8) - 放置在界面右下角 (x=140, y=196)
        int hotbarY = 196;
        int hotbarX = 140;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, hotbarX + i * 18, hotbarY));
        }
    }

    public ScavengerEquipmentContainer getEquipmentContainer() {
        return this.equipmentContainer;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player.level().isClientSide()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            int hotbarStart = 7; // 4 armor + 1 offhand + 1 backpack + 1 safebox = 7
            int hotbarEnd = 16;  // 7 + 9 = 16

            if (slotIndex < hotbarStart) {
                // 安全箱不可被取出！
                if (slotIndex == 6) {
                    return ItemStack.EMPTY;
                }

                // 从装备槽脱下：优先存入随身战术背囊网格（支持 2x2/2x3 等大尺寸装备放入）
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                        && serverPlayer instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                    me.tuanzi.world.scavenger.profile.PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();

                    // 若脱下的是背囊本身 (slotIndex == 5)，必须背囊网格为空
                    if (slotIndex == 5) {
                        if (profile.hasBackpack() && !profile.backpackGrid().isEmpty()) {
                            return ItemStack.EMPTY;
                        }
                    } else if (profile.hasBackpack() && profile.backpackGrid() != null) {
                        // 护甲等脱下优先存入背囊网格
                        me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.backpackGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                        if (fit.isSuccess()) {
                            slotStack.shrink(1);
                            profile.markDirty();
                            me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(serverPlayer);
                            slot.setByPlayer(ItemStack.EMPTY);
                            return result;
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

                // D. 放入战术背囊网格；若背囊未装备或满，存入安全箱网格！
                boolean insertedToGrid = false;
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                        && serverPlayer instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                    me.tuanzi.world.scavenger.profile.PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
                    if (profile.hasBackpack() && profile.backpackGrid() != null) {
                        me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.backpackGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                        if (fit.isSuccess()) {
                            slotStack.shrink(1);
                            insertedToGrid = true;
                        }
                    }
                    if (!insertedToGrid && profile.hasSafebox() && profile.safeboxGrid() != null) {
                        me.tuanzi.world.scavenger.loot.grid.GridPlacementResult fit = profile.safeboxGrid().quickInsertFirstFit(slotStack.copyWithCount(1), 0);
                        if (fit.isSuccess()) {
                            slotStack.shrink(1);
                            insertedToGrid = true;
                        }
                    }
                    if (insertedToGrid) {
                        profile.markDirty();
                        me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(serverPlayer);
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
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL;
    }
}

