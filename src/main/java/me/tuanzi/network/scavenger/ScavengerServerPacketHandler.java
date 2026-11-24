package me.tuanzi.network.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.base.BaseFacilityService;
import me.tuanzi.world.scavenger.loot.container.IScavengerLootContainer;
import me.tuanzi.world.scavenger.loot.container.ScavengerLootContainerState;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridInventory;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.grid.GridPlacementResult;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import me.tuanzi.world.scavenger.loot.search.SearchSessionManager;
import me.tuanzi.world.scavenger.profile.CorridorWeightController;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 拾荒回廊服务端权威网络包处理器与安全校验中心。
 */
public final class ScavengerServerPacketHandler {

    private static final net.minecraft.world.item.Item[] SAFEBOX_TIERS = new net.minecraft.world.item.Item[]{
            me.tuanzi.init.ModItems.PROTO_LOCKBOX_1X2,
            me.tuanzi.init.ModItems.REINFORCED_SAFEBOX_2X2,
            me.tuanzi.init.ModItems.CIPHER_SAFEBOX_2X3,
            me.tuanzi.init.ModItems.AEGIS_CORE_VAULT_3X3
    };

    private ScavengerServerPacketHandler() {
    }

    /**
     * 向指定玩家下发其个人回廊战术装备档案全量同步包。
     */
    public static void syncProfile(ServerPlayer player) {
        if (!(player instanceof ScavengerPlayerTracker tracker)) {
            return;
        }
        PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
        float totalWeight = CorridorWeightController.computePlayerTotalWeightKg(player, profile);
        boolean overloaded = tracker.tuanzis_mod$isCorridorOverloaded();

        ItemStack backpack = profile.equippedBackpack();
        int bpCols = profile.backpackGrid() != null ? profile.backpackGrid().columns() : 0;
        int bpRows = profile.backpackGrid() != null ? profile.backpackGrid().rows() : 0;
        List<GridItemEntry> bpEntries = profile.backpackGrid() != null
                ? List.copyOf(profile.backpackGrid().entries())
                : List.of();

        ItemStack safebox = profile.equippedSafebox();
        int sbCols = profile.safeboxGrid() != null ? profile.safeboxGrid().columns() : 0;
        int sbRows = profile.safeboxGrid() != null ? profile.safeboxGrid().rows() : 0;
        List<GridItemEntry> sbEntries = profile.safeboxGrid() != null
                ? List.copyOf(profile.safeboxGrid().entries())
                : List.of();

        ScavengerProfileSyncS2CPacket packet = new ScavengerProfileSyncS2CPacket(
                backpack, bpCols, bpRows, bpEntries,
                safebox, sbCols, sbRows, sbEntries,
                profile.weightLevel(),
                profile.stashLevel(),
                totalWeight,
                overloaded
        );
        ServerPlayNetworking.send(player, packet);
    }

    /**
     * 向指定玩家下发特定搜刮容器的防透视脱敏快照。
     */
    public static void syncContainer(ServerPlayer player, IScavengerLootContainer container) {
        ScavengerLootContainerState state = container.scavengerLootState();
        Predicate<UUID> isTeammate = uuid -> {
            ServerPlayer other = player.level().getServer().getPlayerList().getPlayer(uuid);
            return other != null && ScavengerTeamManager.isSameTeam(player, other);
        };

        SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
        int activeId = session != null ? session.activeEntryId() : -1;
        int elapsed = session != null ? session.activeElapsedTicks() : 0;

        List<RedactedGridEntryView> views = state.buildRedactedViews(player.getUUID(), isTeammate, activeId, elapsed);
        ScavengerContainerSyncS2CPacket packet = new ScavengerContainerSyncS2CPacket(
                container.scavengerContainerDefId(),
                container.containerWorldPos(),
                state.columns(),
                state.rows(),
                views
        );
        ServerPlayNetworking.send(player, packet);
    }

    /**
     * 向指定玩家下发其个人基地独立仓库的完整条目快照。
     */
    public static void syncBaseStash(ServerPlayer player, BlockPos cabinetPos) {
        if (!(player instanceof ScavengerPlayerTracker tracker)) {
            return;
        }
        PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
        GridInventory stash = profile.baseStashGrid();
        List<RedactedGridEntryView> views = new java.util.ArrayList<>();
        for (GridItemEntry entry : stash.entries()) {
            views.add(new RedactedGridEntryView(
                    entry.entryId(),
                    entry.anchorX(),
                    entry.anchorY(),
                    entry.resolveMeta().width(),
                    entry.resolveMeta().height(),
                    entry.rotated(),
                    me.tuanzi.world.scavenger.loot.search.SearchEntryPhase.REVEALED,
                    entry.solidifiedSearchTicks(),
                    entry.solidifiedSearchTicks(),
                    entry.copyStack()
            ));
        }
        ScavengerContainerSyncS2CPacket packet = new ScavengerContainerSyncS2CPacket(
                ScavengerContainerSyncS2CPacket.BASE_STASH_DEF_ID,
                cabinetPos,
                stash.columns(),
                stash.rows(),
                views
        );
        ServerPlayNetworking.send(player, packet);
    }

    public static void handleGridAction(ServerPlayer player, ScavengerGridActionC2SPacket packet) {
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return;
        }
        if (!(player instanceof ScavengerPlayerTracker tracker)) {
            return;
        }
        PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();

        switch (packet.actionType()) {
            case MOVE_OR_ROTATE -> {
                if (packet.source() == packet.destination()) {
                    GridInventory grid = resolveGridBySource(player, profile, packet.source());
                    if (grid != null) {
                        var result = grid.moveOrRotate(packet.entryId(), packet.targetX(), packet.targetY(), packet.rotated(), false);
                        if (result.isSuccess()) {
                            markSourceDirty(player, profile, packet.source());
                        }
                    }
                } else {
                    handleCrossGridMove(player, profile, packet);
                }
            }
            case AUTO_SORT -> {
                GridInventory grid = resolveGridBySource(player, profile, packet.source());
                if (grid != null && packet.sortOrder() != null) {
                    grid.autoSort(packet.sortOrder());
                    markSourceDirty(player, profile, packet.source());
                }
            }
            case QUICK_TRANSFER -> {
                handleQuickTransfer(player, profile, packet);
            }
            case EQUIP_FROM_GRID -> {
                handleEquipFromGrid(player, profile, packet);
            }
            case SWITCH_SAFEBOX -> {
                handleSwitchSafebox(player, profile, packet);
            }
            case EQUIP_BACKPACK -> {
                for (int i = 0; i < 9; i++) {
                    ItemStack hotbarStack = player.getInventory().getItem(i);
                    if (!hotbarStack.isEmpty() && profile.equipBackpack(hotbarStack, player.registryAccess())) {
                        hotbarStack.shrink(1);
                        profile.markDirty();
                        break;
                    }
                }
            }
            case UNEQUIP_BACKPACK -> {
                if (profile.hasBackpack() && profile.backpackGrid() != null && profile.backpackGrid().isEmpty()) {
                    ItemStack cleanCopy = profile.equippedBackpack().copyWithCount(1);
                    GridInventory.writeToContainerStack(cleanCopy, null, player.registryAccess());
                    if (tryStoreUnequippedEquipment(player, profile, cleanCopy, false)) {
                        profile.unequipBackpack(player.registryAccess());
                    } else {
                        player.sendSystemMessage(Component.literal("§c快捷栏与当前可用网格空间不足，无法卸下！"), true);
                    }
                }
            }
            case EQUIP_SAFEBOX -> {
                for (int i = 0; i < 9; i++) {
                    ItemStack hotbarStack = player.getInventory().getItem(i);
                    if (!hotbarStack.isEmpty() && profile.equipSafebox(hotbarStack, player.registryAccess())) {
                        hotbarStack.shrink(1);
                        profile.markDirty();
                        break;
                    }
                }
            }
            case UNEQUIP_SAFEBOX -> {
                if (profile.hasSafebox() && profile.safeboxGrid() != null && profile.safeboxGrid().isEmpty()) {
                    ItemStack cleanCopy = profile.equippedSafebox().copyWithCount(1);
                    GridInventory.writeToContainerStack(cleanCopy, null, player.registryAccess());
                    if (tryStoreUnequippedEquipment(player, profile, cleanCopy, true)) {
                        profile.unequipSafebox(player.registryAccess());
                    } else {
                        player.sendSystemMessage(Component.literal("§c快捷栏与当前可用网格空间不足，无法卸下！"), true);
                    }
                }
            }
        }

        syncProfile(player);

        SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
        if (session != null) {
            syncContainer(player, session.container());
        }
        if (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.BaseStashMenu stashMenu
                && stashMenu.stillValid(player)) {
            syncBaseStash(player, stashMenu.getCabinetPos());
        }
    }

    private static boolean tryStoreUnequippedEquipment(
            ServerPlayer player,
            PlayerCorridorProfile profile,
            ItemStack cleanStack,
            boolean allowBackpackGrid
    ) {
        if (cleanStack == null || cleanStack.isEmpty()) {
            return false;
        }
        // 1. 优先存入快捷栏空位 (0..8)
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, cleanStack);
                markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.HOTBAR);
                return true;
            }
        }
        // 2. 仅安全箱允许退入当前随身背囊网格
        if (allowBackpackGrid && profile.hasBackpack() && profile.backpackGrid() != null) {
            GridPlacementResult fit = profile.backpackGrid().quickInsertFirstFit(cleanStack, 0);
            if (fit.isSuccess()) {
                markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BACKPACK);
                return true;
            }
        }
        // 3. 当前正打开合法的个人基地仓库菜单 -> 尝试放入基地仓库
        if (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.BaseStashMenu stashMenu
                && stashMenu.stillValid(player)
                && profile.baseStashGrid() != null) {
            GridPlacementResult fit = profile.baseStashGrid().quickInsertFirstFit(cleanStack, 0);
            if (fit.isSuccess()) {
                markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BASE_STASH);
                return true;
            }
        }
        // 4. 当前正打开合法的野外搜刮菜单 -> 尝试放入搜刮容器并立即标记揭示
        if (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.ScavengerTacticalMenu tacticalMenu
                && tacticalMenu.stillValid(player)) {
            SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
            if (session != null && session.container() != null) {
                GridPlacementResult fit = session.container().scavengerLootState().grid().quickInsertFirstFit(cleanStack, 0);
                if (fit.isSuccess() && fit.placedEntry() != null) {
                    session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                    session.container().markContainerChanged();
                    return true;
                }
            }
        }
        return false;
    }

    private static void handleCrossGridMove(ServerPlayer player, PlayerCorridorProfile profile, ScavengerGridActionC2SPacket packet) {
        // 1. 目标是快捷栏 (HOTBAR)
        if (packet.destination() == ScavengerGridActionC2SPacket.GridSource.HOTBAR) {
            GridInventory srcGrid = resolveGridBySource(player, profile, packet.source());
            if (srcGrid == null) return;

            if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER) {
                SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                if (session == null || !isContainerEntryRevealed(player, session.container(), packet.entryId())) {
                    return;
                }
            }

            Optional<GridItemEntry> entryOpt = srcGrid.getEntry(packet.entryId());
            if (entryOpt.isEmpty()) return;
            GridItemEntry entry = entryOpt.get();
            ItemStack stack = entry.copyStack();
            ScavengerItemMeta meta = entry.resolveMeta();

            if (!meta.isAllowedInHotbar(stack)) {
                // 若为战术背囊且背囊槽为空 -> 优先穿戴
                if (PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.BACKPACK).isPresent() && !profile.hasBackpack()) {
                    srcGrid.removeEntry(packet.entryId());
                    profile.equipBackpack(stack, player.registryAccess());
                    markSourceDirty(player, profile, packet.source());
                    profile.markDirty();
                    syncProfile(player);
                    ModLog.debug(player, null, "网格快捷转移到快捷栏被拦截，改优先装备背囊: " + stack);
                    return;
                }
                // 若为护甲且对应部位为空 -> 优先穿戴
                net.minecraft.world.entity.EquipmentSlot equipSlot = player.getEquipmentSlotForItem(stack);
                if (equipSlot.isArmor() && player.getItemBySlot(equipSlot).isEmpty()) {
                    srcGrid.removeEntry(packet.entryId());
                    player.setItemSlot(equipSlot, stack);
                    markSourceDirty(player, profile, packet.source());
                    player.inventoryMenu.broadcastChanges();
                    profile.markDirty();
                    syncProfile(player);
                    ModLog.debug(player, null, "网格快捷转移到快捷栏被拦截，改优先穿戴护甲: " + equipSlot + " -> " + stack);
                    return;
                }

                player.sendSystemMessage(Component.literal("§c该物品大于 1×1 规格，无法放入随身快捷栏！"), true);
                return;
            }

            int slot = packet.targetX();
            if (slot < 0 || slot >= 9) {
                slot = -1;
                for (int i = 0; i < 9; i++) {
                    if (player.getInventory().getItem(i).isEmpty()) {
                        slot = i;
                        break;
                    }
                }
            }
            if (slot < 0 || slot >= 9) {
                player.sendSystemMessage(Component.literal("§c随身快捷栏对应槽位不可用！"), true);
                return;
            }

            ItemStack existing = player.getInventory().getItem(slot);
            if (existing.isEmpty()) {
                player.getInventory().setItem(slot, stack);
                srcGrid.removeEntry(packet.entryId());
                markSourceDirty(player, profile, packet.source());
                player.inventoryMenu.broadcastChanges();
                ModLog.debug(player, null, "网格物品成功移入快捷栏槽位 " + slot + ": " + meta.itemId());
            } else {
                ScavengerItemMeta existingMeta = ScavengerDataRegistry.resolveItemMeta(existing);
                if (existing.getCount() == 1
                        && srcGrid.acceptsItem(existing)
                        && srcGrid.canPlace(existingMeta, entry.anchorX(), entry.anchorY(), entry.rotated(), entry.entryId())) {
                    srcGrid.removeEntry(packet.entryId());
                    GridPlacementResult placed = srcGrid.placeAt(
                            existing.copyWithCount(1), entry.anchorX(), entry.anchorY(), entry.rotated(), 0, false
                    );
                    if (!placed.isSuccess()) {
                        srcGrid.restoreEntry(entry);
                        return;
                    }
                    if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && placed.placedEntry() != null) {
                        SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                        if (session != null && session.container() != null) {
                            session.container().scavengerLootState().markRevealedFor(placed.placedEntry().entryId(), player.getUUID());
                        }
                    }
                    player.getInventory().setItem(slot, stack);
                    markSourceDirty(player, profile, packet.source());
                    player.inventoryMenu.broadcastChanges();
                    ModLog.debug(player, null, "网格与快捷栏互换成功");
                }
            }
            return;
        }

        // 2. 源是快捷栏 (HOTBAR)
        if (packet.source() == ScavengerGridActionC2SPacket.GridSource.HOTBAR) {
            GridInventory dstGrid = resolveGridBySource(player, profile, packet.destination());
            if (dstGrid == null) return;

            int slot = packet.entryId();
            if (slot < 0 || slot >= 9) {
                slot = player.getInventory().getSelectedSlot();
            }
            ItemStack hotbarStack = player.getInventory().getItem(slot);
            if (hotbarStack.isEmpty()) return;

            GridPlacementResult fit = dstGrid.placeAt(
                    hotbarStack.copyWithCount(1),
                    packet.targetX(),
                    packet.targetY(),
                    packet.rotated(),
                    0,
                    false
            );
            if (fit.isSuccess()) {
                hotbarStack.shrink(1);
                if (packet.destination() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && fit.placedEntry() != null) {
                    SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                    if (session != null && session.container() != null) {
                        session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                    }
                }
                markSourceDirty(player, profile, packet.destination());
                player.inventoryMenu.broadcastChanges();
                ModLog.debug(player, null, "快捷栏物品成功放入网格: " + packet.destination());
            }
            return;
        }

        // 3. 两个网格之间 (CONTAINER, BACKPACK, SAFEBOX, BASE_STASH)
        GridInventory srcGrid = resolveGridBySource(player, profile, packet.source());
        GridInventory dstGrid = resolveGridBySource(player, profile, packet.destination());
        if (srcGrid == null || dstGrid == null) return;

        if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER) {
            SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
            if (session == null || !isContainerEntryRevealed(player, session.container(), packet.entryId())) {
                return;
            }
        }

        Optional<GridItemEntry> srcEntryOpt = srcGrid.getEntry(packet.entryId());
        if (srcEntryOpt.isEmpty()) return;
        GridItemEntry srcEntry = srcEntryOpt.get();

        GridPlacementResult fit = dstGrid.placeAt(
                srcEntry.copyStack(),
                packet.targetX(),
                packet.targetY(),
                packet.rotated(),
                srcEntry.solidifiedSearchTicks(),
                false
        );
        if (fit.isSuccess()) {
            srcGrid.removeEntry(packet.entryId());
            if (packet.destination() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && fit.placedEntry() != null) {
                SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                if (session != null && session.container() != null) {
                    session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                }
            }
            markSourceDirty(player, profile, packet.source());
            markSourceDirty(player, profile, packet.destination());
            ModLog.debug(player, null, "跨网格移动成功: " + packet.source() + " -> " + packet.destination() + ", entry=" + packet.entryId());
        }
    }

    private static void handleQuickTransfer(ServerPlayer player, PlayerCorridorProfile profile, ScavengerGridActionC2SPacket packet) {
        // 1. 目标是快捷栏 (HOTBAR)
        if (packet.destination() == ScavengerGridActionC2SPacket.GridSource.HOTBAR) {
            GridInventory srcGrid = resolveGridBySource(player, profile, packet.source());
            if (srcGrid == null) return;

            if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER) {
                SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                if (session == null || !isContainerEntryRevealed(player, session.container(), packet.entryId())) {
                    return;
                }
            }

            Optional<GridItemEntry> srcEntryOpt = srcGrid.getEntry(packet.entryId());
            if (srcEntryOpt.isEmpty()) return;
            GridItemEntry srcEntry = srcEntryOpt.get();
            ItemStack stack = srcEntry.copyStack();
            ScavengerItemMeta meta = srcEntry.resolveMeta();

            if (!meta.isAllowedInHotbar(stack)) {
                // 若为战术背囊且背囊槽为空 -> 优先穿戴
                if (PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.BACKPACK).isPresent() && !profile.hasBackpack()) {
                    srcGrid.removeEntry(packet.entryId());
                    profile.equipBackpack(stack, player.registryAccess());
                    markSourceDirty(player, profile, packet.source());
                    profile.markDirty();
                    syncProfile(player);
                    ModLog.debug(player, null, "网格快捷转移到快捷栏被拦截，改优先装备背囊: " + stack);
                    return;
                }
                // 若为护甲且对应部位为空 -> 优先穿戴
                net.minecraft.world.entity.EquipmentSlot equipSlot = player.getEquipmentSlotForItem(stack);
                if (equipSlot.isArmor() && player.getItemBySlot(equipSlot).isEmpty()) {
                    srcGrid.removeEntry(packet.entryId());
                    player.setItemSlot(equipSlot, stack);
                    markSourceDirty(player, profile, packet.source());
                    player.inventoryMenu.broadcastChanges();
                    profile.markDirty();
                    syncProfile(player);
                    ModLog.debug(player, null, "网格快捷转移到快捷栏被拦截，改优先穿戴护甲: " + equipSlot + " -> " + stack);
                    return;
                }

                player.sendSystemMessage(Component.literal("§c该物品大于 1×1 规格，无法放入随身快捷栏！"), true);
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getItem(i).isEmpty()) {
                    player.getInventory().setItem(i, stack);
                    srcGrid.removeEntry(packet.entryId());
                    markSourceDirty(player, profile, packet.source());
                    player.inventoryMenu.broadcastChanges();
                    ModLog.debug(player, null, "网格物品快速转移至快捷栏槽位 " + i + ": " + meta.itemId());
                    return;
                }
            }
            player.sendSystemMessage(Component.literal("§c随身快捷栏已满！"), true);
            return;
        }

        // 2. 源是快捷栏 (HOTBAR)
        if (packet.source() == ScavengerGridActionC2SPacket.GridSource.HOTBAR) {
            GridInventory dstGrid = resolveGridBySource(player, profile, packet.destination());
            if (dstGrid == null) return;

            int slot = packet.entryId();
            if (slot < 0 || slot >= 9) {
                slot = player.getInventory().getSelectedSlot();
            }
            ItemStack hotbarStack = player.getInventory().getItem(slot);
            if (hotbarStack.isEmpty()) return;

            GridPlacementResult fit = dstGrid.quickInsertFirstFit(hotbarStack.copyWithCount(1), 0);
            if (fit.isSuccess()) {
                hotbarStack.shrink(1);
                if (packet.destination() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && fit.placedEntry() != null) {
                    SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                    if (session != null && session.container() != null) {
                        session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                    }
                }
                markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.HOTBAR);
                markSourceDirty(player, profile, packet.destination());
                ModLog.debug(player, null, "快捷栏物品快速转移至网格: " + packet.destination());
            }
            return;
        }

        // 3. 两个网格之间 (CONTAINER, BACKPACK, SAFEBOX, BASE_STASH)
        GridInventory srcGrid = resolveGridBySource(player, profile, packet.source());
        GridInventory dstGrid = resolveGridBySource(player, profile, packet.destination());
        if (srcGrid != null && dstGrid != null && srcGrid != dstGrid) {
            if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER) {
                SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                if (session == null || !isContainerEntryRevealed(player, session.container(), packet.entryId())) {
                    return;
                }
            }

            Optional<GridItemEntry> srcEntry = srcGrid.getEntry(packet.entryId());
            if (srcEntry.isPresent()) {
                ItemStack stack = srcEntry.get().copyStack();
                // 1. 若为战术背囊且当前未装备背囊 -> 优先直接穿戴背囊！
                if (PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.BACKPACK).isPresent() && !profile.hasBackpack()) {
                    srcGrid.removeEntry(packet.entryId());
                    profile.equipBackpack(stack, player.registryAccess());
                    markSourceDirty(player, profile, packet.source());
                    profile.markDirty();
                    syncProfile(player);
                    ModLog.debug(player, null, "网格快捷转移：直接装备背囊: " + stack);
                    return;
                }
                // 2. 若为护甲且对应部位为空 -> 优先直接穿戴护甲！
                net.minecraft.world.entity.EquipmentSlot equipSlot = player.getEquipmentSlotForItem(stack);
                if (equipSlot.isArmor() && player.getItemBySlot(equipSlot).isEmpty()) {
                    srcGrid.removeEntry(packet.entryId());
                    player.setItemSlot(equipSlot, stack);
                    markSourceDirty(player, profile, packet.source());
                    player.inventoryMenu.broadcastChanges();
                    profile.markDirty();
                    syncProfile(player);
                    ModLog.debug(player, null, "网格快捷转移：直接穿上护甲: " + equipSlot + " -> " + stack);
                    return;
                }

                GridPlacementResult fit = dstGrid.quickInsertFirstFit(
                        srcEntry.get().copyStack(),
                        srcEntry.get().solidifiedSearchTicks()
                );
                if (fit.isSuccess()) {
                    srcGrid.removeEntry(packet.entryId());
                    if (packet.destination() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && fit.placedEntry() != null) {
                        SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                        if (session != null && session.container() != null) {
                            session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                        }
                    }
                    markSourceDirty(player, profile, packet.source());
                    markSourceDirty(player, profile, packet.destination());
                    ModLog.debug(player, null, "网格间快速转移成功: " + packet.source() + " -> " + packet.destination());
                }
            }
        }
    }

    private static boolean isContainerEntryRevealed(ServerPlayer player, IScavengerLootContainer container, int entryId) {
        if (container == null) return false;
        ScavengerLootContainerState state = container.scavengerLootState();
        return state.isRevealedFor(entryId, player.getUUID(), teammateId -> {
            ServerPlayer other = player.level().getServer().getPlayerList().getPlayer(teammateId);
            return other != null && ScavengerTeamManager.isSameTeam(player, other);
        });
    }

    private static void markSourceDirty(
            ServerPlayer player,
            PlayerCorridorProfile profile,
            ScavengerGridActionC2SPacket.GridSource source
    ) {
        switch (source) {
            case BACKPACK, SAFEBOX, BASE_STASH -> profile.markDirty();
            case CONTAINER -> {
                SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                if (session != null) {
                    session.container().markContainerChanged();
                }
            }
            case HOTBAR -> {
                player.inventoryMenu.broadcastChanges();
                if (player.containerMenu != null) {
                    player.containerMenu.broadcastChanges();
                }
            }
        }
    }

    public static void handlePrioritizeSearch(ServerPlayer player, ScavengerPrioritizeSearchC2SPacket packet) {
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(packet.containerPos());
        if (player.distanceToSqr(center) > SearchSessionManager.MAX_INTERACTION_DISTANCE_SQ) {
            return;
        }
        SearchSessionManager.prioritizeEntry(player, packet.entryId());
    }

    public static void handleBaseUpgrade(ServerPlayer player, ScavengerBaseUpgradeC2SPacket packet) {
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(packet.stationPos());
        if (player.distanceToSqr(center) > SearchSessionManager.MAX_INTERACTION_DISTANCE_SQ) {
            player.sendSystemMessage(Component.literal("§c距离整备升级台过远！"));
            return;
        }
        if (!BaseFacilityService.isAuthorizedOwner(player, packet.stationPos())) {
            player.sendSystemMessage(Component.literal("§c你不是该私人基地设施的所有者！"));
            return;
        }

        if (!(player instanceof ScavengerPlayerTracker tracker)) {
            return;
        }
        PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();

        net.minecraft.core.NonNullList<ItemStack> hotbar = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            hotbar.set(i, player.getInventory().getItem(i));
        }

        BaseFacilityService.UpgradeResult result;
        if (packet.target() == ScavengerBaseUpgradeC2SPacket.UpgradeTarget.WEIGHT_SYSTEM) {
            result = BaseFacilityService.tryExecuteWeightUpgrade(profile, hotbar);
        } else {
            result = BaseFacilityService.tryExecuteStashUpgrade(profile, hotbar);
        }

        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, hotbar.get(i));
        }

        switch (result) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.literal("§a基地整备升级成功！"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            case MAX_LEVEL_REACHED -> player.sendSystemMessage(Component.literal("§e该系统已达到当前版本最高等级！"));
            case INSUFFICIENT_MATERIALS ->
                    player.sendSystemMessage(Component.literal("§c升级材料不足！请检查快捷栏、背包或基地仓库。"));
            case NO_COST_CONFIGURED -> player.sendSystemMessage(Component.literal("§7未配置下一级升级消耗。"));
        }

        syncProfile(player);

        SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
        if (session != null) {
            syncContainer(player, session.container());
        }
        if (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.BaseStashMenu stashMenu
                && stashMenu.stillValid(player)) {
            syncBaseStash(player, stashMenu.getCabinetPos());
        } else if (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.BaseUpgradeStationMenu stationMenu
                && stationMenu.stillValid(player)) {
            syncBaseStash(player, stationMenu.getStationPos());
        }
    }

    private static GridInventory resolveGridBySource(
            ServerPlayer player,
            PlayerCorridorProfile profile,
            ScavengerGridActionC2SPacket.GridSource source
    ) {
        return switch (source) {
            case BACKPACK -> profile.backpackGrid();
            case SAFEBOX -> profile.safeboxGrid();
            case BASE_STASH -> (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.BaseStashMenu menu
                    && menu.stillValid(player))
                    ? profile.baseStashGrid()
                    : null;
            case CONTAINER -> {
                SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                yield (session != null
                        && session.container() != null
                        && player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.ScavengerTacticalMenu menu
                        && menu.stillValid(player))
                        ? session.container().scavengerLootState().grid()
                        : null;
            }
            case HOTBAR -> null;
        };
    }

    private static void handleEquipFromGrid(ServerPlayer player, PlayerCorridorProfile profile, ScavengerGridActionC2SPacket packet) {
        boolean fromHotbar = (packet.source() == ScavengerGridActionC2SPacket.GridSource.HOTBAR);
        GridInventory srcGrid = fromHotbar ? null : resolveGridBySource(player, profile, packet.source());
        if (!fromHotbar && srcGrid == null) return;

        if (!fromHotbar && packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER) {
            SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
            if (session == null || !isContainerEntryRevealed(player, session.container(), packet.entryId())) {
                return;
            }
        }

        ItemStack hotbarStack = ItemStack.EMPTY;
        ItemStack stack;
        GridItemEntry entry = null;
        int hotbarSlot = packet.entryId();
        if (fromHotbar) {
            if (hotbarSlot < 0 || hotbarSlot >= 9) return;
            hotbarStack = player.getInventory().getItem(hotbarSlot);
            if (hotbarStack.isEmpty()) return;
            stack = hotbarStack.copyWithCount(1);
        } else {
            Optional<GridItemEntry> entryOpt = srcGrid.getEntry(packet.entryId());
            if (entryOpt.isEmpty()) return;
            entry = entryOpt.get();
            stack = entry.copyStack();
        }
        if (stack.isEmpty()) return;

        int targetSlot = packet.targetX(); // 0: 头, 1: 胸, 2: 腿, 3: 脚, 4: 副手, 5: 背包

        if (targetSlot == 5) {
            // 目标是战术背囊槽
            Optional<me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec> specOpt = PlayerCorridorProfile.resolveEquipmentSpec(stack, me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec.SlotType.BACKPACK);
            if (specOpt.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c该物品不是合法的战术背囊！"), true);
                return;
            }

            if (!profile.hasBackpack()) {
                if (fromHotbar) {
                    hotbarStack.shrink(1);
                    if (hotbarStack.isEmpty()) {
                        player.getInventory().setItem(hotbarSlot, ItemStack.EMPTY);
                    }
                } else {
                    srcGrid.removeEntry(packet.entryId());
                }
                profile.equipBackpack(stack, player.registryAccess());
                markSourceDirty(player, profile, packet.source());
                profile.markDirty();
                syncProfile(player);
                ModLog.debug(player, null, "成功从" + (fromHotbar ? "快捷栏" : "网格") + "装备背囊: " + stack);
            } else {
                // 已装备背囊 -> 尝试置换
                if (profile.backpackGrid() != null && !profile.backpackGrid().isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c请先清空当前背囊内的全部物资，再更换背囊！"), true);
                    return;
                }
                ItemStack oldBp = profile.equippedBackpack().copyWithCount(1);
                GridInventory.writeToContainerStack(oldBp, null, player.registryAccess());

                if (fromHotbar) {
                    if (hotbarStack.getCount() == 1) {
                        hotbarStack.shrink(1);
                        player.getInventory().setItem(hotbarSlot, oldBp);
                    } else {
                        int emptySlot = findEmptyHotbarSlot(player);
                        if (emptySlot == -1) {
                            player.sendSystemMessage(Component.literal("§c快捷栏空间不足，无法容纳替换下的背囊！"), true);
                            return;
                        }
                        hotbarStack.shrink(1);
                        player.getInventory().setItem(emptySlot, oldBp);
                    }
                } else {
                    srcGrid.removeEntry(packet.entryId());
                    if (!oldBp.isEmpty()) {
                        GridPlacementResult fit = srcGrid.quickInsertFirstFit(oldBp, 0);
                        if (!fit.isSuccess()) {
                            srcGrid.restoreEntry(entry);
                            player.sendSystemMessage(Component.literal("§c网格空间不足，无法容纳替换下的背囊！"), true);
                            return;
                        }
                        if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && fit.placedEntry() != null) {
                            SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                            if (session != null && session.container() != null) {
                                session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                            }
                        }
                    }
                }
                profile.unequipBackpack(player.registryAccess());
                profile.equipBackpack(stack, player.registryAccess());
                markSourceDirty(player, profile, packet.source());
                profile.markDirty();
                syncProfile(player);
                ModLog.debug(player, null, "成功置换背囊: " + stack);
            }
            return;
        }

        // 目标是护甲槽或副手槽
        net.minecraft.world.entity.EquipmentSlot equipSlot = null;
        switch (targetSlot) {
            case 0 -> equipSlot = net.minecraft.world.entity.EquipmentSlot.HEAD;
            case 1 -> equipSlot = net.minecraft.world.entity.EquipmentSlot.CHEST;
            case 2 -> equipSlot = net.minecraft.world.entity.EquipmentSlot.LEGS;
            case 3 -> equipSlot = net.minecraft.world.entity.EquipmentSlot.FEET;
            case 4 -> equipSlot = net.minecraft.world.entity.EquipmentSlot.OFFHAND;
        }
        if (equipSlot == null) return;

        // 检查该物品是否能装备在该部位
        boolean canEquip = false;
        if (targetSlot == 4) {
            canEquip = true; // 副手允许放置任意物品
        } else {
            net.minecraft.world.entity.EquipmentSlot naturalSlot = player.getEquipmentSlotForItem(stack);
            if (naturalSlot == equipSlot) {
                canEquip = true;
            }
        }

        if (!canEquip) {
            player.sendSystemMessage(Component.literal("§c该物品无法装备到此部位！"), true);
            return;
        }

        ItemStack currentEquipped = player.getItemBySlot(equipSlot);
        if (currentEquipped.isEmpty()) {
            if (fromHotbar) {
                hotbarStack.shrink(1);
                if (hotbarStack.isEmpty()) {
                    player.getInventory().setItem(hotbarSlot, ItemStack.EMPTY);
                }
            } else {
                srcGrid.removeEntry(packet.entryId());
            }
            player.setItemSlot(equipSlot, stack);
            markSourceDirty(player, profile, packet.source());
            player.inventoryMenu.broadcastChanges();
            profile.markDirty();
            syncProfile(player);
            ModLog.debug(player, null, "成功装备护甲到 " + equipSlot + ": " + stack);
        } else {
            // 置换已有的旧护甲
            ItemStack oldCopy = currentEquipped.copy();
            if (fromHotbar) {
                if (hotbarStack.getCount() == 1) {
                    hotbarStack.shrink(1);
                    player.getInventory().setItem(hotbarSlot, oldCopy);
                    player.setItemSlot(equipSlot, stack);
                } else {
                    int emptySlot = findEmptyHotbarSlot(player);
                    if (emptySlot == -1) {
                        player.sendSystemMessage(Component.literal("§c快捷栏空间不足，无法容纳替换下的装备！"), true);
                        return;
                    }
                    hotbarStack.shrink(1);
                    player.getInventory().setItem(emptySlot, oldCopy);
                    player.setItemSlot(equipSlot, stack);
                }
            } else {
                if (currentEquipped.getCount() > 1) {
                    String msg = (targetSlot == 4)
                            ? "§c副手物品堆叠数量大于 1，无法直接与网格物品置换！"
                            : "§c当前装备槽位物品堆叠数量大于 1，无法直接与网格物品置换！";
                    player.sendSystemMessage(Component.literal(msg), true);
                    return;
                }
                srcGrid.removeEntry(packet.entryId());
                GridPlacementResult fit = srcGrid.quickInsertFirstFit(oldCopy, 0);
                if (!fit.isSuccess()) {
                    // 如果放不进原网格，无损恢复原条目（保留原 entryId 与揭示状态）
                    srcGrid.restoreEntry(entry);
                    player.sendSystemMessage(Component.literal("§c网格空间不足，无法容纳替换下的装备！"), true);
                    return;
                }
                if (packet.source() == ScavengerGridActionC2SPacket.GridSource.CONTAINER && fit.placedEntry() != null) {
                    SearchSessionManager.SearchSession session = SearchSessionManager.getSession(player.getUUID());
                    if (session != null && session.container() != null) {
                        session.container().scavengerLootState().markRevealedFor(fit.placedEntry().entryId(), player.getUUID());
                    }
                }
                player.setItemSlot(equipSlot, stack);
            }
            markSourceDirty(player, profile, packet.source());
            player.inventoryMenu.broadcastChanges();
            profile.markDirty();
            syncProfile(player);
            ModLog.debug(player, null, "成功置换护甲槽 " + equipSlot + ": " + stack);
        }
    }

    private static int findEmptyHotbarSlot(ServerPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static void handleSwitchSafebox(ServerPlayer player, PlayerCorridorProfile profile, ScavengerGridActionC2SPacket packet) {
        int tierIndex = packet.targetX();
        if (tierIndex < 0 || tierIndex >= SAFEBOX_TIERS.length) return;
        net.minecraft.world.item.Item targetItem = SAFEBOX_TIERS[tierIndex];

        if (profile.hasSafebox() && profile.equippedSafebox().is(targetItem)) {
            player.sendSystemMessage(Component.literal("§e当前已装配该规格安全箱！"), true);
            return;
        }

        // 严格遵循非空不可更换规则
        if (profile.hasSafebox() && profile.safeboxGrid() != null && !profile.safeboxGrid().isEmpty()) {
            player.sendSystemMessage(Component.literal("§c请先清空当前安全箱内的全部物资，再更换黑匣规格！"), true);
            return;
        }

        boolean canAccessBaseStash = (player.containerMenu instanceof me.tuanzi.world.inventory.scavenger.BaseStashMenu stashMenu
                && stashMenu.stillValid(player));

        // 检查持有资格：创造模式直接放行；生存/冒险模式检查玩家随身/仓库是否持有该道具
        boolean isCreative = player.isCreative();
        boolean foundItem = isCreative;
        int foundHotbarSlot = -1;
        ScavengerGridActionC2SPacket.GridSource foundSource = null;
        int foundEntryId = -1;

        if (!foundItem) {
            // 查快捷栏
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getItem(i).is(targetItem)) {
                    foundItem = true;
                    foundHotbarSlot = i;
                    break;
                }
            }
            // 查随身背包
            if (!foundItem && profile.hasBackpack() && profile.backpackGrid() != null) {
                for (GridItemEntry entry : profile.backpackGrid().entries()) {
                    if (entry.copyStack().is(targetItem)) {
                        foundItem = true;
                        foundSource = ScavengerGridActionC2SPacket.GridSource.BACKPACK;
                        foundEntryId = entry.entryId();
                        break;
                    }
                }
            }
            // 查基地仓库（仅当正打开合法基地仓库界面时）
            if (!foundItem && canAccessBaseStash && profile.baseStashGrid() != null) {
                for (GridItemEntry entry : profile.baseStashGrid().entries()) {
                    if (entry.copyStack().is(targetItem)) {
                        foundItem = true;
                        foundSource = ScavengerGridActionC2SPacket.GridSource.BASE_STASH;
                        foundEntryId = entry.entryId();
                        break;
                    }
                }
            }
        }

        if (!foundItem) {
            player.sendSystemMessage(Component.literal("§c你尚未持有该黑匣安全箱道具，无法装配！"), true);
            return;
        }

        // 消耗新安全箱，退还旧安全箱（两阶段校验与失败回滚）
        ItemStack oldSafebox = profile.hasSafebox() ? profile.equippedSafebox().copyWithCount(1) : ItemStack.EMPTY;
        if (!oldSafebox.isEmpty()) {
            GridInventory.writeToContainerStack(oldSafebox, null, player.registryAccess());
        }
        if (!isCreative) {
            if (foundHotbarSlot != -1) {
                ItemStack hotbarStack = player.getInventory().getItem(foundHotbarSlot);
                if (oldSafebox.isEmpty()) {
                    hotbarStack.shrink(1);
                    if (hotbarStack.isEmpty()) {
                        player.getInventory().setItem(foundHotbarSlot, ItemStack.EMPTY);
                    }
                } else if (hotbarStack.getCount() == 1) {
                    hotbarStack.shrink(1);
                    player.getInventory().setItem(foundHotbarSlot, oldSafebox);
                } else {
                    int emptySlot = findEmptyHotbarSlot(player);
                    if (emptySlot != -1) {
                        hotbarStack.shrink(1);
                        player.getInventory().setItem(emptySlot, oldSafebox);
                    } else if (profile.hasBackpack() && profile.backpackGrid() != null
                            && profile.backpackGrid().quickInsertFirstFit(oldSafebox, 0).isSuccess()) {
                        hotbarStack.shrink(1);
                        markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BACKPACK);
                    } else if (canAccessBaseStash && profile.baseStashGrid() != null
                            && profile.baseStashGrid().quickInsertFirstFit(oldSafebox, 0).isSuccess()) {
                        hotbarStack.shrink(1);
                        markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BASE_STASH);
                    } else {
                        player.sendSystemMessage(Component.literal("§c快捷栏与当前可用网格空间不足，无法容纳替换下的安全箱！"), true);
                        return;
                    }
                }
            } else if (foundSource == ScavengerGridActionC2SPacket.GridSource.BACKPACK && profile.backpackGrid() != null) {
                Optional<GridItemEntry> removedOpt = profile.backpackGrid().removeEntry(foundEntryId);
                if (removedOpt.isEmpty()) return;
                GridItemEntry removedEntry = removedOpt.get();
                if (!oldSafebox.isEmpty()) {
                    boolean stored = profile.backpackGrid().quickInsertFirstFit(oldSafebox, 0).isSuccess();
                    if (!stored && canAccessBaseStash && profile.baseStashGrid() != null) {
                        stored = profile.baseStashGrid().quickInsertFirstFit(oldSafebox, 0).isSuccess();
                        if (stored) {
                            markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BASE_STASH);
                        }
                    }
                    if (!stored) {
                        int emptySlot = findEmptyHotbarSlot(player);
                        if (emptySlot != -1) {
                            player.getInventory().setItem(emptySlot, oldSafebox);
                            markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.HOTBAR);
                            stored = true;
                        }
                    }
                    if (!stored) {
                        profile.backpackGrid().restoreEntry(removedEntry);
                        player.sendSystemMessage(Component.literal("§c空间不足，无法容纳替换下的安全箱！"), true);
                        return;
                    }
                }
                markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BACKPACK);
            } else if (foundSource == ScavengerGridActionC2SPacket.GridSource.BASE_STASH && canAccessBaseStash && profile.baseStashGrid() != null) {
                Optional<GridItemEntry> removedOpt = profile.baseStashGrid().removeEntry(foundEntryId);
                if (removedOpt.isEmpty()) return;
                GridItemEntry removedEntry = removedOpt.get();
                if (!oldSafebox.isEmpty()) {
                    boolean stored = profile.baseStashGrid().quickInsertFirstFit(oldSafebox, 0).isSuccess();
                    if (!stored && profile.hasBackpack() && profile.backpackGrid() != null) {
                        stored = profile.backpackGrid().quickInsertFirstFit(oldSafebox, 0).isSuccess();
                        if (stored) {
                            markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BACKPACK);
                        }
                    }
                    if (!stored) {
                        int emptySlot = findEmptyHotbarSlot(player);
                        if (emptySlot != -1) {
                            player.getInventory().setItem(emptySlot, oldSafebox);
                            markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.HOTBAR);
                            stored = true;
                        }
                    }
                    if (!stored) {
                        profile.baseStashGrid().restoreEntry(removedEntry);
                        player.sendSystemMessage(Component.literal("§c基地仓库空间不足，无法容纳替换下的安全箱！"), true);
                        return;
                    }
                }
                markSourceDirty(player, profile, ScavengerGridActionC2SPacket.GridSource.BASE_STASH);
            } else {
                return;
            }
        }

        // 卸下旧安全箱并装备新安全箱
        if (profile.hasSafebox()) {
            profile.unequipSafebox(player.registryAccess());
        }
        profile.equipSafebox(new ItemStack(targetItem), player.registryAccess());
        profile.markDirty();
        syncProfile(player);
        player.inventoryMenu.broadcastChanges();

        player.sendSystemMessage(Component.literal("§a已成功装配安全箱规格: " + targetItem.getName(new ItemStack(targetItem)).getString()), true);
        ModLog.debug(player, null, "安全箱规格切换成功: " + targetItem);
    }
}
