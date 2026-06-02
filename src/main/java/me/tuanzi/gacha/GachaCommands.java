package me.tuanzi.gacha;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.tuanzi.gacha.gui.GachaHistoryMenu;
import me.tuanzi.gacha.gui.GachaPoolEditMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;

public class GachaCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cardpool")
                .then(Commands.literal("info")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§6=== 活跃卡池列表 ==="));
                        PoolManager.getPools().forEach((id, pool) -> {
                            player.sendSystemMessage(Component.literal("§eID: §f" + id + " §8| §e卡池名: §f" + pool.getPoolName()));
                        });
                        return 1;
                    })
                )
                .then(Commands.literal("stats")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        showStats(player, player);
                        return 1;
                    })
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            showStats(player, target);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("history")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        
                        // 判定手持物品以确定展示常驻池还是限定池的历史记录标题
                        String poolTitle = "§d限定卡池抽卡历史记录"; // 默认限定池
                        net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                        net.minecraft.world.item.ItemStack offHand = player.getOffhandItem();
                        
                        boolean holdsNormal = false;
                        if (mainHand != null && !mainHand.isEmpty() && mainHand.getItem() instanceof me.tuanzi.item.GachaItem g1 && "normal".equals(g1.getPoolId())) {
                            holdsNormal = true;
                        } else if (offHand != null && !offHand.isEmpty() && offHand.getItem() instanceof me.tuanzi.item.GachaItem g2 && "normal".equals(g2.getPoolId())) {
                            holdsNormal = true;
                        }
                        
                        if (holdsNormal) {
                            poolTitle = "§6常驻卡池抽卡历史记录";
                        }

                        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                                (id, playerInv, p) -> new GachaHistoryMenu(id, playerInv),
                                Component.literal(poolTitle)
                        ));
                        return 1;
                    })
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PoolManager.loadAllPools(player.level().getServer().registryAccess());
                        player.sendSystemMessage(Component.literal("§a卡池配置文件已重新装载！"));
                        return 1;
                    })
                )
                .then(Commands.literal("reset")
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("pool", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                String pool = StringArgumentType.getString(context, "pool");
                                resetStats(player, target, pool);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("setcounter")
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("pool", StringArgumentType.word())
                            .then(Commands.argument("counterType", StringArgumentType.word())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                        String pool = StringArgumentType.getString(context, "pool");
                                        String counterType = StringArgumentType.getString(context, "counterType");
                                        int value = IntegerArgumentType.getInteger(context, "value");
                                        setCounter(player, target, pool, counterType, value);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("test_roll")
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                    .then(Commands.argument("pool", StringArgumentType.word())
                        .then(Commands.argument("times", IntegerArgumentType.integer(1, 100000))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                String pool = StringArgumentType.getString(context, "pool");
                                int times = IntegerArgumentType.getInteger(context, "times");
                                testRoll(player, pool, times);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("edit")
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                    .then(Commands.argument("pool", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String poolId = StringArgumentType.getString(context, "pool");
                            GachaPool pool = PoolManager.getPool(poolId);
                            if (pool == null) {
                                player.sendSystemMessage(Component.literal("§c未找到指定的卡池: " + poolId));
                                return 0;
                            }
                            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                                    (id, playerInv, p) -> new GachaPoolEditMenu(id, playerInv, pool),
                                    Component.literal("活跃卡池编辑器 - " + pool.getPoolName())
                            ));
                            return 1;
                        })
                    )
                )
        );
    }

    private static void showStats(ServerPlayer player, ServerPlayer target) {
        PlayerGachaState state = PlayerGachaManager.getOrCreatePlayerState(target.getUUID());
        player.sendSystemMessage(Component.literal("§6=== 玩家 [" + target.getScoreboardName() + "] 抽卡保底状态 ==="));
        
        double normalP = 0.02;
        int normalL = state.getNormalLegendaryCounter();
        if (normalL >= 60) normalP = 0.02 + 0.05 * (normalL + 1 - 60);
        
        double sakuraP = 0.02;
        int sakuraL = state.getSakuraLegendaryCounter();
        if (sakuraL >= 70) sakuraP = 0.02 + 0.05 * (sakuraL + 1 - 70);

        player.sendSystemMessage(Component.literal(String.format("§e常驻卡池: §f已累积 §a%d§f 抽 (保底上限 80 抽) | 当前传说概率: §a%.1f%%§f | 史诗未出: §d%d§f 抽", normalL, normalP * 100.0, state.getNormalEpicCounter())));
        player.sendSystemMessage(Component.literal(String.format("§e限定卡池: §f已累积 §a%d§f 抽 (保底上限 90 抽) | 当前传说概率: §a%.1f%%§f | 史诗未出: §d%d§f 抽", sakuraL, sakuraP * 100.0, state.getSakuraEpicCounter())));
    }

    private static void showHistory(ServerPlayer player, int page) {
        PlayerGachaState state = PlayerGachaManager.getOrCreatePlayerState(player.getUUID());
        java.util.List<GachaHistoryEntry> history = state.getHistory();
        
        if (history.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c你目前没有任何抽卡历史记录！"));
            return;
        }

        int pageSize = 10; 
        int totalItems = history.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        if (page > totalPages) {
            player.sendSystemMessage(Component.literal("§c超出页码上限，最大页码为: " + totalPages));
            return;
        }

        player.sendSystemMessage(Component.literal("§6=== 个人抽卡历史记录 (第 " + page + " 页 / 共 " + totalPages + " 页) ==="));
        
        int startIdx = totalItems - 1 - (page - 1) * pageSize;
        int endIdx = Math.max(0, startIdx - pageSize + 1);

        for (int i = startIdx; i >= endIdx; i--) {
            GachaHistoryEntry entry = history.get(i);
            String rarityColor = "§f";
            if (entry.getRarity().equals("legendary")) rarityColor = "§6[传说] ";
            else if (entry.getRarity().equals("epic")) rarityColor = "§d[史诗] ";
            else if (entry.getRarity().equals("rare")) rarityColor = "§b[稀有] ";
            else if (entry.getRarity().equals("uncommon")) rarityColor = "§2[优秀] ";
            else rarityColor = "§f[普通] ";

            player.sendSystemMessage(Component.literal(String.format("§7[%s] %s%s §f(卡池: %s)", entry.getTimestamp(), rarityColor, entry.getItemName(), entry.getPoolName())));
        }
    }

    private static void resetStats(ServerPlayer op, ServerPlayer target, String poolId) {
        PlayerGachaState state = PlayerGachaManager.getOrCreatePlayerState(target.getUUID());
        synchronized (state) {
            if (poolId.equalsIgnoreCase("normal") || poolId.equalsIgnoreCase("all")) {
                state.setNormalLegendaryCounter(0);
                state.setNormalEpicCounter(0);
            }
            if (poolId.equalsIgnoreCase("sakura_moon") || poolId.equalsIgnoreCase("all")) {
                state.setSakuraLegendaryCounter(0);
                state.setSakuraEpicCounter(0);
            }
        }
        op.sendSystemMessage(Component.literal("§a已成功重置玩家 [" + target.getScoreboardName() + "] 的保底计数器！"));
    }

    private static void setCounter(ServerPlayer op, ServerPlayer target, String poolId, String counterType, int value) {
        PlayerGachaState state = PlayerGachaManager.getOrCreatePlayerState(target.getUUID());
        boolean isSakura = poolId.equalsIgnoreCase("sakura_moon");
        boolean isLegendary = counterType.equalsIgnoreCase("legendary");

        synchronized (state) {
            if (isSakura) {
                if (isLegendary) state.setSakuraLegendaryCounter(value);
                else state.setSakuraEpicCounter(value);
            } else {
                if (isLegendary) state.setNormalLegendaryCounter(value);
                else state.setNormalEpicCounter(value);
            }
        }
        op.sendSystemMessage(Component.literal(String.format("§a已成功修改玩家 [%s] 的卡池 %s %s保底值为 %d！", target.getScoreboardName(), poolId, counterType, value)));
    }

    private static void testRoll(ServerPlayer player, String poolId, int times) {
        GachaPool pool = PoolManager.getPool(poolId);
        if (pool == null) {
            player.sendSystemMessage(Component.literal("§c未找到指定的卡池: " + poolId));
            return;
        }

        PlayerGachaState tempState = new PlayerGachaState(player.getUUID());
        java.util.Random rand = new java.util.Random();

        int legendaryCount = 0;
        int epicCount = 0;
        int rareCount = 0;
        int uncommonCount = 0;
        int commonCount = 0;

        int maxLegendaryGap = 0;
        int currentLegendaryGap = 0;
        int maxEpicGap = 0;
        int currentEpicGap = 0;

        boolean isSakura = poolId.equalsIgnoreCase("sakura_moon");

        for (int i = 0; i < times; i++) {
            int legCounter = isSakura ? tempState.getSakuraLegendaryCounter() : tempState.getNormalLegendaryCounter();
            int epCounter = isSakura ? tempState.getSakuraEpicCounter() : tempState.getNormalEpicCounter();

            double pLegendary = 0.02;
            int hardLegendaryLimit = isSakura ? 90 : 80;
            int softLegendaryStart = isSakura ? 70 : 60;

            if (legCounter >= softLegendaryStart) {
                pLegendary = 0.02 + 0.05 * (legCounter + 1 - softLegendaryStart);
            }
            if (legCounter >= hardLegendaryLimit - 1) {
                pLegendary = 1.0;
            }

            double pEpic = 0.10;
            if (epCounter >= 19) {
                pEpic = 1.0 - pLegendary;
            }

            double r = rand.nextDouble();
            String rarity = "common";
            
            if (r < pLegendary) {
                rarity = "legendary";
            } else if (r < pLegendary + pEpic) {
                rarity = "epic";
            } else if (r < pLegendary + pEpic + 0.15) {
                rarity = "rare";
            } else if (r < pLegendary + pEpic + 0.15 + 0.23) {
                rarity = "uncommon";
            }

            currentLegendaryGap++;
            currentEpicGap++;

            if (rarity.equals("legendary")) {
                legendaryCount++;
                if (currentLegendaryGap > maxLegendaryGap) maxLegendaryGap = currentLegendaryGap;
                currentLegendaryGap = 0;

                tempState.setSakuraLegendaryCounter(0);
                tempState.setSakuraEpicCounter(0);
                tempState.setNormalLegendaryCounter(0);
                tempState.setNormalEpicCounter(0);
            } else if (rarity.equals("epic")) {
                epicCount++;
                if (currentEpicGap > maxEpicGap) maxEpicGap = currentEpicGap;
                currentEpicGap = 0;

                tempState.setSakuraLegendaryCounter(legCounter + 1);
                tempState.setSakuraEpicCounter(0);
                tempState.setNormalLegendaryCounter(legCounter + 1);
                tempState.setNormalEpicCounter(0);
            } else {
                if (rarity.equals("rare")) rareCount++;
                else if (rarity.equals("uncommon")) uncommonCount++;
                else commonCount++;

                tempState.setSakuraLegendaryCounter(legCounter + 1);
                tempState.setSakuraEpicCounter(epCounter + 1);
                tempState.setNormalLegendaryCounter(legCounter + 1);
                tempState.setNormalEpicCounter(epCounter + 1);
            }
        }

        player.sendSystemMessage(Component.literal("§6=== 模拟抽卡统计报告 (" + times + "次) ==="));
        player.sendSystemMessage(Component.literal("§e卡池ID: §f" + poolId + " (" + pool.getPoolName() + ")"));
        player.sendSystemMessage(Component.literal(String.format("§a传说(金): §f%d次 (%.2f%%) | 最长未出: %d抽", legendaryCount, (legendaryCount * 100.0 / times), maxLegendaryGap)));
        player.sendSystemMessage(Component.literal(String.format("§d史诗(紫): §f%d次 (%.2f%%) | 最长未出: %d抽", epicCount, (epicCount * 100.0 / times), maxEpicGap)));
        player.sendSystemMessage(Component.literal(String.format("§b稀有(蓝): §f%d次 (%.2f%%)", rareCount, (rareCount * 100.0 / times))));
        player.sendSystemMessage(Component.literal(String.format("§2优秀(绿): §f%d次 (%.2f%%)", uncommonCount, (uncommonCount * 100.0 / times))));
        player.sendSystemMessage(Component.literal(String.format("§f普通(白): §f%d次 (%.2f%%)", commonCount, (commonCount * 100.0 / times))));
    }
}
