package me.tuanzi.gacha;

import me.tuanzi.util.ModLog;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GachaLogic {
    private static final Random random = new Random();

    public static class RollResult {
        private final GachaPoolItem poolItem;
        private final String rarity;

        public RollResult(GachaPoolItem poolItem, String rarity) {
            this.poolItem = poolItem;
            this.rarity = rarity;
        }

        public GachaPoolItem getPoolItem() {
            return poolItem;
        }

        public String getRarity() {
            return rarity;
        }
    }

    /**
     * 核心抽卡方法（支持单抽或十连）
     */
    public static List<RollResult> performRoll(ServerPlayer player, String poolId, boolean isTenFold) {
        HolderLookup.Provider registries = player.level().getServer().registryAccess();
        GachaPool pool = PoolManager.getPool(poolId);
        
        if (pool == null) {
            player.sendSystemMessage(Component.literal("§c未找到指定的卡池: " + poolId));
            return new ArrayList<>();
        }

        boolean isSakura = poolId.equalsIgnoreCase("sakura_moon");
        PlayerGachaState state = PlayerGachaManager.getOrCreatePlayerState(player.getUUID());
        
        List<RollResult> results = new ArrayList<>();
        int rollsCount = isTenFold ? 10 : 1;
        boolean hasRareOrAboveInTenFold = false;

        synchronized (state) {
            for (int i = 0; i < rollsCount; i++) {
                // 判定是否是十连的最后一次保底 (第 10 抽，且前 9 抽没有任何稀有以上)
                boolean isGuaranteedRare = isTenFold && (i == 9) && !hasRareOrAboveInTenFold;
                
                RollResult singleResult = rollSingle(player, state, pool, isSakura, isGuaranteedRare);
                results.add(singleResult);

                String rarity = singleResult.getRarity();
                if (rarity.equals("legendary") || rarity.equals("epic") || rarity.equals("rare")) {
                    hasRareOrAboveInTenFold = true;
                }

                // 写入玩家无上限历史记录
                ItemStack itemStack = singleResult.getPoolItem().getItem();
                String itemName = itemStack.getHoverName().getString();
                String itemRegistryId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
                
                DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timestamp = LocalDateTime.now().format(dft);
                
                GachaHistoryEntry historyEntry = new GachaHistoryEntry(
                        pool.getPoolName(),
                        rarity,
                        itemName,
                        timestamp,
                        itemRegistryId,
                        itemStack.getCount() // 写入本件抽卡结果的实际堆叠数量
                );
                state.addHistoryEntry(historyEntry);

                // 触发 on_obtain_commands 指令
                executeObtainCommands(player, singleResult.getPoolItem());
            }
        }

        return results;
    }

    /**
     * 单次抽卡概率计算与保底判定
     */
    private static RollResult rollSingle(ServerPlayer player, PlayerGachaState state, GachaPool pool, boolean isSakura, boolean isGuaranteedRare) {
        // 1. 获取计数器
        int legendaryCounter = isSakura ? state.getSakuraLegendaryCounter() : state.getNormalLegendaryCounter();
        int epicCounter = isSakura ? state.getSakuraEpicCounter() : state.getNormalEpicCounter();

        // 2. 计算传说概率 (软保底与硬保底)
        double pLegendary = 0.02;
        int hardLegendaryLimit = isSakura ? 90 : 80;
        int softLegendaryStart = isSakura ? 70 : 60;

        if (legendaryCounter >= softLegendaryStart) {
            pLegendary = 0.02 + 0.05 * (legendaryCounter + 1 - softLegendaryStart);
            ModLog.debug(player, null, String.format("概率计算 -> 触发传说软保底！累计抽数: %d | 概率由 2.0%% 递增至 %.1f%%", legendaryCounter + 1, pLegendary * 100.0));
        }
        if (legendaryCounter >= hardLegendaryLimit - 1) {
            pLegendary = 1.0; // 硬保底触发
            ModLog.debug(player, null, String.format("概率计算 -> 触发传说硬保底（达到 %d 抽上限，100%% 爆金）！", hardLegendaryLimit));
        }

        // 3. 计算史诗概率
        double pEpic = 0.10;
        if (epicCounter >= 19) {
            pEpic = 1.0 - pLegendary; // 触发 20 抽必定得史诗保底 (在排除传说概率后的剩余空间里 100%)
            ModLog.debug(player, null, String.format("概率计算 -> 触发史诗硬保底（累计未出史诗 %d 抽，100%% 爆紫）！", epicCounter + 1));
        }

        // 4. 计算其它品质概率
        double pRare = 0.15;
        double pUncommon = 0.23;
        double pCommon = 0.50;

        // 5. 依据十连稀有保底或者常规概率进行分配
        String raritySelected = "common";
        double r = random.nextDouble();

        if (isGuaranteedRare && (pLegendary + pEpic < 1.0)) {
            // 触发十连保底：排除优秀与普通，在传说、史诗、稀有中随机
            double currentRare = 1.0 - pLegendary - pEpic;
            
            ModLog.debug(player, null, String.format(
                    "概率计算 -> 触发十连稀有保底（第 10 抽）！区间划分 -> 传说: [0.0, %.4f) | 史诗: [%.4f, %.4f) | 稀有保底: [%.4f, 1.0) | 随机因子: %.4f",
                    pLegendary, pLegendary, pLegendary + pEpic, pLegendary + pEpic, r
            ));
            
            if (r < pLegendary) {
                raritySelected = "legendary";
            } else if (r < pLegendary + pEpic) {
                raritySelected = "epic";
            } else {
                raritySelected = "rare";
            }
        } else {
            // 常规判定
            ModLog.debug(player, null, String.format(
                    "概率计算 -> 常规随机抽取！区间划分 -> 传说: [0.0, %.4f) | 史诗: [%.4f, %.4f) | 稀有: [%.4f, %.4f) | 优秀: [%.4f, %.4f) | 普通: [%.4f, 1.0) | 随机因子: %.4f",
                    pLegendary, pLegendary, pLegendary + pEpic, pLegendary + pEpic, pLegendary + pEpic + pRare, pLegendary + pEpic + pRare, pLegendary + pEpic + pRare + pUncommon, pLegendary + pEpic + pRare + pUncommon, r
            ));
            if (r < pLegendary) {
                raritySelected = "legendary";
            } else if (r < pLegendary + pEpic) {
                raritySelected = "epic";
            } else if (r < pLegendary + pEpic + pRare) {
                raritySelected = "rare";
            } else if (r < pLegendary + pEpic + pRare + pUncommon) {
                raritySelected = "uncommon";
            } else {
                raritySelected = "common";
            }
        }

        // 6. 从对应品质的卡池中按权重随机选择具体物品
        List<GachaPoolItem> list = pool.getListByRarity(raritySelected);
        
        // 品质顺降保护：若当前稀有度无任何物品，顺降一级直到有物品，防止死锁或空指针
        if (list.isEmpty()) {
            String[] rarities = {"legendary", "epic", "rare", "uncommon", "common"};
            int startIdx = 0;
            for (int i = 0; i < rarities.length; i++) {
                if (rarities[i].equals(raritySelected)) {
                    startIdx = i;
                    break;
                }
            }
            for (int i = startIdx; i < rarities.length; i++) {
                List<GachaPoolItem> fallbackList = pool.getListByRarity(rarities[i]);
                if (!fallbackList.isEmpty()) {
                    list = fallbackList;
                    raritySelected = rarities[i];
                    break;
                }
            }
        }

        // 如果连普通池都是空的，给出紧急备份物品
        if (list.isEmpty()) {
            ModLog.debug(player, null, "警告：卡池 " + pool.getPoolId() + " 没有任何可用物品！");
            GachaPoolItem fallbackItem = new GachaPoolItem("fallback", 10, new ArrayList<>(), new ItemStack(net.minecraft.world.item.Items.DIRT));
            list = List.of(fallbackItem);
            raritySelected = "common";
        }

        // 7. 限定池传说 UP 特性
        GachaPoolItem selectedItem;
        if (isSakura && raritySelected.equals("legendary")) {
            // 限定池一旦抽中传说，直接 100% 获得当期限定传说级物品的第一个
            selectedItem = list.get(0);
        } else {
            // 常规加权随机
            selectedItem = selectWeightedItem(list);
        }

        // 8. 调试概率计算日志输出 ( me.tuanzi.util.ModLog.debug )
        ModLog.debug(player, null, String.format(
                "抽卡逻辑计算 -> 抽数累计[传说/史诗]: %d/%d | 当前单抽概率[传说/史诗]: %.1f%%/%.1f%% | 随机因子: %.4f | 选定品质: %s | 获得物品: %s",
                legendaryCounter + 1, epicCounter + 1, pLegendary * 100.0, pEpic * 100.0, r, raritySelected, selectedItem.getItem().getHoverName().getString()
        ));

        // 9. 更新状态计数器
        if (raritySelected.equals("legendary")) {
            if (isSakura) {
                state.setSakuraLegendaryCounter(0);
                state.setSakuraEpicCounter(0);
            } else {
                state.setNormalLegendaryCounter(0);
                state.setNormalEpicCounter(0);
            }
        } else if (raritySelected.equals("epic")) {
            if (isSakura) {
                state.setSakuraLegendaryCounter(legendaryCounter + 1);
                state.setSakuraEpicCounter(0);
            } else {
                state.setNormalLegendaryCounter(legendaryCounter + 1);
                state.setNormalEpicCounter(0);
            }
        } else {
            if (isSakura) {
                state.setSakuraLegendaryCounter(legendaryCounter + 1);
                state.setSakuraEpicCounter(epicCounter + 1);
            } else {
                state.setNormalLegendaryCounter(legendaryCounter + 1);
                state.setNormalEpicCounter(epicCounter + 1);
            }
        }

        return new RollResult(selectedItem, raritySelected);
    }

    /**
     * 加权随机选择器
     */
    private static GachaPoolItem selectWeightedItem(List<GachaPoolItem> list) {
        int totalWeight = list.stream().mapToInt(GachaPoolItem::getWeight).sum();
        if (totalWeight <= 0) {
            return list.get(random.nextInt(list.size()));
        }
        int r = random.nextInt(totalWeight);
        int currentSum = 0;
        for (GachaPoolItem item : list) {
            currentSum += item.getWeight();
            if (r < currentSum) {
                return item;
            }
        }
        return list.get(list.size() - 1);
    }

    /**
     * 动态指令触发与替换引擎
     */
    private static void executeObtainCommands(ServerPlayer player, GachaPoolItem item) {
        List<String> cmds = item.getOnObtainCommands();
        if (cmds == null || cmds.isEmpty()) return;

        net.minecraft.commands.CommandSourceStack source = player.level().getServer().createCommandSourceStack()
                .withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS) // OP 全权限
                .withSuppressedOutput();

        for (String cmd : cmds) {
            String processedCmd = cmd.replace("%player%", player.getScoreboardName())
                    .replace("%player_x%", String.format("%.2f", player.getX()))
                    .replace("%player_y%", String.format("%.2f", player.getY()))
                    .replace("%player_z%", String.format("%.2f", player.getZ()));
            
            player.level().getServer().getCommands().performPrefixedCommand(source, processedCmd);
            ModLog.debug(player, null, "卡池特殊出货指令执行: " + processedCmd);
        }
    }
}
