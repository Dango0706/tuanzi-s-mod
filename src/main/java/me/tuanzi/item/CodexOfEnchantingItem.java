package me.tuanzi.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class CodexOfEnchantingItem extends Item {
    public CodexOfEnchantingItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.codex_of_enchanting.tooltip.line1").withStyle(ChatFormatting.GOLD));
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.codex_of_enchanting.tooltip.line2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.codex_of_enchanting.tooltip.line3").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
