package me.tuanzi.world.scavenger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * 拾荒回廊玩家专属出生点管理器
 * 确保每位玩家的专属出生点坐标 x、z 均大于等于 10000 格，网格间距为 500 格，
 * 采用方阵拐角扩展算法有序排布，彼此完全隔离且互不碰撞。
 * 兼容正版与离线模式玩家识别，自动持久化存储。
 */
public class CorridorSpawnManager extends SavedData {

    public static final int GRID_SPACING = 500;
    public static final int BASE_OFFSET = 10000;
    public static final Codec<CorridorSpawnManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("next_index", 1).forGetter(m -> m.nextIndex),
            Codec.list(PlayerSpawnEntry.CODEC).optionalFieldOf("entries", List.of()).forGetter(CorridorSpawnManager::getEntriesList)
    ).apply(instance, CorridorSpawnManager::new));
    public static final SavedDataType<CorridorSpawnManager> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "corridor_spawns"),
            CorridorSpawnManager::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
    private final Map<String, PlayerSpawnEntry> spawnsByLowerName = new HashMap<>();
    private int nextIndex = 1;
    public CorridorSpawnManager() {
    }

    public CorridorSpawnManager(int nextIndex, List<PlayerSpawnEntry> entries) {
        this.nextIndex = Math.max(1, nextIndex);
        for (PlayerSpawnEntry entry : entries) {
            this.spawnsByLowerName.put(entry.playerName().toLowerCase(Locale.ROOT), entry);
        }
    }

    public static CorridorSpawnManager get(ServerLevel corridorLevel) {
        return corridorLevel.getDataStorage().computeIfAbsent(TYPE);
    }

    public static BlockPos calculateSpiralPos(int n, int spacing, int y) {
        return calculateSpawnPos(n, spacing, BASE_OFFSET, y);
    }

    public static BlockPos calculateSpiralPos(int n, int spacing, int startDistance, int y) {
        return calculateSpawnPos(n, spacing, startDistance, y);
    }

    /**
     * 计算玩家专属出生点坐标：
     * 保证 x >= baseOffset 且 z >= baseOffset，两两之间网格间距为 spacing（500 格）。
     * 采用方阵拐角扩展算法（Square Corner Sequence），紧密无缝且绝无碰撞。
     */
    public static BlockPos calculateSpawnPos(int n, int spacing, int baseOffset, int y) {
        if (n <= 0) {
            return new BlockPos(baseOffset, y, baseOffset);
        }

        // 将序号 (1-based) 映射为局部二维非负网格索引 (gx, gz)
        int m = n - 1;
        int k = (int) Math.sqrt(m);
        int rem = m - k * k;

        int gx;
        int gz;
        if (rem <= k) {
            gx = k;
            gz = rem;
        } else {
            gx = 2 * k - rem;
            gz = k;
        }

        int x = baseOffset + gx * spacing;
        int z = baseOffset + gz * spacing;
        return new BlockPos(x, y, z);
    }

    private List<PlayerSpawnEntry> getEntriesList() {
        return new ArrayList<>(this.spawnsByLowerName.values());
    }

    /**
     * 获取或生成玩家在拾荒回廊的专属出生点
     */
    public BlockPos getOrCreateSpawnPos(ServerPlayer player, ServerLevel corridorLevel) {
        String playerName = player.getGameProfile().name();
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        String uuidStr = player.getUUID().toString();

        PlayerSpawnEntry entry = this.spawnsByLowerName.get(lowerName);
        if (entry == null) {
            // 尝试通过 UUID 查找，防止玩家改名后丢失出生点
            for (PlayerSpawnEntry e : this.spawnsByLowerName.values()) {
                if (e.playerUuid().equalsIgnoreCase(uuidStr)) {
                    entry = e;
                    this.spawnsByLowerName.remove(e.playerName().toLowerCase(Locale.ROOT));
                    entry = new PlayerSpawnEntry(playerName, uuidStr, e.x(), e.y(), e.z(), e.index());
                    this.spawnsByLowerName.put(lowerName, entry);
                    this.setDirty();
                    ModLog.debug(player, null, "【拾荒回廊出生点】玩家 UUID " + uuidStr + " 改名为 " + playerName + "，已迁移绑定回廊出生点。");
                    break;
                }
            }
        }

        if (entry == null) {
            // 分配新序号并基于方阵展开算法生成坐标 (x >= 10000 且 z >= 10000，间距 500)
            int index = this.nextIndex++;
            BlockPos spawnPos = calculateSpawnPos(index, GRID_SPACING, BASE_OFFSET, 64);
            entry = new PlayerSpawnEntry(playerName, uuidStr, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), index);
            this.spawnsByLowerName.put(lowerName, entry);
            this.setDirty();
            ModLog.debug(player, null, "【拾荒回廊出生点】已为玩家 " + playerName + " (UUID: " + uuidStr + ") 分配专属出生点: (" +
                    spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + ")，网格序号: " + index + " (x, z 均 >= 10000，间距 500 格)");
        } else {
            // 若旧出生点未满足 x >= 10000 且 z >= 10000，自动重新迁移
            if (entry.x() < BASE_OFFSET || entry.z() < BASE_OFFSET) {
                int newIndex = this.nextIndex++;
                BlockPos spawnPos = calculateSpawnPos(newIndex, GRID_SPACING, BASE_OFFSET, 64);
                entry = new PlayerSpawnEntry(playerName, uuidStr, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), newIndex);
                this.spawnsByLowerName.put(lowerName, entry);
                this.setDirty();
                ModLog.debug(player, null, "【拾荒回廊出生点】检测到玩家 " + playerName + " 旧出生点未满足 (x, z 均 >= 10000)，已自动迁移至新出生点: (" +
                        spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + ")。");
            } else if (!entry.playerUuid().equalsIgnoreCase(uuidStr) || !entry.playerName().equals(playerName)) {
                // 更新最后已知 UUID 或大小写
                entry = new PlayerSpawnEntry(playerName, uuidStr, entry.x(), entry.y(), entry.z(), entry.index());
                this.spawnsByLowerName.put(lowerName, entry);
                this.setDirty();
            }
        }

        BlockPos pos = entry.toBlockPos();
        if (corridorLevel != null) {
            ModDimensions.ensurePlatform(corridorLevel, pos);
        }
        return pos;
    }

    public record PlayerSpawnEntry(String playerName, String playerUuid, int x, int y, int z, int index) {
        public static final Codec<PlayerSpawnEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("player_name").forGetter(PlayerSpawnEntry::playerName),
                Codec.STRING.fieldOf("player_uuid").forGetter(PlayerSpawnEntry::playerUuid),
                Codec.INT.fieldOf("x").forGetter(PlayerSpawnEntry::x),
                Codec.INT.fieldOf("y").forGetter(PlayerSpawnEntry::y),
                Codec.INT.fieldOf("z").forGetter(PlayerSpawnEntry::z),
                Codec.INT.fieldOf("index").forGetter(PlayerSpawnEntry::index)
        ).apply(instance, PlayerSpawnEntry::new));

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }
}
