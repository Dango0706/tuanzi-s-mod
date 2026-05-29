package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.effect.AdrenalineStatusEffect;
import me.tuanzi.effect.AdrenalineOverdrawStatusEffect;
import me.tuanzi.effect.BloodRageStatusEffect;
import me.tuanzi.effect.FlightStatusEffect;
import me.tuanzi.effect.UndyingStatusEffect;
import me.tuanzi.effect.TearingStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public class ModStatusEffects {
    public static final Holder<MobEffect> FLIGHT = register("flight", new FlightStatusEffect());
    public static final Holder<MobEffect> UNDYING = register("undying", new UndyingStatusEffect());
    public static final Holder<MobEffect> BLOOD_RAGE = register("blood_rage", new BloodRageStatusEffect());
    public static final Holder<MobEffect> ADRENALINE = register("adrenaline", new AdrenalineStatusEffect());
    public static final Holder<MobEffect> ADRENALINE_OVERDRAW = register("adrenaline_overdraw", new AdrenalineOverdrawStatusEffect());
    public static final Holder<MobEffect> TEARING = register("tearing", new TearingStatusEffect());

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, name), effect);
    }

    public static void initialize() {}
}
