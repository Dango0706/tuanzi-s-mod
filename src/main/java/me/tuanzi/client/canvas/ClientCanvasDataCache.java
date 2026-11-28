package me.tuanzi.client.canvas;

import me.tuanzi.network.canvas.CanvasDataChunkS2CPacket;
import me.tuanzi.network.canvas.CanvasDataRequestC2SPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.canvas.CanvasDataCodec;
import me.tuanzi.world.canvas.CanvasVoxelData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端打印模型数据与静态烘焙网格 LRU 缓存，以及分片同步拼包器。
 */
@Environment(EnvType.CLIENT)
public final class ClientCanvasDataCache {
    private static final int MAX_ENTRIES = 64;

    public record CachedEntry(CanvasVoxelData voxelData, @Nullable PrintedModelChunkedMesh mesh) {
    }

    private static final Map<String, CachedEntry> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedEntry> eldest) {
                    return size() > MAX_ENTRIES;
                }
            }
    );

    private static final Set<String> IN_FLIGHT_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final Map<String, byte[][]> PARTIAL_CHUNKS = new HashMap<>();

    private ClientCanvasDataCache() {
    }

    private static String cacheKey(UUID uuid, String hash) {
        return uuid.toString() + ":" + (hash != null ? hash : "");
    }

    public static void clearAll() {
        CACHE.clear();
        IN_FLIGHT_REQUESTS.clear();
        PARTIAL_CHUNKS.clear();
    }

    public static @Nullable CanvasVoxelData getVoxelData(UUID uuid, String hash) {
        if (uuid == null) {
            return null;
        }
        CachedEntry entry = CACHE.get(cacheKey(uuid, hash));
        return entry != null ? entry.voxelData() : null;
    }

    public static @Nullable PrintedModelChunkedMesh getOrBuildMesh(Minecraft mc, UUID uuid, String hash) {
        if (uuid == null) {
            return null;
        }
        String key = cacheKey(uuid, hash);
        CachedEntry entry = CACHE.get(key);
        if (entry == null) {
            requestIfMissing(uuid, hash);
            return null;
        }
        if (entry.mesh() != null) {
            return entry.mesh();
        }
        PrintedModelChunkedMesh built = PrintedModelChunkedMesh.build(mc, entry.voxelData());
        CACHE.put(key, new CachedEntry(entry.voxelData(), built));
        return built;
    }

    public static boolean requestIfMissing(UUID uuid, String hash) {
        if (uuid == null) {
            return false;
        }
        String key = cacheKey(uuid, hash);
        if (CACHE.containsKey(key) || !IN_FLIGHT_REQUESTS.add(key)) {
            return false;
        }
        ClientPlayNetworking.send(new CanvasDataRequestC2SPacket(uuid.toString(), hash != null ? hash : ""));
        return true;
    }

    public static void handleChunkPacket(CanvasDataChunkS2CPacket packet) {
        String key = packet.modelUuid() + ":" + (packet.dataHash() != null ? packet.dataHash() : "");
        int total = Math.max(1, packet.totalChunks());
        int idx = packet.chunkIndex();
        if (idx < 0 || idx >= total) {
            return;
        }

        byte[][] chunks = PARTIAL_CHUNKS.computeIfAbsent(key, k -> new byte[total][]);
        if (chunks.length != total) {
            chunks = new byte[total][];
            PARTIAL_CHUNKS.put(key, chunks);
        }
        chunks[idx] = packet.payload();

        for (byte[] part : chunks) {
            if (part == null) {
                return;
            }
        }
        PARTIAL_CHUNKS.remove(key);
        IN_FLIGHT_REQUESTS.remove(key);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte[] part : chunks) {
            baos.writeBytes(part);
        }
        CanvasDataCodec.DecodeResult decoded = CanvasDataCodec.decode(baos.toByteArray());
        if (decoded.isOk() && decoded.data() != null) {
            CACHE.put(key, new CachedEntry(decoded.data(), null));
            ModLog.debug("[ClientCanvasDataCache] 接收并缓存模型体素数据: key=" + key
                    + ", nonEmpty=" + decoded.data().getNonEmptyCount());
        } else {
            ModLog.warn("[ClientCanvasDataCache] 客户端解码模型数据失败: " + key + " -> " + decoded.errorReason());
        }
    }
}
