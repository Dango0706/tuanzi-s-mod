package me.tuanzi.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

public class TideCleaverItem extends Item {
    public TideCleaverItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (!level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
            boolean changed = false;

            if (!tag.contains("WearValue")) {
                // 自动生成 0.0001 到 1.0 之间的随机数
                double wear = 0.0001 + 0.9999 * level.getRandom().nextDouble();
                tag.putDouble("WearValue", wear);

                // 设置 CUSTOM_MODEL_DATA 组件，第 0 个 float 即为磨损度
                CustomModelData cmd = new CustomModelData(
                    List.of((float) wear), List.of(), List.of(), List.of()
                );
                stack.set(DataComponents.CUSTOM_MODEL_DATA, cmd);
                changed = true;

                me.tuanzi.util.ModLog.debug(entity, null, "【潮汐切割者】生成磨损度: " + String.format("%.4f", wear));
            }

            // 如果遗留了旧的倒计时数据，将其清除以保持整洁，但仅执行一次，之后不再每 tick 触发改变
            if (tag.contains("BurstCooldown")) {
                tag.remove("BurstCooldown");
                changed = true;
            }

            if (changed) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    private boolean isShiftDown() {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            return me.tuanzi.client.ClientTooltipHelper.isShiftDown();
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);

        if (isShiftDown()) {
            double wearValue = -1.0;
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("WearValue")) {
                    wearValue = tag.getDoubleOr("WearValue", 0.0);
                }
            }

            if (wearValue >= 0.0) {
                // 磨损度百分比与对应的外观描述
                String statusKey;
                String descKey;
                if (wearValue < 0.2) {
                    statusKey = "item.tuanzis_mod.tide_cleaver.status.1";
                    descKey = "item.tuanzis_mod.tide_cleaver.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.tide_cleaver.status.2";
                    descKey = "item.tuanzis_mod.tide_cleaver.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.tide_cleaver.status.3";
                    descKey = "item.tuanzis_mod.tide_cleaver.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.tide_cleaver.status.4";
                    descKey = "item.tuanzis_mod.tide_cleaver.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.tide_cleaver.status.5";
                    descKey = "item.tuanzis_mod.tide_cleaver.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.unidentified"));
            }

            // 故事文本（Lore）
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.lore.3"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.lore.4"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.tide_cleaver.shift_hint"));
        }
    }
}
