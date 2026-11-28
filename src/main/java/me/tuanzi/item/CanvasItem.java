package me.tuanzi.item;

import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

/**
 * 画布物品：承载尺寸、2D/3D 类型、所有者、权限与体素统计等元数据，体素正文存于 painted/<uuid>.painted。
 * 不可直接放置在世界中，只能放入画板或 3D 打印机。
 */
public class CanvasItem extends Item {
    public CanvasItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag tooltipFlag
    ) {
        CanvasMetadata meta = CanvasMetadata.read(stack);
        String sizeDesc = meta.type() == CanvasType.VOXEL_3D
                ? (meta.size() + "×" + meta.size() + "×" + meta.size())
                : (meta.size() + "×" + meta.size());
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.size", sizeDesc).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.type",
                Component.translatable(meta.type().getTranslationKey())).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.voxels", meta.nonEmptyVoxels()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.permission",
                Component.translatable(meta.permission().getTranslationKey())).withStyle(ChatFormatting.YELLOW));
        if (!meta.ownerName().isEmpty()) {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.owner", meta.ownerName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (meta.hasPendingUpgrade()) {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.pending_upgrade").withStyle(ChatFormatting.GREEN));
        }
        if (meta.corrupted()) {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.corrupted").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
    }
}
