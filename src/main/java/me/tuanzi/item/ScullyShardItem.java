package me.tuanzi.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

public class ScullyShardItem extends Item {
    public ScullyShardItem(Properties properties) {
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
                CustomModelData cmd = new CustomModelData(
                    List.of((float) wear), List.of(), List.of(), List.of()
                );
                stack.set(DataComponents.CUSTOM_MODEL_DATA, cmd);

                // 打印调试日志
                me.tuanzi.util.ModLog.debug(entity, null, "【幽匿裂片】生成磨损度: " + String.format("%.4f", wear));
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);

        double wearValue = 0.0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("WearValue")) {
                wearValue = tag.getDoubleOr("WearValue", 0.0);
            }
        }

        Level level = target.level();

        // 根据磨损度等级播放对应的命中音效
        if (wearValue < 0.2) {
            // 等级 I：深空回响，监守者低鸣清澈悠远
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_LISTENING, SoundSource.PLAYERS, 0.4f, 1.4f);
        } else if (wearValue < 0.4) {
            // 等级 II：微纹共鸣，不稳定的闪烁与杂乱的颤音
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_LISTENING_ANGRY, SoundSource.PLAYERS, 0.4f, 1.0f);
        } else if (wearValue < 0.6) {
            // 等级 III：脉动裂痕，撕裂为断续的混响
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.PLAYERS, 0.5f, 0.8f);
        } else if (wearValue < 0.8) {
            // 等级 IV：衰变尖啸，刺耳的、类似监守者受创时的破碎尖啸
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_HURT, SoundSource.PLAYERS, 0.6f, 1.2f);
        } else {
            // 等级 V：静默残片，沉闷的钝响
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.5f, 0.9f);
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
                String statusKey;
                String descKey;
                if (wearValue < 0.2) {
                    statusKey = "item.tuanzis_mod.scully_shard.status.1";
                    descKey = "item.tuanzis_mod.scully_shard.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.scully_shard.status.2";
                    descKey = "item.tuanzis_mod.scully_shard.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.scully_shard.status.3";
                    descKey = "item.tuanzis_mod.scully_shard.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.scully_shard.status.4";
                    descKey = "item.tuanzis_mod.scully_shard.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.scully_shard.status.5";
                    descKey = "item.tuanzis_mod.scully_shard.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.unidentified"));
            }

            // Lore 故事背景
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.lore.3"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.lore.4"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.scully_shard.shift_hint"));
        }
    }
}
