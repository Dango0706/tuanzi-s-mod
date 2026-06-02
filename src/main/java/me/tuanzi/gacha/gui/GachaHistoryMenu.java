package me.tuanzi.gacha.gui;

import me.tuanzi.gacha.GachaHistoryEntry;
import me.tuanzi.gacha.PlayerGachaState;
import me.tuanzi.gacha.PlayerGachaManager;
import me.tuanzi.gacha.GachaPool;
import me.tuanzi.gacha.PoolManager;
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

public class GachaHistoryMenu extends ChestMenu {
    private final ServerPlayer player;
    private final PlayerGachaState state;
    private final String targetPoolId;
    private int currentPage = 0;

    public GachaHistoryMenu(int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
        this.player = (ServerPlayer) playerInventory.player;
        this.state = PlayerGachaManager.getOrCreatePlayerState(player.getUUID());
        
        // 判定手持物品以确定展示常驻池还是限定池的历史记录
        String detectedPoolId = "sakura_moon"; // 默认限定池
        net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
        net.minecraft.world.item.ItemStack offHand = player.getOffhandItem();
        if (isNormalGachaItem(mainHand) || isNormalGachaItem(offHand)) {
            detectedPoolId = "normal";
        }
        this.targetPoolId = detectedPoolId;
        
        refreshSlots();
    }

    private boolean isNormalGachaItem(net.minecraft.world.item.ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            if (stack.getItem() instanceof me.tuanzi.item.GachaItem gachaItem) {
                return "normal".equals(gachaItem.getPoolId());
            }
        }
        return false;
    }

    private List<GachaHistoryEntry> getFilteredHistory() {
        List<GachaHistoryEntry> history = state.getHistory();
        List<GachaHistoryEntry> filtered = new java.util.ArrayList<>();
        GachaPool pool = PoolManager.getPool(targetPoolId);
        String targetPoolName = pool != null ? pool.getPoolName() : null;

        for (GachaHistoryEntry entry : history) {
            if (targetPoolName != null) {
                if (targetPoolName.equalsIgnoreCase(entry.getPoolName())) {
                    filtered.add(entry);
                }
            } else {
                // 找不到卡池定义时的模糊匹配兜底
                if ("normal".equals(targetPoolId)) {
                    if (entry.getPoolName().contains("常驻") || entry.getPoolName().contains("星旅") || entry.getPoolName().contains("Star")) {
                        filtered.add(entry);
                    }
                } else {
                    if (entry.getPoolName().contains("限定") || entry.getPoolName().contains("樱") || entry.getPoolName().contains("Sakura") || entry.getPoolName().contains("Moon")) {
                        filtered.add(entry);
                    }
                }
            }
        }
        return filtered;
    }

    public void refreshSlots() {
        Container container = this.getContainer();
        // 1. 清空所有槽位
        for (int i = 0; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        List<GachaHistoryEntry> filteredHistory = getFilteredHistory();
        int totalItems = filteredHistory.size();
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        // 2. 绘制下层功能翻页栏 (第 6 行: Slot 45-53)
        ItemStack grayPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        grayPane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 45; i < 54; i++) {
            if (i == 45) {
                container.setItem(i, createNamedItem(Items.ARROW, "§e◀ 上一页"));
            } else if (i == 49) {
                container.setItem(i, createNamedItem(Items.BOOK, "§6第 " + (currentPage + 1) + " 页 §7/ §6共 " + totalPages + " 页 §8(总计 " + totalItems + " 条)"));
            } else if (i == 53) {
                container.setItem(i, createNamedItem(Items.ARROW, "§e下一页 ▶"));
            } else {
                container.setItem(i, grayPane.copy());
            }
        }

        // 3. 填充当前页的历史存根 (满载 45 个记录，平铺在 Slot 0-44 中)
        int startIdx = currentPage * itemsPerPage;
        for (int slotIdx = 0; slotIdx < itemsPerPage; slotIdx++) {
            int historyIdx = totalItems - 1 - (startIdx + slotIdx);
            if (historyIdx >= 0 && historyIdx < totalItems) {
                GachaHistoryEntry entry = filteredHistory.get(historyIdx);
                container.setItem(slotIdx, createHistoryItem(entry));
            } else {
                // 没有历史记录，直接保持为 ItemStack.EMPTY
                container.setItem(slotIdx, ItemStack.EMPTY);
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
            List<GachaHistoryEntry> filteredHistory = getFilteredHistory();
            int totalPages = (int) Math.ceil((double) filteredHistory.size() / 45);
            if (currentPage + 1 < totalPages) {
                currentPage++;
                refreshSlots();
            }
        }

        // 无论是哪里的点击，全数强行拦截并广播，阻止玩家移动或拿走任何历史记录和背包物品！
        this.broadcastFullState();
    }

    private static final java.util.Map<String, net.minecraft.world.item.Item> ITEM_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private net.minecraft.world.item.Item findItemByName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }
        
        // 1. 净化名字：去除颜色格式代码（如 §a, §b）
        String cleanName = rawName.replaceAll("§[0-9a-fk-orxX]", "").trim();
        if (cleanName.isEmpty()) {
            return null;
        }

        // 2. 查缓存
        if (ITEM_CACHE.containsKey(cleanName)) {
            return ITEM_CACHE.get(cleanName);
        }

        // 3. 遍历注册表精确匹配
        net.minecraft.world.item.Item matched = null;
        
        // 先精确比对去除样式代码后的 HoverName
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack tempStack = new ItemStack(item);
            String hover = tempStack.getHoverName().getString().replaceAll("§[0-9a-fk-orxX]", "").trim();
            if (hover.equalsIgnoreCase(cleanName)) {
                matched = item;
                break;
            }
        }

        // 如果没找到，尝试匹配翻译键的末尾或整体
        if (matched == null) {
            for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                String descId = item.getDescriptionId(); // 如 item.minecraft.diamond_sword
                if (descId.equalsIgnoreCase(cleanName) || descId.endsWith("." + cleanName)) {
                    matched = item;
                    break;
                }
            }
        }

        // 如果还没找到，尝试包含关系匹配
        if (matched == null) {
            for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack tempStack = new ItemStack(item);
                String hover = tempStack.getHoverName().getString().replaceAll("§[0-9a-fk-orxX]", "").trim();
                if (!hover.isEmpty() && (cleanName.contains(hover) || hover.contains(cleanName))) {
                    matched = item;
                    break;
                }
            }
        }

        // 如果还没找到，比对 Registry 中的 Path 名字
        if (matched == null) {
            for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                net.minecraft.resources.Identifier loc = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                if (loc != null) {
                    String path = loc.getPath();
                    if (path.equalsIgnoreCase(cleanName) || cleanName.contains(path)) {
                        matched = item;
                        break;
                    }
                }
            }
        }

        if (matched != null) {
            ITEM_CACHE.put(cleanName, matched);
        }
        return matched;
    }

    private ItemStack createHistoryItem(GachaHistoryEntry entry) {
        // 优先根据记录的物品 Registry ID 创建真实的物品 ItemStack，实现真实抽中物品渲染
        ItemStack stack = null;
        if (entry.getItemRegistryId() != null && !entry.getItemRegistryId().isEmpty()) {
            try {
                net.minecraft.resources.Identifier loc = net.minecraft.resources.Identifier.tryParse(entry.getItemRegistryId());
                if (loc != null) {
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(loc);
                    if (item != null && item != Items.AIR) {
                        stack = new ItemStack(item);
                    }
                }
            } catch (Exception e) {
                // 解析失败时优雅忽略，走降级匹配或兜底逻辑
            }
        }

        // 如果没有真实的物品信息（如历史老旧记录），则通过智能名称匹配获取真实物品
        if (stack == null && entry.getItemName() != null && !entry.getItemName().isEmpty()) {
            net.minecraft.world.item.Item matchedItem = findItemByName(entry.getItemName());
            if (matchedItem != null && matchedItem != Items.AIR) {
                stack = new ItemStack(matchedItem);
            }
        }

        // 如果连名称都匹配失败，则根据出货品质降级为高质感的“真实代表物品”，绝对不显示彩色玻璃板
        if (stack == null) {
            stack = switch (entry.getRarity().toLowerCase()) {
                case "legendary" -> new ItemStack(Items.NETHERITE_INGOT);      // 传说用下界合金锭
                case "epic" -> new ItemStack(Items.DIAMOND);                   // 史诗用钻石
                case "rare" -> new ItemStack(Items.AMETHYST_SHARD);             // 稀有用紫水晶
                case "uncommon" -> new ItemStack(Items.IRON_INGOT);             // 优秀用铁锭
                default -> new ItemStack(Items.COPPER_INGOT);                  // 普通用铜锭
            };
        }

        String colorPrefix = switch (entry.getRarity().toLowerCase()) {
            case "legendary" -> "§6[传说] ";
            case "epic" -> "§d[史诗] ";
            case "rare" -> "§b[稀有] ";
            case "uncommon" -> "§2[优秀] ";
            default -> "§f[普通] ";
        };

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(colorPrefix + entry.getItemName()));

        List<String> lore = List.of(
                "§7====================",
                "§e✦ 抽卡历史存根 ✦",
                "§7所属卡池: §f" + entry.getPoolName(),
                "§7获得物品: §f" + entry.getItemName(),
                "§7出货品质: " + getRarityName(entry.getRarity()),
                "§7抽取时间: §7" + entry.getTimestamp(),
                "§7===================="
        );
        
        List<Component> compLore = new ArrayList<>();
        for (String s : lore) {
            compLore.add(Component.literal(s));
        }
        stack.set(DataComponents.LORE, new ItemLore(compLore));

        return stack;
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

    private ItemStack getBorderPane(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "legendary" -> new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);  // 传说用橙色玻璃
            case "epic" -> new ItemStack(Items.PURPLE_STAINED_GLASS_PANE);       // 史诗用紫色玻璃
            case "rare" -> new ItemStack(Items.BLUE_STAINED_GLASS_PANE);         // 稀有用蓝色玻璃
            case "uncommon" -> new ItemStack(Items.GREEN_STAINED_GLASS_PANE);    // 优秀用绿色玻璃
            default -> new ItemStack(Items.WHITE_STAINED_GLASS_PANE);           // 普通用白色玻璃
        };
    }

    private String getRarityColor(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "legendary" -> "§6";
            case "epic" -> "§d";
            case "rare" -> "§b";
            case "uncommon" -> "§2";
            default -> "§f";
        };
    }

    private ItemStack createBorderItem(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        List<String> lore = List.of(
                "§7====================",
                "§7✦ 抽卡历史存根边框 ✦",
                "§7点击中间的真实物品可查看详情",
                "§7===================="
        );
        List<Component> compLore = new ArrayList<>();
        for (String s : lore) {
            compLore.add(Component.literal(s));
        }
        stack.set(DataComponents.LORE, new ItemLore(compLore));
        return stack;
    }
}
