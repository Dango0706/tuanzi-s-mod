package me.tuanzi.integration.voxy;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.cortex.voxy.client.ICheekyClientChunkCache;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.common.world.service.VoxelIngestService;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Voxy 客户端实时联动桥接器。
 * 仅在检测到已安装 Voxy 模组时初始化，负责监听彩色方块实体变色并按区块段防抖刷新至 Voxy。
 */
public final class VoxyClientBridge {
    private static final LongOpenHashSet DIRTY_SECTIONS = new LongOpenHashSet();
    private static boolean initialized = false;

    private VoxyClientBridge() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        VoxyColorSupport.registerClientHooks(VoxyClientBridge::markDirty, VoxyClientBridge::extractSnapshotFromClientLevel);
        ClientTickEvents.END_CLIENT_TICK.register(VoxyClientBridge::flushDirtySections);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            synchronized (DIRTY_SECTIONS) {
                DIRTY_SECTIONS.clear();
            }
        });
        ModLog.debug("[VoxyBridge] Registered Voxy color LOD client hooks.");
    }

    private static void markDirty(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel)) {
            return;
        }
        long secKey = SectionPos.asLong(pos);
        synchronized (DIRTY_SECTIONS) {
            DIRTY_SECTIONS.add(secKey);
        }
    }

    private static void flushDirtySections(Minecraft client) {
        long[] pending;
        synchronized (DIRTY_SECTIONS) {
            if (DIRTY_SECTIONS.isEmpty()) {
                return;
            }
            pending = DIRTY_SECTIONS.toLongArray();
            DIRTY_SECTIONS.clear();
        }

        ClientLevel level = client.level;
        if (level == null || VoxyCommon.getInstance() == null || !VoxyConfig.CONFIG.ingestEnabled) {
            return;
        }

        WorldIdentifier worldId = WorldIdentifier.of(level);
        if (worldId == null) {
            return;
        }

        var lightEngine = level.getLightEngine();
        var blockLightLayer = lightEngine.getLayerListener(LightLayer.BLOCK);
        var skyLightLayer = lightEngine.getLayerListener(LightLayer.SKY);
        int bottomSectionY = level.getMinSectionY();

        for (long secKey : pending) {
            int sx = SectionPos.x(secKey);
            int sy = SectionPos.y(secKey);
            int sz = SectionPos.z(secKey);

            LevelChunk chunk = level.getChunkSource().getChunk(sx, sz, ChunkStatus.FULL, false);
            if (chunk == null) {
                continue;
            }
            int sectionIndex = sy - bottomSectionY;
            LevelChunkSection[] sections = chunk.getSections();
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                continue;
            }
            LevelChunkSection section = sections[sectionIndex];
            if (section == null) {
                continue;
            }

            SectionPos sectionPos = SectionPos.of(sx, sy, sz);
            DataLayer bl = blockLightLayer.getDataLayerData(sectionPos);
            DataLayer sl = skyLightLayer.getDataLayerData(sectionPos);
            boolean ingested = VoxelIngestService.rawIngest(
                    worldId,
                    section,
                    sx,
                    sy,
                    sz,
                    bl == null ? null : bl.copy(),
                    sl == null ? null : sl.copy()
            );
            ModLog.debug("[VoxyBridge] Flushed dirty color section (" + sx + ", " + sy + ", " + sz + "), rawIngest=" + ingested);
        }
    }

    private static @Nullable VoxyColorSectionSnapshot extractSnapshotFromClientLevel(int sectionX, int sectionY, int sectionZ) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }
        ClientChunkCache chunkSource = level.getChunkSource();
        LevelChunk chunk = chunkSource.getChunk(sectionX, sectionZ, ChunkStatus.FULL, false);
        if (chunk == null && chunkSource instanceof ICheekyClientChunkCache cheeky) {
            chunk = cheeky.voxy$cheekyGetChunk(sectionX, sectionZ);
        }
        if (chunk == null) {
            return null;
        }
        return VoxyColorSectionSnapshot.fromChunkSection(chunk, sectionY);
    }
}
