package me.tuanzi.item;

import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.inventory.GoldenPocketMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class GoldenPocketItem extends Item {
    public GoldenPocketItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * 判断物品是否禁止放入黄金口袋（防套娃与防潜影盒）
     */
    public static boolean isForbiddenInPocket(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 1. 禁止再次放入黄金口袋
        if (stack.is(ModItems.GOLDEN_POCKET)) {
            return true;
        }
        // 2. 禁止放入潜影盒 (标签检查)
        try {
            if (stack.is(ItemTags.SHULKER_BOXES)) {
                return true;
            }
        } catch (Exception ignored) {
            // 单元测试或离线环境中 tags 未绑定时跳过
        }
        // 3. 禁止放入潜影盒 (方块实例检查)
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            int slotIndex = (hand == InteractionHand.MAIN_HAND) ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;

            SimpleContainer container = new SimpleContainer(GoldenPocketMenu.CONTAINER_SIZE);
            ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            contents.copyInto(container.getItems());

            Component title = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName() : Component.translatable("container.tuanzis_mod.golden_pocket");

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, playerInv, p) -> new GoldenPocketMenu(containerId, playerInv, container, stack, slotIndex),
                    title
            ));

            ModLog.debug(player, null, "玩家打开了黄金口袋，手持槽位: " + slotIndex);
        }
        return InteractionResult.SUCCESS;
    }
}
