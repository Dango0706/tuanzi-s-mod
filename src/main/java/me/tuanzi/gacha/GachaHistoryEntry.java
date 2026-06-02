package me.tuanzi.gacha;

public class GachaHistoryEntry {
    private final String poolName;
    private final String rarity;
    private final String itemName;
    private final String timestamp;
    private final String itemRegistryId;

    public GachaHistoryEntry(String poolName, String rarity, String itemName, String timestamp) {
        this(poolName, rarity, itemName, timestamp, null);
    }

    public GachaHistoryEntry(String poolName, String rarity, String itemName, String timestamp, String itemRegistryId) {
        this.poolName = poolName;
        this.rarity = rarity;
        this.itemName = itemName;
        this.timestamp = timestamp;
        this.itemRegistryId = itemRegistryId;
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
}
