package me.tuanzi.gacha.gui;

import me.tuanzi.gacha.GachaPool;
import me.tuanzi.gacha.GachaPoolItem;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;

public class GachaImportMenu extends ChestMenu {
    public static final class ImportSessionState {
        public final GachaPool pool;
        public final String rarity;
        public final String parentRarity;
        public final int parentPage;
        public final NonNullList<ItemStack> stagedItems = NonNullList.withSize(27, ItemStack.EMPTY);
        public final Map<Integer, Integer> tempWeights = new HashMap<>();

        public ImportSessionState(GachaPool pool, String rarity, String parentRarity, int parentPage) {
            this.pool = pool;
            this.rarity = rarity;
            this.parentRarity = (parentRarity != null && !parentRarity.isEmpty()) ? parentRarity : "legendary";
            this.parentPage = Math.max(0, parentPage);
        }
    }

    private final ImportSessionState state;
    private final ServerPlayer player;

    public GachaImportMenu(int containerId, Inventory playerInventory, GachaPool pool, String rarity, GachaPoolEditMenu parentMenu) {
        this(containerId, playerInventory, new ImportSessionState(
                pool,
                rarity,
                parentMenu != null ? parentMenu.getCurrentRarity() : rarity,
                parentMenu != null ? parentMenu.getCurrentPage() : 0
        ));
    }

    public GachaImportMenu(int containerId, Inventory playerInventory, ImportSessionState state) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(54), 6);
        this.state = state;
        this.player = (ServerPlayer) playerInventory.player;

        // 初始化功能控制按钮与待导入区
        refreshSlots();
    }

    public ImportSessionState getSessionState() {
        return this.state;
    }

    public void refreshSlots() {
        Container container = this.getContainer();

        // 我们只在非 0-26 的格子填充装饰与控制项。因为 0-26 是由玩家放置的物品。
        // 第 4 行 (Slot 27-35)：分隔与功能
        ItemStack grayPane = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        grayPane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));

        for (int i = 27; i < 36; i++) {
            if (i == 35) {
                container.setItem(i, createNamedItem(Items.HOPPER, "§6§l一键装载生存背包的所有物品"));
            } else {
                container.setItem(i, grayPane.copy());
            }
        }

        // 第 5 行 (Slot 36-44)：装饰与信息
        for (int i = 36; i < 45; i++) {
            if (i == 40) {
                List<String> lore = List.of(
                        "§7当前目标卡池: §f" + state.pool.getPoolName(),
                        "§7目标稀有度: " + getRarityName(state.rarity),
                        "§7操作说明:",
                        "  §f- 从下方背包中拖拽/Shift点击物品放入上方缓存区",
                        "  §f- 左键点击上方缓存物品: 微调批量导入的该物品权重",
                        "  §f- 右键点击上方缓存物品: 从导入列表中清除此项",
                        "  §f- 中键点击上方缓存物品: 复制此物品到背包"
                );
                container.setItem(i, createDetailedItem(Items.BOOK, "§e✦ 批量导入说明 ✦", lore));
            } else {
                container.setItem(i, grayPane.copy());
            }
        }

        // 第 6 行 (Slot 45-53)：控制操作
        for (int i = 45; i < 54; i++) {
            if (i == 45) {
                container.setItem(i, createNamedItem(Items.ARROW, "§7◀ 返回卡池编辑器"));
            } else if (i == 53) {
                container.setItem(i, createNamedItem(Items.EMERALD_BLOCK, "§a§l✔ 确认批量导入所有物品"));
            } else {
                container.setItem(i, grayPane.copy());
            }
        }

        // 刷新前 27 格物品以追加临时权重的 Lore 提示（不污染 stagedItems 中的纯净原物品）
        for (int i = 0; i < 27; i++) {
            ItemStack rawStack = state.stagedItems.get(i);
            if (rawStack.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
            } else {
                int weight = state.tempWeights.computeIfAbsent(i, k -> 10);

                ItemStack displayStack = rawStack.copy();
                ItemLore originalLore = displayStack.get(DataComponents.LORE);
                List<Component> newLore = new ArrayList<>();
                if (originalLore != null) {
                    newLore.addAll(originalLore.lines());
                }
                newLore.add(Component.literal(""));
                newLore.add(Component.literal("§7===================="));
                newLore.add(Component.literal("§7待导入临时权重: §a" + weight));
                newLore.add(Component.literal("§e左键 §7- §b微调导入权重"));
                newLore.add(Component.literal("§e右键 §7- §c移出待导入队列"));
                newLore.add(Component.literal("§e中键/Shift §7- §a复制到背包"));
                newLore.add(Component.literal("§7===================="));
                displayStack.set(DataComponents.LORE, new ItemLore(newLore));
                container.setItem(i, displayStack);
            }
        }
    }

    // 支持在被权重微调后返回并保持现场
    public void setTempWeight(int slotIdx, int weight) {
        state.tempWeights.put(slotIdx, weight);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index >= 54;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index >= 54;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        // 如果 slotIndex < 0 (例如 -999 表示点击槽位外部，比如丢弃物品或JEI特殊拖拽交互)，直接交给原版处理，决不进行拦截
        if (slotIndex < 0) {
            super.clicked(slotIndex, buttonNum, containerInput, player);
            return;
        }
        if (slotIndex >= 90) {
            return;
        }

        // 1. 点击了待导入区 (Slot 0-26)
        if (slotIndex < 27) {
            ItemStack carried = this.getCarried();
            ItemStack current = state.stagedItems.get(slotIndex);

            // 1.1 手中拿着物品 -> 放入/复制进去并清空手持以完成“拖放”
            if (!carried.isEmpty()) {
                // 复制一份纯净原物品放入暂存区（不消耗玩家本人的背包物品，极其安全！）
                ItemStack putItem = carried.copy();
                putItem.setCount(carried.getCount()); // 保留手中原本的正确数量
                state.stagedItems.set(slotIndex, putItem);
                state.tempWeights.put(slotIndex, 10); // 重置为此格子默认权重

                // 核心修复：成功拖入后清空手持物品，完成完美、自然的“拖入放下”操作，彻底解决 JEI 拖入和手持粘连卡死问题
                this.setCarried(ItemStack.EMPTY);

                refreshSlots();
            }
            // 1.2 手中没有拿着物品 -> 进行微调、删除或复制
            else if (!current.isEmpty()) {
                if (containerInput == ContainerInput.QUICK_MOVE || buttonNum == 2) {
                    // 中键或 Shift 点击复制到背包（保留原 Lore，无损拷贝）
                    ItemStack giveStack = current.copy();
                    giveStack.setCount(current.getCount());
                    if (player.getInventory().add(giveStack)) {
                        player.sendSystemMessage(Component.literal("§a已复制物品 " + giveStack.getHoverName().getString() + "x" + giveStack.getCount() + " 到你的背包。"));
                    }
                } else if (buttonNum == 0 && containerInput == ContainerInput.PICKUP) {
                    // 左键微调权重
                    int currentWeight = state.tempWeights.getOrDefault(slotIndex, 10);
                    ItemStack cleanStack = current.copy();

                    GachaPoolItem tempItem = new GachaPoolItem("temp_" + slotIndex, currentWeight, new ArrayList<>(), cleanStack);

                    net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                            (id, playerInv, p) -> new GachaWeightEditMenu(id, playerInv, state.pool, state.rarity, tempItem, this),
                            Component.literal("修改待导入物品权重")
                    );
                    player.openMenu(provider);
                    return;
                } else if (buttonNum == 1 && containerInput == ContainerInput.PICKUP) {
                    // 右键删除
                    state.stagedItems.set(slotIndex, ItemStack.EMPTY);
                    state.tempWeights.remove(slotIndex);
                    refreshSlots();
                }
            }
            this.broadcastFullState();
        }
        // 2. 点击了下层控制与功能区 (Slot 27-53)
        else if (slotIndex < 54) {
            // 2.1 漏斗一键装载 (Slot 35)
            if (slotIndex == 35) {
                int fillCount = 0;
                // 扫描生存背包 (Slots 9-35 以及 0-8 快捷栏)
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack invStack = inv.getItem(i);
                    if (!invStack.isEmpty()) {
                        // 寻找前 27 格的空位放入
                        int emptySlot = -1;
                        for (int j = 0; j < 27; j++) {
                            if (state.stagedItems.get(j).isEmpty()) {
                                emptySlot = j;
                                break;
                            }
                        }
                        if (emptySlot != -1) {
                            ItemStack copyItem = invStack.copy();
                            copyItem.setCount(invStack.getCount()); // 一键导入时保留原本堆叠的正确数量
                            state.stagedItems.set(emptySlot, copyItem);
                            state.tempWeights.put(emptySlot, 10);
                            fillCount++;
                        } else {
                            break; // 导入缓存区已满
                        }
                    }
                }
                player.sendSystemMessage(Component.literal("§a[!] 成功一键装载了 " + fillCount + " 件背包物品至导入缓存区！"));
                refreshSlots();
            }
            // 2.2 返回主界面 (Slot 45)
            else if (slotIndex == 45) {
                net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                        (id, playerInv, p) -> new GachaPoolEditMenu(id, playerInv, state.pool, state.parentRarity, state.parentPage),
                        Component.literal("活跃卡池编辑器")
                );
                player.openMenu(provider);
                return;
            }
            // 2.3 确认批量导入 (Slot 53)
            else if (slotIndex == 53) {
                int importedCount = 0;
                for (int i = 0; i < 27; i++) {
                    ItemStack stack = state.stagedItems.get(i);
                    if (!stack.isEmpty()) {
                        ItemStack cleanStack = stack.copy();

                        int weight = state.tempWeights.getOrDefault(i, 10);

                        GachaPoolItem newItem = new GachaPoolItem(
                                UUID.randomUUID().toString(),
                                weight,
                                new ArrayList<>(),
                                cleanStack
                        );
                        state.pool.getListByRarity(state.rarity).add(newItem);
                        importedCount++;
                    }
                }

                player.sendSystemMessage(Component.literal("§a[!] 成功批量导入了 " + importedCount + " 件物品至 " + getRarityName(state.rarity) + " 卡池！"));

                // 自动保存并备份到本地
                if (importedCount > 0) {
                    me.tuanzi.gacha.PoolManager.savePoolSafe(state.pool, this.player.level().getServer().registryAccess());
                    me.tuanzi.gacha.PoolManager.backupPoolSafe(state.pool, this.player.level().getServer().registryAccess());
                    player.sendSystemMessage(Component.literal("§a[!] 卡池配置已自动保存并备份到本地！"));
                }

                // 返回主卡池编辑器（使用新 containerId 构造全新实例）
                net.minecraft.world.SimpleMenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                        (id, playerInv, p) -> new GachaPoolEditMenu(id, playerInv, state.pool, state.parentRarity, state.parentPage),
                        Component.literal("活跃卡池编辑器")
                );
                player.openMenu(provider);
                return;
            }

            this.broadcastFullState();
        }
        // 3. 点击了生存背包 (Slot >= 54)
        else {
            // 支持 Shift 点击快速拷贝到顶部的导入缓存区
            if (containerInput == ContainerInput.QUICK_MOVE) {
                Slot slot = this.slots.get(slotIndex);
                if (slot != null && slot.hasItem()) {
                    ItemStack invStack = slot.getItem();
                    // 寻空位放入
                    int emptySlot = -1;
                    for (int j = 0; j < 27; j++) {
                        if (state.stagedItems.get(j).isEmpty()) {
                            emptySlot = j;
                            break;
                        }
                    }
                    if (emptySlot != -1) {
                        ItemStack copyItem = invStack.copy();
                        copyItem.setCount(invStack.getCount()); // Shift点击导入时，保留原本拥有的正确数量
                        state.stagedItems.set(emptySlot, copyItem);
                        state.tempWeights.put(emptySlot, 10);
                        refreshSlots();
                    } else {
                        player.sendSystemMessage(Component.literal("§c[!] 待导入区已满，无法继续放入物品。"));
                    }
                }
            } else if (containerInput != ContainerInput.PICKUP_ALL) {
                // 如果不是 Shift 快捷装载，则为普通在生存背包内的拖拽、移位、整理操作（禁止 PICKUP_ALL 吸取上方装饰/暂存物品）
                super.clicked(slotIndex, buttonNum, containerInput, player);
            }
            // 强行广播全状态，确保生存背包的点击整理完全同步
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
