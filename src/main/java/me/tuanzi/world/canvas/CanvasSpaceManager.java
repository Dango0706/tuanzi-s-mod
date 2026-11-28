package me.tuanzi.world.canvas;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import me.tuanzi.block.entity.ColorBlockBlockEntity;
import me.tuanzi.block.entity.ColorSlabBlockEntity;
import me.tuanzi.block.entity.EaselBlockEntity;
import me.tuanzi.config.ModConfig;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.network.canvas.CanvasDataChunkS2CPacket;
import me.tuanzi.network.canvas.CanvasSpaceSyncS2CPacket;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 画布独立编辑空间核心管理器：
 * 负责隔离维度槽位加载/回写、并发编辑锁与只读保护、创造/生存双背包热切换、边界与权限校验、系统白色底面管理。
 */
public final class CanvasSpaceManager {
    public enum ExitReason {
        VOLUNTARY,
        EASEL_REMOVED,
        DISCONNECT_OR_SHUTDOWN
    }

    public static final BlockPos STANDARD_ORIGIN = BlockPos.ZERO;
    private static final Map<UUID, CanvasEditSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_CANVAS = new ConcurrentHashMap<>();

    private CanvasSpaceManager() {
    }

    public static final class CanvasEditSession {
        public static final int UTILITY_BUFFER_MARGIN = 8;

        public final UUID canvasUuid;
        public final UUID ownerUuid;
        public final ResourceKey<Level> canvasDimension;
        public final ResourceKey<Level> easelDimension;
        public final BlockPos easelPos;
        public final BlockPos origin;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final CanvasType type;
        public @Nullable UUID primaryEditor;
        public final Set<UUID> coEditors = ConcurrentHashMap.newKeySet();
        public final Set<UUID> grantedEditors = ConcurrentHashMap.newKeySet();
        public final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        public final Map<UUID, Boolean> borderVisible = new ConcurrentHashMap<>();
        public final Map<UUID, Boolean> readOnlyFlags = new ConcurrentHashMap<>();

        CanvasEditSession(
                UUID canvasUuid,
                @Nullable UUID ownerUuid,
                ResourceKey<Level> canvasDimension,
                ResourceKey<Level> easelDimension,
                BlockPos easelPos,
                BlockPos origin,
                int sizeX,
                int sizeY,
                int sizeZ,
                CanvasType type
        ) {
            this.canvasUuid = canvasUuid;
            this.ownerUuid = ownerUuid;
            this.canvasDimension = canvasDimension;
            this.easelDimension = easelDimension;
            this.easelPos = easelPos;
            this.origin = origin;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.type = type;
        }

        public boolean isEmpty() {
            return primaryEditor == null && coEditors.isEmpty() && viewers.isEmpty();
        }

        public boolean hasOtherEditors(UUID excludingPlayerUuid) {
            if (primaryEditor != null && !primaryEditor.equals(excludingPlayerUuid)) {
                return true;
            }
            for (UUID uid : coEditors) {
                if (!uid.equals(excludingPlayerUuid)) {
                    return true;
                }
            }
            return false;
        }

        public boolean canPlayerEdit(UUID playerUuid) {
            return !Boolean.TRUE.equals(readOnlyFlags.get(playerUuid))
                    && (playerUuid.equals(primaryEditor) || coEditors.contains(playerUuid));
        }

        public boolean isInsideEditBounds(BlockPos pos) {
            int x = pos.getX() - origin.getX();
            int y = pos.getY() - origin.getY();
            int z = pos.getZ() - origin.getZ();
            return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
        }

        public boolean isInsideUtilityBounds(BlockPos pos) {
            return isInsideUtilityBounds(origin, sizeX, sizeY, sizeZ, pos);
        }

        public static boolean isInsideUtilityBounds(BlockPos origin, int sizeX, int sizeY, int sizeZ, BlockPos pos) {
            int x = pos.getX() - origin.getX();
            int y = pos.getY() - origin.getY();
            int z = pos.getZ() - origin.getZ();
            return x >= -UTILITY_BUFFER_MARGIN
                    && x < sizeX + UTILITY_BUFFER_MARGIN
                    && z >= -UTILITY_BUFFER_MARGIN
                    && z < sizeZ + UTILITY_BUFFER_MARGIN
                    && y >= -1
                    && y < sizeY + UTILITY_BUFFER_MARGIN;
        }

        public boolean isBaseBlockPos(BlockPos pos) {
            int x = pos.getX() - origin.getX();
            int y = pos.getY() - origin.getY();
            int z = pos.getZ() - origin.getZ();
            return y == -1 && x >= 0 && x < sizeX && z >= 0 && z < sizeZ;
        }
    }

    public static void clearServerSessions() {
        ACTIVE_SESSIONS.clear();
        PLAYER_TO_CANVAS.clear();
        CanvasDimensionManager.clearState();
    }

    public static @Nullable CanvasEditSession getSessionByCanvas(UUID canvasUuid) {
        return canvasUuid != null ? ACTIVE_SESSIONS.get(canvasUuid) : null;
    }

    public static @Nullable CanvasEditSession getSessionForPlayer(UUID playerUuid) {
        UUID canvasUuid = PLAYER_TO_CANVAS.get(playerUuid);
        return canvasUuid != null ? ACTIVE_SESSIONS.get(canvasUuid) : null;
    }

    public static @Nullable CanvasEditSession getSessionByLevel(@Nullable Level level) {
        if (level == null || !ModDimensions.isCanvasSpaceLevel(level)) {
            return null;
        }
        ResourceKey<Level> dim = level.dimension();
        for (CanvasEditSession session : ACTIVE_SESSIONS.values()) {
            if (session.canvasDimension.equals(dim)) {
                return session;
            }
        }
        return null;
    }

    public static boolean canPlayerEditCurrentCanvas(@Nullable ServerPlayer player) {
        if (player == null || !ModDimensions.isCanvasSpaceLevel(player.level())) {
            return true;
        }
        CanvasEditSession session = getSessionForPlayer(player.getUUID());
        return session != null && session.canPlayerEdit(player.getUUID());
    }

    public static void notifyCanvasEditors(ServerLevel canvasLevel, @Nullable CanvasEditSession session, Component message) {
        MinecraftServer server = canvasLevel.getServer();
        boolean sent = false;
        if (session != null && server != null) {
            if (session.primaryEditor != null) {
                ServerPlayer primary = server.getPlayerList().getPlayer(session.primaryEditor);
                if (primary != null && primary.level() == canvasLevel) {
                    primary.sendSystemMessage(message);
                    sent = true;
                }
            }
            for (UUID uid : session.coEditors) {
                ServerPlayer co = server.getPlayerList().getPlayer(uid);
                if (co != null && co.level() == canvasLevel) {
                    co.sendSystemMessage(message);
                    sent = true;
                }
            }
        }
        if (!sent) {
            for (ServerPlayer player : canvasLevel.players()) {
                player.sendSystemMessage(message);
            }
        }
    }

    public static boolean isEaselOccupied(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        for (CanvasEditSession session : ACTIVE_SESSIONS.values()) {
            if (session.easelDimension.equals(dim) && session.easelPos.equals(pos) && !session.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean enterCanvasSpace(
            ServerPlayer player,
            ServerLevel easelLevel,
            BlockPos easelPos,
            EaselBlockEntity easel
    ) {
        if (ModDimensions.isCanvasSpaceLevel(player.level())) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.already_in").withStyle(ChatFormatting.RED));
            return false;
        }
        ItemStack canvasStack = easel.getCanvasStack();
        if (canvasStack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.no_canvas").withStyle(ChatFormatting.RED));
            return false;
        }

        CanvasMetadata meta = CanvasFileStorage.ensureMaterialized(canvasStack);
        if (meta.type() == CanvasType.UNSET) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.select_type_first").withStyle(ChatFormatting.RED));
            return false;
        }

        MinecraftServer server = easelLevel.getServer();

        // 若是首次分配 UUID，先检查单玩家画布数量上限并分配
        CanvasSlotSavedData slotData = CanvasSlotSavedData.get(server);
        if (!meta.hasAssignedUuid()) {
            int created = slotData.getOwnerCount(player.getUUID());
            if (!player.canUseGameMasterBlocks() && created >= ModConfig.getMaxCanvasesPerPlayer()) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.quota_exceeded",
                        ModConfig.getMaxCanvasesPerPlayer()).withStyle(ChatFormatting.RED));
                return false;
            }
            UUID newUuid = UUID.randomUUID();
            meta = meta.withUuid(newUuid);
            if (meta.ownerUuid() == null) {
                meta = meta.withTypeAndOwner(meta.type(), player.getUUID(), player.getName().getString());
            }
            CanvasMetadata.write(canvasStack, meta);
            slotData.incrementOwnerCount(player.getUUID());
            easel.setChanged();
        }

        UUID canvasUuid = meta.uuid();
        ServerLevel canvasLevel = CanvasDimensionManager.getOrCreateCanvasLevel(server, canvasUuid);
        if (canvasLevel == null) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.dim_missing").withStyle(ChatFormatting.RED));
            return false;
        }

        ServerPlayer onlineOwner = meta.ownerUuid() != null ? server.getPlayerList().getPlayer(meta.ownerUuid()) : null;
        CanvasEditSession existingSession = ACTIVE_SESSIONS.get(canvasUuid);
        Set<UUID> granted = existingSession != null ? existingSession.grantedEditors : Set.of();

        CanvasPermissionHelper.AccessDecision access = CanvasPermissionHelper.evaluateCanvasAccess(
                meta, player, onlineOwner, granted
        );
        if (!access.canEnter()) {
            if (existingSession == null || existingSession.isEmpty()) {
                CanvasDimensionManager.scheduleUnload(canvasLevel.dimension());
            }
            player.sendSystemMessage(Component.translatable(access.denyReasonKey()).withStyle(ChatFormatting.RED));
            return false;
        }

        boolean wantEdit = access.canEdit();
        boolean readOnlyLockedByOther = false;
        if (existingSession != null && existingSession.primaryEditor != null && !existingSession.primaryEditor.equals(player.getUUID())) {
            if (existingSession.grantedEditors.contains(player.getUUID())) {
                wantEdit = true;
            } else {
                wantEdit = false;
                readOnlyLockedByOther = true;
            }
        }

        BlockPos origin = STANDARD_ORIGIN;
        int sizeX = meta.size();
        int sizeY = meta.type().getHeightForSize(meta.size());
        int sizeZ = meta.size();

        final CanvasMetadata finalMeta = meta;
        final ResourceKey<Level> canvasDimKey = canvasLevel.dimension();
        CanvasEditSession session = ACTIVE_SESSIONS.computeIfAbsent(canvasUuid, id ->
                new CanvasEditSession(id, finalMeta.ownerUuid(), canvasDimKey, easelLevel.dimension(), easelPos.immutable(),
                        origin, sizeX, sizeY, sizeZ, finalMeta.type()));

        boolean needPopulateWorld = session.isEmpty();
        if (needPopulateWorld) {
            CanvasDataCodec.DecodeResult loaded = CanvasFileStorage.loadDraftOrSaved(canvasUuid, meta.size(), meta.type());
            if (loaded.corrupted()) {
                wantEdit = false;
                meta = meta.withStats(meta.nonEmptyVoxels(), meta.lastEditTime(), meta.dataHash(), true);
                CanvasMetadata.write(canvasStack, meta);
                easel.setChanged();
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.corrupted_readonly")
                        .withStyle(ChatFormatting.RED));
            }
            CanvasVoxelData voxelData = loaded.data() != null
                    ? loaded.data()
                    : CanvasVoxelData.createEmpty(meta.size(), meta.type());
            populateSlotInWorld(canvasLevel, session, voxelData);
        }

        // 记录会话成员角色
        if (wantEdit) {
            if (session.primaryEditor == null) {
                session.primaryEditor = player.getUUID();
            } else if (!session.primaryEditor.equals(player.getUUID())) {
                session.coEditors.add(player.getUUID());
            }
            session.readOnlyFlags.put(player.getUUID(), false);
        } else {
            session.viewers.add(player.getUUID());
            session.readOnlyFlags.put(player.getUUID(), true);
        }
        session.borderVisible.put(player.getUUID(), true);
        PLAYER_TO_CANVAS.put(player.getUUID(), canvasUuid);

        // 捕获返回快照并切换状态
        saveReturnSnapshotAndApplyMode(player, easelLevel, easelPos, canvasUuid);

        // 传送进入画布空间中心上方
        double spawnX = origin.getX() + sizeX / 2.0;
        double spawnY = origin.getY() + (meta.type() == CanvasType.FLAT_2D ? 1.5 : 1.0);
        double spawnZ = origin.getZ() + sizeZ / 2.0;
        player.teleport(new TeleportTransition(
                canvasLevel,
                new Vec3(spawnX, spawnY, spawnZ),
                Vec3.ZERO,
                0.0f,
                25.0f,
                TeleportTransition.DO_NOTHING
        ));

        syncClientCanvasState(player, session, !wantEdit);

        if (readOnlyLockedByOther) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.entered_readonly_locked")
                    .withStyle(ChatFormatting.YELLOW));
        } else if (!wantEdit) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.entered_readonly")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.entered_edit",
                    sizeX, sizeY, sizeZ).withStyle(ChatFormatting.GREEN));
        }

        ModLog.debug(player, "【画布空间】进入独立画布空间: uuid=" + canvasUuid + ", dim=" + canvasDimKey.identifier()
                + ", origin=(0,0,0), edit=" + wantEdit + ", creative=" + ModConfig.isCanvasCreativeMode());
        return true;
    }

    public static boolean exitCanvasSpace(ServerPlayer player, boolean forceFromEaselBreak) {
        return exitCanvasSpace(player, forceFromEaselBreak ? ExitReason.EASEL_REMOVED : ExitReason.VOLUNTARY);
    }

    public static boolean exitCanvasSpace(ServerPlayer player, ExitReason reason) {
        CanvasPlayerTracker tracker = (CanvasPlayerTracker) player;
        CompoundTag sessionTag = tracker.tuanzis_mod$getCanvasSessionData();
        boolean inCanvas = sessionTag.getBooleanOr("InCanvasSpace", false)
                || ModDimensions.isCanvasSpaceLevel(player.level());
        if (!inCanvas) {
            return false;
        }

        UUID canvasUuid = PLAYER_TO_CANVAS.get(player.getUUID());
        if (canvasUuid == null && sessionTag.contains("CanvasUuid")) {
            try {
                canvasUuid = UUID.fromString(sessionTag.getStringOr("CanvasUuid", ""));
            } catch (IllegalArgumentException ignored) {
            }
        }

        MinecraftServer server = player.level().getServer();
        CanvasEditSession session = canvasUuid != null ? ACTIVE_SESSIONS.get(canvasUuid) : null;
        ServerLevel canvasLevel = (server != null && session != null)
                ? server.getLevel(session.canvasDimension)
                : (ModDimensions.isCanvasSpaceLevel(player.level()) && player.level() instanceof ServerLevel sl ? sl : null);

        boolean wasEditor = session != null && session.canPlayerEdit(player.getUUID());
        boolean isLastEditor = wasEditor && !session.hasOtherEditors(player.getUUID());
        boolean noEditorsRemain = session != null
                && (isLastEditor || (session.primaryEditor == null && session.coEditors.isEmpty() && session.viewers.size() <= 1));

        // 1. 主动退出校验：最后一位编辑者退出前必须先收走残留的潜影盒、蓝图大炮与蓝图桌
        if (reason == ExitReason.VOLUNTARY && session != null && canvasLevel != null && isLastEditor) {
            List<BlockPos> remaining = findRemainingUtilityBlocks(canvasLevel, session);
            if (!remaining.isEmpty()) {
                BlockPos first = remaining.getFirst();
                player.sendSystemMessage(Component.translatable(
                        "message.tuanzis_mod.canvas.exit_blocked_utility_remaining",
                        remaining.size(), first.getX(), first.getY(), first.getZ()
                ).withStyle(ChatFormatting.RED));
                ModLog.debug(player, "【画布空间】拦截主动退出：仍有 " + remaining.size()
                        + " 个临时施工工具未收走，首个位于 (" + first.getX() + "," + first.getY() + "," + first.getZ() + ")");
                return false;
            }
        }

        PLAYER_TO_CANVAS.remove(player.getUUID());
        List<ItemStack> recoveredItems = List.of();

        if (session != null && canvasLevel != null) {
            if (wasEditor) {
                CanvasVoxelData scanned = scanWorldToVoxelData(canvasLevel, session);
                try {
                    CanvasFileStorage.writeDraft(session.canvasUuid, scanned);
                    ModLog.debug(player, "【画布空间】退出自动保存草稿: uuid=" + session.canvasUuid
                            + ", nonEmpty=" + scanned.getNonEmptyCount());
                } catch (IOException e) {
                    ModLog.error("[CanvasSpaceManager] 退出保存草稿失败: " + session.canvasUuid, e);
                }
            }

            // 2. 强制退出（拆画板 / 断线 / 关服）若无剩余编辑者则自动回收临时施工工具，防止维度卸载丢失物品
            if (reason != ExitReason.VOLUNTARY && noEditorsRemain) {
                List<BlockPos> remaining = findRemainingUtilityBlocks(canvasLevel, session);
                if (!remaining.isEmpty()) {
                    boolean swappedCreative = sessionTag.getBooleanOr("SwappedInventory", false);
                    boolean preserveItems = !swappedCreative;
                    recoveredItems = collectAndClearUtilityBlocks(canvasLevel, remaining, preserveItems);
                    ModLog.debug(player, "【画布空间】强制退出自动清理/回收施工工具: count=" + remaining.size()
                            + ", preserveItems=" + preserveItems + ", stacks=" + recoveredItems.size() + ", reason=" + reason);
                }
            }

            if (player.getUUID().equals(session.primaryEditor)) {
                session.primaryEditor = null;
                if (!session.coEditors.isEmpty()) {
                    session.primaryEditor = session.coEditors.iterator().next();
                    session.coEditors.remove(session.primaryEditor);
                }
            } else {
                session.coEditors.remove(player.getUUID());
            }
            session.viewers.remove(player.getUUID());
            session.borderVisible.remove(player.getUUID());
            session.readOnlyFlags.remove(player.getUUID());
            if (session.isEmpty()) {
                ACTIVE_SESSIONS.remove(session.canvasUuid);
                CanvasDimensionManager.scheduleUnload(session.canvasDimension);
            }
        }

        restorePlayerSnapshotAndTeleportBack(player, server, sessionTag);
        tracker.tuanzis_mod$setCanvasSessionData(new CompoundTag());
        ServerPlayNetworking.send(player, CanvasSpaceSyncS2CPacket.cleared());

        if (!recoveredItems.isEmpty()) {
            boolean queuedOverflow = false;
            for (ItemStack stack : recoveredItems) {
                if (stack.isEmpty()) {
                    continue;
                }
                if (!player.getInventory().add(stack)) {
                    if (reason == ExitReason.DISCONNECT_OR_SHUTDOWN && player.level() instanceof ServerLevel sl) {
                        CanvasPendingReturnSavedData.enqueue(sl, player.getUUID(), stack);
                        queuedOverflow = true;
                    } else {
                        player.drop(stack, false, Prediction.SERVER_ONLY);
                    }
                }
            }
            player.sendSystemMessage(Component.translatable(
                    "message.tuanzis_mod.canvas.utility_auto_recovered", recoveredItems.size()
            ).withStyle(ChatFormatting.YELLOW));
            if (queuedOverflow) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.utility_queued_return")
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        player.sendSystemMessage(Component.translatable(
                reason == ExitReason.EASEL_REMOVED
                        ? "message.tuanzis_mod.canvas.forced_exit_easel_removed"
                        : "message.tuanzis_mod.canvas.exited"
        ).withStyle(reason == ExitReason.EASEL_REMOVED ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        return true;
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        if (ModDimensions.isCanvasSpaceLevel(player.level())
                || ((CanvasPlayerTracker) player).tuanzis_mod$getCanvasSessionData().getBooleanOr("InCanvasSpace", false)) {
            exitCanvasSpace(player, ExitReason.DISCONNECT_OR_SHUTDOWN);
        }
    }

    /**
     * 玩家登录兜底恢复：若因异常崩溃导致上一次画布编辑会话未正常退出（仍保留会话快照），自动还原背包与返回坐标。
     */
    public static void onPlayerJoin(ServerPlayer player) {
        CanvasPlayerTracker tracker = (CanvasPlayerTracker) player;
        CompoundTag sessionTag = tracker.tuanzis_mod$getCanvasSessionData();
        if (!sessionTag.getBooleanOr("InCanvasSpace", false) && !ModDimensions.isCanvasSpaceLevel(player.level())) {
            return;
        }
        if (getSessionForPlayer(player.getUUID()) == null) {
            restorePlayerSnapshotAndTeleportBack(player, player.level().getServer(), sessionTag);
            tracker.tuanzis_mod$setCanvasSessionData(new CompoundTag());
            ServerPlayNetworking.send(player, CanvasSpaceSyncS2CPacket.cleared());
            ModLog.debug(player, "【画布空间】检测到上次异常中断的画布会话，已自动还原背包与返回坐标。");
        }
    }

    public static void onEaselRemoved(ServerLevel easelLevel, BlockPos easelPos, @Nullable UUID canvasUuid) {
        ResourceKey<Level> dim = easelLevel.dimension();
        List<CanvasEditSession> matching = new ArrayList<>();
        for (CanvasEditSession s : ACTIVE_SESSIONS.values()) {
            if ((canvasUuid != null && s.canvasUuid.equals(canvasUuid))
                    || (s.easelDimension.equals(dim) && s.easelPos.equals(easelPos))) {
                matching.add(s);
            }
        }
        MinecraftServer server = easelLevel.getServer();
        for (CanvasEditSession s : matching) {
            List<UUID> participants = new ArrayList<>();
            if (s.primaryEditor != null) participants.add(s.primaryEditor);
            participants.addAll(s.coEditors);
            participants.addAll(s.viewers);
            for (UUID uid : participants) {
                ServerPlayer p = server.getPlayerList().getPlayer(uid);
                if (p != null) {
                    exitCanvasSpace(p, true);
                }
            }
        }
    }

    public static boolean commitSaveFromEasel(ServerPlayer player, EaselBlockEntity easel) {
        ItemStack stack = easel.getCanvasStack();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.no_canvas").withStyle(ChatFormatting.RED));
            return false;
        }
        CanvasMetadata meta = CanvasFileStorage.ensureMaterialized(stack);
        if (meta.type() == CanvasType.UNSET) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.select_type_first").withStyle(ChatFormatting.RED));
            return false;
        }
        if (meta.corrupted()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.corrupted_readonly").withStyle(ChatFormatting.RED));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        ServerPlayer onlineOwner = meta.ownerUuid() != null ? server.getPlayerList().getPlayer(meta.ownerUuid()) : null;
        CanvasEditSession session = meta.uuid() != null ? ACTIVE_SESSIONS.get(meta.uuid()) : null;
        Set<UUID> granted = session != null ? session.grantedEditors : Set.of();

        CanvasPermissionHelper.AccessDecision access = CanvasPermissionHelper.evaluateCanvasAccess(meta, player, onlineOwner, granted);
        if (!access.canEdit()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.no_edit_permission").withStyle(ChatFormatting.RED));
            return false;
        }

        if (!meta.hasAssignedUuid()) {
            UUID newUuid = UUID.randomUUID();
            meta = meta.withUuid(newUuid);
            if (meta.ownerUuid() == null) {
                meta = meta.withTypeAndOwner(meta.type(), player.getUUID(), player.getName().getString());
            }
        }

        CanvasVoxelData dataToSave = null;
        ServerLevel canvasLevel = session != null ? server.getLevel(session.canvasDimension) : null;
        if (session != null && canvasLevel != null && !session.isEmpty()) {
            dataToSave = scanWorldToVoxelData(canvasLevel, session);
        } else {
            CanvasDataCodec.DecodeResult loaded = CanvasFileStorage.loadDraftOrSaved(meta.uuid(), meta.size(), meta.type());
            if (loaded.corrupted()) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.corrupted_readonly").withStyle(ChatFormatting.RED));
                return false;
            }
            dataToSave = loaded.data() != null ? loaded.data() : CanvasVoxelData.createEmpty(meta.size(), meta.type());
        }

        try {
            CanvasFileStorage.writeSaved(meta.uuid(), dataToSave);
            CanvasFileStorage.clearDraftMarker(meta.uuid());
            String hash = dataToSave.computeSha256Hex();
            int nonEmpty = dataToSave.getNonEmptyCount();
            long now = System.currentTimeMillis();
            CanvasMetadata updated = meta.withStats(nonEmpty, now, hash, false);
            CanvasMetadata.write(stack, updated);
            easel.setChanged();
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.save_success", nonEmpty)
                    .withStyle(ChatFormatting.GREEN));
            ModLog.debug(player, "【画布保存】正式写入画布: uuid=" + updated.uuid()
                    + ", nonEmpty=" + nonEmpty + ", hash=" + hash);
            return true;
        } catch (IOException e) {
            ModLog.error("[CanvasSpaceManager] 正式保存画布失败: " + meta.uuid(), e);
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.save_fail").withStyle(ChatFormatting.RED));
            return false;
        }
    }

    public static boolean canPlaceAt(@Nullable Player player, Level level, BlockPos pos, BlockState state) {
        if (!ModDimensions.isCanvasSpaceLevel(level)) {
            return true;
        }
        if (!(player instanceof ServerPlayer sp)) {
            // 客户端预测检查：放行由服务端权威校验同步
            return true;
        }
        CanvasEditSession session = getSessionForPlayer(sp.getUUID());
        if (session == null) {
            return false;
        }
        if (!session.canPlayerEdit(sp.getUUID())) {
            sp.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.readonly_action_denied").withStyle(ChatFormatting.RED));
            return false;
        }
        if (CanvasBlockRules.isCanvasUtilityBlock(state)) {
            if (!level.isInValidBounds(pos) || !session.isInsideUtilityBounds(pos)) {
                sp.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.out_of_bounds").withStyle(ChatFormatting.RED));
                return false;
            }
            ModLog.debug(sp, "【画布空间】允许放置临时施工工具: state=" + state.getBlock() + ", pos=" + pos);
            return true;
        }
        if (!session.isInsideEditBounds(pos)) {
            sp.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.out_of_bounds").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!CanvasBlockRules.isAllowedAsCanvasVoxel(state)) {
            sp.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.block_forbidden").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    public static boolean canBreakAt(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!ModDimensions.isCanvasSpaceLevel(level)) {
            return true;
        }
        CanvasEditSession session = getSessionForPlayer(player.getUUID());
        if (session == null) {
            return false;
        }
        if (!session.canPlayerEdit(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.readonly_action_denied").withStyle(ChatFormatting.RED));
            return false;
        }
        if (session.isBaseBlockPos(pos) && state.is(ModBlocks.CANVAS_BASE_BLOCK)) {
            return true;
        }
        if (CanvasBlockRules.isCanvasUtilityBlock(state) && session.isInsideUtilityBounds(pos)) {
            return true;
        }
        if (!session.isInsideEditBounds(pos)) {
            player.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.out_of_bounds").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    public static boolean toggleBorder(ServerPlayer player, @Nullable Boolean explicit) {
        CanvasEditSession session = getSessionForPlayer(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.not_in").withStyle(ChatFormatting.RED));
            return false;
        }
        boolean next = explicit != null ? explicit : !session.borderVisible.getOrDefault(player.getUUID(), true);
        session.borderVisible.put(player.getUUID(), next);
        syncClientCanvasState(player, session, !session.canPlayerEdit(player.getUUID()));
        player.sendSystemMessage(Component.translatable(
                next ? "message.tuanzis_mod.canvas.border_on" : "message.tuanzis_mod.canvas.border_off"
        ).withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static boolean toggleNoclip(ServerPlayer player, @Nullable Boolean explicit) {
        CanvasEditSession session = getSessionForPlayer(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.not_in").withStyle(ChatFormatting.RED));
            return false;
        }
        CompoundTag sessionTag = ((CanvasPlayerTracker) player).tuanzis_mod$getCanvasSessionData();
        boolean current = sessionTag.getBooleanOr("NoclipActive", false);
        boolean next = explicit != null ? explicit : !current;
        sessionTag.putBoolean("NoclipActive", next);
        ((CanvasPlayerTracker) player).tuanzis_mod$setCanvasSessionData(sessionTag);

        if (next) {
            player.setGameMode(GameType.SPECTATOR);
        } else {
            GameType targetMode = ModConfig.isCanvasCreativeMode() ? GameType.CREATIVE : GameType.SURVIVAL;
            player.setGameMode(targetMode);
            if (targetMode == GameType.SURVIVAL) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
        }
        player.sendSystemMessage(Component.translatable(
                next ? "message.tuanzis_mod.canvas.noclip_on" : "message.tuanzis_mod.canvas.noclip_off"
        ).withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static boolean clearOrRestoreSystemBase(ServerPlayer player, boolean restore) {
        CanvasEditSession session = getSessionForPlayer(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel canvasLevel)
                || !ModDimensions.isCanvasSpaceLevel(canvasLevel)) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.not_in").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!session.canPlayerEdit(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.readonly_action_denied").withStyle(ChatFormatting.RED));
            return false;
        }
        BlockState targetState = restore ? ModBlocks.CANVAS_BASE_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
        int baseY = session.origin.getY() - 1;
        for (int x = 0; x < session.sizeX; x++) {
            for (int z = 0; z < session.sizeZ; z++) {
                BlockPos pos = new BlockPos(session.origin.getX() + x, baseY, session.origin.getZ() + z);
                BlockState cur = canvasLevel.getBlockState(pos);
                if (restore && cur.isAir()) {
                    canvasLevel.setBlock(pos, targetState, Block.UPDATE_CLIENTS);
                } else if (!restore && cur.is(ModBlocks.CANVAS_BASE_BLOCK)) {
                    canvasLevel.setBlock(pos, targetState, Block.UPDATE_CLIENTS);
                }
            }
        }
        player.sendSystemMessage(Component.translatable(
                restore ? "message.tuanzis_mod.canvas.base_restored" : "message.tuanzis_mod.canvas.base_cleared"
        ).withStyle(ChatFormatting.GREEN));
        return true;
    }

    public static boolean grantOrRevokeEditor(ServerPlayer sender, ServerPlayer target, boolean grant) {
        CanvasEditSession session = getSessionForPlayer(sender.getUUID());
        if (session == null) {
            sender.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.not_in").withStyle(ChatFormatting.RED));
            return false;
        }
        boolean isOwnerOrOp = sender.canUseGameMasterBlocks()
                || (session.ownerUuid != null && session.ownerUuid.equals(sender.getUUID()));
        if (!isOwnerOrOp) {
            sender.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.only_owner_allow").withStyle(ChatFormatting.RED));
            return false;
        }
        if (grant) {
            session.grantedEditors.add(target.getUUID());
            if (session.viewers.remove(target.getUUID())) {
                session.coEditors.add(target.getUUID());
                session.readOnlyFlags.put(target.getUUID(), false);
                syncClientCanvasState(target, session, false);
                target.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.granted_target").withStyle(ChatFormatting.GREEN));
            }
            sender.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.granted_owner",
                    target.getName().getString()).withStyle(ChatFormatting.GREEN));
        } else {
            session.grantedEditors.remove(target.getUUID());
            if (session.coEditors.remove(target.getUUID())) {
                session.viewers.add(target.getUUID());
                session.readOnlyFlags.put(target.getUUID(), true);
                syncClientCanvasState(target, session, true);
                target.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.revoked_target").withStyle(ChatFormatting.YELLOW));
            }
            sender.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.revoked_owner",
                    target.getName().getString()).withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    public static void handleConfigHotReload(@Nullable MinecraftServer server) {
        if (server == null) {
            return;
        }
        boolean creativeConfigured = ModConfig.isCanvasCreativeMode();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ModDimensions.isCanvasSpaceLevel(player.level())) {
                continue;
            }
            CanvasEditSession session = getSessionForPlayer(player.getUUID());
            if (session == null) {
                continue;
            }
            CompoundTag tag = ((CanvasPlayerTracker) player).tuanzis_mod$getCanvasSessionData();
            boolean noclip = tag.getBooleanOr("NoclipActive", false);
            if (noclip) {
                continue;
            }
            boolean currentlySwapped = tag.getBooleanOr("SwappedInventory", false);
            if (creativeConfigured && !currentlySwapped) {
                tag.put("SavedInventory", captureInventoryTag(player));
                tag.putBoolean("SwappedInventory", true);
                ((CanvasPlayerTracker) player).tuanzis_mod$setCanvasSessionData(tag);
                clearInventoryAndXp(player);
                player.setGameMode(GameType.CREATIVE);
                syncClientCanvasState(player, session, !session.canPlayerEdit(player.getUUID()));
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.mode_hot_creative").withStyle(ChatFormatting.AQUA));
            } else if (!creativeConfigured && currentlySwapped) {
                restoreInventoryTag(player, tag.getCompoundOrEmpty("SavedInventory"));
                tag.remove("SavedInventory");
                tag.putBoolean("SwappedInventory", false);
                ((CanvasPlayerTracker) player).tuanzis_mod$setCanvasSessionData(tag);
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                syncClientCanvasState(player, session, !session.canPlayerEdit(player.getUUID()));
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.mode_hot_survival").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    public static void tickPlayerInCanvasSpace(ServerPlayer player) {
        CanvasEditSession session = getSessionForPlayer(player.getUUID());
        if (session == null) {
            // 若未知玩家直接通过原版传送进画布维度，安全起见不强行踢出管理员，但给非管理员拉回主世界
            if (!player.canUseGameMasterBlocks()) {
                ServerLevel overworld = player.level().getServer().overworld();
                BlockPos spawn = overworld.getRespawnData().pos();
                player.teleport(new TeleportTransition(overworld, Vec3.atBottomCenterOf(spawn), Vec3.ZERO, 0.0f, 0.0f, TeleportTransition.DO_NOTHING));
            }
            return;
        }
        // 保持生存模式玩家的飞行能力与防虚空掉落
        CompoundTag tag = ((CanvasPlayerTracker) player).tuanzis_mod$getCanvasSessionData();
        if (!tag.getBooleanOr("NoclipActive", false) && !ModConfig.isCanvasCreativeMode()) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        }
        double minX = session.origin.getX() - 16.0;
        double maxX = session.origin.getX() + session.sizeX + 16.0;
        double minY = session.origin.getY() - 24.0;
        double minZ = session.origin.getZ() - 16.0;
        double maxZ = session.origin.getZ() + session.sizeZ + 16.0;
        if (player.getY() < minY || player.getX() < minX || player.getX() > maxX || player.getZ() < minZ || player.getZ() > maxZ) {
            double cx = session.origin.getX() + session.sizeX / 2.0;
            double cy = session.origin.getY() + 1.5;
            double cz = session.origin.getZ() + session.sizeZ / 2.0;
            player.teleportTo(cx, cy, cz);
            player.resetFallDistance();
            player.sendOverlayMessage(Component.translatable("message.tuanzis_mod.canvas.pulled_back_to_bounds").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static void sendCanvasDataToClient(ServerPlayer player, String modelUuidStr, String requestedHash) {
        UUID modelUuid;
        try {
            modelUuid = UUID.fromString(modelUuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        try {
            byte[] rawBytes = CanvasFileStorage.readRawSavedBytes(modelUuid);
            if (rawBytes.length == 0) {
                return;
            }
            int chunkSize = CanvasDataChunkS2CPacket.CHUNK_SIZE;
            int totalChunks = Math.max(1, (rawBytes.length + chunkSize - 1) / chunkSize);
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(rawBytes.length, start + chunkSize);
                byte[] slice = Arrays.copyOfRange(rawBytes, start, end);
                ServerPlayNetworking.send(player, new CanvasDataChunkS2CPacket(modelUuidStr, requestedHash, i, totalChunks, slice));
            }
            ModLog.debug(player, "【画布网络同步】向客户端分片下发模型数据: uuid=" + modelUuid + ", bytes=" + rawBytes.length + ", chunks=" + totalChunks);
        } catch (IOException e) {
            ModLog.error("[CanvasSpaceManager] 读取发送画布模型数据失败: " + modelUuidStr, e);
        }
    }

    private static void populateSlotInWorld(ServerLevel canvasLevel, CanvasEditSession session, CanvasVoxelData data) {
        int ox = session.origin.getX();
        int oy = session.origin.getY();
        int oz = session.origin.getZ();

        // 清理槽位内部与底部参考面，铺设默认白色系统底面
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int y = -1; y <= session.sizeY; y++) {
            for (int z = 0; z < session.sizeZ; z++) {
                for (int x = 0; x < session.sizeX; x++) {
                    mpos.set(ox + x, oy + y, oz + z);
                    BlockState target = (y == -1)
                            ? ModBlocks.CANVAS_BASE_BLOCK.defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                    if (!canvasLevel.getBlockState(mpos).equals(target)) {
                        canvasLevel.setBlock(mpos, target, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // 还原所有非空体素及其自定义色彩
        for (var entry : data.getVoxelMap().int2IntEntrySet()) {
            int packed = entry.getIntKey();
            int pIdx = entry.getIntValue();
            if (pIdx < 0 || pIdx >= data.getPalette().size()) {
                continue;
            }
            CanvasPaletteEntry pe = data.getPalette().get(pIdx);
            int vx = CanvasVoxelData.unpackX(packed);
            int vy = CanvasVoxelData.unpackY(packed);
            int vz = CanvasVoxelData.unpackZ(packed);
            if (vx < 0 || vx >= session.sizeX || vy < 0 || vy >= session.sizeY || vz < 0 || vz >= session.sizeZ) {
                continue;
            }
            BlockPos pos = new BlockPos(ox + vx, oy + vy, oz + vz);
            BlockState state = pe.resolveBlockState();
            canvasLevel.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (pe.colorRgb() != CanvasPaletteEntry.NO_COLOR) {
                BlockEntity be = canvasLevel.getBlockEntity(pos);
                if (be instanceof ColorBlockBlockEntity colorBe) {
                    colorBe.setColor(pe.colorRgb());
                } else if (be instanceof ColorSlabBlockEntity slabBe) {
                    slabBe.setBottomColor(pe.colorRgb());
                    if (pe.altColorRgb() != CanvasPaletteEntry.NO_COLOR) {
                        slabBe.setTopColor(pe.altColorRgb());
                    }
                }
            }
        }
    }

    private static CanvasVoxelData scanWorldToVoxelData(ServerLevel canvasLevel, CanvasEditSession session) {
        CanvasVoxelData data = new CanvasVoxelData(session.sizeX, session.sizeY, session.sizeZ, session.type);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int ox = session.origin.getX();
        int oy = session.origin.getY();
        int oz = session.origin.getZ();

        for (int y = 0; y < session.sizeY; y++) {
            for (int z = 0; z < session.sizeZ; z++) {
                for (int x = 0; x < session.sizeX; x++) {
                    mpos.set(ox + x, oy + y, oz + z);
                    BlockState state = canvasLevel.getBlockState(mpos);
                    if (state.isAir() || state.is(ModBlocks.CANVAS_BASE_BLOCK) || CanvasBlockRules.isCanvasUtilityBlock(state)) {
                        continue;
                    }
                    int colorRgb = CanvasPaletteEntry.NO_COLOR;
                    int altColorRgb = CanvasPaletteEntry.NO_COLOR;
                    Block block = state.getBlock();
                    if (block instanceof ColorBlock || block instanceof ColorStairs || block instanceof ColorSlab) {
                        BlockEntity be = canvasLevel.getBlockEntity(mpos);
                        if (be instanceof ColorBlockBlockEntity colorBe) {
                            colorRgb = colorBe.getColor();
                        } else if (be instanceof ColorSlabBlockEntity slabBe) {
                            colorRgb = slabBe.getBottomColor();
                            altColorRgb = slabBe.getTopColor();
                        }
                    }
                    data.setVoxel(x, y, z, CanvasPaletteEntry.of(state, colorRgb, altColorRgb));
                }
            }
        }
        return data;
    }

    /**
     * 扫描画布缓冲区及编辑区内尚未收走的临时施工工具（潜影盒、蓝图大炮、蓝图桌）。
     * 通过已加载区块的方块实体索引遍历，避免全尺寸体素暴力扫描。
     */
    public static List<BlockPos> findRemainingUtilityBlocks(ServerLevel canvasLevel, CanvasEditSession session) {
        List<BlockPos> found = new ArrayList<>();
        int minChunkX = SectionPos.blockToSectionCoord(session.origin.getX() - CanvasEditSession.UTILITY_BUFFER_MARGIN);
        int maxChunkX = SectionPos.blockToSectionCoord(session.origin.getX() + session.sizeX + CanvasEditSession.UTILITY_BUFFER_MARGIN - 1);
        int minChunkZ = SectionPos.blockToSectionCoord(session.origin.getZ() - CanvasEditSession.UTILITY_BUFFER_MARGIN);
        int maxChunkZ = SectionPos.blockToSectionCoord(session.origin.getZ() + session.sizeZ + CanvasEditSession.UTILITY_BUFFER_MARGIN - 1);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = canvasLevel.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos bePos = entry.getKey();
                    if (!session.isInsideUtilityBounds(bePos)) {
                        continue;
                    }
                    BlockState state = canvasLevel.getBlockState(bePos);
                    if (CanvasBlockRules.isCanvasUtilityBlock(state)) {
                        found.add(bePos.immutable());
                    }
                }
            }
        }
        found.sort(Comparator.<BlockPos>comparingInt(p -> p.getY())
                .thenComparingInt(p -> p.getX())
                .thenComparingInt(p -> p.getZ()));
        return found;
    }

    /**
     * 打包提取并移除残留的临时施工工具（非创造画布模式保留潜影盒内容组件与机器内部物品；创造画布模式仅清理方块防止刷物品）。
     */
    private static List<ItemStack> collectAndClearUtilityBlocks(
            ServerLevel canvasLevel,
            List<BlockPos> positions,
            boolean preserveItems
    ) {
        List<ItemStack> recovered = new ArrayList<>();
        int clearFlags = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
        for (BlockPos pos : positions) {
            BlockState state = canvasLevel.getBlockState(pos);
            if (!CanvasBlockRules.isCanvasUtilityBlock(state)) {
                continue;
            }
            BlockEntity be = canvasLevel.getBlockEntity(pos);
            if (be instanceof ShulkerBoxBlockEntity shulkerBe) {
                if (preserveItems) {
                    ItemStack boxStack = new ItemStack(state.getBlock());
                    boxStack.applyComponents(shulkerBe.collectComponents());
                    recovered.add(boxStack);
                }
                shulkerBe.clearContent();
            } else {
                if (preserveItems) {
                    recovered.add(new ItemStack(state.getBlock()));
                    if (be instanceof Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            ItemStack slotStack = container.getItem(i);
                            if (!slotStack.isEmpty()) {
                                recovered.add(slotStack.copy());
                            }
                        }
                    }
                }
                if (be instanceof Container container) {
                    container.clearContent();
                }
            }
            canvasLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), clearFlags);
        }
        return recovered;
    }

    private static void saveReturnSnapshotAndApplyMode(
            ServerPlayer player,
            ServerLevel returnLevel,
            BlockPos easelPos,
            UUID canvasUuid
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("InCanvasSpace", true);
        tag.putString("CanvasUuid", canvasUuid.toString());
        tag.putString("EaselDim", returnLevel.dimension().identifier().toString());
        tag.putInt("EaselX", easelPos.getX());
        tag.putInt("EaselY", easelPos.getY());
        tag.putInt("EaselZ", easelPos.getZ());

        tag.putString("ReturnDimension", player.level().dimension().identifier().toString());
        tag.putDouble("ReturnX", player.getX());
        tag.putDouble("ReturnY", player.getY());
        tag.putDouble("ReturnZ", player.getZ());
        tag.putFloat("ReturnYaw", player.getYRot());
        tag.putFloat("ReturnPitch", player.getXRot());

        GameType origMode = player.gameMode.getGameModeForPlayer();
        tag.putString("OriginalGameMode", origMode.name());
        tag.putBoolean("OriginalMayFly", player.getAbilities().mayfly);
        tag.putBoolean("OriginalFlying", player.getAbilities().flying);
        tag.putBoolean("NoclipActive", false);

        if (ModConfig.isCanvasCreativeMode()) {
            tag.putBoolean("SwappedInventory", true);
            tag.put("SavedInventory", captureInventoryTag(player));
            clearInventoryAndXp(player);
            player.setGameMode(GameType.CREATIVE);
        } else {
            tag.putBoolean("SwappedInventory", false);
            if (origMode == GameType.ADVENTURE || origMode == GameType.SPECTATOR) {
                player.setGameMode(GameType.SURVIVAL);
            }
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        ((CanvasPlayerTracker) player).tuanzis_mod$setCanvasSessionData(tag);
    }

    private static void restorePlayerSnapshotAndTeleportBack(
            ServerPlayer player,
            @Nullable MinecraftServer server,
            CompoundTag tag
    ) {
        if (tag.getBooleanOr("SwappedInventory", false) && tag.contains("SavedInventory")) {
            restoreInventoryTag(player, tag.getCompoundOrEmpty("SavedInventory"));
        }

        String origModeName = tag.getStringOr("OriginalGameMode", GameType.SURVIVAL.name());
        GameType origMode;
        try {
            origMode = GameType.valueOf(origModeName);
        } catch (IllegalArgumentException e) {
            origMode = GameType.SURVIVAL;
        }
        player.setGameMode(origMode);

        boolean origMayFly = tag.getBooleanOr("OriginalMayFly", false);
        boolean origFlying = tag.getBooleanOr("OriginalFlying", false);
        if (origMode == GameType.CREATIVE || origMode == GameType.SPECTATOR) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = origFlying;
        } else if (player.hasEffect(ModStatusEffects.FLIGHT)) {
            player.getAbilities().mayfly = true;
        } else {
            player.getAbilities().mayfly = origMayFly;
            player.getAbilities().flying = origMayFly && origFlying;
        }
        player.onUpdateAbilities();

        if (server != null) {
            String dimStr = tag.getStringOr("ReturnDimension", "minecraft:overworld");
            ResourceKey<Level> returnDim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            ServerLevel targetLevel = server.getLevel(returnDim);
            if (targetLevel == null) {
                targetLevel = server.overworld();
            }
            double rx = tag.getDoubleOr("ReturnX", targetLevel.getRespawnData().pos().getX() + 0.5);
            double ry = tag.getDoubleOr("ReturnY", targetLevel.getRespawnData().pos().getY());
            double rz = tag.getDoubleOr("ReturnZ", targetLevel.getRespawnData().pos().getZ() + 0.5);
            float yaw = tag.getFloatOr("ReturnYaw", 0.0f);
            float pitch = tag.getFloatOr("ReturnPitch", 0.0f);

            player.teleport(new TeleportTransition(
                    targetLevel,
                    new Vec3(rx, ry, rz),
                    Vec3.ZERO,
                    yaw,
                    pitch,
                    TeleportTransition.DO_NOTHING
            ));
        }
    }

    private static CompoundTag captureInventoryTag(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        var registries = player.registryAccess();
        Inventory inv = player.getInventory();
        ListTag invList = new ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.put("Item", ItemStackNbtHelper.write(registries, stack));
                invList.add(itemTag);
            }
        }
        tag.put("Inventory", invList);
        tag.putInt("SelectedItemSlot", inv.getSelectedSlot());
        tag.putInt("XpLevel", player.experienceLevel);
        tag.putFloat("XpP", player.experienceProgress);
        tag.putInt("XpTotal", player.totalExperience);
        return tag;
    }

    private static void clearInventoryAndXp(ServerPlayer player) {
        player.getInventory().clearContent();
        player.experienceLevel = 0;
        player.experienceProgress = 0.0f;
        player.totalExperience = 0;
        player.connection.send(new ClientboundSetExperiencePacket(0.0f, 0, 0));
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    private static void restoreInventoryTag(ServerPlayer player, CompoundTag tag) {
        var registries = player.registryAccess();
        Inventory inv = player.getInventory();
        inv.clearContent();
        ListTag invList = tag.getListOrEmpty("Inventory");
        for (int i = 0; i < invList.size(); i++) {
            CompoundTag itemTag = invList.getCompoundOrEmpty(i);
            int slot = itemTag.getIntOr("Slot", -1);
            if (slot >= 0 && slot < inv.getContainerSize()) {
                inv.setItem(slot, ItemStackNbtHelper.read(registries, itemTag.getCompoundOrEmpty("Item")));
            }
        }
        inv.setSelectedSlot(tag.getIntOr("SelectedItemSlot", 0));
        player.experienceLevel = tag.getIntOr("XpLevel", 0);
        player.experienceProgress = tag.getFloatOr("XpP", 0.0f);
        player.totalExperience = tag.getIntOr("XpTotal", 0);
        player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    private static void syncClientCanvasState(ServerPlayer player, CanvasEditSession session, boolean readOnly) {
        boolean border = session.borderVisible.getOrDefault(player.getUUID(), true);
        ServerPlayNetworking.send(player, new CanvasSpaceSyncS2CPacket(
                true,
                session.origin,
                session.sizeX,
                session.sizeY,
                session.sizeZ,
                border,
                readOnly,
                ModConfig.isCanvasCreativeMode()
        ));
    }
}
