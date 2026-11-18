package me.tuanzi.item;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StarRiftBladeItem extends Item {
    public static final Identifier GAP_VERDICT_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath("tuanzis_mod", "gap_verdict_attack_speed");
    private static final Map<UUID, Integer> SPEED_BUFF_TICKS = new ConcurrentHashMap<>();

    public StarRiftBladeItem(Properties properties) {
        super(properties);
    }

    public static void applyGapVerdictSpeedBuff(LivingEntity entity, double bonusRatio, int durationTicks) {
        if (entity == null || entity.level().isClientSide()) return;
        AttributeInstance speedAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(GAP_VERDICT_SPEED_MODIFIER_ID);
            if (bonusRatio > 0.0) {
                speedAttr.addTransientModifier(new AttributeModifier(
                        GAP_VERDICT_SPEED_MODIFIER_ID, bonusRatio, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
                SPEED_BUFF_TICKS.put(entity.getUUID(), durationTicks);
            }
        }
    }

    public static void tickGapVerdictSpeedBuff(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        UUID uuid = entity.getUUID();
        Integer remaining = SPEED_BUFF_TICKS.get(uuid);
        if (remaining != null) {
            if (remaining <= 1) {
                SPEED_BUFF_TICKS.remove(uuid);
                AttributeInstance speedAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
                if (speedAttr != null) {
                    speedAttr.removeModifier(GAP_VERDICT_SPEED_MODIFIER_ID);
                }
            } else {
                SPEED_BUFF_TICKS.put(uuid, remaining - 1);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
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
                me.tuanzi.util.ModLog.debug(entity, null, "【星隙裁刃】生成磨损度: " + String.format("%.4f", wear));
            }

            if (entity instanceof LivingEntity livingEntity) {
                tickGapVerdictSpeedBuff(livingEntity);
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

        // 播放星隙音效，音调随磨损度加深而偏低
        float pitch = 1.5f - (float) wearValue * 0.7f;
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, pitch);

        // 仅在服务器端处理星隙粒子与机制
        if (!target.level().isClientSide() && target.level() instanceof ServerLevel serverLevel) {
            // 星芒粒子
            int particleCount = 6 + (int) (wearValue * 10);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    particleCount, 0.25, 0.35, 0.25, 0.05);

            if (wearValue >= 0.6) {
                // 星屑剥落与暗淡将熄阶段产生散落星屑（灰烬与烟雾）
                serverLevel.sendParticles(ParticleTypes.ASH,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        8, 0.2, 0.2, 0.2, 0.02);
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
                String statusKey;
                String descKey;
                if (wearValue < 0.2) {
                    statusKey = "item.tuanzis_mod.star_rift_blade.status.1";
                    descKey = "item.tuanzis_mod.star_rift_blade.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.star_rift_blade.status.2";
                    descKey = "item.tuanzis_mod.star_rift_blade.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.star_rift_blade.status.3";
                    descKey = "item.tuanzis_mod.star_rift_blade.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.star_rift_blade.status.4";
                    descKey = "item.tuanzis_mod.star_rift_blade.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.star_rift_blade.status.5";
                    descKey = "item.tuanzis_mod.star_rift_blade.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.unidentified"));
            }

            // 故事文本（Lore）
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.lore.3"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.lore.4"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.star_rift_blade.shift_hint"));
        }
    }
}
