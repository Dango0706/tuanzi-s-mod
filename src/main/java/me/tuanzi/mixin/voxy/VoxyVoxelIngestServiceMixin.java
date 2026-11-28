package me.tuanzi.mixin.voxy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.service.VoxelIngestService;
import me.tuanzi.integration.voxy.VoxyColorIngestContext;
import me.tuanzi.integration.voxy.VoxyColorSectionSnapshot;
import me.tuanzi.integration.voxy.VoxyColorSupport;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Pseudo
@Mixin(value = VoxelIngestService.class, remap = false)
public class VoxyVoxelIngestServiceMixin {
    @Unique
    private volatile Map<Object, VoxyColorSectionSnapshot> tuanzis_mod$pendingColors;

    @Unique
    private static final ThreadLocal<Int2ObjectMap<VoxyColorSectionSnapshot>> TUANZIS_MOD$CHUNK_SNAPSHOT = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<VoxyColorSectionSnapshot> TUANZIS_MOD$RAW_SNAPSHOT = new ThreadLocal<>();

    @Unique
    private static volatile @Nullable Method TUANZIS_MOD$INGEST_SECTION_CY;

    @Unique
    private Map<Object, VoxyColorSectionSnapshot> tuanzis_mod$getPendingColors() {
        Map<Object, VoxyColorSectionSnapshot> map = this.tuanzis_mod$pendingColors;
        if (map == null) {
            synchronized (this) {
                map = this.tuanzis_mod$pendingColors;
                if (map == null) {
                    map = Collections.synchronizedMap(new IdentityHashMap<>());
                    this.tuanzis_mod$pendingColors = map;
                }
            }
        }
        return map;
    }

    @Unique
    private static int tuanzis_mod$extractSectionY(Object ingestSection) {
        try {
            Method m = TUANZIS_MOD$INGEST_SECTION_CY;
            if (m == null) {
                m = ingestSection.getClass().getDeclaredMethod("cy");
                m.setAccessible(true);
                TUANZIS_MOD$INGEST_SECTION_CY = m;
            }
            return (int) m.invoke(ingestSection);
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    @Inject(method = "enqueueIngest", at = @At("HEAD"), require = 0)
    private void tuanzis_mod$captureChunkColors(WorldEngine engine, LevelChunk chunk, CallbackInfoReturnable<Boolean> cir) {
        Int2ObjectMap<VoxyColorSectionSnapshot> map = VoxyColorSectionSnapshot.fromChunkBlockEntities(chunk);
        if (map != null && !map.isEmpty()) {
            TUANZIS_MOD$CHUNK_SNAPSHOT.set(map);
        } else {
            TUANZIS_MOD$CHUNK_SNAPSHOT.remove();
        }
    }

    @Inject(method = "enqueueIngest", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$clearChunkColors(WorldEngine engine, LevelChunk chunk, CallbackInfoReturnable<Boolean> cir) {
        TUANZIS_MOD$CHUNK_SNAPSHOT.remove();
    }

    @Inject(method = "rawIngest0", at = @At("HEAD"), require = 0)
    private void tuanzis_mod$captureRawColors(
            WorldEngine engine,
            LevelChunkSection section,
            int x,
            int y,
            int z,
            DataLayer bl,
            DataLayer sl,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (VoxyColorSupport.sectionMayContainColorBlocks(section)) {
            VoxyColorSectionSnapshot snapshot = VoxyColorSupport.extractSectionSnapshot(x, y, z);
            if (snapshot != null && !snapshot.isEmpty()) {
                TUANZIS_MOD$RAW_SNAPSHOT.set(snapshot);
                return;
            }
        }
        TUANZIS_MOD$RAW_SNAPSHOT.remove();
    }

    @Inject(method = "rawIngest0", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$clearRawColors(
            WorldEngine engine,
            LevelChunkSection section,
            int x,
            int y,
            int z,
            DataLayer bl,
            DataLayer sl,
            CallbackInfoReturnable<Boolean> cir
    ) {
        TUANZIS_MOD$RAW_SNAPSHOT.remove();
    }

    @Redirect(
            method = "enqueueIngest",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentLinkedDeque;add(Ljava/lang/Object;)Z"
            ),
            require = 0
    )
    private boolean tuanzis_mod$attachChunkSectionSnapshot(ConcurrentLinkedDeque<Object> queue, Object item) {
        Int2ObjectMap<VoxyColorSectionSnapshot> map = TUANZIS_MOD$CHUNK_SNAPSHOT.get();
        if (map != null) {
            int cy = tuanzis_mod$extractSectionY(item);
            if (cy != Integer.MIN_VALUE) {
                VoxyColorSectionSnapshot snapshot = map.get(cy);
                if (snapshot != null && !snapshot.isEmpty()) {
                    tuanzis_mod$getPendingColors().put(item, snapshot);
                }
            }
        }
        return queue.add(item);
    }

    @Redirect(
            method = "rawIngest0",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentLinkedDeque;add(Ljava/lang/Object;)Z"
            ),
            require = 0
    )
    private boolean tuanzis_mod$attachRawSectionSnapshot(ConcurrentLinkedDeque<Object> queue, Object item) {
        VoxyColorSectionSnapshot snapshot = TUANZIS_MOD$RAW_SNAPSHOT.get();
        if (snapshot != null && !snapshot.isEmpty()) {
            tuanzis_mod$getPendingColors().put(item, snapshot);
        }
        return queue.add(item);
    }

    @Redirect(
            method = "processJob",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentLinkedDeque;pop()Ljava/lang/Object;"
            ),
            require = 0
    )
    private Object tuanzis_mod$bindSectionSnapshot(ConcurrentLinkedDeque<Object> queue) {
        Object task = queue.pop();
        Map<Object, VoxyColorSectionSnapshot> pending = this.tuanzis_mod$pendingColors;
        VoxyColorSectionSnapshot snapshot = pending != null ? pending.remove(task) : null;
        VoxyColorIngestContext.setCurrent(snapshot);
        return task;
    }

    @Inject(method = "processJob", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$unbindSectionSnapshot(CallbackInfo ci) {
        VoxyColorIngestContext.clear();
    }

    @Inject(method = "shutdown", at = @At("TAIL"), require = 0)
    private void tuanzis_mod$clearPendingColors(CallbackInfo ci) {
        Map<Object, VoxyColorSectionSnapshot> pending = this.tuanzis_mod$pendingColors;
        if (pending != null) {
            pending.clear();
        }
    }
}
