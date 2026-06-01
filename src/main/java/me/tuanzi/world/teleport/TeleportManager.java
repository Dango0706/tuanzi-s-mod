package me.tuanzi.world.teleport;

import me.tuanzi.init.ModItems;
import me.tuanzi.item.TravelersNotebookItem;
import me.tuanzi.item.SignpostRuneItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private static final Map<UUID, TeleportTask> activeTasks = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public static void initialize() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, TeleportTask>> iterator = activeTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, TeleportTask> entry = iterator.next();
                TeleportTask task = entry.getValue();
                ServerPlayer player = task.player;

                // 玩家离线或离开服务器，则移除
                if (player == null || player.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                // 移动检测：若距离初始位置超过 0.1 格，则打断传送
                if (player.position().distanceToSqr(task.startVec) > 0.01) {
                    player.sendSystemMessage(Component.translatable("message.tuanzis_mod.travelers_notebook.interrupted"));
                    me.tuanzi.util.ModLog.debug(player, null, "旅者手札传送打断：玩家移动距离过大。");
                    iterator.remove();
                    continue;
                }

                task.remainingTicks--;

                if (task.remainingTicks <= 0) {
                    // 执行传送！
                    performTeleport(task);
                    iterator.remove();
                }
            }
        });
    }

    public static void handleTeleportRequest(ServerPlayer player, int slotIdx, boolean isMainHand) {
        ItemStack notebook = player.getItemInHand(isMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!notebook.is(ModItems.TRAVELERS_NOTEBOOK)) {
            return;
        }

        // 冷却检查
        long currentTick = player.level().getGameTime();
        long cooldownEnd = cooldowns.getOrDefault(player.getUUID(), 0L);
        if (currentTick < cooldownEnd) {
            double remainingSecs = (cooldownEnd - currentTick) / 20.0;
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.travelers_notebook.cooldown", String.format("%.1f", remainingSecs)));
            return;
        }

        // 获取对应的道标符石数据
        CustomData customData = notebook.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        
        CompoundTag notebookTag = customData.copyTag();
        ListTag listTag = notebookTag.getListOrEmpty("Items");
        ItemStack runeStack = ItemStack.EMPTY;
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompoundOrEmpty(i);
            int slot = itemTag.getByteOr("Slot", (byte) 0) & 255;
            if (slot == slotIdx) {
                runeStack = me.tuanzi.util.ItemStackNbtHelper.read(player.registryAccess(), itemTag.getCompoundOrEmpty("Item"));
                break;
            }
        }

        if (runeStack.isEmpty() || !runeStack.is(ModItems.SIGNPOST_RUNE) || !SignpostRuneItem.isRuneBound(runeStack)) {
            return;
        }

        // 判断所需能量
        String targetDimStr = SignpostRuneItem.getRuneDimension(runeStack);
        String currentDimStr = player.level().dimension().identifier().toString();
        boolean crossDim = !targetDimStr.equals(currentDimStr);
        int neededEnergy = crossDim ? 2 : 1;

        int currentEnergy = TravelersNotebookItem.getEnergy(notebook);
        if (currentEnergy < neededEnergy) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.travelers_notebook.insufficient_energy"));
            return;
        }

        // 构造传送任务
        double tx = SignpostRuneItem.getRuneX(runeStack);
        double ty = SignpostRuneItem.getRuneY(runeStack);
        double tz = SignpostRuneItem.getRuneZ(runeStack);
        float yaw = SignpostRuneItem.getRuneYaw(runeStack);
        float pitch = SignpostRuneItem.getRunePitch(runeStack);
        
        ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(targetDimStr));

        TeleportTask task = new TeleportTask(
                player,
                player.position(),
                targetDimKey,
                tx, ty, tz, yaw, pitch,
                60, // 3秒引导 (60 ticks)
                neededEnergy,
                isMainHand
        );

        activeTasks.put(player.getUUID(), task);
        
        // 提示传送引导开始
        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.travelers_notebook.starting"));
        me.tuanzi.util.ModLog.debug(player, null, "旅者手札传送引导开始！目标维度: " + targetDimStr + "，目标坐标: (" + tx + ", " + ty + ", " + tz + ")，消耗能量: " + neededEnergy);
    }

    public static void onPlayerDamage(ServerPlayer player) {
        if (activeTasks.containsKey(player.getUUID())) {
            activeTasks.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.travelers_notebook.interrupted"));
            me.tuanzi.util.ModLog.debug(player, null, "旅者手札传送打断：玩家受到伤害。");
        }
    }

    private static void performTeleport(TeleportTask task) {
        ServerPlayer player = task.player;
        ServerLevel targetLevel = player.level().getServer().getLevel(task.targetDim);

        if (targetLevel != null) {
            // 扣除能量
            ItemStack notebook = player.getItemInHand(task.isMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            if (notebook.is(ModItems.TRAVELERS_NOTEBOOK)) {
                int curEnergy = TravelersNotebookItem.getEnergy(notebook);
                TravelersNotebookItem.setEnergy(notebook, curEnergy - task.energyCost);
            }

            // 播放起点的传送音效与粒子
            ServerLevel originLevel = (ServerLevel) player.level();
            me.tuanzi.util.ModLog.debug(player, null, "旅者手札传送引导成功，执行传送！从 " + originLevel.dimension().identifier() + " (" + player.getX() + ", " + player.getY() + ", " + player.getZ() + ") 传送至 " + task.targetDim.identifier() + " (" + task.tx + ", " + task.ty + ", " + task.tz + ")，扣除能量: " + task.energyCost);
            originLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            originLevel.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.4, 0.6, 0.4, 0.1);

            // 跨维度/同维度传送
            net.minecraft.world.level.portal.TeleportTransition transition = new net.minecraft.world.level.portal.TeleportTransition(
                    targetLevel,
                    new Vec3(task.tx, task.ty, task.tz),
                    Vec3.ZERO,
                    task.yaw,
                    task.pitch,
                    net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING
            );
            player.teleport(transition);

            // 播放终点的传送音效与粒子
            targetLevel.playSound(null, task.tx, task.ty, task.tz,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            targetLevel.sendParticles(ParticleTypes.PORTAL,
                    task.tx, task.ty + 1.0, task.tz,
                    40, 0.4, 0.6, 0.4, 0.1);

            // 设置传送冷却：同维度 3秒 (60 ticks)，跨维度 5秒 (100 ticks)
            long currentTick = player.level().getGameTime();
            long cooldownTicks = (originLevel.dimension() == task.targetDim) ? 60 : 100;
            cooldowns.put(player.getUUID(), currentTick + cooldownTicks);
        }
    }

    private static class TeleportTask {
        final ServerPlayer player;
        final Vec3 startVec;
        final ResourceKey<Level> targetDim;
        final double tx, ty, tz;
        final float yaw, pitch;
        int remainingTicks;
        final int energyCost;
        final boolean isMainHand;

        TeleportTask(ServerPlayer player, Vec3 startVec, ResourceKey<Level> targetDim,
                     double tx, double ty, double tz, float yaw, float pitch,
                     int remainingTicks, int energyCost, boolean isMainHand) {
            this.player = player;
            this.startVec = startVec;
            this.targetDim = targetDim;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.yaw = yaw;
            this.pitch = pitch;
            this.remainingTicks = remainingTicks;
            this.energyCost = energyCost;
            this.isMainHand = isMainHand;
        }
    }
}
