package me.tuanzi.item;

import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PaintBucketItem extends Item {

    public PaintBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);

            if (WorldSculptorsPenItem.isColorBlockSeries(state.getBlock())) {
                int color = WorldSculptorsPenItem.getBlockColor(level, pos, state, context.getClickLocation());
                ItemStack stack = context.getItemInHand();
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));

                if (!level.isClientSide()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.translatable("message.tuanzis_mod.paint_bucket.color_picked",
                                String.format("#%06X", color)), true);
                    }
                    ModLog.debug(player, null, "油漆桶快速吸取方块颜色: " + String.format("#%06X", color) + " at " + pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            int color = DyedItemColor.getOrDefault(stack, 0xFFFFFF);
            me.tuanzi.client.gui.screens.PaintBucketScreenHelper.openScreen(hand, color);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
