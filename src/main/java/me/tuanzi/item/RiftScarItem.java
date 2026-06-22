package me.tuanzi.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;

public class RiftScarItem extends Item {
    public RiftScarItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (!level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
            if (!tag.contains("WearValue")) {
                // 自动生成 0.0001 到 1.0 之间的随机磨损值
                double wear = 0.0001 + 0.9999 * level.getRandom().nextDouble();
                tag.putDouble("WearValue", wear);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                // 设置 CUSTOM_MODEL_DATA 组件，第 0 个 float 即为磨损度
                CustomModelData cmd = new CustomModelData(
                    List.of((float) wear), List.of(), List.of(), List.of()
                );
                stack.set(DataComponents.CUSTOM_MODEL_DATA, cmd);

                // 打印调试日志
                me.tuanzi.util.ModLog.debug(entity, null, "【裂虚之痕】生成磨损度: " + String.format("%.4f", wear));
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        
        // 读取磨损度
        double wearValue = 0.0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("WearValue")) {
                wearValue = tag.getDoubleOr("WearValue", 0.0);
            }
        }
        
        // 播放末影传送/虚空音效，音调随磨损度加深而愈发低沉，符合虚空崩解崩落细碎虚空尘/残响暗影的设定
        float pitch = 1.6f - (float) wearValue * 1.1f;
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45f, pitch);

        // 仅在服务器端播放虚空粒子效果
        if (!target.level().isClientSide() && target.level() instanceof ServerLevel serverLevel) {
            // 根据不同的磨损度，释放不同密度的虚空传送门/传送粒子，以及龙息粒子
            int particleCount = 5 + (int)(wearValue * 15);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1.0, target.getZ(),
                particleCount, 0.2, 0.3, 0.2, 0.1);
            
            if (wearValue >= 0.6) {
                // 崩解和残响阶段产生女巫紫色粒子（虚空尘）
                serverLevel.sendParticles(ParticleTypes.WITCH,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    6, 0.15, 0.15, 0.15, 0.02);
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
                    statusKey = "item.tuanzis_mod.rift_scar.status.1";
                    descKey = "item.tuanzis_mod.rift_scar.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.rift_scar.status.2";
                    descKey = "item.tuanzis_mod.rift_scar.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.rift_scar.status.3";
                    descKey = "item.tuanzis_mod.rift_scar.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.rift_scar.status.4";
                    descKey = "item.tuanzis_mod.rift_scar.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.rift_scar.status.5";
                    descKey = "item.tuanzis_mod.rift_scar.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.unidentified"));
            }

            // 故事文本（Lore）
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.lore.3"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.lore.4"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.lore.5"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.rift_scar.shift_hint"));
        }
    }
}
