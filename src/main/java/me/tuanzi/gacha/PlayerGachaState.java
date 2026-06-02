package me.tuanzi.gacha;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerGachaState {
    private final UUID uuid;
    
    // 常驻池保底计数器
    private int normalLegendaryCounter = 0;
    private int normalEpicCounter = 0;

    // 限定池保底计数器（全局限定池共享）
    private int sakuraLegendaryCounter = 0;
    private int sakuraEpicCounter = 0;

    // 抽卡历史记录列表（无上限）
    private final List<GachaHistoryEntry> history = new ArrayList<>();

    // 内存脏标记，transient 避免序列化入 JSON
    private transient boolean isDirty = false;

    public PlayerGachaState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getNormalLegendaryCounter() {
        return normalLegendaryCounter;
    }

    public void setNormalLegendaryCounter(int normalLegendaryCounter) {
        this.normalLegendaryCounter = normalLegendaryCounter;
        this.isDirty = true;
    }

    public int getNormalEpicCounter() {
        return normalEpicCounter;
    }

    public void setNormalEpicCounter(int normalEpicCounter) {
        this.normalEpicCounter = normalEpicCounter;
        this.isDirty = true;
    }

    public int getSakuraLegendaryCounter() {
        return sakuraLegendaryCounter;
    }

    public void setSakuraLegendaryCounter(int sakuraLegendaryCounter) {
        this.sakuraLegendaryCounter = sakuraLegendaryCounter;
        this.isDirty = true;
    }

    public int getSakuraEpicCounter() {
        return sakuraEpicCounter;
    }

    public void setSakuraEpicCounter(int sakuraEpicCounter) {
        this.sakuraEpicCounter = sakuraEpicCounter;
        this.isDirty = true;
    }

    public List<GachaHistoryEntry> getHistory() {
        return history;
    }

    public synchronized void addHistoryEntry(GachaHistoryEntry entry) {
        this.history.add(entry);
        this.isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }
}
