package me.tuanzi.gacha.gui;

import me.tuanzi.gacha.GachaPool;
import me.tuanzi.gacha.GachaPoolItem;
import me.tuanzi.gacha.PlayerGachaState;
import me.tuanzi.gacha.PlayerGachaManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public class GachaPreviewMenu extends ChestMenu {
    private final GachaPool pool;
    private final ServerPlayer player;
    private final PlayerGachaState state;
    private final List<GachaPoolItem> sortedItems = new ArrayList<>();
    private int currentPage = 0;

    public GachaPreviewMenu(int containerId, Inventory playerInventory, GachaPool pool) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
        this.pool = pool;
        this.player = (ServerPlayer) playerInventory.player;
        this.state = PlayerGachaManager.getOrCreatePlayerState(player.getUUID());
        
        // 整理卡池内所有物品，按品质高低排序展示
        String[] rarities = {"legendary", "epic", "rare", "uncommon", "common"};
        for (String r : rarities) {
            sortedItems.addAll(pool.getListByRarity(r));
        }

        refreshSlots();
    }

    public void refreshSlots() {
        Container container = this.getContainer();
        // 1. 清空所有槽位
        for (int i = 0; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        int totalItems = sortedItems.size();
        int totalPages = (int) Math.ceil((double) totalItems / 45);
        if (totalPages == 0) totalPages = 1;

        // 2. 绘制翻页与实时保底概率展示栏 (第 6 行: Slot 45-53)
        ItemStack grayPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        grayPane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 45; i < 54; i++) {
            if (i == 45) {
                container.setItem(i, createNamedItem(Items.ARROW, "§e◀ 上一页"));
            } else if (i == 49) {
                // 动态计算该玩家当前的抽卡实时概率
                List<String> statsLore = getPlayerStatsLore();
                container.setItem(i, createDetailedItem(Items.BOOK, "§6§l✦ 你的实时出货概率与保底 ✦", statsLore));
            } else if (i == 53) {
                container.setItem(i, createNamedItem(Items.ARROW, "§e下一页 ▶"));
            } else {
                container.setItem(i, grayPane.copy());
            }
        }

        // 3. 填充当前页的物品 (Slot 0-44，展示原本属性，Lore 尾部加上概率占比)
        int startIdx = currentPage * 45;
        // 计算当前卡池对应品质的总权重，从而算出物品在此品质内的出货占比
        for (int i = 0; i < 45; i++) {
            int itemIdx = startIdx + i;
            if (itemIdx < sortedItems.size()) {
                GachaPoolItem poolItem = sortedItems.get(itemIdx);
                ItemStack displayStack = poolItem.getItem().copy();

                // 获取品质占比
                String rarity = getRarityOfItem(poolItem);
                int totalRarityWeight = pool.getListByRarity(rarity).stream().mapToInt(GachaPoolItem::getWeight).sum();
                double percentInRarity = totalRarityWeight > 0 ? (poolItem.getWeight() * 100.0 / totalRarityWeight) : 0.0;

                // 准备 Lore
                ItemLore originalLore = displayStack.get(DataComponents.LORE);
                List<Component> displayLore = new ArrayList<>();
                if (originalLore != null) {
                    displayLore.addAll(originalLore.lines());
                }
                displayLore.add(Component.literal(""));
                displayLore.add(Component.literal("§7===================="));
                displayLore.add(Component.literal("§7所属品质: " + getRarityName(rarity)));
                displayLore.add(Component.literal(String.format("§7同品质内随机占比: §a%.2f%%", percentInRarity)));
                displayLore.add(Component.literal("§7===================="));
                displayStack.set(DataComponents.LORE, new ItemLore(displayLore));

                container.setItem(i, displayStack);
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex < 0 || slotIndex >= 90) {
            return;
        }

        // 处理箱子界面的翻页控制 (Slot 45 和 53)
        if (slotIndex == 45) {
            if (currentPage > 0) {
                currentPage--;
                refreshSlots();
            }
        } else if (slotIndex == 53) {
            int totalPages = (int) Math.ceil((double) sortedItems.size() / 45);
            if (currentPage + 1 < totalPages) {
                currentPage++;
                refreshSlots();
            }
        }

        // 强行拦截所有点击并广播
        this.broadcastFullState();
    }

    private List<String> getPlayerStatsLore() {
        boolean isSakura = pool.getPoolId().equalsIgnoreCase("sakura_moon");
        
        int legendaryCounter = isSakura ? state.getSakuraLegendaryCounter() : state.getNormalLegendaryCounter();
        int epicCounter = isSakura ? state.getSakuraEpicCounter() : state.getNormalEpicCounter();
        
        double pLegendary = 0.02;
        int hardLegendaryLimit = isSakura ? 90 : 80;
        int softLegendaryStart = isSakura ? 70 : 60;

        if (legendaryCounter >= softLegendaryStart) {
            pLegendary = 0.02 + 0.05 * (legendaryCounter + 1 - softLegendaryStart);
        }
        if (legendaryCounter >= hardLegendaryLimit - 1) {
            pLegendary = 1.0;
        }

        double pEpic = 0.10;
        if (epicCounter >= 19) {
            pEpic = 1.0 - pLegendary;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7当前卡池: §f" + pool.getPoolName());
        lore.add(String.format("§7已累积未出传说: §a%d 抽 §8(上限 %d 抽)", legendaryCounter, hardLegendaryLimit));
        lore.add(String.format("§7已累积未出史诗: §d%d 抽 §8(上限 20 抽)", epicCounter));
        lore.add(Component.literal("").getString());
        lore.add(String.format("§7▶ 下一抽传说级(金)概率: §6%.1f%%", pLegendary * 100.0));
        lore.add(String.format("§7▶ 下一抽史诗级(紫)概率: §d%.1f%%", pEpic * 100.0));
        
        if (isSakura) {
            lore.add("§8* 限定池若中金，直接 100% 获得当期限定传说大奖！");
        }

        return lore;
    }

    private String getRarityOfItem(GachaPoolItem item) {
        String[] rarities = {"legendary", "epic", "rare", "uncommon", "common"};
        for (String r : rarities) {
            if (pool.getListByRarity(r).contains(item)) {
                return r;
            }
        }
        return "common";
    }

    private String getRarityName(String key) {
        return switch (key.toLowerCase()) {
            case "legendary" -> "§6传说 (Legendary)";
            case "epic" -> "§d史诗 (Epic)";
            case "rare" -> "§b稀有 (Rare)";
            case "uncommon" -> "§2优秀 (Uncommon)";
            default -> "§f普通 (Common)";
        };
    }

    private ItemStack createNamedItem(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private ItemStack createDetailedItem(net.minecraft.world.item.Item item, String name, List<String> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        List<Component> compLore = new ArrayList<>();
        for (String s : lore) {
            compLore.add(Component.literal(s));
        }
        stack.set(DataComponents.LORE, new ItemLore(compLore));
        return stack;
    }
}
