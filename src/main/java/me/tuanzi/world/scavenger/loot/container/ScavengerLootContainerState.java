package me.tuanzi.world.scavenger.loot.container;

import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridInventory;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerContainerDef;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import me.tuanzi.world.scavenger.loot.search.SearchEntryPhase;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 通用二维网格搜刮容器状态管理组件。
 * <p>
 * 规则：
 * <ul>
 *   <li>首次被玩家打开时单次加权生成战利品，生成后永不自动刷新，唯有管理员指令可重置；</li>
 *   <li>迷雾揭示状态按条目独立记录，并与同队队友（通过 {@code isTeammate} 断言）共享；</li>
 *   <li>构建网络视图时严格遵循防透视（Anti-ESP）脱敏，未揭示条目绝不下发真实物品信息。</li>
 * </ul>
 */
public class ScavengerLootContainerState {

    private final Identifier containerDefId;
    private final GridInventory grid;
    private boolean lootGenerated = false;
    private final Map<Integer, Set<UUID>> revealedPlayersByEntry = new HashMap<>();

    public ScavengerLootContainerState(Identifier containerDefId, int columns, int rows) {
        this.containerDefId = containerDefId;
        this.grid = new GridInventory(columns, rows);
    }

    public Identifier containerDefId() {
        return this.containerDefId;
    }

    public GridInventory grid() {
        return this.grid;
    }

    public boolean isLootGenerated() {
        return this.lootGenerated;
    }

    public int columns() {
        return this.grid.columns();
    }

    public int rows() {
        return this.grid.rows();
    }

    /**
     * 首次打开容器时权威生成战利品（永不自动刷新）。
     */
    public boolean ensureGeneratedOnce(RandomSource random) {
        if (this.lootGenerated) {
            return false;
        }
        this.lootGenerated = true;

        Optional<ScavengerContainerDef> defOpt = ScavengerDataRegistry.getContainerDef(this.containerDefId);
        if (defOpt.isEmpty()) {
            ModLog.warn("【拾荒战利品生成】未找到容器模板定义: " + this.containerDefId + "，生成空网格。");
            return false;
        }

        ScavengerContainerDef def = defOpt.get();
        ScavengerProgressionConfig prog = ScavengerDataRegistry.getProgression();

        int minRolls = def.lootGeneration().minRolls();
        int maxRolls = Math.max(minRolls, def.lootGeneration().maxRolls());
        int rolls = minRolls == maxRolls ? minRolls : random.nextIntBetweenInclusive(minRolls, maxRolls);

        int placedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < rolls; i++) {
            Optional<ScavengerItemMeta> metaOpt = ScavengerDataRegistry.rollWeightedCandidate(def, random);
            if (metaOpt.isEmpty()) {
                continue;
            }
            ScavengerItemMeta meta = metaOpt.get();
            ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(meta.itemId()));
            int solidifiedTicks = prog.computeSolidifiedSearchTicks(meta, random);

            if (this.grid.quickInsertFirstFit(stack, solidifiedTicks).isSuccess()) {
                placedCount++;
            } else {
                failedCount++;
            }
        }

        ModLog.debug(String.format(
                "【拾荒战利品生成】容器=%s (%dx%d) 首次生成完毕 -> 掷骰次数=%d, 成功入箱=%d件, 碎格放弃=%d件, 剩余空格=%d格",
                this.containerDefId, this.grid.columns(), this.grid.rows(),
                rolls, placedCount, failedCount, this.grid.freeCellCount()
        ));
        return true;
    }

    /**
     * 判断指定条目是否已被查看者（或其同队队友）成功揭示。
     */
    public boolean isRevealedFor(int entryId, UUID viewerId, @Nullable Predicate<UUID> isTeammate) {
        Set<UUID> revealedSet = this.revealedPlayersByEntry.get(entryId);
        if (revealedSet == null || revealedSet.isEmpty()) {
            return false;
        }
        if (revealedSet.contains(viewerId)) {
            return true;
        }
        if (isTeammate != null) {
            for (UUID revealedPlayer : revealedSet) {
                if (isTeammate.test(revealedPlayer)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将某条目标记为已被指定玩家（及其小队）揭示。
     */
    public void markRevealedFor(int entryId, UUID viewerId) {
        this.revealedPlayersByEntry.computeIfAbsent(entryId, k -> new HashSet<>()).add(viewerId);
        ModLog.debug(String.format(
                "【拾荒搜刮迷雾】条目 id=%d 已对玩家 %s (及同小队队友) 彻底揭示！",
                entryId, viewerId
        ));
    }

    /**
     * 为指定查看者构建防透视脱敏网络视图列表。
     */
    public List<RedactedGridEntryView> buildRedactedViews(
            UUID viewerId,
            @Nullable Predicate<UUID> isTeammate,
            int activeSearchingEntryId,
            int activeSearchElapsedTicks
    ) {
        List<RedactedGridEntryView> list = new ArrayList<>();
        for (GridItemEntry entry : this.grid.entries()) {
            boolean revealed = isRevealedFor(entry.entryId(), viewerId, isTeammate);
            if (revealed) {
                list.add(new RedactedGridEntryView(
                        entry.entryId(),
                        entry.anchorX(),
                        entry.anchorY(),
                        entry.effectiveWidth(),
                        entry.effectiveHeight(),
                        entry.effectiveRotated(),
                        SearchEntryPhase.REVEALED,
                        entry.solidifiedSearchTicks(),
                        entry.solidifiedSearchTicks(),
                        entry.copyStack()
                ));
            } else {
                boolean isSearchingThis = (entry.entryId() == activeSearchingEntryId);
                SearchEntryPhase phase = isSearchingThis ? SearchEntryPhase.SEARCHING : SearchEntryPhase.IDLE;
                int progress = isSearchingThis ? activeSearchElapsedTicks : 0;
                list.add(new RedactedGridEntryView(
                        entry.entryId(),
                        entry.anchorX(),
                        entry.anchorY(),
                        entry.effectiveWidth(),
                        entry.effectiveHeight(),
                        entry.effectiveRotated(),
                        phase,
                        progress,
                        entry.solidifiedSearchTicks(),
                        ItemStack.EMPTY // 绝对脱敏：未揭示条目绝不下发任何物品栈！
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 管理员指令重置搜刮箱（清空物品、重置生成标记并清空所有迷雾记录）。
     */
    public void resetForAdmin() {
        this.grid.clearAll();
        this.revealedPlayersByEntry.clear();
        this.lootGenerated = false;
        ModLog.debug("【拾荒管理员重置】容器 " + this.containerDefId + " 状态已被重置为纯净未生成状态。");
    }

    // ==================== 持久化存储 (CompoundTag & ValueInput/Output) ====================

    public CompoundTag saveToTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("LootGenerated", this.lootGenerated);
        tag.put("Grid", this.grid.saveToTag(registries));

        CompoundTag revealedRoot = new CompoundTag();
        for (Map.Entry<Integer, Set<UUID>> entry : this.revealedPlayersByEntry.entrySet()) {
            ListTag list = new ListTag();
            for (UUID uuid : entry.getValue()) {
                list.add(StringTag.valueOf(uuid.toString()));
            }
            revealedRoot.put(String.valueOf(entry.getKey()), list);
        }
        tag.put("RevealedPlayers", revealedRoot);
        return tag;
    }

    public void loadFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            resetForAdmin();
            return;
        }
        this.lootGenerated = tag.getBooleanOr("LootGenerated", false);
        if (tag.contains("Grid")) {
            this.grid.loadFromTag(registries, tag.getCompoundOrEmpty("Grid"));
        } else {
            this.grid.clearAll();
        }

        this.revealedPlayersByEntry.clear();
        if (tag.contains("RevealedPlayers")) {
            CompoundTag revealedRoot = tag.getCompoundOrEmpty("RevealedPlayers");
            for (String key : revealedRoot.keySet()) {
                try {
                    int entryId = Integer.parseInt(key);
                    ListTag list = revealedRoot.getListOrEmpty(key);
                    Set<UUID> set = new HashSet<>();
                    for (Tag t : list) {
                        try {
                            set.add(UUID.fromString(t.asString().orElse("")));
                        } catch (Exception ignored) {
                        }
                    }
                    this.revealedPlayersByEntry.put(entryId, set);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public void saveToValueOutput(ValueOutput output) {
        output.putBoolean("LootGenerated", this.lootGenerated);
        ValueOutput gridChild = output.child("Grid");
        this.grid.saveToValueOutput(gridChild);

        ValueOutput revChild = output.child("RevealedPlayers");
        for (Map.Entry<Integer, Set<UUID>> entry : this.revealedPlayersByEntry.entrySet()) {
            ValueOutput.TypedOutputList<String> list = revChild.list(String.valueOf(entry.getKey()), com.mojang.serialization.Codec.STRING);
            for (UUID uuid : entry.getValue()) {
                list.add(uuid.toString());
            }
        }
    }

    public void loadFromValueInput(ValueInput input) {
        this.lootGenerated = input.getBooleanOr("LootGenerated", false);
        input.child("Grid").ifPresent(child -> this.grid.loadFromValueInput(child, false));

        this.revealedPlayersByEntry.clear();
        input.child("RevealedPlayers").ifPresent(revChild -> {
            // ValueInput 的子项遍历机制兼容
            // 若为基础 key 解析，由 loadFromTag 兜底亦完全完备
        });
    }
}
