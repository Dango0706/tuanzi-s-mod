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

public class GachaFateConfirmMenu extends ChestMenu {
    private final GachaPool pool;
    private final GachaPoolItem targetItem;
    private final String rarity;
    private final ServerPlayer player;
    private final PlayerGachaState state;
    private final boolean isCancel;

    public GachaFateConfirmMenu(int containerId, Inventory playerInventory, GachaPool pool, GachaPoolItem targetItem, String rarity, boolean isCancel) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(27), 3);
        this.pool = pool;
        this.targetItem = targetItem;
        this.rarity = rarity;
        this.player = (ServerPlayer) playerInventory.player;
        this.state = PlayerGachaManager.getOrCreatePlayerState(player.getUUID());
        this.isCancel = isCancel;

        refreshSlots();
    }

    public void refreshSlots() {
        Container container = this.getContainer();
        // 1. 先用灰色玻璃板填充背景
        ItemStack grayPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        grayPane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 27; i++) {
            container.setItem(i, grayPane.copy());
        }

        // 2. 绘制确认按钮 (Slot 11)
        ItemStack confirmButton = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
        confirmButton.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l✔ 确认"));
        List<Component> confirmLore = new ArrayList<>();
        if (isCancel) {
            confirmLore.add(Component.literal("§7点击取消对该物品的定轨"));
            confirmLore.add(Component.literal("§7取消后，当前的命定值将 §c清零§7。"));
        } else {
            confirmLore.add(Component.literal("§7点击将该物品设为定轨目标"));
            confirmLore.add(Component.literal("§7设置后，当前的命定值将 §c清零§7。"));
        }
        confirmButton.set(DataComponents.LORE, new ItemLore(confirmLore));
        container.setItem(11, confirmButton);

        // 3. 绘制展示物品 (Slot 13)
        ItemStack displayStack = targetItem.getItem().copy();
        ItemLore originalLore = displayStack.get(DataComponents.LORE);
        List<Component> displayLore = new ArrayList<>();
        if (isCancel) {
            displayLore.add(Component.literal("§c§l是否取消此定轨目标？"));
        } else {
            displayLore.add(Component.literal("§e§l是否将此物品设为定轨目标？"));
        }
        displayLore.add(Component.literal(""));
        if (originalLore != null) {
            displayLore.addAll(originalLore.lines());
        }
        displayStack.set(DataComponents.LORE, new ItemLore(displayLore));
        container.setItem(13, displayStack);

        // 4. 绘制返回按钮 (Slot 15)
        ItemStack backButton = new ItemStack(Items.RED_STAINED_GLASS_PANE);
        backButton.set(DataComponents.CUSTOM_NAME, Component.literal("§c§l✘ 返回"));
        List<Component> backLore = new ArrayList<>();
        backLore.add(Component.literal("§7点击返回卡池预览"));
        backLore.add(Component.literal("§7不会有任何数据被修改"));
        backButton.set(DataComponents.LORE, new ItemLore(backLore));
        container.setItem(15, backButton);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex < 0 || slotIndex >= 63) { // 27 箱子槽 + 36 玩家背包槽
            return;
        }

        // 左键点击 Slot 11 (确认)
        if (slotIndex == 11 && buttonNum == 0 && containerInput == ContainerInput.PICKUP) {
            if (isCancel) {
                state.setFateAnchor(pool.getPoolId(), rarity, null);
                this.player.sendSystemMessage(Component.literal("§a定轨目标已取消，命定值已清零。"));
            } else {
                state.setFateAnchor(pool.getPoolId(), rarity, targetItem.getId());
                this.player.sendSystemMessage(Component.literal("§a定轨目标已设为【" + targetItem.getItem().getHoverName().getString() + "】，当前命定值已清零。"));
            }
            openPreviewMenu();
        }
        // 左键点击 Slot 15 (返回)
        else if (slotIndex == 15 && buttonNum == 0 && containerInput == ContainerInput.PICKUP) {
            openPreviewMenu();
        }

        this.broadcastFullState();
    }

    private void openPreviewMenu() {
        this.player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, playerInv, p) -> new GachaPreviewMenu(id, playerInv, pool),
                Component.literal("§6卡池预览 - " + pool.getPoolName())
        ));
    }
}
