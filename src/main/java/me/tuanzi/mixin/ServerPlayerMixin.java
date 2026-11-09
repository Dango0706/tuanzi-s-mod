package me.tuanzi.mixin;

import me.tuanzi.util.WitherKillTracker;
import me.tuanzi.world.teleport.TeleportManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements WitherKillTracker {

    @org.spongepowered.asm.mixin.Unique
    private boolean tuanzis_mod$hadWolfCommandInOffhand = false;

    @org.spongepowered.asm.mixin.Unique
    private int tuanzis_mod$witherKills = 0;

    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.world.phys.Vec3 tuanzis_mod$lastSafePos = null;

    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.server.level.ServerLevel tuanzis_mod$lastSafeLevel = null;

    @org.spongepowered.asm.mixin.Unique
    private long tuanzis_mod$lastVoidAnchorGameTime = -6000L;

    @Override
    public int tuanzis_mod$getWitherKills() {
        return this.tuanzis_mod$witherKills;
    }

    @Override
    public void tuanzis_mod$setWitherKills(int kills) {
        this.tuanzis_mod$witherKills = Math.max(0, kills);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount > 0.0f) {
            TeleportManager.onPlayerDamage((ServerPlayer) (Object) this);
            me.tuanzi.world.scavenger.loot.search.SearchSessionManager.onPlayerDamaged((ServerPlayer) (Object) this);
        }

        // 虚空锚检测
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            net.minecraft.world.item.ItemStack boots = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
            if (!boots.isEmpty()) {
                var enchRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var voidAnchorHolder = enchRegistry.getOrThrow(me.tuanzi.init.ModEnchantments.VOID_ANCHOR);
                int anchorLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(voidAnchorHolder, boots);
                if (anchorLevel > 0) {
                    long currentGameTime = level.getGameTime();
                    // 冷却 300 秒 (6000 刻)
                    if (currentGameTime - this.tuanzis_mod$lastVoidAnchorGameTime >= 6000L) {
                        this.tuanzis_mod$lastVoidAnchorGameTime = currentGameTime;

                        // 传送回最近一次站立的安全地面
                        net.minecraft.world.phys.Vec3 targetPos = this.tuanzis_mod$lastSafePos;
                        ServerLevel targetLevel = this.tuanzis_mod$lastSafeLevel != null ? this.tuanzis_mod$lastSafeLevel : level;
                        if (targetPos == null) {
                            net.minecraft.core.BlockPos spawnPos = level.getLevelData().getRespawnData().globalPos().pos();
                            targetPos = net.minecraft.world.phys.Vec3.atBottomCenterOf(spawnPos);
                        }

                        // 每次触发消耗 10 点耐久
                        boots.hurtAndBreak(10, player, net.minecraft.world.entity.EquipmentSlot.FEET);

                        // 重置动量与掉落距离
                        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                        player.resetFallDistance();

                        // 传送
                        player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);

                        me.tuanzi.util.ModLog.debug(player, "【虚空锚】附魔生效！免除虚空伤害，传送回安全位置: (%.2f, %.2f, %.2f)，靴子消耗 10 点耐久，进入 300 秒冷却", targetPos.x, targetPos.y, targetPos.z);
                        cir.setReturnValue(false);
                        return;
                    } else {
                        long remainingSeconds = (6000L - (currentGameTime - this.tuanzis_mod$lastVoidAnchorGameTime)) / 20L;
                        me.tuanzi.util.ModLog.debug(player, "【虚空锚】处于冷却中（剩余 %d 秒），无法触发传送！", remainingSeconds);
                    }
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickBerserkCharm(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().isClientSide()) return;

        net.minecraft.world.item.ItemStack offhandStack = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (offhandStack.is(me.tuanzi.init.ModItems.BERSERK_CHARM)) {
            // 冷却时间检查
            if (!player.getCooldowns().isOnCooldown(offhandStack)) {
                // 生命值低于 30%
                if (player.getHealth() < player.getMaxHealth() * 0.3f) {
                    // 消耗 1 个护符
                    offhandStack.shrink(1);
                    // 获得 15 秒（300 刻）的力量 II（等级 1）与速度 II（等级 1）
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 300, 1));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 300, 1));
                    me.tuanzi.util.ModLog.debug(player, null, "狂暴护符激活！消耗 1 个护符，当前副手剩余: " + offhandStack.getCount() + "，已获得 15 秒的力量 II 与速度 II 效果。");

                    // 进入 15 秒冷却（300 刻）
                    player.getCooldowns().addCooldown(offhandStack, 300);

                    // 播放激活音效：不死图腾使用音效
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

                    // 产生粒子效果
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
                    }
                }
            }
        }

        // 战狼护符移除副手CD检测
        boolean hasWolfCommand = offhandStack.is(me.tuanzi.init.ModItems.WOLF_COMMAND);
        if (tuanzis_mod$hadWolfCommandInOffhand && !hasWolfCommand) {
            // 移除了，给予 5s (100 ticks) 的 CD 时间
            player.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(me.tuanzi.init.ModItems.WOLF_COMMAND), 100);
            
            // 播放一个护符移开的提示音，让反馈更真实
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
        }
        tuanzis_mod$hadWolfCommandInOffhand = hasWolfCommand;

        // 记录虚空锚安全落脚点
        if (player.onGround() && !player.isInLava() && !player.isOnFire() && player.getY() > player.level().getMinY()) {
            net.minecraft.core.BlockPos below = player.blockPosition().below();
            net.minecraft.world.level.block.state.BlockState belowState = player.level().getBlockState(below);
            if (!belowState.isAir() && belowState.isSolid() && !belowState.is(net.minecraft.world.level.block.Blocks.LAVA) && !belowState.is(net.minecraft.world.level.block.Blocks.FIRE)) {
                this.tuanzis_mod$lastSafePos = player.position();
                this.tuanzis_mod$lastSafeLevel = (ServerLevel) player.level();
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void tuanzis_mod$onPlayerDie(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.level().isClientSide() && source.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper && creeper.isPowered()) {
            net.minecraft.world.item.ItemStack skullStack = new net.minecraft.world.item.ItemStack(me.tuanzi.init.ModItems.CHROMATIC_SKULL);
            
            // 输出调试日志
            me.tuanzi.util.ModLog.debug("[彩色变化头颅掉落] 玩家 " + player.getName().getString() + " 被闪电苦力怕炸死，掉落彩色变化头颅。");
            
            player.spawnAtLocation((ServerLevel) player.level(), skullStack);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$writeWitherKills(ValueOutput output, CallbackInfo ci) {
        output.putInt("tuanzis_mod$wither_kills", this.tuanzis_mod$witherKills);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$readWitherKills(ValueInput input, CallbackInfo ci) {
        this.tuanzis_mod$witherKills = input.getIntOr("tuanzis_mod$wither_kills", 0);
        me.tuanzi.util.WitherKillManager.setKills((ServerPlayer) (Object) this, this.tuanzis_mod$witherKills);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void tuanzis_mod$restoreWitherKills(ServerPlayer oldPlayer, boolean wonGame, CallbackInfo ci) {
        if (oldPlayer instanceof WitherKillTracker tracker) {
            this.tuanzis_mod$witherKills = tracker.tuanzis_mod$getWitherKills();
        } else {
            this.tuanzis_mod$witherKills = me.tuanzi.util.WitherKillManager.getKills(oldPlayer);
        }
        me.tuanzi.util.WitherKillManager.setKills((ServerPlayer) (Object) this, this.tuanzis_mod$witherKills);
    }

    @org.spongepowered.asm.mixin.Unique
    private float tuanzis_mod$applySatiationExhaustionReduction(float exhaustion, String actionType) {
        if (exhaustion <= 0.0f) {
            return exhaustion;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        net.minecraft.world.item.ItemStack chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!chestStack.isEmpty()) {
            var enchRegistry = player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var satiationEnch = enchRegistry.getOrThrow(me.tuanzi.init.ModEnchantments.SATIATION);
            int level = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(satiationEnch, chestStack);
            if (level > 0) {
                float reductionRate = Math.min(1.0f, 0.08f * level);
                float finalExhaustion = exhaustion * (1.0f - reductionRate);
                me.tuanzi.util.ModLog.debug(player, null, "【饱腹】附魔生效（等级: " + level + "）！降低" + actionType + "饥饿消耗 "
                        + String.format(java.util.Locale.ROOT, "%.0f%%", reductionRate * 100.0f)
                        + "，原消耗: " + String.format(java.util.Locale.ROOT, "%.4f", exhaustion)
                        + "，现消耗: " + String.format(java.util.Locale.ROOT, "%.4f", finalExhaustion));
                return finalExhaustion;
            }
        }
        return exhaustion;
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method = "checkMovementStatistics",
        at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V")
    )
    private float tuanzis_mod$modifyMovementExhaustion(float exhaustion) {
        return tuanzis_mod$applySatiationExhaustionReduction(exhaustion, "移动/疾跑");
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method = "jumpFromGround",
        at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V")
    )
    private float tuanzis_mod$modifyJumpExhaustion(float exhaustion) {
        return tuanzis_mod$applySatiationExhaustionReduction(exhaustion, "跳跃");
    }

    @org.spongepowered.asm.mixin.Unique
    private static final net.minecraft.resources.Identifier tuanzis_mod$AMONG_FLOWERS_SPEED_ID = net.minecraft.resources.Identifier.fromNamespaceAndPath(me.tuanzi.Tuanzis_mod.MOD_ID, "among_flowers_speed");

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$onServerPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel level = player.level();

        // 1. 衔月 (Moon Embrace)
        net.minecraft.world.item.ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            var enchRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var moonHolder = enchRegistry.getOrThrow(me.tuanzi.init.ModEnchantments.MOON_EMBRACE);
            int moonLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(moonHolder, helmet);
            if (moonLevel > 0) {
                int interval = (10 - 2 * moonLevel) * 20; // 8s, 6s, 4s -> 160, 120, 80 ticks
                if (player.tickCount % interval == 0) {
                    // 夜晚且头顶无方块遮挡，饥饿值 >= 14 时生效
                    long dayTime = level.getDefaultClockTime() % 24000L;
                    if (dayTime < 0) dayTime += 24000L;
                    boolean isNight = !level.dimensionType().hasFixedTime() && dayTime >= 12000L;
                    if (isNight && level.canSeeSky(player.blockPosition().above()) && player.getFoodData().getFoodLevel() >= 14) {
                        if (player.getHealth() < player.getMaxHealth()) {
                            player.heal(1.0F);
                            me.tuanzi.util.ModLog.debug(player, "【衔月】夜间头顶无遮挡触发生命恢复 1 点（当前生命: %.1f/%.1f，等级: %d）",
                                player.getHealth(), player.getMaxHealth(), moonLevel);
                        }
                    }
                }
            }
        }

        // 2. 花间 (Among Flowers)
        net.minecraft.world.item.ItemStack leggings = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        var speedAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (!leggings.isEmpty()) {
            var enchRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var flowerHolder = enchRegistry.getOrThrow(me.tuanzi.init.ModEnchantments.AMONG_FLOWERS);
            int flowerLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(flowerHolder, leggings);
            if (flowerLevel > 0) {
                int radius = 4 + 2 * (flowerLevel - 1); // 4, 6, 8 blocks
                boolean hasFlowers = false;
                net.minecraft.core.BlockPos playerPos = player.blockPosition();
                net.minecraft.core.BlockPos.MutableBlockPos mut = new net.minecraft.core.BlockPos.MutableBlockPos();
                for (int x = -radius; x <= radius && !hasFlowers; x++) {
                    for (int y = -radius; y <= radius && !hasFlowers; y++) {
                        for (int z = -radius; z <= radius && !hasFlowers; z++) {
                            if (x * x + y * y + z * z <= radius * radius) {
                                mut.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                                if (level.getBlockState(mut).is(net.minecraft.tags.BlockTags.FLOWERS)) {
                                    hasFlowers = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (hasFlowers) {
                    if (speedAttr != null) {
                        double speedBoost = 0.05 * flowerLevel; // 5%, 10%, 15%
                        speedAttr.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            tuanzis_mod$AMONG_FLOWERS_SPEED_ID,
                            speedBoost,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ));
                    }

                    // 每 10/8/6 秒恢复 1 点饱和度，饥饿值 >= 14 时生效
                    int satInterval = (12 - 2 * flowerLevel) * 20; // 200, 160, 120 ticks
                    if (player.tickCount % satInterval == 0 && player.getFoodData().getFoodLevel() >= 14) {
                        float currentSat = player.getFoodData().getSaturationLevel();
                        float maxSat = (float) player.getFoodData().getFoodLevel();
                        if (currentSat < maxSat) {
                            player.getFoodData().setSaturation(Math.min(maxSat, currentSat + 1.0F));
                            me.tuanzi.util.ModLog.debug(player, "【花间】花香沁脾，恢复 1 点饱和度（当前饱和度: %.1f，饱食度: %d，等级: %d）",
                                player.getFoodData().getSaturationLevel(), player.getFoodData().getFoodLevel(), flowerLevel);
                        }
                    }
                } else {
                    if (speedAttr != null) {
                        speedAttr.removeModifier(tuanzis_mod$AMONG_FLOWERS_SPEED_ID);
                    }
                }
            } else {
                if (speedAttr != null) {
                    speedAttr.removeModifier(tuanzis_mod$AMONG_FLOWERS_SPEED_ID);
                }
            }
        } else {
            if (speedAttr != null) {
                speedAttr.removeModifier(tuanzis_mod$AMONG_FLOWERS_SPEED_ID);
            }
        }

        // 3. 休眠修复 (Dormant Repair) - 每 20 ticks (1s) 检查背包/快捷栏/装备栏物品
        if (player.tickCount % 20 == 0) {
            me.tuanzi.util.DormantRepairHelper.tickRepair(player);
        }

        // 4. 牧养 (Husbandry) - 每 10 ticks (0.5s) 加速 8 格内幼崽成长
        if (player.tickCount % 10 == 0) {
            me.tuanzi.util.HusbandryHelper.tickBabyGrowth(player);
        }
    }
}
