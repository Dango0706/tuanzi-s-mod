package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.effect.FlightStatusEffect;
import me.tuanzi.effect.UndyingStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public class ModStatusEffects {
    public static final Holder<MobEffect> FLIGHT = register("flight", new FlightStatusEffect());
    public static final Holder<MobEffect> UNDYING = register("undying", new UndyingStatusEffect());

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, name), effect);
    }

    public static void initialize() {}
}
