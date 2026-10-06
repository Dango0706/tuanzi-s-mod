package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ReservedChamberHelper;
import me.tuanzi.util.ReservedChamberProjectileAccessor;
import me.tuanzi.util.SeekingArrowAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin extends ProjectileWeaponItem {

    protected CrossbowItemMixin(Item.Properties properties) {
        super(properties);
    }

    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void onGetChargeDuration(ItemStack crossbow, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        int chamberLevel = ReservedChamberHelper.getReservedChamberLevel(user != null ? user.level() : null, crossbow);
        if (chamberLevel > 0) {
            // 拉弦时间额外延长 (1 + 0.25 * level) 秒 (20 + 5 * level ticks)
            int extraTicks = 20 + 5 * chamberLevel;
            cir.setReturnValue(cir.getReturnValue() + extraTicks);
        }
    }

    @Inject(method = "createProjectile", at = @At("RETURN"))
    private void onCreateProjectile(Level level, LivingEntity shooter, ItemStack heldItem, ItemStack projectile, boolean isCrit, CallbackInfoReturnable<Projectile> cir) {
        Projectile projectileEntity = cir.getReturnValue();

        // 预备弹仓：副箭伤害衰减传递
        CustomData customData = projectile.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains("ReservedChamberSubsequent")) {
                int chamberLvl = tag.getIntOr("ReservedChamberLevel", 1);
                float penaltyRate = (60.0f - 10.0f * chamberLvl) / 100.0f;
                if (projectileEntity instanceof ReservedChamberProjectileAccessor accessor) {
                    accessor.tuanzis_mod$setReservedChamberDamagePenalty(penaltyRate);
                    accessor.tuanzis_mod$setReservedChamberLevel(chamberLvl);
                    me.tuanzi.util.ModLog.debug(shooter, null, "【预备弹仓】发射副发弹药！等级: " + chamberLvl + "，副箭伤害降低: " + String.format("%.0f%%", penaltyRate * 100));
                }
            }
        }

        // 寻踪箭附魔逻辑
        if (projectileEntity instanceof SeekingArrowAccessor seekingArrow && shooter instanceof Player player) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var seekingEnch = lookup.getOrThrow(ModEnchantments.SEEKING_ARROW);
            if (EnchantmentHelper.getItemEnchantmentLevel(seekingEnch, heldItem) > 0) {
                // 进行射线检测以在服务端确定目标
                double maxDistance = 64.0;
                Vec3 eyePos = player.getEyePosition(1.0f);
                Vec3 viewVec = player.getViewVector(1.0f);
                Vec3 endPos = eyePos.add(viewVec.scale(maxDistance));
                
                AABB searchArea = player.getBoundingBox().expandTowards(viewVec.scale(maxDistance)).inflate(1.0);
                EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                    player,
                    eyePos,
                    endPos,
                    searchArea,
                    entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
                    maxDistance * maxDistance
                );
                
                if (entityHitResult != null) {
                    seekingArrow.tuanzis_mod$setSeekingTargetEntity(entityHitResult.getEntity());
                } else {
                    // 方块射线检测
                    BlockHitResult blockHitResult = level.clip(new net.minecraft.world.level.ClipContext(
                        eyePos,
                        endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                    ));
                    if (blockHitResult.getType() != HitResult.Type.MISS) {
                        seekingArrow.tuanzis_mod$setSeekingTargetPos(blockHitResult.getLocation());
                    } else {
                        seekingArrow.tuanzis_mod$setSeekingTargetPos(endPos);
                    }
                }
            }
        }
    }

    @Inject(method = "tryLoadProjectiles", at = @At("RETURN"))
    private static void onTryLoadProjectiles(LivingEntity shooter, ItemStack heldItem, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Level level = shooter.level();

            // 预备弹仓：扩充储备弹仓容量为 (Level + 1) 次连续射击
            int chamberLevel = ReservedChamberHelper.getReservedChamberLevel(level, heldItem);
            if (chamberLevel > 0) {
                ChargedProjectiles charged = heldItem.get(DataComponents.CHARGED_PROJECTILES);
                if (charged != null && !charged.isEmpty() && charged.size() == 1) {
                    ItemStack firstShot = charged.itemCopies().findFirst().orElse(ItemStack.EMPTY);
                    if (!firstShot.isEmpty()) {
                        List<ItemStack> bullets = new ArrayList<>();
                        bullets.add(firstShot);
                        for (int i = 0; i < chamberLevel; i++) {
                            ItemStack subShot = firstShot.copyWithCount(1);
                            subShot.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                            CustomData.update(DataComponents.CUSTOM_DATA, subShot, tag -> {
                                tag.putBoolean("ReservedChamberSubsequent", true);
                                tag.putInt("ReservedChamberLevel", chamberLevel);
                            });
                            bullets.add(subShot);
                        }
                        heldItem.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(bullets));
                        me.tuanzi.util.ModLog.debug(shooter, null, "【预备弹仓】装填完成！等级: " + chamberLevel + "，拉弦耗时增加 " + String.format("%.2f", 1.0f + 0.25f * chamberLevel) + " 秒，已备好 " + bullets.size() + " 发连续射击弹药！");
                    }
                }
            }

            // 游侠速装逻辑
            if (shooter instanceof Player player) {
                if (me.tuanzi.util.RangerReloadHelper.hasRangerReload(player.level(), heldItem)) {
                    if (player instanceof me.tuanzi.util.RangerReloadTracker tracker) {
                        tracker.tuanzis_mod$setRangerReloadWindowTicks(50);
                    }
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 50, 1, false, false, true));
                    me.tuanzi.util.ModLog.debug(player, null, "【游侠速装】装填弩完成，获得 2.5 秒速度 II（+40% 移动速度）疾跑加速！");
                }
            }
        }
    }

    @Inject(method = "performShooting", at = @At("HEAD"), cancellable = true)
    private void onPerformShooting(Level level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, float power, float uncertainty, LivingEntity targetOverride, CallbackInfo ci) {
        if (shooter instanceof Player player && shooter instanceof me.tuanzi.util.RangerReloadTracker tracker) {
            if (tracker.tuanzis_mod$hasRangerReloadWindow()) {
                me.tuanzi.util.ModLog.debug(player, null, "【游侠速装】在 2.5 秒窗口期内射击弩，保持疾跑状态！");
                player.setSprinting(true);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            int chamberLevel = ReservedChamberHelper.getReservedChamberLevel(serverLevel, weapon);
            if (chamberLevel > 0) {
                ChargedProjectiles charged = weapon.get(DataComponents.CHARGED_PROJECTILES);
                if (charged != null && !charged.isEmpty()) {
                    List<ItemStack> projectiles = charged.itemCopies().toList();
                    if (!projectiles.isEmpty()) {
                        ItemStack toShoot = projectiles.get(0);
                        List<ItemStack> remaining = projectiles.subList(1, projectiles.size());

                        if (remaining.isEmpty()) {
                            weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
                        } else {
                            weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(remaining));
                        }

                        this.shoot(serverLevel, shooter, hand, weapon, List.of(toShoot), power, uncertainty, shooter instanceof Player, targetOverride);

                        if (shooter instanceof ServerPlayer player) {
                            net.minecraft.advancements.triggers.CriteriaTriggers.SHOT_CROSSBOW.trigger(player, weapon);
                            player.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
                        }

                        me.tuanzi.util.ModLog.debug(shooter, null, "【预备弹仓】触发连续射击！发射 1 发，弹仓剩余: " + remaining.size() + " 发。");
                        ci.cancel();
                    }
                }
            }
        }
    }
}

