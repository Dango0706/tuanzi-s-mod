package me.tuanzi.item;

import me.tuanzi.world.inventory.CraftsmanCharmMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CraftsmanCharmItem extends Item {

    public CraftsmanCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            me.tuanzi.util.ModLog.debug(player, null, "玩家右击使用工匠护符，手部: " + hand.name() + "，准备打开便携合成界面。");
            player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CraftsmanCharmMenu(containerId, playerInventory, hand),
                Component.translatable("container.tuanzis_mod.craftsman_charm")
            ));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.craftsman_charm.tooltip"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 在物品栏中时，微微散发紫色光晕
        return true;
    }
}
