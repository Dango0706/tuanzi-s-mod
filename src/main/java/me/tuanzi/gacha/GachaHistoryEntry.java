package me.tuanzi.gacha;

public class GachaHistoryEntry {
    private final String poolName;
    private final String rarity;
    private final String itemName;
    private final String timestamp;
    private final String itemRegistryId;
    private final int count; // 核心修复：存储物品被抽中时的堆叠数量

    public GachaHistoryEntry(String poolName, String rarity, String itemName, String timestamp) {
        this(poolName, rarity, itemName, timestamp, null, 1);
    }

    public GachaHistoryEntry(String poolName, String rarity, String itemName, String timestamp, String itemRegistryId) {
        this(poolName, rarity, itemName, timestamp, itemRegistryId, 1);
    }

    public GachaHistoryEntry(String poolName, String rarity, String itemName, String timestamp, String itemRegistryId, int count) {
        this.poolName = poolName;
        this.rarity = rarity;
        this.itemName = itemName;
        this.timestamp = timestamp;
        this.itemRegistryId = itemRegistryId;
        this.count = count;
    }

    public String getPoolName() {
        return poolName;
    }

    public String getRarity() {
        return rarity;
    }

    public String getItemName() {
        return itemName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getItemRegistryId() {
        return itemRegistryId;
    }

    public int getCount() {
        // 向下兼容：对于旧的历史存档（JSON 中无 count 字段或 count <= 0），一律自动退化恢复为 1
        return count <= 0 ? 1 : count;
    }
}
