package me.tuanzi.world.scavenger.loot.search;

import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.container.IScavengerLootContainer;
import me.tuanzi.world.scavenger.loot.container.ScavengerLootContainerState;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerRarity;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 服务端全局搜刮状态机调度中心。
 * <p>
 * 负责：
 * <ul>
 *   <li>逐刻驱动容器搜索进度（严格按阅读序自动排队推进，点击不影响搜索顺序）；</li>
 *   <li>阶段高潮脉冲判定（根据稀有度阶段阈值在对应刻度触发脉冲音效与粒子广播）；</li>
 *   <li>搜索完成揭示与小队同步；</li>
 *   <li>五大中断保护：距离超过 6m、受到伤害、关闭 GUI、跨维度、断开连接时自动清理并中断。</li>
 * </ul>
 */
public final class SearchSessionManager {

    public static final double MAX_INTERACTION_DISTANCE_SQ = 36.0; // 6.0 米
    private static final Map<UUID, SearchSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private SearchSessionManager() {
    }

    /**
     * 玩家打开可搜刮容器时开启搜索会话。
     */
    public static SearchSession openSession(
            ServerPlayer player,
            IScavengerLootContainer container,
            @Nullable SessionEventListener listener
    ) {
        closeSession(player.getUUID(), "重新开启新搜刮会话");

        // 确保容器完成首次战利品掷骰
        container.scavengerLootState().ensureGeneratedOnce(player.getRandom());

        SearchSession session = new SearchSession(
                player.getUUID(),
                container,
                container.containerWorldPos(),
                player.level().dimension(),
                listener
        );

        // 收集该玩家及其队友尚未揭示的全部条目，按阅读序（y 升序 -> x 升序）排入搜索队列
        Predicate<UUID> isTeammate = uuid -> {
            ServerPlayer other = player.level().getServer().getPlayerList().getPlayer(uuid);
            return other != null && ScavengerTeamManager.isSameTeam(player, other);
        };

        List<GridItemEntry> unrevealed = new ArrayList<>();
        ScavengerLootContainerState state = container.scavengerLootState();
        for (GridItemEntry entry : state.grid().entries()) {
            if (!state.isRevealedFor(entry.entryId(), player.getUUID(), isTeammate)) {
                unrevealed.add(entry);
            }
        }
        unrevealed.sort(Comparator.comparingInt(GridItemEntry::anchorY).thenComparingInt(GridItemEntry::anchorX));

        for (GridItemEntry entry : unrevealed) {
            session.queue.addLast(entry.entryId());
        }

        ACTIVE_SESSIONS.put(player.getUUID(), session);
        ModLog.debug(player, null, String.format(
                "【拾荒搜索会话】玩家打开容器 %s (%s) -> 待搜索未揭示物品=%d件",
                container.scavengerContainerDefId(), container.containerWorldPos().toShortString(),
                session.queue.size()
        ));

        // 立即激活首个条目
        advanceNextInQueue(player, session);
        return session;
    }

    /**
     * 玩家在客户端点击某个未揭示条目时调用。
     * 根据规范要求：点击不影响格子的搜索顺序，依旧是按照顺序依次搜索。
     */
    public static boolean prioritizeEntry(ServerPlayer player, int targetEntryId) {
        // 点击不影响顺序，严格按照队列既定顺序搜索
        return false;
    }

    /**
     * 玩家受到伤害时调用：打断当前搜刮会话。
     */
    public static void onPlayerDamaged(ServerPlayer player) {
        if (ACTIVE_SESSIONS.containsKey(player.getUUID())) {
            closeSession(player.getUUID(), "玩家受到伤害，搜索被打断！");
        }
    }

    /**
     * 关闭并注销玩家当前的搜刮会话。
     */
    public static void closeSession(UUID playerId, String reason) {
        SearchSession session = ACTIVE_SESSIONS.remove(playerId);
        if (session != null) {
            ModLog.debug(String.format(
                    "【拾荒搜索会话结束】玩家=%s, 原因: %s",
                    playerId, reason
            ));
        }
    }

    public static @Nullable SearchSession getSession(UUID playerId) {
        return ACTIVE_SESSIONS.get(playerId);
    }

    /**
     * 服务端周期性推进全部活跃搜刮会话。
     */
    public static void tickAllSessions(MinecraftServer server) {
        if (ACTIVE_SESSIONS.isEmpty()) {
            return;
        }

        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, SearchSession> mapEntry : ACTIVE_SESSIONS.entrySet()) {
            UUID playerId = mapEntry.getKey();
            SearchSession session = mapEntry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            // 1. 安全校验：玩家离线、跨维度、容器破坏或距离 > 6m
            if (player == null || player.isRemoved()) {
                toRemove.add(playerId);
                continue;
            }
            if (player.level().dimension() != session.dimension) {
                session.listener.onSessionClosed(player, "跨维度传送");
                toRemove.add(playerId);
                continue;
            }
            if (session.container.isContainerRemoved()) {
                session.listener.onSessionClosed(player, "容器方块被破坏");
                toRemove.add(playerId);
                continue;
            }
            Vec3 containerCenter = Vec3.atCenterOf(session.containerPos);
            if (player.distanceToSqr(containerCenter) > MAX_INTERACTION_DISTANCE_SQ) {
                session.listener.onSessionClosed(player, "超出交互距离(>6m)");
                toRemove.add(playerId);
                continue;
            }

            // 2. 检查或推进当前活动搜索项
            if (session.activeEntryId == -1) {
                if (!advanceNextInQueue(player, session)) {
                    continue; // 队列已清空，全部物品均已揭示
                }
            }

            ScavengerLootContainerState state = session.container.scavengerLootState();
            Optional<GridItemEntry> currentOpt = state.grid().getEntry(session.activeEntryId);
            if (currentOpt.isEmpty()) {
                // 物品已被其他并发玩家取走
                session.activeEntryId = -1;
                session.activeElapsedTicks = 0;
                continue;
            }

            GridItemEntry entry = currentOpt.get();
            session.activeElapsedTicks++;
            session.listener.onSearchTick(player, entry.entryId(), session.activeElapsedTicks, session.activeTotalTicks);

            // 3. 阶段高潮脉冲检测
            ScavengerItemMeta meta = entry.resolveMeta();
            float[] thresholds = meta.rarity().stagePulseThresholds();
            float fraction = (float) session.activeElapsedTicks / (float) session.activeTotalTicks;
            for (int i = 0; i < thresholds.length; i++) {
                if (i > session.lastPulseStageIndex && fraction >= thresholds[i]) {
                    session.lastPulseStageIndex = i;
                    session.listener.onStagePulse(player, entry.entryId(), thresholds[i], meta.rarity());
                    ModLog.debug(player, null, String.format(
                            "【拾荒搜索脉冲】条目 id=%d(%s) 触发第 %d 阶段脉冲 (阈值=%.0f%%)",
                            entry.entryId(), meta.rarity().getSerializedName(), i + 1, thresholds[i] * 100.0f
                    ));
                    break;
                }
            }

            // 4. 搜索耗时达标 -> 揭示完成！
            if (session.activeElapsedTicks >= session.activeTotalTicks) {
                state.markRevealedFor(entry.entryId(), player.getUUID());
                session.container.markContainerChanged();
                session.listener.onItemRevealed(player, entry.entryId(), entry.copyStack());

                ModLog.debug(player, null, String.format(
                        "【拾荒搜索完成】条目 id=%d(%s, %s, %dx%d) 已对玩家及其小队成功揭示！",
                        entry.entryId(), meta.itemId(), meta.rarity().getSerializedName(),
                        entry.effectiveWidth(), entry.effectiveHeight()
                ));

                // 推进至下一个
                session.activeEntryId = -1;
                session.activeElapsedTicks = 0;
                session.lastPulseStageIndex = -1;
                advanceNextInQueue(player, session);
            }
        }

        for (UUID id : toRemove) {
            ACTIVE_SESSIONS.remove(id);
        }
    }

    private static boolean advanceNextInQueue(ServerPlayer player, SearchSession session) {
        ScavengerLootContainerState state = session.container.scavengerLootState();
        Predicate<UUID> isTeammate = uuid -> {
            ServerPlayer other = player.level().getServer().getPlayerList().getPlayer(uuid);
            return other != null && ScavengerTeamManager.isSameTeam(player, other);
        };

        while (!session.queue.isEmpty()) {
            int candidateId = session.queue.removeFirst();
            Optional<GridItemEntry> entryOpt = state.grid().getEntry(candidateId);
            if (entryOpt.isEmpty()) {
                continue;
            }
            if (state.isRevealedFor(candidateId, player.getUUID(), isTeammate)) {
                continue;
            }
            GridItemEntry entry = entryOpt.get();
            session.activeEntryId = entry.entryId();
            session.activeElapsedTicks = 0;
            session.activeTotalTicks = entry.solidifiedSearchTicks();
            session.lastPulseStageIndex = -1;
            return true;
        }

        session.activeEntryId = -1;
        return false;
    }

    public interface SessionEventListener {
        void onSearchTick(ServerPlayer player, int entryId, int elapsedTicks, int totalTicks);

        void onStagePulse(ServerPlayer player, int entryId, float threshold, ScavengerRarity rarity);

        void onItemRevealed(ServerPlayer player, int entryId, ItemStack revealedStack);

        void onSessionClosed(ServerPlayer player, String reason);
    }

    public static final class SearchSession {
        private final UUID playerId;
        private final IScavengerLootContainer container;
        private final BlockPos containerPos;
        private final ResourceKey<Level> dimension;
        private final Deque<Integer> queue = new ArrayDeque<>();
        private final SessionEventListener listener;
        private int activeEntryId = -1;
        private int activeElapsedTicks = 0;
        private int activeTotalTicks = 20;
        private int lastPulseStageIndex = -1;

        public SearchSession(
                UUID playerId,
                IScavengerLootContainer container,
                BlockPos containerPos,
                ResourceKey<Level> dimension,
                SessionEventListener listener
        ) {
            this.playerId = playerId;
            this.container = container;
            this.containerPos = containerPos;
            this.dimension = dimension;
            this.listener = listener != null ? listener : new DummyListener();
        }

        public int activeEntryId() {
            return this.activeEntryId;
        }

        public int activeElapsedTicks() {
            return this.activeElapsedTicks;
        }

        public int activeTotalTicks() {
            return this.activeTotalTicks;
        }

        public IScavengerLootContainer container() {
            return this.container;
        }
    }

    private static final class DummyListener implements SessionEventListener {
        @Override
        public void onSearchTick(ServerPlayer player, int entryId, int elapsedTicks, int totalTicks) {
        }

        @Override
        public void onStagePulse(ServerPlayer player, int entryId, float threshold, ScavengerRarity rarity) {
        }

        @Override
        public void onItemRevealed(ServerPlayer player, int entryId, ItemStack revealedStack) {
        }

        @Override
        public void onSessionClosed(ServerPlayer player, String reason) {
        }
    }
}
