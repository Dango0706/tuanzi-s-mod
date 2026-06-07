package me.tuanzi.gacha.gui;

import me.tuanzi.gacha.GachaPool;
import me.tuanzi.gacha.GachaPoolItem;
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

public class GachaWeightEditMenu extends ChestMenu {
    private final GachaPool pool;
    private final String rarity;
    private final GachaPoolItem targetItem;
    private final ChestMenu parentMenu;
    private final ServerPlayer player;
    private int currentWeight;

    public GachaWeightEditMenu(int containerId, Inventory playerInventory, GachaPool pool, String rarity, GachaPoolItem targetItem, ChestMenu parentMenu) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(27), 3);
        this.pool = pool;
        this.rarity = rarity;
        this.targetItem = targetItem;
        this.parentMenu = parentMenu;
        this.player = (ServerPlayer) playerInventory.player;
        this.currentWeight = targetItem.getWeight();
        refreshSlots();
    }

    public void refreshSlots() {
        Container container = this.getContainer();
        // 1. 清空所有槽位
        for (int i = 0; i < 27; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // 2. 绘制背景 (灰色染色玻璃板)
        ItemStack grayPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        grayPane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 27; i++) {
            container.setItem(i, grayPane.copy());
        }

        // 3. 放置减少权重的控制块
        container.setItem(10, createNamedItem(Items.RED_STAINED_GLASS_PANE, "§c§l权重 -100"));
        container.setItem(11, createNamedItem(Items.ORANGE_STAINED_GLASS_PANE, "§6§l权重 -10"));
        container.setItem(12, createNamedItem(Items.YELLOW_STAINED_GLASS_PANE, "§e§l权重 -1"));

        // 4. 中心展示槽位
        ItemStack displayStack = targetItem.getItem().copy();
        ItemLore originalLore = displayStack.get(DataComponents.LORE);
        List<Component> displayLore = new ArrayList<>();
        if (originalLore != null) {
            displayLore.addAll(originalLore.lines());
        }
        displayLore.add(Component.literal(""));
        displayLore.add(Component.literal("§7===================="));
        displayLore.add(Component.literal("§7正在调整当前物品权重"));
        displayLore.add(Component.literal("§7当前微调值: §a" + currentWeight));
        displayLore.add(Component.literal("§7===================="));
        displayStack.set(DataComponents.LORE, new ItemLore(displayLore));
        container.setItem(13, displayStack);

        // 5. 放置增加权重的控制块
        container.setItem(14, createNamedItem(Items.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§l权重 +1"));
        container.setItem(15, createNamedItem(Items.BLUE_STAINED_GLASS_PANE, "§9§l权重 +10"));
        container.setItem(16, createNamedItem(Items.PURPLE_STAINED_GLASS_PANE, "§d§l权重 +100"));

        // 6. 功能操作栏
        container.setItem(20, createNamedItem(Items.EMERALD_BLOCK, "§a§l✔ 确认修改"));
        container.setItem(22, createNamedItem(Items.REDSTONE_BLOCK, "§c§l✖ 重置为默认权重 10"));
        container.setItem(24, createNamedItem(Items.BARRIER, "§7§l◀ 放弃修改并返回"));
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex < 0 || slotIndex >= 63) {
            return;
        }

        // 拦截点击箱子槽位
        if (slotIndex < 27) {
            // 点击微调
            if (slotIndex == 10) currentWeight = Math.max(1, currentWeight - 100);
            else if (slotIndex == 11) currentWeight = Math.max(1, currentWeight - 10);
            else if (slotIndex == 12) currentWeight = Math.max(1, currentWeight - 1);
            else if (slotIndex == 14) currentWeight = Math.min(10000, currentWeight + 1);
            else if (slotIndex == 15) currentWeight = Math.min(10000, currentWeight + 10);
            else if (slotIndex == 16) currentWeight = Math.min(10000, currentWeight + 100);
            
            // 功能按钮
            else if (slotIndex == 20) {
                // 确认修改
                if (parentMenu instanceof GachaImportMenu importMenu) {
                    // 如果来自批量导入，我们需要提取槽位并设置临时权重
                    String id = targetItem.getId();
                    if (id.startsWith("temp_")) {
                        try {
                            int slotIdx = Integer.parseInt(id.split("_")[1]);
                            importMenu.setTempWeight(slotIdx, currentWeight);
                            // 将修改过临时权重的物品放回导入缓存中，保持状态
                            importMenu.getContainer().setItem(slotIdx, targetItem.getItem().copy());
                        } catch (Exception ignored) {}
                    }
                } else {
                    targetItem.setWeight(currentWeight);
                    // 自动保存并备份到本地
                    me.tuanzi.gacha.PoolManager.savePoolSafe(pool, this.player.level().getServer().registryAccess());
                    me.tuanzi.gacha.PoolManager.backupPoolSafe(pool, this.player.level().getServer().registryAccess());
                    player.sendSystemMessage(Component.literal("§a[!] 卡池配置已自动保存并备份到本地！"));
                }
                player.sendSystemMessage(Component.literal("§a[!] 成功将权重修改为: " + currentWeight));
                returnToParent();
                return;
            } else if (slotIndex == 22) {
                // 重置为 10
                currentWeight = 10;
            } else if (slotIndex == 24) {
                // 放弃返回
                returnToParent();
                return;
            }

            refreshSlots();
            this.broadcastFullState();
        } else {
            // 彻底拦截对生存背包的任何点击拖入/shift放入箱子行为
            this.broadcastFullState();
        }
    }

    private void returnToParent() {
        if (parentMenu instanceof GachaImportMenu importMenu) {
            net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                    (id, playerInv, p) -> {
                        importMenu.refreshSlots();
                        return importMenu;
                    },
                    Component.literal("从生存背包批量导入物品")
            );
            player.openMenu(provider);
        } else if (parentMenu instanceof GachaPoolEditMenu editMenu) {
            net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                    (id, playerInv, p) -> {
                        editMenu.refreshSlots();
                        return editMenu;
                    },
                    Component.literal("活跃卡池编辑器")
            );
            player.openMenu(provider);
        } else {
            player.closeContainer();
        }
    }

    private ItemStack createNamedItem(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
