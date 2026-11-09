package me.tuanzi.client.scavenger;

import me.tuanzi.network.scavenger.ScavengerContainerSyncS2CPacket;
import me.tuanzi.network.scavenger.ScavengerItemRevealedS2CPacket;
import me.tuanzi.network.scavenger.ScavengerProfileSyncS2CPacket;
import me.tuanzi.network.scavenger.ScavengerSearchProgressS2CPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerRarity;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import me.tuanzi.world.scavenger.loot.search.SearchEntryPhase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拾荒回廊客户端权威状态镜像与缓存中心。
 * 缓存个人战术档案、当前打开搜刮容器脱敏快照及逐刻搜寻进度。
 */
@Environment(EnvType.CLIENT)
public final class ScavengerClientState {

    // ==================== 个人档案状态 ====================
    private static ItemStack equippedBackpack = ItemStack.EMPTY;
    private static int backpackColumns = 0;
    private static int backpackRows = 0;
    private static final List<GridItemEntry> backpackEntries = new ArrayList<>();

    private static ItemStack equippedSafebox = ItemStack.EMPTY;
    private static int safeboxColumns = 0;
    private static int safeboxRows = 0;
    private static final List<GridItemEntry> safeboxEntries = new ArrayList<>();

    private static int weightLevel = 1;
    private static int stashLevel = 1;
    private static float totalWeightKg = 0.0f;
    private static boolean isOverloaded = false;

    // ==================== 活动容器状态 ====================
    private static @Nullable Identifier containerDefId = null;
    private static BlockPos containerPos = BlockPos.ZERO;
    private static int containerColumns = 0;
    private static int containerRows = 0;
    private static final Map<Integer, RedactedGridEntryView> containerEntries = new HashMap<>();

    // ==================== 逐刻搜索状态 ====================
    private static int activeSearchEntryId = -1;
    private static int activeSearchProgressTicks = 0;
    private static int activeSearchTotalTicks = 20;
    private static float activePulseThreshold = 0.0f;
    private static ScavengerRarity activePulseRarity = ScavengerRarity.COMMON;

    private ScavengerClientState() {
    }

    public static synchronized void updateProfile(ScavengerProfileSyncS2CPacket pkt) {
        equippedBackpack = pkt.equippedBackpack() != null ? pkt.equippedBackpack().copy() : ItemStack.EMPTY;
        backpackColumns = pkt.backpackColumns();
        backpackRows = pkt.backpackRows();
        backpackEntries.clear();
        if (pkt.backpackEntries() != null) {
            backpackEntries.addAll(pkt.backpackEntries());
        }

        equippedSafebox = pkt.equippedSafebox() != null ? pkt.equippedSafebox().copy() : ItemStack.EMPTY;
        safeboxColumns = pkt.safeboxColumns();
        safeboxRows = pkt.safeboxRows();
        safeboxEntries.clear();
        if (pkt.safeboxEntries() != null) {
            safeboxEntries.addAll(pkt.safeboxEntries());
        }

        weightLevel = pkt.weightLevel();
        stashLevel = pkt.stashLevel();
        totalWeightKg = pkt.totalWeightKg();
        isOverloaded = pkt.isOverloaded();

        ModLog.debug((net.minecraft.world.entity.Entity) null, (net.minecraft.world.entity.Entity) null, String.format(
                "【客户端回廊状态】个人档案已同步: 背包=%s(%dx%d, %d件), 安全箱=%s(%dx%d, %d件), 负重Lv.%d(%.2fkg, 超负荷=%b)",
                equippedBackpack.getItem(), backpackColumns, backpackRows, backpackEntries.size(),
                equippedSafebox.getItem(), safeboxColumns, safeboxRows, safeboxEntries.size(),
                weightLevel, totalWeightKg, isOverloaded
        ));

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.screen() instanceof me.tuanzi.client.gui.screens.scavenger.CorridorTacticalLoadoutScreen screen) {
            mc.execute(screen::refreshLoadout);
        }
    }

    public static synchronized void updateContainer(ScavengerContainerSyncS2CPacket pkt) {
        containerDefId = pkt.containerDefId();
        containerPos = pkt.containerPos();
        containerColumns = pkt.columns();
        containerRows = pkt.rows();
        containerEntries.clear();
        if (pkt.entries() != null) {
            for (RedactedGridEntryView view : pkt.entries()) {
                containerEntries.put(view.entryId(), view);
            }
        }
        ModLog.debug((net.minecraft.world.entity.Entity) null, (net.minecraft.world.entity.Entity) null, String.format(
                "【客户端回廊状态】容器快照已同步: %s (%s, %dx%d, %d条目)",
                containerDefId, containerPos.toShortString(), containerColumns, containerRows, containerEntries.size()
        ));
    }

    public static synchronized void updateSearchProgress(ScavengerSearchProgressS2CPacket pkt) {
        if (!containerPos.equals(pkt.containerPos())) {
            return;
        }
        activeSearchEntryId = pkt.entryId();
        activeSearchProgressTicks = pkt.progressTicks();
        activeSearchTotalTicks = pkt.totalTicks();
        activePulseThreshold = pkt.pulseThreshold();
        activePulseRarity = pkt.rarity();

        RedactedGridEntryView existing = containerEntries.get(pkt.entryId());
        if (existing != null) {
            containerEntries.put(pkt.entryId(), new RedactedGridEntryView(
                    existing.entryId(),
                    existing.anchorX(),
                    existing.anchorY(),
                    existing.width(),
                    existing.height(),
                    existing.rotated(),
                    SearchEntryPhase.SEARCHING,
                    activeSearchProgressTicks,
                    activeSearchTotalTicks,
                    existing.stack()
            ));
        }

        // 播放战术搜刮扫描音效
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            if (activePulseThreshold > 0) {
                // 稀有度脉冲高潮音效
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.6f
                ));
            } else if (activeSearchProgressTicks > 0 && activeSearchProgressTicks % 4 == 0) {
                // 逐刻雷达搜寻扫描滴答音效
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.8f, 0.2f
                ));
            }
        }
    }

    public static synchronized void updateItemRevealed(ScavengerItemRevealedS2CPacket pkt) {
        if (!containerPos.equals(pkt.containerPos())) {
            return;
        }
        RedactedGridEntryView existing = containerEntries.get(pkt.entryId());
        if (existing != null) {
            containerEntries.put(pkt.entryId(), new RedactedGridEntryView(
                    existing.entryId(),
                    existing.anchorX(),
                    existing.anchorY(),
                    existing.width(),
                    existing.height(),
                    existing.rotated(),
                    SearchEntryPhase.REVEALED,
                    existing.totalSearchTicks(),
                    existing.totalSearchTicks(),
                    pkt.stack().copy()
            ));
        }
        if (activeSearchEntryId == pkt.entryId()) {
            activeSearchEntryId = -1;
            activeSearchProgressTicks = 0;
        }

        // 播放搜刮揭示成功音效
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 1.2f, 0.8f
            ));
            if (!pkt.stack().isEmpty()) {
                ScavengerItemMeta meta = me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry.resolveItemMeta(pkt.stack());
                if (meta.rarity().ordinal() >= ScavengerRarity.EPIC.ordinal()) {
                    mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.6f, 0.5f
                    ));
                }
            }
        }

        ModLog.debug((net.minecraft.world.entity.Entity) null, (net.minecraft.world.entity.Entity) null, "【客户端回廊状态】条目已揭示: id=" + pkt.entryId() + " -> " + pkt.stack());
    }

    public static synchronized void clearContainer() {
        containerDefId = null;
        containerPos = BlockPos.ZERO;
        containerColumns = 0;
        containerRows = 0;
        containerEntries.clear();
        activeSearchEntryId = -1;
        activeSearchProgressTicks = 0;
    }

    // ==================== Getters ====================

    public static ItemStack getEquippedBackpack() { return equippedBackpack; }
    public static int getBackpackColumns() { return backpackColumns; }
    public static int getBackpackRows() { return backpackRows; }
    public static List<GridItemEntry> getBackpackEntries() { return new ArrayList<>(backpackEntries); }

    public static ItemStack getEquippedSafebox() { return equippedSafebox; }
    public static int getSafeboxColumns() { return safeboxColumns; }
    public static int getSafeboxRows() { return safeboxRows; }
    public static List<GridItemEntry> getSafeboxEntries() { return new ArrayList<>(safeboxEntries); }

    public static int getWeightLevel() { return weightLevel; }
    public static int getStashLevel() { return stashLevel; }
    public static float getTotalWeightKg() { return totalWeightKg; }
    public static boolean isOverloaded() { return isOverloaded; }

    public static @Nullable Identifier getContainerDefId() { return containerDefId; }
    public static BlockPos getContainerPos() { return containerPos; }
    public static int getContainerColumns() { return containerColumns; }
    public static int getContainerRows() { return containerRows; }
    public static Map<Integer, RedactedGridEntryView> getContainerEntries() { return new HashMap<>(containerEntries); }

    public static int getActiveSearchEntryId() { return activeSearchEntryId; }
    public static int getActiveSearchProgressTicks() { return activeSearchProgressTicks; }
    public static int getActiveSearchTotalTicks() { return activeSearchTotalTicks; }
    public static float getActivePulseThreshold() { return activePulseThreshold; }
    public static ScavengerRarity getActivePulseRarity() { return activePulseRarity; }
}
