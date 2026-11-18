package me.tuanzi.item;

import me.tuanzi.client.ClientTooltipHelper;
import me.tuanzi.entity.TrialDummyEntity;
import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.DamageCalculator;
import me.tuanzi.util.EarthEchoTracker;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 地脉定桩锤 (Earth Pile Hammer)
 * 基础攻击力 16，攻击速度 0.8。
 * 专属机制：定桩重击 (冷却>=80% 造成 100% 伤害并触发 2.5格 35% 伤害震荡波及 20% 减速 1.5s；<80% 造成 65% 伤害且自身获得 0.5s 缓慢I)
 * 地脉回响：定桩重击后 2 秒内静止，下一次攻击冷却恢复速度 +10%，锁存消耗。
 * 5 阶段随机磨损度系统 (星纹崭新、桩纹微裂、地脉失稳、核心裸露、余震将熄)。
 */
public class EarthPileHammerItem extends Item {

    public EarthPileHammerItem(Properties properties) {
        super(properties);
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

                ModLog.debug(entity, null, "【地脉定桩锤】生成磨损度: " + String.format("%.4f", wear));
            }
        }
    }

    /**
     * 触发定桩重击震荡波
     *
     * @param attacker                攻击者
     * @param target                  主目标
     * @param baseHammerAttackDamage 本次定桩重击读取的攻击力快照
     * @param wearValue               磨损度
     */
    public static void triggerEarthShockwave(LivingEntity attacker, LivingEntity target, float baseHammerAttackDamage, double wearValue) {
        if (target == null || target.level().isClientSide() || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = target.position();
        double radius = 2.5;
        AABB box = new AABB(
                center.x - radius, center.y - 1.5, center.z - radius,
                center.x + radius, center.y + 1.5, center.z + radius
        );

        List<LivingEntity> potentialTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == attacker || e == target) return false;
            if (!e.isAlive()) return false;
            if (e.isInvulnerable()) return false;

            // 队友与友军过滤
            if (e instanceof ServerPlayer targetPlayer && attacker instanceof ServerPlayer attackerPlayer) {
                if (ScavengerTeamManager.areTeammates(attackerPlayer, targetPlayer)) return false;
                if (!attackerPlayer.canHarmPlayer(targetPlayer)) return false;
            }

            // 驯服生物排除
            if (e instanceof TamableAnimal tamable && tamable.isTame()) {
                if (attacker instanceof Player p && tamable.isOwnedBy(p)) return false;
            }

            // 假人可受伤
            if (e instanceof TrialDummyEntity) return true;

            // 排除动物、村民、铁傀儡等友方生物
            if (e instanceof net.minecraft.world.entity.npc.villager.Villager || e instanceof net.minecraft.world.entity.animal.golem.IronGolem) {
                return false;
            }
            if (e instanceof net.minecraft.world.entity.animal.Animal) {
                return false;
            }

            return true;
        });

        // 按到中心点距离由近到远排序，相同按网络 ID 升序
        potentialTargets.sort(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(center))
                .thenComparingInt(Entity::getId));

        int targetLimit = Math.min(3, potentialTargets.size());
        List<LivingEntity> selectedTargets = potentialTargets.subList(0, targetLimit);

        float shockDamage = baseHammerAttackDamage * 0.35f;
        DamageSource shockSource = attacker instanceof Player player
                ? serverLevel.damageSources().playerAttack(player)
                : serverLevel.damageSources().mobAttack(attacker);

        ModLog.debug(attacker, target, "【地脉定桩锤·定桩重击】触发震荡波！中心: ("
                + String.format("%.2f, %.2f, %.2f", center.x, center.y, center.z)
                + ")，筛选到目标数: " + selectedTargets.size()
                + "，基础攻击力快照: " + String.format("%.2f", baseHammerAttackDamage)
                + "，单体震荡伤害: " + String.format("%.2f", shockDamage));

        try {
            DamageCalculator.IS_SHOCKWAVE_ATTACK.set(true);
            for (LivingEntity victim : selectedTargets) {
                float finalShockDmg = DamageCalculator.calculateDamage(shockDamage, shockSource, victim);
                boolean hurt = victim.hurtServer(serverLevel, shockSource, finalShockDmg);

                if (hurt && victim.isAlive()) {
                    // 试炼假人不施加减速
                    if (!(victim instanceof TrialDummyEntity)) {
                        boolean isBoss = isBossTarget(victim);
                        int amp = isBoss ? 0 : 1;
                        victim.addEffect(new MobEffectInstance(ModStatusEffects.EARTH_PULSE_SLOW, 30, amp, false, true, true));
                    }
                }
            }
        } finally {
            DamageCalculator.IS_SHOCKWAVE_ATTACK.set(false);
        }

        // 播放定桩音效与粒子
        float pitch = 1.2f - (float) wearValue * 0.6f;
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.2f, pitch);
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, pitch * 0.8f);

        // 震荡波地面碎裂与烟雾粒子
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState()),
                center.x, center.y + 0.1, center.z, 20, 1.2, 0.2, 1.2, 0.1);
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x, center.y + 0.2, center.z, 12, 1.0, 0.1, 1.0, 0.05);

        // 核心裸露 (>= 0.6) 时火星四溅
        if (wearValue >= 0.6) {
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    center.x, center.y + 0.3, center.z, 15, 0.8, 0.3, 0.8, 0.1);
        }
        // 余震将熄 (>= 0.8) 时灰烬与碎屑
        if (wearValue >= 0.8) {
            serverLevel.sendParticles(ParticleTypes.ASH,
                    center.x, center.y + 0.5, center.z, 25, 1.2, 0.4, 1.2, 0.05);
        }
    }

    public static boolean isBossTarget(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return true;
        if (entity instanceof me.tuanzi.entity.CinderSovereignEntity) return true;
        if (entity instanceof net.minecraft.world.entity.monster.warden.Warden) return true;
        if (entity instanceof net.minecraft.world.entity.monster.ElderGuardian) return true;
        return false;
    }

        private boolean isShiftDown() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientTooltipHelper.isShiftDown();
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
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
                    statusKey = "item.tuanzis_mod.earth_pile_hammer.status.1";
                    descKey = "item.tuanzis_mod.earth_pile_hammer.desc.1";
                } else if (wearValue < 0.4) {
                    statusKey = "item.tuanzis_mod.earth_pile_hammer.status.2";
                    descKey = "item.tuanzis_mod.earth_pile_hammer.desc.2";
                } else if (wearValue < 0.6) {
                    statusKey = "item.tuanzis_mod.earth_pile_hammer.status.3";
                    descKey = "item.tuanzis_mod.earth_pile_hammer.desc.3";
                } else if (wearValue < 0.8) {
                    statusKey = "item.tuanzis_mod.earth_pile_hammer.status.4";
                    descKey = "item.tuanzis_mod.earth_pile_hammer.desc.4";
                } else {
                    statusKey = "item.tuanzis_mod.earth_pile_hammer.status.5";
                    descKey = "item.tuanzis_mod.earth_pile_hammer.desc.5";
                }
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.earth_pile_hammer.wear", String.format("%.2f%%", wearValue * 100)));
                tooltipComponents.accept(Component.translatable(statusKey));
                tooltipComponents.accept(Component.translatable(descKey));
            } else {
                tooltipComponents.accept(Component.translatable("item.tuanzis_mod.earth_pile_hammer.unidentified"));
            }

            // 故事文本（Lore）
            tooltipComponents.accept(Component.literal(""));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.earth_pile_hammer.lore.1"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.earth_pile_hammer.lore.2"));
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.earth_pile_hammer.lore.3"));
        } else {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.earth_pile_hammer.shift_hint"));
        }
    }
}
