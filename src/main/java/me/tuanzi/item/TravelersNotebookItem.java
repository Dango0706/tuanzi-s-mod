package me.tuanzi.item;

import me.tuanzi.init.ModItems;
import me.tuanzi.world.inventory.TravelersNotebookMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class TravelersNotebookItem extends Item {

    public TravelersNotebookItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // 潜行：打开内部存储界面
            if (!level.isClientSide()) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new TravelersNotebookMenu(containerId, playerInventory, hand),
                    Component.translatable("container.tuanzis_mod.travelers_notebook")
                ));
            }
            return InteractionResult.SUCCESS;
        } else {
            // 非潜行：打开水晶书本传送界面
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                // 向客户端发送 S2C 打开界面网络包
                ServerPlayNetworking.send(serverPlayer, new me.tuanzi.network.OpenBookScreenS2CPacket(hand == InteractionHand.MAIN_HAND));
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        
        int energy = getEnergy(stack);
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.travelers_notebook.tooltip.energy", energy, 64));
    }

    // ──────────────────────────────────────────────
    // 能量操作工具方法
    // ──────────────────────────────────────────────
    public static int getEnergy(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getIntOr("Energy", 0);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        CompoundTag tag = new CompoundTag();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt("Energy", Math.max(0, Math.min(64, energy)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
