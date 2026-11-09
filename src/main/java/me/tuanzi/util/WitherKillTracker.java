package me.tuanzi.util;

public interface WitherKillTracker {
    int tuanzis_mod$getWitherKills();
    void tuanzis_mod$setWitherKills(int kills);
    default int tuanzis_mod$addWitherKills(int amount) {
        int newKills = Math.max(0, tuanzis_mod$getWitherKills() + amount);
        tuanzis_mod$setWitherKills(newKills);
        return newKills;
    }
}
