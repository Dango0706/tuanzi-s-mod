package me.tuanzi.config;

import me.tuanzi.network.OverworldBuildHeightSyncS2CPacket;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 主世界建筑高度限制控制与服务端/客户端热重载同步。
 */
public final class OverworldBuildHeightControl {
    private static final ThreadLocal<Boolean> EXPANDING_DIMENSION_TYPE_REGISTRY = ThreadLocal.withInitial(() -> false);
    private static volatile Integer clientSyncedOverworldMaxBuildHeight = null;

    private OverworldBuildHeightControl() {
    }

    public static void setExpandingDimensionTypeRegistry(boolean expanding) {
        if (expanding) {
            EXPANDING_DIMENSION_TYPE_REGISTRY.set(true);
        } else {
            EXPANDING_DIMENSION_TYPE_REGISTRY.remove();
        }
    }

    public static boolean isExpandingDimensionTypeRegistry() {
        return Boolean.TRUE.equals(EXPANDING_DIMENSION_TYPE_REGISTRY.get());
    }

    /**
     * 将主世界 DimensionType 的物理最大建筑空间扩展至原版引擎安全上限 (MAX_Y = 2031，对应 height = 2096)。
     */
    public static DimensionType expandOverworldDimensionType(DimensionType original) {
        if (original == null) {
            return null;
        }
        int targetMaxYPlusOne = DimensionType.MAX_Y + 1;
        int expandedHeight = targetMaxYPlusOne - original.minY();
        if (expandedHeight <= original.height() || expandedHeight % 16 != 0) {
            return original;
        }
        ModLog.debug("[OverworldBuildHeightControl] 扩展主世界 DimensionType 物理高度上限: minY="
                + original.minY() + ", 原 height=" + original.height() + " -> 新 height=" + expandedHeight
                + " (maxY=" + (original.minY() + expandedHeight - 1) + ")");
        return new DimensionType(
                original.hasFixedTime(),
                original.hasSkyLight(),
                original.hasCeiling(),
                original.hasEnderDragonFight(),
                original.coordinateScale(),
                original.minY(),
                expandedHeight,
                original.logicalHeight(),
                original.infiniburn(),
                original.ambientLight(),
                original.monsterSettings(),
                original.skybox(),
                original.cardinalLightType(),
                original.attributes(),
                original.timelines(),
                original.defaultClock()
        );
    }

    public static boolean isOverworld(Level level) {
        return level != null && Level.OVERWORLD.equals(level.dimension());
    }

    public static void applyClientSync(int overworldMaxBuildHeight) {
        int clamped = ModConfig.clampOverworldMaxBuildHeight(overworldMaxBuildHeight);
        clientSyncedOverworldMaxBuildHeight = clamped;
        ModLog.debug("[OverworldBuildHeightControl] 客户端已同步主世界最大建筑高度: " + clamped
                + " (maxY=" + (clamped - 1) + ")");
    }

    public static void clearClientSync() {
        clientSyncedOverworldMaxBuildHeight = null;
    }

    public static Integer getClientSyncedOverworldMaxBuildHeight() {
        return clientSyncedOverworldMaxBuildHeight;
    }

    /**
     * 获取当前维度生效的最大建筑高度上界（开区间上界，例如 320 表示最大可放置 Y=319）。
     */
    public static int getEffectiveMaxBuildHeight(Level level) {
        if (!isOverworld(level)) {
            return level.getMaxY() + 1;
        }
        int configured = ModConfig.getOverworldMaxBuildHeight();
        if (level.isClientSide()) {
            Integer synced = clientSyncedOverworldMaxBuildHeight;
            if (synced != null) {
                configured = synced;
            }
        }
        int physicalLimit = level.getMaxY() + 1;
        return Math.min(ModConfig.clampOverworldMaxBuildHeight(configured), physicalLimit);
    }

    /**
     * 获取当前维度生效的最大可放置方块 Y 坐标（例如 319 ~ 2031）。
     */
    public static int getEffectiveMaxY(Level level) {
        return getEffectiveMaxBuildHeight(level) - 1;
    }

    public static boolean isWithinBuildLimit(Level level, BlockPos pos) {
        if (!isOverworld(level) || pos == null) {
            return true;
        }
        int y = pos.getY();
        return y >= level.getMinY() && y <= getEffectiveMaxY(level);
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int maxBuildHeight = ModConfig.getOverworldMaxBuildHeight();
        ServerPlayNetworking.send(player, new OverworldBuildHeightSyncS2CPacket(maxBuildHeight));
        ModLog.debug(player, null, "[OverworldBuildHeightControl] 已向玩家同步主世界最大建筑高度: "
                + maxBuildHeight + " (maxY=" + (maxBuildHeight - 1) + ")");
    }

    public static void syncToAllPlayers(MinecraftServer server) {
        if (server == null) {
            return;
        }
        int maxBuildHeight = ModConfig.getOverworldMaxBuildHeight();
        OverworldBuildHeightSyncS2CPacket packet = new OverworldBuildHeightSyncS2CPacket(maxBuildHeight);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
        ModLog.debug("[OverworldBuildHeightControl] 已向全部在线玩家广播主世界最大建筑高度: "
                + maxBuildHeight + " (maxY=" + (maxBuildHeight - 1) + ")");
    }

    public static void handleHotReload(MinecraftServer server, boolean previousCasinoRecipesEnabled) {
        ModLog.debug("[OverworldBuildHeightControl] 配置热加载生效: overworldMaxBuildHeight="
                + ModConfig.getOverworldMaxBuildHeight()
                + " (maxY=" + ModConfig.getOverworldMaxPlaceableY() + ")"
                + ", enableCasinoRecipes=" + ModConfig.isCasinoRecipesEnabled()
                + ", traumaHudConflictMode=" + ModConfig.getTraumaHudConflictMode());
        if (server != null) {
            server.execute(() -> {
                syncToAllPlayers(server);
                if (previousCasinoRecipesEnabled != ModConfig.isCasinoRecipesEnabled()) {
                    CasinoRecipeControl.reloadServerResources(server);
                }
            });
        }
    }
}
