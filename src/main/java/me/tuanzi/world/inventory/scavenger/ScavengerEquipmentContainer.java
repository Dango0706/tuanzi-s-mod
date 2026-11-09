package me.tuanzi.world.inventory.scavenger;

import me.tuanzi.network.scavenger.ScavengerServerPacketHandler;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 战术角色装备槽专属容器（持有战术背囊槽与黑匣安全箱槽）。
 * <p>
 * 槽位规范：
 * <ul>
 *   <li>0: 战术背囊槽位 (BACKPACK)</li>
 *   <li>1: 黑匣安全箱槽位 (SAFEBOX)</li>
 * </ul>
 * 严格执行非空不可卸下规则，并在装备发生变动时权威持久化并网络同步。
 */
public class ScavengerEquipmentContainer extends SimpleContainer {

    private final Player player;

    public ScavengerEquipmentContainer(Player player) {
        super(2);
        this.player = player;
        refreshFromSource();
    }

    public void refreshFromSource() {
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer && serverPlayer instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
            this.items.set(0, profile.equippedBackpack().copy());
            this.items.set(1, profile.equippedSafebox().copy());
        } else if (this.player.level().isClientSide()) {
            this.items.set(0, me.tuanzi.client.scavenger.ScavengerClientState.getEquippedBackpack().copy());
            this.items.set(1, me.tuanzi.client.scavenger.ScavengerClientState.getEquippedSafebox().copy());
        }
    }

    @Override
    public ItemStack getItem(int slot) {
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer && serverPlayer instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
            if (slot == 0) return profile.equippedBackpack();
            if (slot == 1) return profile.equippedSafebox();
        }
        return super.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer && serverPlayer instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
            if (slot == 0) {
                if (stack.isEmpty()) {
                    if (profile.hasBackpack() && profile.backpackGrid().isEmpty()) {
                        profile.unequipBackpack(serverPlayer.registryAccess());
                    }
                } else {
                    if (profile.hasBackpack() && profile.backpackGrid().isEmpty()) {
                        profile.unequipBackpack(serverPlayer.registryAccess());
                    }
                    if (!profile.hasBackpack()) {
                        profile.equipBackpack(stack, serverPlayer.registryAccess());
                    }
                }
            } else if (slot == 1) {
                if (stack.isEmpty()) {
                    if (profile.hasSafebox() && profile.safeboxGrid().isEmpty()) {
                        profile.unequipSafebox(serverPlayer.registryAccess());
                    }
                } else {
                    if (profile.hasSafebox() && profile.safeboxGrid().isEmpty()) {
                        profile.unequipSafebox(serverPlayer.registryAccess());
                    }
                    if (!profile.hasSafebox()) {
                        profile.equipSafebox(stack, serverPlayer.registryAccess());
                    }
                }
            }
            profile.markDirty();
            ScavengerServerPacketHandler.syncProfile(serverPlayer);
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer && serverPlayer instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
            if (slot == 0) {
                if (profile.hasBackpack() && profile.backpackGrid().isEmpty()) {
                    Optional<ItemStack> unequipped = profile.unequipBackpack(serverPlayer.registryAccess());
                    profile.markDirty();
                    ScavengerServerPacketHandler.syncProfile(serverPlayer);
                    super.setItem(0, ItemStack.EMPTY);
                    return unequipped.orElse(ItemStack.EMPTY);
                }
                return ItemStack.EMPTY;
            } else if (slot == 1) {
                if (profile.hasSafebox() && profile.safeboxGrid().isEmpty()) {
                    Optional<ItemStack> unequipped = profile.unequipSafebox(serverPlayer.registryAccess());
                    profile.markDirty();
                    ScavengerServerPacketHandler.syncProfile(serverPlayer);
                    super.setItem(1, ItemStack.EMPTY);
                    return unequipped.orElse(ItemStack.EMPTY);
                }
                return ItemStack.EMPTY;
            }
        }
        return super.removeItem(slot, amount);
    }
}
