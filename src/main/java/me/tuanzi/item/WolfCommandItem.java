package me.tuanzi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class WolfCommandItem extends Item {
    public WolfCommandItem(Properties properties) {
        super(properties.stacksTo(1).durability(3));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.wolf_command.tooltip"));
    }
}
