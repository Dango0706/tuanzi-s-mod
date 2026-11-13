package me.tuanzi.world.scavenger.loot.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.network.scavenger.ScavengerDataSyncS2CPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.meta.ScavengerContainerDef;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.io.Reader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 拾荒回廊数据驱动中心注册表（支持 /reload 实时热加载、S2C 同步快照切换与加权战利品抽取）
 */
public final class ScavengerDataRegistry {

    public static final FileToIdConverter ITEM_CONVERTER = FileToIdConverter.json("scavenger_items");
    public static final FileToIdConverter CONTAINER_CONVERTER = FileToIdConverter.json("scavenger_containers");
    public static final Identifier PROGRESSION_RESOURCE_ID =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_system/progression.json");
    private static final AtomicReference<RegistrySnapshot> SERVER_SNAPSHOT =
            new AtomicReference<>(RegistrySnapshot.empty());
    private static final AtomicReference<RegistrySnapshot> CLIENT_SNAPSHOT =
            new AtomicReference<>(null);
    private ScavengerDataRegistry() {
    }

    /**
     * 从服务端 ResourceManager 全量加载/热重载所有拾荒物品、容器与成长公式 JSON。
     */
    public static void reloadServerData(ResourceManager resourceManager) {
        Map<Identifier, JsonElement> rawItems = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : ITEM_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            Identifier fileId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                rawItems.put(fileId, GsonHelper.parse(reader));
            } catch (Exception e) {
                ModLog.warn("【拾荒数据热加载】读取物品 JSON 文件失败: " + fileId + " -> " + e.getMessage());
            }
        }

        Map<Identifier, JsonElement> rawContainers = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : CONTAINER_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            Identifier fileId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                rawContainers.put(fileId, GsonHelper.parse(reader));
            } catch (Exception e) {
                ModLog.warn("【拾荒数据热加载】读取容器 JSON 文件失败: " + fileId + " -> " + e.getMessage());
            }
        }

        Optional<JsonElement> rawProgression = Optional.empty();
        Optional<Resource> progRes = resourceManager.getResource(PROGRESSION_RESOURCE_ID);
        if (progRes.isPresent()) {
            try (Reader reader = progRes.get().openAsReader()) {
                rawProgression = Optional.of(GsonHelper.parse(reader));
            } catch (Exception e) {
                ModLog.warn("【拾荒数据热加载】读取全局成长配置失败: " + PROGRESSION_RESOURCE_ID + " -> " + e.getMessage());
            }
        }

        loadServerDataFromJsonMaps(rawItems, rawContainers, rawProgression);
    }

    /**
     * 核心 JSON 解码与原子快照切换逻辑（亦供单元测试直接注入原始 JSON 验证热重载）。
     */
    public static RegistrySnapshot loadServerDataFromJsonMaps(
            Map<Identifier, JsonElement> rawItems,
            Map<Identifier, JsonElement> rawContainers,
            Optional<JsonElement> rawProgression
    ) {
        Map<Identifier, ScavengerItemMeta> parsedItems = new LinkedHashMap<>();
        int skippedUnknownItems = 0;

        for (Map.Entry<Identifier, JsonElement> entry : rawItems.entrySet()) {
            Identifier sourceId = entry.getKey();
            DataResult<ScavengerItemMeta> result = ScavengerItemMeta.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
            if (result.error().isPresent()) {
                ModLog.warn("【拾荒数据热加载】解析物品元数据失败 (" + sourceId + "): " + result.error().get().message());
                continue;
            }
            ScavengerItemMeta meta = result.result().orElseThrow();
            if (!BuiltInRegistries.ITEM.containsKey(meta.itemId())) {
                skippedUnknownItems++;
                ModLog.debug("【拾荒数据热加载】跳过未注册的物品 ID: " + meta.itemId() + " (来源: " + sourceId + ")");
                continue;
            }
            parsedItems.put(meta.itemId(), meta);
        }

        Map<Identifier, ScavengerContainerDef> parsedContainers = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : rawContainers.entrySet()) {
            Identifier sourceId = entry.getKey();
            DataResult<ScavengerContainerDef> result = ScavengerContainerDef.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
            if (result.error().isPresent()) {
                ModLog.warn("【拾荒数据热加载】解析容器定义失败 (" + sourceId + "): " + result.error().get().message());
                continue;
            }
            ScavengerContainerDef def = result.result().orElseThrow();
            parsedContainers.put(def.containerId(), def);
        }

        ScavengerProgressionConfig progression = ScavengerProgressionConfig.createDefault();
        if (rawProgression.isPresent()) {
            DataResult<ScavengerProgressionConfig> progResult =
                    ScavengerProgressionConfig.CODEC.parse(JsonOps.INSTANCE, rawProgression.get());
            if (progResult.error().isPresent()) {
                ModLog.warn("【拾荒数据热加载】解析成长配置失败，回退默认配置: " + progResult.error().get().message());
            } else {
                progression = progResult.result().orElseThrow();
            }
        }

        RegistrySnapshot nextSnapshot = new RegistrySnapshot(parsedItems, parsedContainers, progression);
        SERVER_SNAPSHOT.set(nextSnapshot);
        ModLog.debug(String.format(
                "【拾荒数据热加载】快照已原子更新 -> 有效物品数=%d (跳过未知物品=%d), 容器模板数=%d, 负重满级=Lv.%d, 仓库满级=Lv.%d",
                parsedItems.size(), skippedUnknownItems, parsedContainers.size(),
                progression.weightProgression().maxLevel(), progression.stashProgression().maxLevel()
        ));
        return nextSnapshot;
    }

    /**
     * 获取当前生效的数据快照（若客户端已接收远端 S2C 快照则优先返回客户端快照，否则返回本地服务端快照）。
     */
    public static RegistrySnapshot activeSnapshot() {
        RegistrySnapshot client = CLIENT_SNAPSHOT.get();
        return client != null ? client : SERVER_SNAPSHOT.get();
    }

    public static RegistrySnapshot serverSnapshot() {
        return SERVER_SNAPSHOT.get();
    }

    /**
     * 查询某物品显式配置的拾荒元数据（若未配置则返回 Empty）。
     */
    public static Optional<ScavengerItemMeta> getRegisteredItemMeta(Identifier itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeSnapshot().items().get(itemId));
    }

    /**
     * 获取任意 ItemStack 的拾荒元数据；若未在数据包中显式注册，则自动回退为 1x1 兜底元数据。
     */
    public static ScavengerItemMeta resolveItemMeta(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            Identifier airId = BuiltInRegistries.ITEM.getKey(net.minecraft.world.item.Items.AIR);
            return new ScavengerItemMeta(
                    airId,
                    me.tuanzi.world.scavenger.loot.meta.ScavengerCategory.MISC,
                    me.tuanzi.world.scavenger.loot.meta.ScavengerRarity.COMMON,
                    new ScavengerItemMeta.GridSize(1, 1),
                    0.0f, 0, 0.5f, List.of("*"), Optional.empty(),
                    me.tuanzi.world.scavenger.loot.meta.ScavengerSoundProfile.DEFAULT_TACTICAL,
                    Optional.empty()
            );
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ScavengerItemMeta registered = activeSnapshot().items().get(itemId);
        if (registered != null) {
            return registered;
        }
        return ScavengerItemMeta.fallback(itemId);
    }

    public static Optional<ScavengerContainerDef> getContainerDef(Identifier containerId) {
        if (containerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeSnapshot().containers().get(containerId));
    }

    public static Collection<ScavengerItemMeta> getAllRegisteredItems() {
        return activeSnapshot().items().values();
    }

    public static Collection<ScavengerContainerDef> getAllContainerDefs() {
        return activeSnapshot().containers().values();
    }

    public static ScavengerProgressionConfig getProgression() {
        return activeSnapshot().progression();
    }

    /**
     * 构建全量 S2C 同步包。
     */
    public static ScavengerDataSyncS2CPacket createSyncPacket() {
        RegistrySnapshot snap = SERVER_SNAPSHOT.get();
        return new ScavengerDataSyncS2CPacket(
                List.copyOf(snap.items().values()),
                List.copyOf(snap.containers().values()),
                snap.progression()
        );
    }

    public static void syncToPlayer(ServerPlayer player) {
        ScavengerDataSyncS2CPacket packet = createSyncPacket();
        ServerPlayNetworking.send(player, packet);
        ModLog.debug(player, null, String.format(
                "【拾荒数据同步】已向玩家下发 S2C 数据快照 (物品=%d, 容器=%d)",
                packet.items().size(), packet.containers().size()
        ));
    }

    public static void syncToAllPlayers(MinecraftServer server) {
        ScavengerDataSyncS2CPacket packet = createSyncPacket();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            ServerPlayNetworking.send(player, packet);
        }
        ModLog.debug(String.format(
                "【拾荒数据同步】已完成全服热重载广播 -> 在线玩家数=%d, 物品=%d, 容器=%d",
                players.size(), packet.items().size(), packet.containers().size()
        ));
    }

    public static void applyClientSync(ScavengerDataSyncS2CPacket packet) {
        Map<Identifier, ScavengerItemMeta> itemMap = new LinkedHashMap<>();
        for (ScavengerItemMeta meta : packet.items()) {
            itemMap.put(meta.itemId(), meta);
        }
        Map<Identifier, ScavengerContainerDef> containerMap = new LinkedHashMap<>();
        for (ScavengerContainerDef def : packet.containers()) {
            containerMap.put(def.containerId(), def);
        }
        CLIENT_SNAPSHOT.set(new RegistrySnapshot(itemMap, containerMap, packet.progression()));
        ModLog.debug(String.format(
                "【拾荒客户端同步】成功应用服务端搜刮元数据快照 -> 物品=%d, 容器=%d",
                itemMap.size(), containerMap.size()
        ));
    }

    public static void clearClientSync() {
        CLIENT_SNAPSHOT.set(null);
        ModLog.debug("【拾荒客户端同步】断开连接，已清理远端搜刮元数据快照。");
    }

    public static void resetAllForTest() {
        SERVER_SNAPSHOT.set(RegistrySnapshot.empty());
        CLIENT_SNAPSHOT.set(null);
    }

    /**
     * 计算指定物品在指定容器中的最终掉落权重：默认稀有度权重 * 容器稀有度倍率修正。
     */
    public static double computeEffectiveWeight(ScavengerContainerDef containerDef, ScavengerItemMeta meta) {
        int baseWeight = meta.rarity().defaultPoolWeight();
        float modifier = containerDef.lootGeneration().rarityWeightModifiers().getOrDefault(meta.rarity(), 1.0f);
        return Math.max(0.0, (double) baseWeight * (double) modifier);
    }

    /**
     * 获取符合容器尺寸、容器标签、池过滤标签且有效权重 > 0 的全部候选搜刮物品（按 itemId 稳定排序）。
     */
    public static List<ScavengerItemMeta> getEligibleCandidates(
            ScavengerContainerDef containerDef,
            Predicate<ScavengerItemMeta> extraFilter
    ) {
        List<ScavengerItemMeta> list = new ArrayList<>();
        for (ScavengerItemMeta meta : SERVER_SNAPSHOT.get().items().values()) {
            if (!containerDef.matchesLootCandidate(meta)) {
                continue;
            }
            if (extraFilter != null && !extraFilter.test(meta)) {
                continue;
            }
            // 装备类安全箱默认不在普通野箱刷出，除非容器显式匹配其标签且权重 > 0
            if (computeEffectiveWeight(containerDef, meta) <= 0.0) {
                continue;
            }
            list.add(meta);
        }
        list.sort(Comparator.comparing(ScavengerItemMeta::itemId));
        return list;
    }

    public static Optional<ScavengerItemMeta> rollWeightedCandidate(
            ScavengerContainerDef containerDef,
            RandomSource random
    ) {
        return rollWeightedCandidate(containerDef, random, null);
    }

    /**
     * 执行一次服务端权威加权战利品抽取，并通过 ModLog.debug 完整记录概率明细。
     */
    public static Optional<ScavengerItemMeta> rollWeightedCandidate(
            ScavengerContainerDef containerDef,
            RandomSource random,
            Predicate<ScavengerItemMeta> extraFilter
    ) {
        List<ScavengerItemMeta> candidates = getEligibleCandidates(containerDef, extraFilter);
        if (candidates.isEmpty()) {
            ModLog.debug("【拾荒战利品抽取】容器 " + containerDef.containerId() + " 无可用候选物品，跳过本次抽取。");
            return Optional.empty();
        }

        double totalWeight = 0.0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            double w = computeEffectiveWeight(containerDef, candidates.get(i));
            weights[i] = w;
            totalWeight += w;
        }

        if (totalWeight <= 0.0) {
            ModLog.debug("【拾荒战利品抽取】容器 " + containerDef.containerId() + " 候选总权重为 0，跳过本次抽取。");
            return Optional.empty();
        }

        double cursor = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        ScavengerItemMeta chosen = candidates.get(candidates.size() - 1);
        double chosenWeight = weights[candidates.size() - 1];

        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (cursor < cumulative) {
                chosen = candidates.get(i);
                chosenWeight = weights[i];
                break;
            }
        }

        double probabilityPercent = (chosenWeight / totalWeight) * 100.0;
        ModLog.debug(String.format(
                "【拾荒战利品抽取】容器=%s, 候选池=%d项, 总权重=%.2f, 随机游标=%.2f -> 命中=%s [%s, %dx%d, 单项概率=%.2f%%]",
                containerDef.containerId(),
                candidates.size(),
                totalWeight,
                cursor,
                chosen.itemId(),
                chosen.rarity().getSerializedName(),
                chosen.width(),
                chosen.height(),
                probabilityPercent
        ));
        return Optional.of(chosen);
    }

    public record RegistrySnapshot(
            Map<Identifier, ScavengerItemMeta> items,
            Map<Identifier, ScavengerContainerDef> containers,
            ScavengerProgressionConfig progression
    ) {
        public RegistrySnapshot {
            items = items == null ? Map.of() : Map.copyOf(items);
            containers = containers == null ? Map.of() : Map.copyOf(containers);
            progression = progression == null ? ScavengerProgressionConfig.createDefault() : progression;
        }

        public static RegistrySnapshot empty() {
            return new RegistrySnapshot(Map.of(), Map.of(), ScavengerProgressionConfig.createDefault());
        }
    }
}
