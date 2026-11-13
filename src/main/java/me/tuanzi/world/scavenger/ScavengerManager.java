package me.tuanzi.world.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.mixin.FoodDataAccessor;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public class ScavengerManager {

    private static final ThreadLocal<Boolean> RESTORING_SNAPSHOT = ThreadLocal.withInitial(() -> false);

    public static boolean isRestoringSnapshot() {
        return Boolean.TRUE.equals(RESTORING_SNAPSHOT.get());
    }

    /**
     * 跨维度进入拾荒回廊事件处理（支持任意途径触发）
     */
    public static void onCrossDimensionEnter(ServerPlayer player, ServerLevel oldLevel, ServerLevel newLevel) {
        // 1. 确保安全基台与专属出生点
        ModDimensions.ensureBasePlatform(newLevel);
        CorridorSpawnManager.get(newLevel).getOrCreateSpawnPos(player, newLevel);

        // 2. 保存原维度快照（若已存在快照则不覆盖，防止多重传送破坏原始返回点）
        ScavengerPlayerTracker tracker = (ScavengerPlayerTracker) player;
        CompoundTag existingSnapshot = tracker.tuanzis_mod$getOtherDimensionSnapshot();
        if (existingSnapshot == null || existingSnapshot.isEmpty()) {
            CompoundTag snapshotTag = capturePlayerState(player, true);
            tracker.tuanzis_mod$setOtherDimensionSnapshot(snapshotTag);
            ModLog.debug(player, null, "【拾荒回廊】跨维度检测进入：成功捕获原世界背包与状态快照（返回点: " +
                    player.getX() + ", " + player.getY() + ", " + player.getZ() + "）。");
        } else {
            ModLog.debug(player, null, "【拾荒回廊】跨维度检测进入：检测到已有原世界快照，保留原始返回点。");
        }

        // 3. 载入回廊持久化数据（若首次进入则赋予纯净空背包初始状态）
        CompoundTag corridorData = tracker.tuanzis_mod$getCorridorPersistentData();
        if (corridorData != null && !corridorData.isEmpty()) {
            restorePlayerState(player, corridorData, false);
            ModLog.debug(player, null, "【拾荒回廊】跨维度检测进入：载入玩家已有的回廊持久化背包与状态数据。");
        } else {
            initFreshCorridorState(player);
            ModLog.debug(player, null, "【拾荒回廊】跨维度检测进入：首次进入回廊，赋予纯净初始状态与空背包。");
        }

        // 4. 确保独立饰品槽与战术装备档案（含初始 1x2 安全箱发放）就绪并同步
        tracker.tuanzis_mod$getCorridorAccessories().ensureDefaultBeacons();
        tracker.tuanzis_mod$getCorridorProfile().tryGrantStarterSafeboxIfEligible(player.registryAccess());
        tracker.tuanzis_mod$getCorridorProfile().syncEquipmentAndStashDimensionsWithConfig();
        syncCorridorAccessoriesToClient(player);
        me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);

        // 5. 强制设为冒险模式
        player.setGameMode(GameType.ADVENTURE);

        player.sendSystemMessage(Component.translatable("command.tuanzis_mod.corridor.enter_success"));
    }

    /**
     * 跨维度离开拾荒回廊事件处理（支持任意途径触发）
     */
    public static void onCrossDimensionLeave(ServerPlayer player, ServerLevel oldLevel, ServerLevel newLevel) {
        // 0. 队员离开拾荒回廊，触发小队生命周期处理
        ScavengerTeamManager.onPlayerLeaveCorridorOrDisconnect(player);

        ScavengerPlayerTracker tracker = (ScavengerPlayerTracker) player;

        // 1. 保存当前在回廊搜刮到的物资与状态至持久化存储（包含独立饰品槽）
        CompoundTag corridorTag = capturePlayerState(player, false);
        corridorTag.put("CorridorAccessories", tracker.tuanzis_mod$getCorridorAccessories().toTag(player.registryAccess()));
        tracker.tuanzis_mod$setCorridorPersistentData(corridorTag);

        // 回廊独占：清空身上的独立饰品槽并通知客户端隐藏，同时移除回廊负重移速惩罚修饰器
        tracker.tuanzis_mod$getCorridorAccessories().clearContent();
        tracker.tuanzis_mod$setCorridorOverloaded(false);
        me.tuanzi.world.scavenger.profile.CorridorWeightController.clearWeightModifiers(player);
        syncCorridorAccessoriesToClient(player);
        me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);

        ModLog.debug(player, null, "【拾荒回廊】跨维度检测离开：已暂存回廊搜刮物资、状态与专属饰品至独立持久化存储。");

        // 2. 获取并还原原世界状态快照
        CompoundTag snapshotTag = tracker.tuanzis_mod$getOtherDimensionSnapshot();
        if (snapshotTag != null && !snapshotTag.isEmpty()) {
            restorePlayerState(player, snapshotTag, true);

            // 还原原游戏模式
            String modeName = snapshotTag.getStringOr("GameMode", "SURVIVAL");
            GameType originalMode = GameType.byName(modeName, GameType.SURVIVAL);
            player.setGameMode(originalMode);

            // 清空已消耗的临时快照
            tracker.tuanzis_mod$setOtherDimensionSnapshot(new CompoundTag());
            ModLog.debug(player, null, "【拾荒回廊】跨维度检测离开：原世界物品与状态已完整还原，游戏模式已还原为 " + originalMode.getName() + "。");
        } else {
            // 异常兜底：若快照丢失，清空背包防止将回廊物资带出，设为生存模式
            player.getInventory().clearContent();
            player.setHealth(player.getMaxHealth());
            player.setGameMode(GameType.SURVIVAL);
            ModLog.debug(player, null, "【拾荒回廊】跨维度检测离开：未检测到原世界快照，已安全清空随身背包以防物资泄露。");
        }

        player.sendSystemMessage(Component.translatable("command.tuanzis_mod.corridor.leave_success"));
    }

    /**
     * 便捷指令进入拾荒回廊（传送到回廊安全基台）
     */
    public static boolean enterCorridor(ServerPlayer player) {
        if (player.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            player.sendSystemMessage(Component.translatable("command.tuanzis_mod.corridor.already_in"));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        ServerLevel corridorLevel = server.getLevel(ModDimensions.SCAVENGER_CORRIDOR_LEVEL);
        if (corridorLevel == null) {
            player.sendSystemMessage(Component.literal("§c无法定位拾荒回廊维度！"));
            return false;
        }

        ModDimensions.ensureBasePlatform(corridorLevel);
        BlockPos spawnPos = CorridorSpawnManager.get(corridorLevel).getOrCreateSpawnPos(player, corridorLevel);

        TeleportTransition transition = new TeleportTransition(
                corridorLevel,
                new Vec3(spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5),
                Vec3.ZERO,
                0.0f,
                0.0f,
                TeleportTransition.DO_NOTHING
        );
        player.teleport(transition);
        return true;
    }

    /**
     * 便捷指令离开拾荒回廊（精准返回进入前的原路坐标）
     */
    public static boolean leaveCorridor(ServerPlayer player) {
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            player.sendSystemMessage(Component.translatable("command.tuanzis_mod.corridor.not_in"));
            return false;
        }

        CompoundTag snapshotTag = ((ScavengerPlayerTracker) player).tuanzis_mod$getOtherDimensionSnapshot();
        MinecraftServer server = player.level().getServer();
        ServerLevel returnLevel;
        Vec3 returnPos;
        float rYaw = 0.0f;
        float rPitch = 0.0f;

        if (snapshotTag == null || snapshotTag.isEmpty()) {
            returnLevel = server.overworld();
            BlockPos spawn = returnLevel.getRespawnData().pos();
            returnPos = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        } else {
            String dimStr = snapshotTag.getStringOr("ReturnDimension", "minecraft:overworld");
            ResourceKey<Level> returnDim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            returnLevel = server.getLevel(returnDim);
            if (returnLevel == null) returnLevel = server.overworld();

            double rx = snapshotTag.getDoubleOr("ReturnX", 0.5);
            double ry = snapshotTag.getDoubleOr("ReturnY", 64.0);
            double rz = snapshotTag.getDoubleOr("ReturnZ", 0.5);
            rYaw = snapshotTag.getFloatOr("ReturnYaw", 0.0f);
            rPitch = snapshotTag.getFloatOr("ReturnPitch", 0.0f);
            returnPos = new Vec3(rx, ry, rz);
        }

        TeleportTransition returnTransition = new TeleportTransition(
                returnLevel,
                returnPos,
                Vec3.ZERO,
                rYaw,
                rPitch,
                TeleportTransition.DO_NOTHING
        );
        player.teleport(returnTransition);
        return true;
    }

    /**
     * 捕获玩家完整状态
     */
    private static CompoundTag capturePlayerState(ServerPlayer player, boolean isSnapshotForReturn) {
        CompoundTag tag = new CompoundTag();
        var registries = player.registryAccess();

        // 背包（覆盖全部 0 ~ 42 格，包含主物品栏、快捷栏、盔甲四件套与副手）
        ListTag invList = new ListTag();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.put("Item", ItemStackNbtHelper.write(registries, stack));
                invList.add(itemTag);
            }
        }
        tag.put("Inventory", invList);
        tag.putInt("SelectedItemSlot", inv.getSelectedSlot());

        // 经验与数值属性
        tag.putInt("XpLevel", player.experienceLevel);
        tag.putFloat("XpP", player.experienceProgress);
        tag.putInt("XpTotal", player.totalExperience);
        tag.putFloat("Health", player.getHealth());
        tag.putFloat("Absorption", player.getAbsorptionAmount());
        tag.putInt("FoodLevel", player.getFoodData().getFoodLevel());
        tag.putFloat("Saturation", player.getFoodData().getSaturationLevel());
        tag.putFloat("Exhaustion", ((FoodDataAccessor) player.getFoodData()).getExhaustionLevel());

        // 状态效果
        ListTag effectsList = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            MobEffectInstance.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), effect)
                    .result()
                    .ifPresent(effectsList::add);
        }
        tag.put("ActiveEffects", effectsList);

        if (isSnapshotForReturn) {
            tag.putString("GameMode", player.gameMode.getGameModeForPlayer().name());
            tag.putString("ReturnDimension", player.level().dimension().identifier().toString());
            tag.putDouble("ReturnX", player.getX());
            tag.putDouble("ReturnY", player.getY());
            tag.putDouble("ReturnZ", player.getZ());
            tag.putFloat("ReturnYaw", player.getYRot());
            tag.putFloat("ReturnPitch", player.getXRot());
        }

        return tag;
    }

    /**
     * 恢复玩家完整状态
     */
    private static void restorePlayerState(ServerPlayer player, CompoundTag tag, boolean isRestoringOtherDimension) {
        RESTORING_SNAPSHOT.set(true);
        try {
            restorePlayerStateInternal(player, tag, isRestoringOtherDimension);
        } finally {
            RESTORING_SNAPSHOT.set(false);
        }
    }

    private static void restorePlayerStateInternal(ServerPlayer player, CompoundTag tag, boolean isRestoringOtherDimension) {
        var registries = player.registryAccess();
        Inventory inv = player.getInventory();
        inv.clearContent();

        // 恢复背包
        ListTag invList = tag.getListOrEmpty("Inventory");
        for (int i = 0; i < invList.size(); i++) {
            CompoundTag itemTag = invList.getCompoundOrEmpty(i);
            int slot = itemTag.getIntOr("Slot", -1);
            if (slot >= 0 && slot < inv.getContainerSize()) {
                ItemStack stack = ItemStackNbtHelper.read(registries, itemTag.getCompoundOrEmpty("Item"));
                inv.setItem(slot, stack);
            }
        }
        inv.setSelectedSlot(tag.getIntOr("SelectedItemSlot", 0));

        // 恢复经验
        player.experienceLevel = tag.getIntOr("XpLevel", 0);
        player.experienceProgress = tag.getFloatOr("XpP", 0.0f);
        player.totalExperience = tag.getIntOr("XpTotal", 0);
        player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));

        // 恢复生命与饱腹度
        player.setHealth(Math.max(1.0f, tag.getFloatOr("Health", player.getMaxHealth())));
        player.setAbsorptionAmount(tag.getFloatOr("Absorption", 0.0f));
        player.getFoodData().setFoodLevel(tag.getIntOr("FoodLevel", 20));
        player.getFoodData().setSaturation(tag.getFloatOr("Saturation", 5.0f));
        ((FoodDataAccessor) player.getFoodData()).setExhaustionLevel(tag.getFloatOr("Exhaustion", 0.0f));

        // 恢复状态效果
        player.removeAllEffects();
        ListTag effectsList = tag.getListOrEmpty("ActiveEffects");
        for (int i = 0; i < effectsList.size(); i++) {
            MobEffectInstance.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), effectsList.get(i))
                    .result()
                    .ifPresent(player::addEffect);
        }

        // 恢复回廊专属饰品槽
        if (!isRestoringOtherDimension && tag.contains("CorridorAccessories")) {
            ((ScavengerPlayerTracker) player).tuanzis_mod$getCorridorAccessories().fromTag(registries, tag.getCompoundOrEmpty("CorridorAccessories"));
        }

        // 刷新客户端物品栏容器
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    /**
     * 向客户端同步回廊独立饰品槽
     */
    public static void syncCorridorAccessoriesToClient(ServerPlayer player) {
        if (player instanceof ScavengerPlayerTracker tracker) {
            var acc = tracker.tuanzis_mod$getCorridorAccessories();
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new me.tuanzi.network.CorridorAccessorySyncPacket(
                    acc.getItem(CorridorAccessoryContainer.SLOT_HALL),
                    acc.getItem(CorridorAccessoryContainer.SLOT_ROOM),
                    acc.getItem(CorridorAccessoryContainer.SLOT_MARKET)
            ));
        }
    }

    /**
     * 处理客户端通过快捷键触发信标激活
     */
    public static void handleBeaconUsePacket(ServerPlayer player, int slot) {
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.beacon.wrong_dimension").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        if (!(player instanceof ScavengerPlayerTracker tracker)) {
            return;
        }

        var acc = tracker.tuanzis_mod$getCorridorAccessories();
        if (slot == CorridorAccessoryContainer.SLOT_HALL) {
            ItemStack stack = acc.getItem(CorridorAccessoryContainer.SLOT_HALL);
            if (stack.is(me.tuanzi.init.ModItems.HALL_RETURN_BEACON)) {
                me.tuanzi.item.scavenger.HallReturnBeaconItem.executeBeaconTeleport(player, stack);
                syncCorridorAccessoriesToClient(player);
                me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);
            }
        } else if (slot == CorridorAccessoryContainer.SLOT_ROOM) {
            ItemStack stack = acc.getItem(CorridorAccessoryContainer.SLOT_ROOM);
            if (stack.is(me.tuanzi.init.ModItems.ROOM_RETURN_BEACON)) {
                me.tuanzi.item.scavenger.RoomReturnBeaconItem.executeBeaconTeleport(player, stack);
                syncCorridorAccessoriesToClient(player);
                me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);
            }
        } else if (slot == CorridorAccessoryContainer.SLOT_MARKET) {
            ItemStack stack = acc.getItem(CorridorAccessoryContainer.SLOT_MARKET);
            if (stack.is(me.tuanzi.init.ModItems.MARKET_RETURN_BEACON)) {
                me.tuanzi.item.scavenger.MarketReturnBeaconItem.executeBeaconTeleport(player, stack);
                syncCorridorAccessoriesToClient(player);
                me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);
            }
        }
    }

    /**
     * 赋予纯净初入回廊状态
     */
    private static void initFreshCorridorState(ServerPlayer player) {
        player.getInventory().clearContent();
        player.experienceLevel = 0;
        player.experienceProgress = 0.0f;
        player.totalExperience = 0;
        player.connection.send(new ClientboundSetExperiencePacket(0.0f, 0, 0));
        player.setHealth(player.getMaxHealth());
        player.setAbsorptionAmount(0.0f);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        ((FoodDataAccessor) player.getFoodData()).setExhaustionLevel(0.0f);
        player.removeAllEffects();
        player.clearFire();

        // 刷新客户端物品栏容器
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }
}
