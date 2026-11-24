package me.tuanzi.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class CreativeCoreItem extends Item {
    public static final String TAG_INTERVAL_TICKS = "IntervalTicks";
    public static final String TAG_SHOTS_PER_CYCLE = "ShotsPerCycle";
    public static final int DEFAULT_INTERVAL_TICKS = 5;
    public static final int DEFAULT_SHOTS_PER_CYCLE = 1;
    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int MAX_INTERVAL_TICKS = 1200;
    public static final int MIN_SHOTS_PER_CYCLE = 1;
    public static final int MAX_SHOTS_PER_CYCLE = 64;

    public CreativeCoreItem(Properties properties) {
        super(properties);
    }

    public static int clampIntervalTicks(int ticks) {
        return Math.clamp(ticks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
    }

    public static int clampShotsPerCycle(int shots) {
        return Math.clamp(shots, MIN_SHOTS_PER_CYCLE, MAX_SHOTS_PER_CYCLE);
    }

    public static int getIntervalTicks(ItemStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT_INTERVAL_TICKS;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return DEFAULT_INTERVAL_TICKS;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_INTERVAL_TICKS)) {
            return DEFAULT_INTERVAL_TICKS;
        }
        return clampIntervalTicks(tag.getIntOr(TAG_INTERVAL_TICKS, DEFAULT_INTERVAL_TICKS));
    }

    public static int getShotsPerCycle(ItemStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT_SHOTS_PER_CYCLE;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return DEFAULT_SHOTS_PER_CYCLE;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_SHOTS_PER_CYCLE)) {
            return DEFAULT_SHOTS_PER_CYCLE;
        }
        return clampShotsPerCycle(tag.getIntOr(TAG_SHOTS_PER_CYCLE, DEFAULT_SHOTS_PER_CYCLE));
    }

    public static void setCannonSpeed(ItemStack stack, int intervalTicks, int shotsPerCycle) {
        if (stack.isEmpty()) {
            return;
        }
        int clampedInterval = clampIntervalTicks(intervalTicks);
        int clampedShots = clampShotsPerCycle(shotsPerCycle);
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putInt(TAG_INTERVAL_TICKS, clampedInterval);
        tag.putInt(TAG_SHOTS_PER_CYCLE, clampedShots);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            me.tuanzi.client.gui.screens.CreativeCoreScreenHelper.openScreen(
                    hand,
                    getIntervalTicks(stack),
                    getShotsPerCycle(stack)
            );
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("tooltip.tuanzis_mod.creative_core.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable(
                "tooltip.tuanzis_mod.creative_core.speed",
                getIntervalTicks(stack),
                getShotsPerCycle(stack)
        ).withStyle(ChatFormatting.AQUA));
        tooltipComponents.accept(Component.translatable("tooltip.tuanzis_mod.creative_core.usage").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
