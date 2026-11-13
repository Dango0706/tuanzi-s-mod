package me.tuanzi.world.scavenger;

import me.tuanzi.world.inventory.ScavengerEnderChestContainer;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public interface ScavengerPlayerTracker {
    ScavengerEnderChestContainer tuanzis_mod$getScavengerEnderChest();

    CorridorAccessoryContainer tuanzis_mod$getCorridorAccessories();

    PlayerCorridorProfile tuanzis_mod$getCorridorProfile();

    boolean tuanzis_mod$isCorridorOverloaded();

    void tuanzis_mod$setCorridorOverloaded(boolean overloaded);

    CompoundTag tuanzis_mod$getOtherDimensionSnapshot();

    void tuanzis_mod$setOtherDimensionSnapshot(CompoundTag tag);

    CompoundTag tuanzis_mod$getCorridorPersistentData();

    void tuanzis_mod$setCorridorPersistentData(CompoundTag tag);

    void tuanzis_mod$copyScavengerDataFrom(ServerPlayer oldPlayer);
}
