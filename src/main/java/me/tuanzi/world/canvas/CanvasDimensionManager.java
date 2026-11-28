package me.tuanzi.world.canvas;

import com.google.common.collect.ImmutableList;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.mixin.MinecraftServerAccessor;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态画布虚空维度生命周期管理器：
 * 为每张画布按 UUID 动态挂载独立的虚空 {@link ServerLevel}，退出后于服务端 Tick 末安全卸载关闭，
 * 并确保 {@code noSave = true} 与关服前清理，避免产生多余维度存档数据。
 */
public final class CanvasDimensionManager {
    private static final Set<ResourceKey<Level>> PENDING_UNLOAD = ConcurrentHashMap.newKeySet();

    private CanvasDimensionManager() {
    }

    /**
     * 获取或动态创建指定画布 UUID 对应的独立虚空维度。
     */
    public static @Nullable ServerLevel getOrCreateCanvasLevel(MinecraftServer server, UUID canvasUuid) {
        ResourceKey<Level> dimKey = ModDimensions.createCanvasInstanceDimension(canvasUuid);
        PENDING_UNLOAD.remove(dimKey);

        ServerLevel existing = server.getLevel(dimKey);
        if (existing != null) {
            existing.noSave = true;
            return existing;
        }

        LevelStem canvasStem = resolveCanvasStem(server);
        if (canvasStem == null) {
            ModLog.error("[CanvasDimensionManager] 未找到画布基准 LevelStem: " + ModDimensions.CANVAS_SPACE_STEM.identifier());
            return null;
        }

        MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
        ServerLevelData overworldData = accessor.tuanzis_mod$getWorldData().overworldData();
        boolean isDebug = accessor.tuanzis_mod$getWorldData().isDebugWorld();
        long biomeZoomSeed = BiomeManager.obfuscateSeed(accessor.tuanzis_mod$getWorldGenSettings().options().seed());
        DerivedLevelData derivedLevelData = new DerivedLevelData(accessor.tuanzis_mod$getWorldData(), overworldData);

        ServerLevel dynamicLevel = new ServerLevel(
                server,
                accessor.tuanzis_mod$getExecutor(),
                accessor.tuanzis_mod$getStorageSource(),
                derivedLevelData,
                dimKey,
                canvasStem,
                isDebug,
                biomeZoomSeed,
                ImmutableList.of(),
                false
        );
        dynamicLevel.noSave = true;
        dynamicLevel.getWorldBorder().setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
        server.getPlayerList().addWorldborderListener(dynamicLevel);

        accessor.tuanzis_mod$getLevels().put(dimKey, dynamicLevel);
        ServerLevelEvents.LOAD.invoker().onLevelLoad(server, dynamicLevel);

        ModLog.debug("【画布动态维度】已创建独立画布空间维度: " + dimKey.identifier());
        return dynamicLevel;
    }

    /**
     * 将空置的动态画布维度加入待卸载队列，等待 Tick 末安全移除。
     */
    public static void scheduleUnload(@Nullable ResourceKey<Level> dimKey) {
        if (dimKey != null && !dimKey.equals(ModDimensions.CANVAS_SPACE_LEVEL)
                && ModDimensions.isCanvasSpaceDimension(dimKey)) {
            PENDING_UNLOAD.add(dimKey);
        }
    }

    /**
     * 在服务端 Tick 结束后安全移除并关闭已空置的动态画布维度。
     */
    public static void processPendingUnloads(@Nullable MinecraftServer server) {
        if (server == null || PENDING_UNLOAD.isEmpty()) {
            return;
        }
        List<ResourceKey<Level>> snapshot = new ArrayList<>(PENDING_UNLOAD);
        Map<ResourceKey<Level>, ServerLevel> levels = ((MinecraftServerAccessor) server).tuanzis_mod$getLevels();

        for (ResourceKey<Level> dimKey : snapshot) {
            PENDING_UNLOAD.remove(dimKey);
            ServerLevel level = levels.get(dimKey);
            if (level == null) {
                continue;
            }
            if (!level.players().isEmpty()) {
                continue;
            }
            unloadSingleLevel(server, levels, dimKey, level);
        }
    }

    /**
     * 停服前强制将所有玩家退出动态画布空间并提前卸载全部动态维度，避免关服阶段把临时虚空区块刷盘。
     */
    public static void unloadAllCanvasLevels(@Nullable MinecraftServer server) {
        PENDING_UNLOAD.clear();
        if (server == null) {
            return;
        }
        List<ServerPlayer> playersSnapshot = new ArrayList<>(server.getPlayerList().getPlayers());
        for (ServerPlayer player : playersSnapshot) {
            if (ModDimensions.isCanvasSpaceLevel(player.level())) {
                CanvasSpaceManager.exitCanvasSpace(player, CanvasSpaceManager.ExitReason.DISCONNECT_OR_SHUTDOWN);
            }
        }

        Map<ResourceKey<Level>, ServerLevel> levels = ((MinecraftServerAccessor) server).tuanzis_mod$getLevels();
        List<ResourceKey<Level>> dynamicKeys = new ArrayList<>();
        for (ResourceKey<Level> key : levels.keySet()) {
            if (!key.equals(ModDimensions.CANVAS_SPACE_LEVEL) && ModDimensions.isCanvasSpaceDimension(key)) {
                dynamicKeys.add(key);
            }
        }
        for (ResourceKey<Level> key : dynamicKeys) {
            ServerLevel level = levels.get(key);
            if (level != null) {
                unloadSingleLevel(server, levels, key, level);
            }
        }
    }

    public static void clearState() {
        PENDING_UNLOAD.clear();
    }

    private static @Nullable LevelStem resolveCanvasStem(MinecraftServer server) {
        var access = server.registryAccess();
        Registry<LevelStem> stems = access.lookupOrThrow(Registries.LEVEL_STEM);
        LevelStem existing = stems.getValue(ModDimensions.CANVAS_SPACE_STEM);
        if (existing != null) {
            return existing;
        }
        var dimTypes = access.lookupOrThrow(Registries.DIMENSION_TYPE);
        Optional<Holder.Reference<DimensionType>> typeOpt = dimTypes.get(ModDimensions.CANVAS_SPACE_TYPE);
        if (typeOpt.isEmpty()) {
            typeOpt = dimTypes.get(BuiltinDimensionTypes.OVERWORLD);
        }
        if (typeOpt.isEmpty()) {
            return null;
        }
        Holder<Biome> voidBiome = access.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_VOID);
        FlatLevelGeneratorSettings flatSettings = new FlatLevelGeneratorSettings(
                Optional.empty(),
                voidBiome,
                List.of()
        ).withBiomeAndLayers(
                List.of(new FlatLayerInfo(1, Blocks.AIR)),
                Optional.of(HolderSet.direct()),
                voidBiome
        );
        return new LevelStem(typeOpt.get(), new FlatLevelSource(flatSettings));
    }

    private static void unloadSingleLevel(
            MinecraftServer server,
            Map<ResourceKey<Level>, ServerLevel> levels,
            ResourceKey<Level> dimKey,
            ServerLevel level
    ) {
        levels.remove(dimKey);
        ServerLevelEvents.UNLOAD.invoker().onLevelUnload(server, level);
        try {
            level.close();
        } catch (IOException e) {
            ModLog.error("[CanvasDimensionManager] 关闭动态画布维度失败: " + dimKey.identifier(), e);
        }
        ModLog.debug("【画布动态维度】已安全卸载并关闭独立画布维度: " + dimKey.identifier());
    }
}
