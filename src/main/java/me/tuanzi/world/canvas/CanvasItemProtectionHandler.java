package me.tuanzi.world.canvas;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 画布掉落物保护与虚空救援处理器：
 * 1. 画布掉落物免疫除 {@code GENERIC_KILL} 外的一切伤害（火焰、岩浆、仙人掌、爆炸、闪电等）。
 * 2. 掉入虚空时按“在线常规维度→在线画布空间回退点→离线待返还队列→无主回退丢出者”策略救援。
 */
public final class CanvasItemProtectionHandler {
    private CanvasItemProtectionHandler() {
    }

    public static boolean isProtectedCanvas(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModItems.CANVAS);
    }

    public static boolean shouldIgnoreDamage(@Nullable ItemStack stack, DamageSource source) {
        return isProtectedCanvas(stack) && !source.is(DamageTypes.GENERIC_KILL);
    }

    /**
     * 处理画布掉落物掉入虚空事件。
     *
     * @return {@code true} 表示已接管处理（调用方应取消原版 discard），{@code false} 表示无主且无丢出者，交由原版销毁
     */
    public static boolean handleVoidRescue(ItemEntity itemEntity) {
        if (itemEntity.level().isClientSide() || !(itemEntity.level() instanceof ServerLevel currentLevel)) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        if (!isProtectedCanvas(stack)) {
            return false;
        }

        UUID targetPlayerUuid = resolveRescueTargetUuid(itemEntity, stack);
        if (targetPlayerUuid == null) {
            ModLog.debug("【画布虚空救援】检测到无主且无丢出者的空白画布掉入虚空，按原版逻辑处理。");
            return false;
        }

        MinecraftServer server = currentLevel.getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(targetPlayerUuid);
        if (onlinePlayer == null) {
            CanvasPendingReturnSavedData.enqueue(currentLevel, targetPlayerUuid, stack);
            itemEntity.discard();
            return true;
        }

        ServerLevel targetLevel;
        Vec3 targetPos;
        String messageKey;
        if (!ModDimensions.isCanvasSpaceLevel(onlinePlayer.level())) {
            targetLevel = (ServerLevel) onlinePlayer.level();
            targetPos = onlinePlayer.position().add(0.0, 0.25, 0.0);
            messageKey = "message.tuanzis_mod.canvas.void_returned";
        } else {
            CompoundTag sessionTag = ((CanvasPlayerTracker) onlinePlayer).tuanzis_mod$getCanvasSessionData();
            String dimStr = sessionTag.getStringOr("ReturnDimension", "minecraft:overworld");
            ResourceKey<Level> returnDim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            ServerLevel resolved = server.getLevel(returnDim);
            targetLevel = resolved != null ? resolved : server.overworld();
            BlockPos fallbackSpawn = targetLevel.getRespawnData().pos();
            double rx = sessionTag.getDoubleOr("ReturnX", fallbackSpawn.getX() + 0.5);
            double ry = sessionTag.getDoubleOr("ReturnY", fallbackSpawn.getY() + 0.5);
            double rz = sessionTag.getDoubleOr("ReturnZ", fallbackSpawn.getZ() + 0.5);
            targetPos = new Vec3(rx, ry, rz);
            messageKey = "message.tuanzis_mod.canvas.void_returned_to_origin";
        }

        relocateItemSafely(itemEntity, currentLevel, targetLevel, targetPos);
        onlinePlayer.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.AQUA));
        ModLog.debug(onlinePlayer, "【画布虚空救援】画布已从虚空传送回目标位置: targetDim="
                + targetLevel.dimension().identifier() + ", pos=" + targetPos);
        return true;
    }

    private static @Nullable UUID resolveRescueTargetUuid(ItemEntity itemEntity, ItemStack stack) {
        CanvasMetadata meta = CanvasMetadata.read(stack);
        if (meta.ownerUuid() != null) {
            return meta.ownerUuid();
        }
        Entity thrower = itemEntity.getOwner();
        if (thrower instanceof Player p) {
            return p.getUUID();
        }
        return null;
    }

    private static void relocateItemSafely(
            ItemEntity itemEntity,
            ServerLevel currentLevel,
            ServerLevel targetLevel,
            Vec3 targetPos
    ) {
        itemEntity.clearFire();
        itemEntity.resetFallDistance();
        itemEntity.setDeltaMovement(Vec3.ZERO);
        itemEntity.setNoPickUpDelay();

        if (currentLevel == targetLevel) {
            itemEntity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            return;
        }

        Entity moved = itemEntity.teleport(new TeleportTransition(
                targetLevel,
                targetPos,
                Vec3.ZERO,
                0.0f,
                0.0f,
                TeleportTransition.DO_NOTHING
        ));
        if (moved instanceof ItemEntity movedItem) {
            movedItem.clearFire();
            movedItem.setDeltaMovement(Vec3.ZERO);
            movedItem.setNoPickUpDelay();
        } else {
            // 跨维度传送返回 null 时在目标维度兜底生成并销毁原实体
            ItemEntity spawned = new ItemEntity(targetLevel, targetPos.x, targetPos.y, targetPos.z, itemEntity.getItem().copy());
            spawned.setDeltaMovement(Vec3.ZERO);
            spawned.setNoPickUpDelay();
            targetLevel.addFreshEntity(spawned);
            itemEntity.discard();
        }
    }
}
