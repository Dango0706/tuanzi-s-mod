package me.tuanzi.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;

public class StructureBlueprintItem extends Item {

    public StructureBlueprintItem(Properties properties) {
        super(properties.stacksTo(1));
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

            if (!tag.contains("Width")) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.invalid").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }

            boolean ghostPlaced = tag.getBooleanOr("GhostPlaced", false);
            if (!ghostPlaced) {
                // 在点击方块的位置（朝上偏移 1 格）生成虚影
                BlockPos targetPos = clickPos.relative(context.getClickedFace());
                tag.putBoolean("GhostPlaced", true);
                tag.putInt("GhostX", targetPos.getX());
                tag.putInt("GhostY", targetPos.getY());
                tag.putInt("GhostZ", targetPos.getZ());
                tag.putInt("OffsetX", 0);
                tag.putInt("OffsetY", 0);
                tag.putInt("OffsetZ", 0);
                tag.putInt("Rotation", 0); // 0, 1, 2, 3
                tag.putBoolean("Mirrored", false);

                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_placed", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN));
            } else {
                // 已生成虚影时，再次点击将其清除
                tag.remove("GhostPlaced");
                tag.remove("GhostX");
                tag.remove("GhostY");
                tag.remove("GhostZ");
                tag.remove("OffsetX");
                tag.remove("OffsetY");
                tag.remove("OffsetZ");
                tag.remove("Rotation");
                tag.remove("Mirrored");

                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_cleared").withStyle(ChatFormatting.YELLOW));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
            if (tag.getBooleanOr("GhostPlaced", false)) {
                tag.remove("GhostPlaced");
                tag.remove("GhostX");
                tag.remove("GhostY");
                tag.remove("GhostZ");
                tag.remove("OffsetX");
                tag.remove("OffsetY");
                tag.remove("OffsetZ");
                tag.remove("Rotation");
                tag.remove("Mirrored");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_cleared").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Width")) {
                int w = tag.getIntOr("Width", 0);
                int h = tag.getIntOr("Height", 0);
                int l = tag.getIntOr("Length", 0);
                int[] blocks = tag.getIntArray("Blocks").orElse(new int[0]);
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.size", w, h, l).withStyle(ChatFormatting.GRAY));
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.blocks", blocks.length).withStyle(ChatFormatting.GRAY));

                // 实体列表显示
                if (tag.contains("Entities")) {
                    net.minecraft.nbt.ListTag entitiesTag = tag.getListOrEmpty("Entities");
                    if (!entitiesTag.isEmpty()) {
                        builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.entities", entitiesTag.size()).withStyle(ChatFormatting.DARK_PURPLE));
                        for (int i = 0; i < entitiesTag.size(); i++) {
                            CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(i);
                            String id = entityNbt.getStringOr("id", "");
                            String displayName = id;
                            if (id.contains(":")) {
                                displayName = id.substring(id.indexOf(":") + 1);
                            }
                            net.minecraft.network.chat.Component entityName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(id))
                                    .map(net.minecraft.world.entity.EntityType::getDescription)
                                    .orElse(Component.literal(displayName));
                            
                            net.minecraft.nbt.ListTag posList = entityNbt.getListOrEmpty("Pos");
                            if (posList.size() >= 3) {
                                double rx = posList.getDoubleOr(0, 0.0);
                                double ry = posList.getDoubleOr(1, 0.0);
                                double rz = posList.getDoubleOr(2, 0.0);
                                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.entity_entry", entityName, rx, ry, rz).withStyle(ChatFormatting.LIGHT_PURPLE));
                            }
                        }
                    }
                }
                if (tag.getBooleanOr("GhostPlaced", false)) {
                    builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.ghost_active", 
                            tag.getIntOr("GhostX", 0) + tag.getIntOr("OffsetX", 0),
                            tag.getIntOr("GhostY", 0) + tag.getIntOr("OffsetY", 0),
                            tag.getIntOr("GhostZ", 0) + tag.getIntOr("OffsetZ", 0)
                    ).withStyle(ChatFormatting.AQUA));
                }
            } else {
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.empty").withStyle(ChatFormatting.RED));
            }
        } else {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.empty").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
    }
}
