package me.tuanzi.util;

public interface NodachiPlayerTracker {
    int tuanzis_mod$getNodachiMomentum();
    void tuanzis_mod$setNodachiMomentum(int momentum);

    int tuanzis_mod$getNodachiExhaustionTicks();
    void tuanzis_mod$setNodachiExhaustionTicks(int ticks);

    int tuanzis_mod$getNodachiInactiveTicks();
    void tuanzis_mod$setNodachiInactiveTicks(int ticks);

    int tuanzis_mod$getCrescentAuraCooldownTicks();
    void tuanzis_mod$setCrescentAuraCooldownTicks(int ticks);

    int tuanzis_mod$getNodachiRhythmGraceTicks();
    void tuanzis_mod$setNodachiRhythmGraceTicks(int ticks);

    void tuanzis_mod$triggerNodachiExhaustion(String reason, int ticks);

    default void tuanzis_mod$triggerNodachiExhaustion(String reason) {
        tuanzis_mod$triggerNodachiExhaustion(reason, 10);
    }

    default void tuanzis_mod$triggerNodachiExhaustion() {
        tuanzis_mod$triggerNodachiExhaustion("未指定原因", 10);
    }
}
