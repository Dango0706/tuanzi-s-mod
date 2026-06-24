package me.tuanzi.item;

import me.tuanzi.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class BlankBlueprintItem extends Item {

    public BlankBlueprintItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos clickPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

            if (!tag.contains("pos1_x")) {
                // 设定第一个角点
                tag.putInt("pos1_x", clickPos.getX());
                tag.putInt("pos1_y", clickPos.getY());
                tag.putInt("pos1_z", clickPos.getZ());
                tag.putString("pos1_dim", level.dimension().identifier().toString());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.pos1_set", clickPos.getX(), clickPos.getY(), clickPos.getZ()).withStyle(ChatFormatting.GREEN));
            } else {
                // 校验维度
                String dim1 = tag.getStringOr("pos1_dim", "");
                String dim2 = level.dimension().identifier().toString();
                if (!dim1.equals(dim2)) {
                    player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.dim_mismatch").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }

                // 设定第二个角点
                tag.putInt("pos2_x", clickPos.getX());
                tag.putInt("pos2_y", clickPos.getY());
                tag.putInt("pos2_z", clickPos.getZ());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.pos2_set", clickPos.getX(), clickPos.getY(), clickPos.getZ()).withStyle(ChatFormatting.GREEN));

                // 发送确认信息，让玩家在聊天栏确认
                Component confirmBtn = Component.translatable("message.tuanzis_mod.blueprint.confirm_btn")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand("/tuanzis_mod_confirm_blueprint")));

                Component msg = Component.translatable("message.tuanzis_mod.blueprint.confirm_prompt")
                        .append(" ")
                        .append(confirmBtn);

                player.sendSystemMessage(msg);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide()) {
            // Shift + 右键空气清除所设定的角点
            stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.cleared").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
