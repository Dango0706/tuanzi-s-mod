package me.tuanzi.mixin;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.inventory.ScavengerEnderChestContainer;
import me.tuanzi.world.scavenger.ScavengerManager;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.profile.CorridorInventoryRules;
import me.tuanzi.world.scavenger.profile.CorridorWeightController;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ScavengerPlayerMixin implements ScavengerPlayerTracker {

    @Unique
    private final ScavengerEnderChestContainer tuanzis_mod$scavengerEnderChest = new ScavengerEnderChestContainer();

    @Unique
    private final me.tuanzi.world.scavenger.CorridorAccessoryContainer tuanzis_mod$corridorAccessories = new me.tuanzi.world.scavenger.CorridorAccessoryContainer();

    @Unique
    private final PlayerCorridorProfile tuanzis_mod$corridorProfile = new PlayerCorridorProfile();

    @Unique
    private boolean tuanzis_mod$corridorOverloaded = false;

    @Unique
    private CompoundTag tuanzis_mod$otherDimensionSnapshot = new CompoundTag();

    @Unique
    private CompoundTag tuanzis_mod$corridorPersistentData = new CompoundTag();

    @Override
    public ScavengerEnderChestContainer tuanzis_mod$getScavengerEnderChest() {
        return this.tuanzis_mod$scavengerEnderChest;
    }

    @Override
    public me.tuanzi.world.scavenger.CorridorAccessoryContainer tuanzis_mod$getCorridorAccessories() {
        return this.tuanzis_mod$corridorAccessories;
    }

    @Override
    public PlayerCorridorProfile tuanzis_mod$getCorridorProfile() {
        return this.tuanzis_mod$corridorProfile;
    }

    @Override
    public boolean tuanzis_mod$isCorridorOverloaded() {
        return this.tuanzis_mod$corridorOverloaded;
    }

    @Override
    public void tuanzis_mod$setCorridorOverloaded(boolean overloaded) {
        this.tuanzis_mod$corridorOverloaded = overloaded;
    }

    @Override
    public CompoundTag tuanzis_mod$getOtherDimensionSnapshot() {
        return this.tuanzis_mod$otherDimensionSnapshot;
    }

    @Override
    public void tuanzis_mod$setOtherDimensionSnapshot(CompoundTag tag) {
        this.tuanzis_mod$otherDimensionSnapshot = tag != null ? tag : new CompoundTag();
    }

    @Override
    public CompoundTag tuanzis_mod$getCorridorPersistentData() {
        return this.tuanzis_mod$corridorPersistentData;
    }

    @Override
    public void tuanzis_mod$setCorridorPersistentData(CompoundTag tag) {
        this.tuanzis_mod$corridorPersistentData = tag != null ? tag : new CompoundTag();
    }

    @Override
    public void tuanzis_mod$copyScavengerDataFrom(ServerPlayer oldPlayer) {
        if (oldPlayer instanceof ScavengerPlayerTracker tracker) {
            this.tuanzis_mod$otherDimensionSnapshot = tracker.tuanzis_mod$getOtherDimensionSnapshot().copy();
            this.tuanzis_mod$corridorPersistentData = tracker.tuanzis_mod$getCorridorPersistentData().copy();

            ScavengerEnderChestContainer oldEnder = tracker.tuanzis_mod$getScavengerEnderChest();
            for (int i = 0; i < oldEnder.getContainerSize(); i++) {
                this.tuanzis_mod$scavengerEnderChest.setItem(i, oldEnder.getItem(i).copy());
            }

            me.tuanzi.world.scavenger.CorridorAccessoryContainer oldAcc = tracker.tuanzis_mod$getCorridorAccessories();
            for (int i = 0; i < oldAcc.getContainerSize(); i++) {
                this.tuanzis_mod$corridorAccessories.setItem(i, oldAcc.getItem(i).copy());
            }

            this.tuanzis_mod$corridorProfile.copyFrom(tracker.tuanzis_mod$getCorridorProfile(), oldPlayer.registryAccess());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickScavengerProfileAndWeight(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        CorridorWeightController.tickServerPlayer(player);
        if (player.tickCount % 20 == 0 && player.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            CorridorInventoryRules.sanitizeHiddenMainInventory(player, ignored -> {
                player.containerMenu.broadcastChanges();
                player.inventoryMenu.broadcastChanges();
            });
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$saveScavengerData(ValueOutput output, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        CompoundTag root = new CompoundTag();

        if (!this.tuanzis_mod$otherDimensionSnapshot.isEmpty()) {
            root.put("OtherDimensionSnapshot", this.tuanzis_mod$otherDimensionSnapshot);
        }
        if (!this.tuanzis_mod$corridorPersistentData.isEmpty()) {
            root.put("CorridorPersistentData", this.tuanzis_mod$corridorPersistentData);
        }

        // 保存专属 9 格末影箱
        ListTag enderList = new ListTag();
        for (int i = 0; i < this.tuanzis_mod$scavengerEnderChest.getContainerSize(); i++) {
            ItemStack stack = this.tuanzis_mod$scavengerEnderChest.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.put("Item", ItemStackNbtHelper.write(player.registryAccess(), stack));
                enderList.add(itemTag);
            }
        }
        root.put("ScavengerEnderItems", enderList);
        root.put("CorridorAccessories", this.tuanzis_mod$corridorAccessories.toTag(player.registryAccess()));
        root.put("CorridorProfile", this.tuanzis_mod$corridorProfile.saveToTag(player.registryAccess()));

        output.store("tuanzis_mod$scavenger_data", CompoundTag.CODEC, root);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$readScavengerData(ValueInput input, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        input.read("tuanzis_mod$scavenger_data", CompoundTag.CODEC).ifPresent(root -> {
            this.tuanzis_mod$otherDimensionSnapshot = root.getCompoundOrEmpty("OtherDimensionSnapshot");
            this.tuanzis_mod$corridorPersistentData = root.getCompoundOrEmpty("CorridorPersistentData");

            ListTag enderList = root.getListOrEmpty("ScavengerEnderItems");
            this.tuanzis_mod$scavengerEnderChest.clearContent();
            for (int i = 0; i < enderList.size(); i++) {
                CompoundTag itemTag = enderList.getCompoundOrEmpty(i);
                int slot = itemTag.getIntOr("Slot", -1);
                if (slot >= 0 && slot < this.tuanzis_mod$scavengerEnderChest.getContainerSize()) {
                    ItemStack stack = ItemStackNbtHelper.read(player.registryAccess(), itemTag.getCompoundOrEmpty("Item"));
                    this.tuanzis_mod$scavengerEnderChest.setItem(slot, stack);
                }
            }

            this.tuanzis_mod$corridorAccessories.fromTag(player.registryAccess(), root.getCompoundOrEmpty("CorridorAccessories"));
            if (root.contains("CorridorProfile")) {
                this.tuanzis_mod$corridorProfile.loadFromTag(player.registryAccess(), root.getCompoundOrEmpty("CorridorProfile"));
            }
        });
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void tuanzis_mod$restoreScavengerDataOnRespawn(ServerPlayer oldPlayer, boolean wonGame, CallbackInfo ci) {
        this.tuanzis_mod$copyScavengerDataFrom(oldPlayer);
        if (!wonGame && oldPlayer.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            player.getInventory().clearContent();
            player.experienceLevel = 0;
            player.experienceProgress = 0.0f;
            player.totalExperience = 0;
            this.tuanzis_mod$corridorOverloaded = false;
            this.tuanzis_mod$corridorProfile.markDirty();
            ModLog.debug(player, null, "【拾荒回廊】在回廊内死亡重生：已清空快捷栏与经验，保留安全箱、基地独立仓库及成长等级。");
        }
    }

    @Inject(method = "teleport", at = @At("HEAD"))
    private void tuanzis_mod$onCrossDimensionTeleportHead(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel newLevel = transition.newLevel();
        ServerLevel oldLevel = player.level();
        ResourceKey<Level> fromDim = oldLevel.dimension();
        ResourceKey<Level> toDim = newLevel.dimension();

        if (fromDim.equals(toDim)) {
            return;
        }

        // 1. 任意途径跨维度进入拾荒回廊
        if (!fromDim.equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL) && toDim.equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            ScavengerManager.onCrossDimensionEnter(player, oldLevel, newLevel);
        }
        // 2. 任意途径跨维度离开拾荒回廊
        else if (fromDim.equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL) && !toDim.equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            ScavengerManager.onCrossDimensionLeave(player, oldLevel, newLevel);
        }
    }

    @Inject(method = "teleport", at = @At("RETURN"))
    private void tuanzis_mod$onCrossDimensionTeleportReturn(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = cir.getReturnValue();
        if (player == null) return;

        // 若当前处于拾荒回廊，无论何种方式到达，均强制确保处于冒险模式
        if (player.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                player.setGameMode(GameType.ADVENTURE);
                ModLog.debug(player, null, "【拾荒回廊】传送完成校验：强制保持冒险模式。");
            }
        }
    }

    @Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$cancelTeamFriendlyFire(Player target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof ServerPlayer targetPlayer) {
            if (ScavengerTeamManager.areTeammates((ServerPlayer) (Object) this, targetPlayer)) {
                cir.setReturnValue(false);
            }
        }
    }
}
