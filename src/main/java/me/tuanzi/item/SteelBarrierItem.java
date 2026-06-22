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

public class SteelBarrierItem extends Item {
    public SteelBarrierItem(Properties properties) {
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
                me.tuanzi.util.ModLog.debug(entity, null, "【钢御壁垒】生成磨损度: " + String.format("%.4f", wear));
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);

        // 限制：攻击冷却器进度小于 95% 时，不叠加钢盾状态效果
        if (attacker instanceof net.minecraft.world.entity.player.Player player) {
            me.tuanzi.util.TideCleaverPlayerTracker tracker = (me.tuanzi.util.TideCleaverPlayerTracker) player;
            float strength = tracker.tuanzis_mod$getLastAttackStrength();
            if (strength < 0.95f) {
                return;
            }
        }

        double wearValue = 0.0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("WearValue")) {
                wearValue = tag.getDoubleOr("WearValue", 0.0);
            }
        }
        
        // 播放格挡音效，音调随磨损度加深而降低
        float pitch = 1.5f - (float) wearValue * 0.8f;
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
            SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, pitch);

        // 仅在服务器端处理状态效果
        if (!attacker.level().isClientSide()) {
            if (!attacker.hasEffect(ModStatusEffects.STEEL_SHIELD)) {
                // 叠加第 1 层 (amplifier = 0)
                attacker.addEffect(new MobEffectInstance(ModStatusEffects.STEEL_SHIELD, 100, 0, false, false, true));
                me.tuanzi.util.ModLog.debug(attacker, target, "【钢御壁垒】命中目标！叠加钢盾第 1 层。");
            } else {
                int amp = attacker.getEffect(ModStatusEffects.STEEL_SHIELD).getAmplifier();
                if (amp < 4) { // 最多叠加到 5 层 (amplifier = 4)
                    attacker.addEffect(new MobEffectInstance(ModStatusEffects.STEEL_SHIELD, 100, amp + 1, false, false, true));
                    me.tuanzi.util.ModLog.debug(attacker, target, "【钢御壁垒】命中目标！叠加钢盾第 " + (amp + 2) + " 层。");
                } else {
                    // 已叠满，刷新持续时间
                    attacker.addEffect(new MobEffectInstance(ModStatusEffects.STEEL_SHIELD, 100, 4, false, false, true));
                    me.tuanzi.util.ModLog.debug(attacker, target, "【钢御壁垒】命中目标！刷新已叠满的 5 层钢盾。");
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
                    statusKey = "item.tuanzis_mod.steel_barrier.status.1";
                    descKey = "item.tuanzis_mod.steel_barrier.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.steel_barrier.status.2";
                    descKey = "item.tuanzis_mod.steel_barrier.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.steel_barrier.status.3";
                    descKey = "item.tuanzis_mod.steel_barrier.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.steel_barrier.status.4";
                    descKey = "item.tuanzis_mod.steel_barrier.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.steel_barrier.status.5";
                    descKey = "item.tuanzis_mod.steel_barrier.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.unidentified"));
            }

            // 故事文本（Lore）
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.lore.3"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.lore.4"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.steel_barrier.shift_hint"));
        }
    }
}
