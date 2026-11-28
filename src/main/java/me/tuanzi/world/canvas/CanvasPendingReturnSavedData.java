package me.tuanzi.world.canvas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.util.ModLog;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 离线玩家虚空救援画布待返还队列：
 * 当画布掉入虚空且主人离线时暂存于主世界存档，待主人下次上线自动发回背包。
 */
public class CanvasPendingReturnSavedData extends SavedData {
    public record PlayerPendingEntry(UUID playerUuid, List<ItemStack> stacks) {
        public static final Codec<PlayerPendingEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerPendingEntry::playerUuid),
                Codec.list(ItemStack.CODEC).fieldOf("stacks").forGetter(PlayerPendingEntry::stacks)
        ).apply(instance, PlayerPendingEntry::new));
    }

    public static final Codec<CanvasPendingReturnSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(PlayerPendingEntry.CODEC).optionalFieldOf("pending", List.of()).forGetter(CanvasPendingReturnSavedData::toEntries)
    ).apply(instance, CanvasPendingReturnSavedData::new));

    public static final SavedDataType<CanvasPendingReturnSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "canvas_pending_returns"),
            CanvasPendingReturnSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, List<ItemStack>> pendingByPlayer = new HashMap<>();

    public CanvasPendingReturnSavedData() {
    }

    public CanvasPendingReturnSavedData(List<PlayerPendingEntry> entries) {
        for (PlayerPendingEntry entry : entries) {
            if (entry.playerUuid() != null && entry.stacks() != null && !entry.stacks().isEmpty()) {
                List<ItemStack> copies = new ArrayList<>();
                for (ItemStack s : entry.stacks()) {
                    if (!s.isEmpty()) {
                        copies.add(s.copy());
                    }
                }
                if (!copies.isEmpty()) {
                    pendingByPlayer.put(entry.playerUuid(), copies);
                }
            }
        }
    }

    public static CanvasPendingReturnSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static void enqueue(ServerLevel anyLevel, UUID playerUuid, ItemStack stack) {
        if (anyLevel == null || playerUuid == null || stack.isEmpty()) {
            return;
        }
        CanvasPendingReturnSavedData data = get(anyLevel.getServer());
        data.addPending(playerUuid, stack.copy());
        ModLog.debug("【画布虚空救援】主人离线，已将画布加入待返还队列: owner=" + playerUuid);
    }

    public static void deliverPendingFor(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return;
        }
        CanvasPendingReturnSavedData data = get(player.level().getServer());
        List<ItemStack> items = data.takePending(player.getUUID());
        if (items.isEmpty()) {
            return;
        }
        int delivered = 0;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack toGive = stack.copy();
            if (!player.getInventory().add(toGive)) {
                ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), toGive);
                drop.setNoPickUpDelay();
                drop.setDeltaMovement(0.0, 0.0, 0.0);
                player.level().addFreshEntity(drop);
            }
            delivered++;
        }
        if (delivered > 0) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.void_returned_offline", delivered)
                    .withStyle(ChatFormatting.AQUA));
            ModLog.debug(player, "【画布虚空救援】玩家上线自动返还离线期间落入虚空的画布: count=" + delivered);
        }
    }

    private synchronized void addPending(UUID playerUuid, ItemStack stack) {
        pendingByPlayer.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(stack);
        setDirty();
    }

    private synchronized List<ItemStack> takePending(UUID playerUuid) {
        List<ItemStack> list = pendingByPlayer.remove(playerUuid);
        if (list != null && !list.isEmpty()) {
            setDirty();
            return list;
        }
        return List.of();
    }

    private synchronized List<PlayerPendingEntry> toEntries() {
        List<PlayerPendingEntry> list = new ArrayList<>(pendingByPlayer.size());
        for (var e : pendingByPlayer.entrySet()) {
            if (!e.getValue().isEmpty()) {
                list.add(new PlayerPendingEntry(e.getKey(), List.copyOf(e.getValue())));
            }
        }
        return list;
    }
}
