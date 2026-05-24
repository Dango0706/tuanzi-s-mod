package me.tuanzi.entity;

import java.util.UUID;

public class TrialDummyTestSession {
    private final UUID playerUuid;
    private final UUID dummyUuid;
    private final long startTime;
    private long lastHitTime;
    private float totalDamage;
    private float maxSingleDamage;
    private int attackCount;
    private boolean finished;

    public TrialDummyTestSession(UUID playerUuid, UUID dummyUuid, long startTime) {
        this.playerUuid = playerUuid;
        this.dummyUuid = dummyUuid;
        this.startTime = startTime;
        this.lastHitTime = startTime;
        this.totalDamage = 0.0f;
        this.maxSingleDamage = 0.0f;
        this.attackCount = 0;
        this.finished = false;
    }

    public void recordHit(float damage, long hitTime) {
        this.lastHitTime = hitTime;
        this.totalDamage += damage;
        this.attackCount++;
        if (damage > this.maxSingleDamage) {
            this.maxSingleDamage = damage;
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getDummyUuid() {
        return dummyUuid;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public float getTotalDamage() {
        return totalDamage;
    }

    public float getMaxSingleDamage() {
        return maxSingleDamage;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * 获取测试持续秒数（保留精度，防止除以0）
     */
    public double getDurationInSeconds(long currentTime) {
        long end = this.finished ? this.lastHitTime : currentTime;
        double diff = (end - this.startTime) / 1000.0;
        return Math.max(0.1, diff); // 限制最小为0.1秒
    }

    /**
     * 获取当前DPS
     */
    public double getDPS(long currentTime) {
        return this.totalDamage / this.getDurationInSeconds(currentTime);
    }
}
