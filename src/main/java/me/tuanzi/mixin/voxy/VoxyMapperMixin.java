package me.tuanzi.mixin.voxy;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.cortex.voxy.common.config.IMappingStorage;
import me.cortex.voxy.common.world.other.Mapper;
import me.tuanzi.integration.voxy.VoxyColorSupport;
import me.tuanzi.integration.voxy.VoxyMapperAccess;
import me.tuanzi.integration.voxy.VoxyStateEntryAccess;
import me.tuanzi.util.ModLog;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Pseudo
@Mixin(value = Mapper.class, remap = false)
public abstract class VoxyMapperMixin implements VoxyMapperAccess {
    @Unique
    private static final int TUANZIS_MOD$BLOCK_STATE_TYPE = 1;

    @Shadow
    @Final
    private IMappingStorage storage;
    @Shadow
    @Final
    private ReentrantLock blockLock;
    @Shadow
    @Final
    private ObjectArrayList<Mapper.StateEntry> blockId2stateEntry;
    @Shadow
    private Consumer<Mapper.StateEntry> newStateCallback;

    @Unique
    private volatile ConcurrentHashMap<ColoredStateKey, Mapper.StateEntry> tuanzis_mod$colored2stateEntry;

    @Unique
    private record ColoredStateKey(BlockState state, int color, int topColor) {
    }

    @Unique
    private ConcurrentHashMap<ColoredStateKey, Mapper.StateEntry> tuanzis_mod$getColoredMap() {
        ConcurrentHashMap<ColoredStateKey, Mapper.StateEntry> map = this.tuanzis_mod$colored2stateEntry;
        if (map == null) {
            synchronized (this) {
                map = this.tuanzis_mod$colored2stateEntry;
                if (map == null) {
                    map = new ConcurrentHashMap<>();
                    this.tuanzis_mod$colored2stateEntry = map;
                }
            }
        }
        return map;
    }

    @Redirect(
            method = "loadFromStorage",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentHashMap;putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            require = 0
    )
    private Object tuanzis_mod$routeColoredEntryOnLoad(ConcurrentHashMap<Object, Object> map, Object key, Object value) {
        if (value instanceof Mapper.StateEntry entry) {
            VoxyStateEntryAccess access = (VoxyStateEntryAccess) (Object) entry;
            int customColor = access.tuanzis_mod$getCustomColor();
            if (customColor >= 0) {
                int topColor = access.tuanzis_mod$getCustomTopColor();
                tuanzis_mod$getColoredMap().putIfAbsent(new ColoredStateKey(entry.state, customColor, topColor), entry);
                return null;
            }
        }
        return map.putIfAbsent(key, value);
    }

    @Inject(
            method = "forceResaveStates",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/common/config/IMappingStorage;flush()V"
            ),
            require = 0
    )
    private void tuanzis_mod$resaveColoredEntries(CallbackInfo ci) {
        for (Mapper.StateEntry entry : tuanzis_mod$getColoredMap().values()) {
            if (entry.state.isAir() && entry.id == 0) {
                continue;
            }
            if (this.blockId2stateEntry.indexOf(entry) != entry.id) {
                throw new IllegalStateException("Colored State Id mismatch: " + entry.id);
            }
            tuanzis_mod$writeEntryToStorage(entry);
        }
    }

    @Unique
    private void tuanzis_mod$writeEntryToStorage(Mapper.StateEntry entry) {
        byte[] serialized = entry.serialize();
        ByteBuffer buffer = MemoryUtil.memAlloc(serialized.length);
        try {
            buffer.put(serialized);
            buffer.rewind();
            this.storage.putIdMapping(entry.id | (TUANZIS_MOD$BLOCK_STATE_TYPE << 30), buffer);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    @Override
    public int tuanzis_mod$getIdForColoredBlockState(BlockState state, int primaryColor, int topColor) {
        if (state == null || state.isAir()) {
            return 0;
        }
        long packed = VoxyColorSupport.normalizeColors(state, primaryColor, topColor);
        int c = VoxyColorSupport.unpackPrimary(packed);
        int tc = VoxyColorSupport.unpackTop(packed);

        ConcurrentHashMap<ColoredStateKey, Mapper.StateEntry> coloredMap = tuanzis_mod$getColoredMap();
        ColoredStateKey key = new ColoredStateKey(state, c, tc);
        Mapper.StateEntry existing = coloredMap.get(key);
        if (existing != null) {
            return existing.id;
        }

        if (coloredMap.size() >= VoxyColorSupport.MAX_EXACT_COLOR_ENTRIES) {
            int qC = VoxyColorSupport.quantizeRgb444(c);
            int qTc = tc >= 0 ? VoxyColorSupport.quantizeRgb444(tc) : VoxyColorSupport.NO_COLOR;
            if (qC != c || qTc != tc) {
                c = qC;
                tc = qTc;
                key = new ColoredStateKey(state, c, tc);
                existing = coloredMap.get(key);
                if (existing != null) {
                    return existing.id;
                }
            }
        }

        Mapper.StateEntry created;
        this.blockLock.lock();
        try {
            existing = coloredMap.get(key);
            if (existing != null) {
                return existing.id;
            }
            int newId = this.blockId2stateEntry.size();
            created = new Mapper.StateEntry(newId, state);
            ((VoxyStateEntryAccess) (Object) created).tuanzis_mod$setCustomColors(c, tc);
            this.blockId2stateEntry.add(created);
            coloredMap.put(key, created);
        } finally {
            this.blockLock.unlock();
        }

        tuanzis_mod$writeEntryToStorage(created);
        if (this.newStateCallback != null) {
            this.newStateCallback.accept(created);
        }
        ModLog.debug("[VoxyMapper] Registered colored state id=" + created.id + " state=" + state
                + " color=" + String.format("#%06X", c)
                + (tc >= 0 ? (" topColor=" + String.format("#%06X", tc)) : ""));
        return created.id;
    }

    @Override
    public @Nullable VoxyStateEntryAccess tuanzis_mod$getStateEntryByBlockId(int blockId) {
        if (blockId < 0 || blockId >= this.blockId2stateEntry.size()) {
            return null;
        }
        return (VoxyStateEntryAccess) (Object) this.blockId2stateEntry.get(blockId);
    }
}
