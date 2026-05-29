package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModStatusEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfMixin extends LivingEntity implements me.tuanzi.util.WolfAccessor {

    protected WolfMixin(net.minecraft.world.entity.EntityType<? extends LivingEntity> entityType, net.minecraft.world.level.Level level) {
        super(entityType, level);
    }

    @Shadow
    private WolfSoundVariant.WolfSoundSet getSoundSet() {
        throw new AssertionError();
    }

    @Unique
    private int tuanzis_mod$invulnerableTicks = 0;
    @Unique
    private int tuanzis_mod$rageTicks = 0;
    @Unique
    private boolean tuanzis_mod$downed = false;
    @Unique
    private int tuanzis_mod$rottenFleshFedCount = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickWolfCharm(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;
        if (wolf.level().isClientSide()) return;

        // 1. 战狼护符副手最大生命值加成
        if (wolf.isTame() && wolf.getOwner() instanceof Player owner) {
            ItemStack offhandStack = owner.getItemInHand(InteractionHand.OFF_HAND);
            var maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                Identifier modId = Identifier.fromNamespaceAndPath("tuanzis_mod", "wolf_command_health");
                if (offhandStack.is(ModItems.WOLF_COMMAND) && !owner.getCooldowns().isOnCooldown(offhandStack)) {
                    if (!maxHealthAttr.hasModifier(modId)) {
                        AttributeModifier modifier = new AttributeModifier(modId, 4.0, AttributeModifier.Operation.ADD_VALUE);
                        maxHealthAttr.addTransientModifier(modifier);
                        this.heal(4.0f);
                    }
                } else {
                    if (maxHealthAttr.hasModifier(modId)) {
                        maxHealthAttr.removeModifier(modId);
                        if (this.getHealth() > this.getMaxHealth()) {
                            this.setHealth(this.getMaxHealth());
                        }
                    }
                }
            }
        } else {
            var maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                Identifier modId = Identifier.fromNamespaceAndPath("tuanzis_mod", "wolf_command_health");
                if (maxHealthAttr.hasModifier(modId)) {
                    maxHealthAttr.removeModifier(modId);
                    if (this.getHealth() > this.getMaxHealth()) {
                        this.setHealth(this.getMaxHealth());
                    }
                }
            }
        }

        // 2. 无敌倒计时递减
        if (tuanzis_mod$invulnerableTicks > 0) {
            tuanzis_mod$invulnerableTicks--;
        }

        // 3. 激怒倒计时与攻击翻倍控制
        if (tuanzis_mod$rageTicks > 0) {
            tuanzis_mod$rageTicks--;

            // 在激怒的 5s 内提供移动速度提升 III 与力量 I
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, 2, false, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 10, 0, false, false, true));

            // 每 10 tick 进行一次激怒攻击，让它极其猛烈
            LivingEntity target = wolf.getTarget();
            if (target != null && target.isAlive() && wolf.distanceToSqr(target) <= 9.0D) {
                if (wolf.tickCount % 10 == 0) {
                    wolf.doHurtTarget((ServerLevel)wolf.level(), target);
                }
            }

            // 产生火花与激怒红石粒子
            if (wolf.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    wolf.getX(), wolf.getY() + 0.3, wolf.getZ(), 1, 0.2, 0.2, 0.2, 0.02);
            }

            if (tuanzis_mod$rageTicks == 0) {
                // 激怒结束，正式力竭倒下
                tuanzis_mod$downed = true;
                tuanzis_mod$rottenFleshFedCount = 0;
                this.setHealth(1.0f);
                wolf.setOrderedToSit(true);

                // 斩断已经绑定的拴绳
                if (wolf.isLeashed()) {
                    wolf.dropLeash();
                }

                // 播放力竭倒地声音（使用变种狼死亡声音）和烟雾粒子
                wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                    this.getSoundSet().deathSound().value(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 0.8F);
                if (wolf.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        wolf.getX(), wolf.getY() + 0.3, wolf.getZ(), 15, 0.3, 0.3, 0.3, 0.0);
                }
            }
        }

        // 4. 倒下状态硬性限制
        if (tuanzis_mod$downed) {
            this.setHealth(1.0f);
            wolf.setOrderedToSit(true);
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            wolf.getNavigation().stop();

            // 每隔 20 tick 产生微小休眠粒子
            if (wolf.tickCount % 20 == 0 && wolf.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    // 5. 实现 WolfAccessor 接口方法
    @Override
    public boolean tuanzis_mod$isDowned() {
        return this.tuanzis_mod$downed;
    }

    @Override
    public void tuanzis_mod$playGrowlSound(LivingEntity target) {
        Wolf wolf = (Wolf) (Object) this;
        wolf.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            this.getSoundSet().growlSound().value(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.2F);
    }

    // 6. 受到致命伤害拦截、无敌及倒地免伤
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float tuanzis_mod$modifyHurtDamage(float damage) {
        if (tuanzis_mod$invulnerableTicks > 0) {
            return 0.0f;
        }
        return damage;
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onHurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Wolf wolf = (Wolf) (Object) this;

        // 倒地状态绝对无敌
        if (tuanzis_mod$downed) {
            cir.setReturnValue(false);
            return;
        }



        // 致命伤害防护
        if (amount >= this.getHealth()) {
            if (wolf.isTame() && wolf.getOwner() instanceof ServerPlayer owner) {
                ItemStack offhandStack = owner.getItemInHand(InteractionHand.OFF_HAND);
                if (offhandStack.is(ModItems.WOLF_COMMAND) && !owner.getCooldowns().isOnCooldown(offhandStack)) {
                    // 消耗护符 1 点耐久
                    offhandStack.hurtAndBreak(1, owner, EquipmentSlot.OFFHAND);

                    // 阻止死亡并回满血
                    this.setHealth(this.getMaxHealth());

                    // 进入 5 秒无敌与激怒
                    tuanzis_mod$invulnerableTicks = 100;
                    tuanzis_mod$rageTicks = 100;
                    tuanzis_mod$downed = false;

                    // 播放变种激怒吼叫和怒火粒子
                    wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                        this.getSoundSet().growlSound().value(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.5F, 1.5F);

                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 15, 0.3, 0.3, 0.3, 0.0);

                    cir.setReturnValue(true);
                }
            }
        }
    }

    // 7. 拦截倒地状态右键喂腐肉复活与栓绳等交互
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolf = (Wolf) (Object) this;

        // 濒死激怒状态（即将死亡释放濒死状态时）下，禁止被玩家喂食
        if (tuanzis_mod$rageTicks > 0) {
            ItemStack stack = player.getItemInHand(hand);
            if (wolf.isFood(stack) || stack.is(Items.ROTTEN_FLESH)) {
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }

        if (tuanzis_mod$downed) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.ROTTEN_FLESH)) {
                if (!wolf.level().isClientSide()) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    tuanzis_mod$rottenFleshFedCount++;

                    // 播放吃东西声音与快乐骨粉粒子
                    wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                        SoundEvents.GENERIC_EAT.value(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);

                    if (wolf.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
                    }

                    // 用 Action bar 发送提示
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.literal("§6战狼恢复进度: §e" + tuanzis_mod$rottenFleshFedCount + " / 10"), true);
                    }

                    if (tuanzis_mod$rottenFleshFedCount >= 10) {
                        // 满血复活！
                        tuanzis_mod$downed = false;
                        tuanzis_mod$rottenFleshFedCount = 0;
                        this.setHealth(this.getMaxHealth());
                        wolf.setOrderedToSit(false);

                        // 播放变种复活吼叫与爱心粒子
                        wolf.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(),
                            this.getSoundSet().growlSound().value(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.2F, 1.0F);

                        if (wolf.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
                        }

                        player.sendSystemMessage(Component.literal("§a你的战狼已满血复活！"));
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
            } else {
                // 倒地状态下，如果不是腐肉，直接拦截，拒绝一切其它交互
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    // 8. 倒下状态时禁止使用栓绳牵着
    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onCanBeLeashed(CallbackInfoReturnable<Boolean> cir) {
        if (tuanzis_mod$downed) {
            cir.setReturnValue(false);
        }
    }

    // 9. 当倒地时，始终不会成为任何生物的攻击目标
    @Override
    public boolean canBeSeenAsEnemy() {
        if (tuanzis_mod$downed) {
            return false;
        }
        return super.canBeSeenAsEnemy();
    }
}
