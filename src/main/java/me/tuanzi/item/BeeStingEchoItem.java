package me.tuanzi.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import me.tuanzi.init.ModStatusEffects;

import java.util.List;

public class BeeStingEchoItem extends Item {
    public BeeStingEchoItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (!level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
            if (!tag.contains("WearValue")) {
                // 自动生成 0.0001 到 1.0 之间的随机数
                double wear = 0.0001 + 0.9999 * level.getRandom().nextDouble();
                tag.putDouble("WearValue", wear);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                // 设置 CUSTOM_MODEL_DATA 组件，第 0 个 float 即为磨损度
                net.minecraft.world.item.component.CustomModelData cmd = new net.minecraft.world.item.component.CustomModelData(
                    List.of((float) wear), List.of(), List.of(), List.of()
                );
                stack.set(DataComponents.CUSTOM_MODEL_DATA, cmd);

                // 打印调试日志
                me.tuanzi.util.ModLog.debug(entity, null, "【蜂刺余响】生成磨损度: " + String.format("%.4f", wear));
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        
        // 播放蜂鸣声，音调随磨损度加深而愈发低沉
        double wearValue = 0.0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("WearValue")) {
                wearValue = tag.getDoubleOr("WearValue", 0.0);
            }
        }
        float pitch = 1.8f - (float) wearValue * 1.3f;
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.BEE_LOOP, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, pitch);

        // 仅在服务器端处理叠毒效果
        if (!target.level().isClientSide()) {
            // 如果目标处于冷却中，无法叠加蜂毒
            if (target.hasEffect(ModStatusEffects.BEE_POISON_COOLDOWN)) {
                return;
            }

            if (!target.hasEffect(ModStatusEffects.BEE_POISON)) {
                // 叠加第 1 层 (amplifier = 0)
                target.addEffect(new MobEffectInstance(ModStatusEffects.BEE_POISON, 100, 0, false, false, true));
                me.tuanzi.util.ModLog.debug(attacker, target, "【蜂刺余响】命中目标！叠加蜂毒第 1 层。");
            } else {
                int amp = target.getEffect(ModStatusEffects.BEE_POISON).getAmplifier();
                if (amp < 3) { // 最多叠加到 4 层，第 5 层 (amp = 4) 已经在 DamageCalculator 中直接引爆了
                    target.addEffect(new MobEffectInstance(ModStatusEffects.BEE_POISON, 100, amp + 1, false, false, true));
                    me.tuanzi.util.ModLog.debug(attacker, target, "【蜂刺余响】命中目标！叠加蜂毒第 " + (amp + 2) + " 层。");
                }
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
                    statusKey = "item.tuanzis_mod.bee_sting_echo.status.1";
                    descKey = "item.tuanzis_mod.bee_sting_echo.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.bee_sting_echo.status.2";
                    descKey = "item.tuanzis_mod.bee_sting_echo.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.bee_sting_echo.status.3";
                    descKey = "item.tuanzis_mod.bee_sting_echo.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.bee_sting_echo.status.4";
                    descKey = "item.tuanzis_mod.bee_sting_echo.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.bee_sting_echo.status.5";
                    descKey = "item.tuanzis_mod.bee_sting_echo.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.unidentified"));
            }

            // 故事文本（Lore）
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.lore.3"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.lore.4"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.bee_sting_echo.shift_hint"));
        }
    }
}
