package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public class ModPotions {
    public static final Holder<Potion> FLIGHT_POTION = register("flight", new Potion("flight", new MobEffectInstance(ModStatusEffects.FLIGHT, 3600))); // 3 min
    public static final Holder<Potion> LONG_FLIGHT_POTION = register("long_flight", new Potion("flight", new MobEffectInstance(ModStatusEffects.FLIGHT, 9600))); // 8 min

    public static final Holder<Potion> UNDYING_POTION = register("undying", new Potion("undying", new MobEffectInstance(ModStatusEffects.UNDYING, 3600))); // 3 min
    public static final Holder<Potion> LONG_UNDYING_POTION = register("long_undying", new Potion("undying", new MobEffectInstance(ModStatusEffects.UNDYING, 9600))); // 8 min

    public static final Holder<Potion> ADRENALINE_POTION = register("adrenaline", new Potion("adrenaline", new MobEffectInstance(ModStatusEffects.ADRENALINE, 900)));
    public static final Holder<Potion> ADRENALINE_POTION_II = register("adrenaline_ii", new Potion("adrenaline", new MobEffectInstance(ModStatusEffects.ADRENALINE, 600, 1)));
    public static final Holder<Potion> LONG_ADRENALINE_POTION = register("long_adrenaline", new Potion("adrenaline", new MobEffectInstance(ModStatusEffects.ADRENALINE, 1800)));

    private static Holder<Potion> register(String name, Potion potion) {
        return Registry.registerForHolder(BuiltInRegistries.POTION, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, name), potion);
    }

    public static void initialize() {}
}
