package me.tuanzi.world.canvas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 画布空间槽位持久化存储：
 * 按画布 UUID 永久绑定 512×512 间隔的独立虚空槽位，并统计每个玩家的画布创建配额。
 */
public class CanvasSlotSavedData extends SavedData {
    public static final int SLOT_SPACING = 512;
    public static final int GRID_COLUMNS = 64;
    public static final int ORIGIN_Y = 0;

    public record SlotEntry(String canvasUuid, int slotIndex) {
        public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(SlotEntry::canvasUuid),
                Codec.INT.fieldOf("slot").forGetter(SlotEntry::slotIndex)
        ).apply(instance, SlotEntry::new));
    }

    public record OwnerCountEntry(String ownerUuid, int count) {
        public static final Codec<OwnerCountEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("owner").forGetter(OwnerCountEntry::ownerUuid),
                Codec.INT.fieldOf("count").forGetter(OwnerCountEntry::count)
        ).apply(instance, OwnerCountEntry::new));
    }

    public static final Codec<CanvasSlotSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("next_slot", 0).forGetter(d -> d.nextSlotIndex),
            Codec.list(SlotEntry.CODEC).optionalFieldOf("slots", List.of()).forGetter(CanvasSlotSavedData::getSlotEntries),
            Codec.list(OwnerCountEntry.CODEC).optionalFieldOf("owner_counts", List.of()).forGetter(CanvasSlotSavedData::getOwnerEntries)
    ).apply(instance, CanvasSlotSavedData::new));

    public static final SavedDataType<CanvasSlotSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "canvas_slots"),
            CanvasSlotSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private int nextSlotIndex = 0;
    private final Map<UUID, Integer> uuidToSlot = new HashMap<>();
    private final Map<UUID, Integer> ownerCanvasCount = new HashMap<>();

    public CanvasSlotSavedData() {
    }

    public CanvasSlotSavedData(int nextSlotIndex, List<SlotEntry> slots, List<OwnerCountEntry> ownerCounts) {
        this.nextSlotIndex = Math.max(0, nextSlotIndex);
        for (SlotEntry entry : slots) {
            try {
                UUID uuid = UUID.fromString(entry.canvasUuid());
                this.uuidToSlot.put(uuid, entry.slotIndex());
                this.nextSlotIndex = Math.max(this.nextSlotIndex, entry.slotIndex() + 1);
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (OwnerCountEntry entry : ownerCounts) {
            try {
                UUID owner = UUID.fromString(entry.ownerUuid());
                this.ownerCanvasCount.put(owner, Math.max(0, entry.count()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static CanvasSlotSavedData get(MinecraftServer server) {
        CanvasSlotSavedData overworldData = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (overworldData.ownerCanvasCount.isEmpty() && overworldData.uuidToSlot.isEmpty()) {
            ServerLevel legacyLevel = server.getLevel(ModDimensions.CANVAS_SPACE_LEVEL);
            if (legacyLevel != null) {
                CanvasSlotSavedData legacy = legacyLevel.getDataStorage().get(TYPE);
                if (legacy != null && (!legacy.ownerCanvasCount.isEmpty() || !legacy.uuidToSlot.isEmpty())) {
                    overworldData.migrateFrom(legacy);
                }
            }
        }
        return overworldData;
    }

    public static CanvasSlotSavedData get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    private synchronized void migrateFrom(CanvasSlotSavedData legacy) {
        this.ownerCanvasCount.putAll(legacy.ownerCanvasCount);
        this.uuidToSlot.putAll(legacy.uuidToSlot);
        this.nextSlotIndex = Math.max(this.nextSlotIndex, legacy.nextSlotIndex);
        setDirty();
    }

    public synchronized int getOrCreateSlot(UUID canvasUuid) {
        Integer existing = uuidToSlot.get(canvasUuid);
        if (existing != null) {
            return existing;
        }
        int allocated = nextSlotIndex++;
        uuidToSlot.put(canvasUuid, allocated);
        setDirty();
        return allocated;
    }

    public synchronized int getOwnerCount(UUID ownerUuid) {
        return ownerUuid != null ? ownerCanvasCount.getOrDefault(ownerUuid, 0) : 0;
    }

    public synchronized void incrementOwnerCount(UUID ownerUuid) {
        if (ownerUuid == null) {
            return;
        }
        ownerCanvasCount.put(ownerUuid, ownerCanvasCount.getOrDefault(ownerUuid, 0) + 1);
        setDirty();
    }

    /**
     * 损毁画布后释放对应画布主人 1 个配额名额（下限为 0）。
     */
    public synchronized void decrementOwnerCount(UUID ownerUuid) {
        if (ownerUuid == null) {
            return;
        }
        int cur = ownerCanvasCount.getOrDefault(ownerUuid, 0);
        if (cur <= 1) {
            ownerCanvasCount.remove(ownerUuid);
        } else {
            ownerCanvasCount.put(ownerUuid, cur - 1);
        }
        setDirty();
    }

    public static BlockPos computeOriginForSlot(int slotIndex) {
        return BlockPos.ZERO;
    }

    private List<SlotEntry> getSlotEntries() {
        List<SlotEntry> list = new ArrayList<>(uuidToSlot.size());
        for (var e : uuidToSlot.entrySet()) {
            list.add(new SlotEntry(e.getKey().toString(), e.getValue()));
        }
        return list;
    }

    private List<OwnerCountEntry> getOwnerEntries() {
        List<OwnerCountEntry> list = new ArrayList<>(ownerCanvasCount.size());
        for (var e : ownerCanvasCount.entrySet()) {
            list.add(new OwnerCountEntry(e.getKey().toString(), e.getValue()));
        }
        return list;
    }
}
