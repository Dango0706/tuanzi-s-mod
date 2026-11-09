package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * 模组自定义声音事件注册表
 */
public class ModSounds {
    public static final Identifier CINDER_SOVEREIGN_THEME_ID = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "music.cinder_sovereign_theme");
    public static final SoundEvent CINDER_SOVEREIGN_THEME = register("music.cinder_sovereign_theme");

    public static final SoundEvent CINDER_SOVEREIGN_SPAWN = register("entity.cinder_sovereign.spawn");
    public static final SoundEvent CINDER_SOVEREIGN_PULSE = register("entity.cinder_sovereign.pulse");
    public static final SoundEvent CINDER_SOVEREIGN_SPOT_TARGET = register("entity.cinder_sovereign.spot_target");
    public static final SoundEvent CINDER_SOVEREIGN_HURT_SHIELD = register("entity.cinder_sovereign.hurt_shield");
    public static final SoundEvent CINDER_SOVEREIGN_HURT_CORE = register("entity.cinder_sovereign.hurt_core");
    public static final SoundEvent CINDER_SOVEREIGN_DEFLECT = register("entity.cinder_sovereign.deflect");
    public static final SoundEvent CINDER_SOVEREIGN_DASH = register("entity.cinder_sovereign.dash");
    public static final SoundEvent CINDER_SOVEREIGN_ALTITUDE_CHANGE = register("entity.cinder_sovereign.altitude_change");
    public static final SoundEvent CINDER_SOVEREIGN_PHASE2_OVERLOAD = register("entity.cinder_sovereign.phase2_overload");
    public static final SoundEvent CINDER_SOVEREIGN_DEATH = register("entity.cinder_sovereign.death");

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {
        // 触发静态成员初始化与声音注册
    }
}
