package me.tuanzi.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 暴露 {@link MinecraftServer} 内部维度表与世界构造上下文，用于按需动态创建和卸载画布独立虚空维度。
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {

    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> tuanzis_mod$getLevels();

    @Accessor("executor")
    Executor tuanzis_mod$getExecutor();

    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess tuanzis_mod$getStorageSource();

    @Accessor("worldData")
    WorldData tuanzis_mod$getWorldData();

    @Accessor("worldGenSettings")
    WorldGenSettings tuanzis_mod$getWorldGenSettings();
}
