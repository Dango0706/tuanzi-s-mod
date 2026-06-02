package me.tuanzi.gacha.gui;

import me.tuanzi.gacha.GachaPool;
import me.tuanzi.gacha.GachaPoolItem;
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
import java.util.UUID;

public class GachaPoolEditMenu extends ChestMenu {
    private final GachaPool pool;
    private final ServerPlayer player;
    private String currentRarity = "legendary";
    private int currentPage = 0;

    public GachaPoolEditMenu(int containerId, Inventory playerInventory, GachaPool pool) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
        this.pool = pool;
        this.player = (ServerPlayer) playerInventory.player;
        refreshSlots();
    }

    public void refreshSlots() {
        Container container = this.getContainer();
        // 1. 清空所有槽位
        for (int i = 0; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // 2. 绘制背景与切板 (行 1: Slot 0-8)
        container.setItem(0, createNamedItem(Items.YELLOW_GLAZED_TERRACOTTA, "§6★ 传说品质物品 (Legendary) §7[点击切换]"));
        container.setItem(1, createNamedItem(Items.PURPLE_GLAZED_TERRACOTTA, "§d★ 史诗品质物品 (Epic) §7[点击切换]"));
        container.setItem(2, createNamedItem(Items.BLUE_GLAZED_TERRACOTTA, "§b★ 稀有品质物品 (Rare) §7[点击切换]"));
        container.setItem(3, createNamedItem(Items.GREEN_GLAZED_TERRACOTTA, "§2★ 优秀品质物品 (Uncommon) §7[点击切换]"));
        container.setItem(4, createNamedItem(Items.WHITE_GLAZED_TERRACOTTA, "§f★ 普通品质物品 (Common) §7[点击切换]"));

        ItemStack grayGlass = createNamedItem(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 5; i < 9; i++) {
            container.setItem(i, grayGlass);
        }

        // 行 2 (Slot 9-17)：展示与背景
        for (int i = 9; i < 18; i++) {
            if (i == 13) {
                List<String> lore = List.of(
                        "§7卡池名称: §f" + pool.getPoolName(),
                        "§7当前稀有度分类: " + getRarityName(currentRarity),
                        "§7当前分类物品总数: §e" + pool.getListByRarity(currentRarity).size() + "§7 个"
                );
                container.setItem(i, createDetailedItem(Items.BOOK, "§6✦ 卡池信息概览 ✦", lore));
            } else {
                container.setItem(i, grayGlass);
            }
        }

        // 行 5 (Slot 36-44)：操作区
        for (int i = 36; i < 45; i++) {
            if (i == 38) {
                container.setItem(i, createNamedItem(Items.EMERALD_BLOCK, "§a§l✔ 保存并重新装载"));
            } else if (i == 40) {
                container.setItem(i, createNamedItem(Items.ANVIL, "§e§l⚒ 添加主手持有的物品 §7(默认权重 10)"));
            } else if (i == 42) {
                container.setItem(i, createNamedItem(Items.CHEST, "§b§l📥 从背包批量导入"));
            } else {
                container.setItem(i, grayGlass);
            }
        }

        // 行 6 (Slot 45-53)：翻页控制
        for (int i = 45; i < 54; i++) {
            if (i == 45) {
                container.setItem(i, createNamedItem(Items.ARROW, "§e◀ 上一页"));
            } else if (i == 49) {
                List<GachaPoolItem> items = pool.getListByRarity(currentRarity);
                int totalPages = (int) Math.ceil((double) items.size() / 18);
                if (totalPages == 0) totalPages = 1;
                container.setItem(i, createNamedItem(Items.BOOK, "§6当前页码: §e" + (currentPage + 1) + " §7/ §e" + totalPages));
            } else if (i == 53) {
                container.setItem(i, createNamedItem(Items.ARROW, "§e下一页 ▶"));
            } else {
                container.setItem(i, grayGlass);
            }
        }

        // 3. 填充当前页的物品 (行 3-4: Slot 18-35，共18个格)
        List<GachaPoolItem> items = pool.getListByRarity(currentRarity);
        int startIdx = currentPage * 18;
        for (int i = 0; i < 18; i++) {
            int currentItemIdx = startIdx + i;
            int targetSlot = 18 + i;
            if (currentItemIdx < items.size()) {
                GachaPoolItem poolItem = items.get(currentItemIdx);
                ItemStack displayStack = poolItem.getItem().copy();
                
                // 给展示物品的 Lore 追加权重信息
                ItemLore originalLoreObj = displayStack.get(DataComponents.LORE);
                List<Component> newLoreList = new ArrayList<>();
                if (originalLoreObj != null) {
                    newLoreList.addAll(originalLoreObj.lines());
                }
                newLoreList.add(Component.literal(""));
                newLoreList.add(Component.literal("§7===================="));
                newLoreList.add(Component.literal("§7当前权重: §a" + poolItem.getWeight()));
                newLoreList.add(Component.literal("§e左键 §7- §b微调权重"));
                newLoreList.add(Component.literal("§e右键 §7- §c删除物品"));
                newLoreList.add(Component.literal("§e中键/Shift §7- §a复制到背包"));
                newLoreList.add(Component.literal("§7===================="));
                
                displayStack.set(DataComponents.LORE, new ItemLore(newLoreList));
                container.setItem(targetSlot, displayStack);
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex < 0 || slotIndex >= 90) {
            return;
        }

        // 拦截点击箱子区域
        if (slotIndex < 54) {
            // 点击行 1 (Slot 0-4) 切换稀有度
            if (slotIndex >= 0 && slotIndex <= 4) {
                switch (slotIndex) {
                    case 0 -> currentRarity = "legendary";
                    case 1 -> currentRarity = "epic";
                    case 2 -> currentRarity = "rare";
                    case 3 -> currentRarity = "uncommon";
                    case 4 -> currentRarity = "common";
                }
                currentPage = 0;
                refreshSlots();
            }
            // 点击操作控制区 (Slot 38, 40, 42)
            else if (slotIndex == 38) {
                // 保存并重载
                PoolManager.savePoolSafe(pool, this.player.level().getServer().registryAccess());
                PoolManager.backupPoolSafe(pool, this.player.level().getServer().registryAccess());
                PoolManager.loadAllPools(this.player.level().getServer().registryAccess());
                this.player.sendSystemMessage(Component.literal("§a[!] 卡池配置已成功热重载并防灾保存！"));
                this.player.closeContainer();
                return;
            } else if (slotIndex == 40) {
                // 添加手持物品
                ItemStack hand = this.player.getMainHandItem();
                if (hand.isEmpty()) {
                    this.player.sendSystemMessage(Component.literal("§c[!] 你手上没有拿着任何物品！"));
                } else {
                    GachaPoolItem newItem = new GachaPoolItem(UUID.randomUUID().toString(), 10, new ArrayList<>(), hand.copy());
                    pool.getListByRarity(currentRarity).add(newItem);
                    refreshSlots();
                }
            } else if (slotIndex == 42) {
                // 打开批量导入 GUI
                net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                        (id, playerInv, p) -> new GachaImportMenu(id, playerInv, pool, currentRarity, this),
                        Component.literal("从生存背包批量导入物品")
                );
                this.player.openMenu(provider);
                return;
            }
            // 点击翻页控制区 (Slot 45, 53)
            else if (slotIndex == 45) {
                if (currentPage > 0) {
                    currentPage--;
                    refreshSlots();
                }
            } else if (slotIndex == 53) {
                List<GachaPoolItem> items = pool.getListByRarity(currentRarity);
                int totalPages = (int) Math.ceil((double) items.size() / 18);
                if (currentPage + 1 < totalPages) {
                    currentPage++;
                    refreshSlots();
                }
            }
            // 点击物品展示区 (Slot 18-35)
            else if (slotIndex >= 18 && slotIndex <= 35) {
                List<GachaPoolItem> items = pool.getListByRarity(currentRarity);
                int itemIdx = currentPage * 18 + (slotIndex - 18);
                if (itemIdx < items.size()) {
                    GachaPoolItem clickedItem = items.get(itemIdx);
                    
                    if (containerInput == ContainerInput.QUICK_MOVE || buttonNum == 2) {
                        // 复制物品到生存背包 (中键或 Shift 点击)
                        ItemStack giveStack = clickedItem.getItem().copy();
                        if (player.getInventory().add(giveStack)) {
                            player.sendSystemMessage(Component.literal("§a已复制物品 " + giveStack.getHoverName().getString() + " 到你的背包。"));
                        }
                    } else if (buttonNum == 0 && containerInput == ContainerInput.PICKUP) {
                        // 左键常规点击 -> 调出权重微调 GUI
                        net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                                (id, playerInv, p) -> new GachaWeightEditMenu(id, playerInv, pool, currentRarity, clickedItem, this),
                                Component.literal("修改物品权重")
                        );
                        player.openMenu(provider);
                        return;
                    } else if (buttonNum == 1 && containerInput == ContainerInput.PICKUP) {
                        // 右键常规点击 -> 直接删除物品
                        pool.getListByRarity(currentRarity).remove(clickedItem);
                        refreshSlots();
                        player.sendSystemMessage(Component.literal("§e已临时移除物品！需点击 §a[保存并重载] §e方可落盘更新。"));
                    }
                }
            }
            
            // 强行广播全状态，防止客户端拿取或发生任何残留闪烁
            this.broadcastFullState();
        } else {
            // 彻底拦截对生存背包的任何点击拖入/shift放入箱子行为
            this.broadcastFullState();
        }
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
