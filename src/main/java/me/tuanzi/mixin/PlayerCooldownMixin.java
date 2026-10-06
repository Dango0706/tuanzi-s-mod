package me.tuanzi.mixin;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.item.NodachiItem;
import me.tuanzi.network.NodachiSyncPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.util.NodachiPlayerTracker;
import me.tuanzi.util.TideCleaverPlayerTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerCooldownMixin implements TideCleaverPlayerTracker, NodachiPlayerTracker {

    private static final Identifier NODACHI_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "nodachi_momentum_speed");

    @Unique
    private int tuanzis_mod$ticksSinceFullyCharged = 0;

    @Unique
    private float tuanzis_mod$lastAttackStrength = 0.0f;

    @Unique
    private int tuanzis_mod$lastAttackFullyChargedTicks = 0;

    @Unique
    private int tuanzis_mod$nodachiMomentum = 0;

    @Unique
    private int tuanzis_mod$nodachiExhaustionTicks = 0;

    @Unique
    private int tuanzis_mod$nodachiInactiveTicks = 0;

    @Unique
    private int tuanzis_mod$crescentAuraCooldownTicks = 0;

    @Unique
    private int tuanzis_mod$nodachiRhythmGraceTicks = 0;

    @Override
    public float tuanzis_mod$getLastAttackStrength() {
        return this.tuanzis_mod$lastAttackStrength;
    }

    @Override
    public int tuanzis_mod$getLastAttackFullyChargedTicks() {
        return this.tuanzis_mod$lastAttackFullyChargedTicks;
    }

    @Override
    public int tuanzis_mod$getCurrentFullyChargedTicks() {
        return this.tuanzis_mod$ticksSinceFullyCharged;
    }

    @Override
    public int tuanzis_mod$getNodachiMomentum() {
        return this.tuanzis_mod$nodachiMomentum;
    }

    @Override
    public void tuanzis_mod$setNodachiMomentum(int momentum) {
        this.tuanzis_mod$nodachiMomentum = momentum;
        this.tuanzis_mod$updateNodachiSpeedModifier();
    }

    @Override
    public int tuanzis_mod$getNodachiExhaustionTicks() {
        return this.tuanzis_mod$nodachiExhaustionTicks;
    }

    @Override
    public void tuanzis_mod$setNodachiExhaustionTicks(int ticks) {
        this.tuanzis_mod$nodachiExhaustionTicks = ticks;
    }

    @Override
    public int tuanzis_mod$getNodachiInactiveTicks() {
        return this.tuanzis_mod$nodachiInactiveTicks;
    }

    @Override
    public void tuanzis_mod$setNodachiInactiveTicks(int ticks) {
        this.tuanzis_mod$nodachiInactiveTicks = ticks;
    }

    @Override
    public int tuanzis_mod$getCrescentAuraCooldownTicks() {
        return this.tuanzis_mod$crescentAuraCooldownTicks;
    }

    @Override
    public void tuanzis_mod$setCrescentAuraCooldownTicks(int ticks) {
        this.tuanzis_mod$crescentAuraCooldownTicks = ticks;
    }

    @Override
    public int tuanzis_mod$getNodachiRhythmGraceTicks() {
        return this.tuanzis_mod$nodachiRhythmGraceTicks;
    }

    @Override
    public void tuanzis_mod$setNodachiRhythmGraceTicks(int ticks) {
        this.tuanzis_mod$nodachiRhythmGraceTicks = ticks;
    }

    @Override
    public void tuanzis_mod$triggerNodachiExhaustion(String reason) {
        Player player = (Player) (Object) this;
        int prevMomentum = this.tuanzis_mod$nodachiMomentum;
        this.tuanzis_mod$nodachiMomentum = 0;
        this.tuanzis_mod$nodachiExhaustionTicks = 10; // 0.5s = 10 ticks
        this.tuanzis_mod$nodachiInactiveTicks = 0;
        this.tuanzis_mod$updateNodachiSpeedModifier();

        String side = player.level().isClientSide() ? "客户端" : "服务端";
        String logMessage = String.format("【野太刀·脱力】[%s] 玩家 %s 触发脱力！脱力原因: %s (原有势层数: %d，势槽已归零，陷入 0.5 秒脱力软僵直无法攻击)",
                side, player.getName().getString(), reason, prevMomentum);

        // 控制台明确输出
        ModLog.info(logMessage);
        // 调试日志记录
        ModLog.debug(player, null, logMessage);

        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 0.7f, 0.6f);
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new NodachiSyncPacket(0, 10));
            }
        }
    }

    @Override
    public void tuanzis_mod$triggerNodachiExhaustion() {
        this.tuanzis_mod$triggerNodachiExhaustion("未指定原因");
    }

    @Unique
    private void tuanzis_mod$updateNodachiSpeedModifier() {
        Player player = (Player) (Object) this;
        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr == null) return;

        speedAttr.removeModifier(NODACHI_SPEED_MODIFIER_ID);

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof NodachiItem && this.tuanzis_mod$nodachiMomentum > 0) {
            double bonus = switch (this.tuanzis_mod$nodachiMomentum) {
                case 1 -> 0.2; // 1.3 -> 1.5
                case 2 -> 0.4; // 1.3 -> 1.7
                case 3 -> 0.6; // 1.3 -> 1.9
                default -> 0.0;
            };
            if (bonus > 0.0) {
                speedAttr.addTransientModifier(new AttributeModifier(
                    NODACHI_SPEED_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE
                ));
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickCooldown(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        // 检测完全充能状态
        if (player.getAttackStrengthScale(0.5f) >= 1.0f) {
            this.tuanzis_mod$ticksSinceFullyCharged++;
        } else {
            this.tuanzis_mod$ticksSinceFullyCharged = 0;
        }

        // 野太刀逻辑更新
        if (this.tuanzis_mod$crescentAuraCooldownTicks > 0) {
            this.tuanzis_mod$crescentAuraCooldownTicks--;
        }

        if (this.tuanzis_mod$nodachiRhythmGraceTicks > 0) {
            this.tuanzis_mod$nodachiRhythmGraceTicks--;
        }

        boolean holdingNodachi = player.getMainHandItem().getItem() instanceof NodachiItem;
        if (holdingNodachi) {
            if (this.tuanzis_mod$nodachiExhaustionTicks > 0) {
                this.tuanzis_mod$nodachiExhaustionTicks--;
            }

            // 超时脱力惩罚必须严格仅在服务端进行判定！客户端绝不独立判定超时
            if (!player.level().isClientSide()) {
                if (this.tuanzis_mod$nodachiMomentum > 0) {
                    this.tuanzis_mod$nodachiInactiveTicks++;
                    // 惩罚机制：超过 5 秒 (100 ticks) 未造成伤害，“势”立即归零，并触发 0.5 秒脱力软僵直
                    if (this.tuanzis_mod$nodachiInactiveTicks > 100) {
                        this.tuanzis_mod$triggerNodachiExhaustion("攻击间隔超时 (超过 5 秒 / 100刻 未造成伤害)");
                    }
                }
            }
        } else {
            if (this.tuanzis_mod$nodachiMomentum > 0) {
                this.tuanzis_mod$nodachiMomentum = 0;
                this.tuanzis_mod$nodachiInactiveTicks = 0;
                this.tuanzis_mod$updateNodachiSpeedModifier();
                if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    ServerPlayNetworking.send(serverPlayer, new NodachiSyncPacket(0, this.tuanzis_mod$nodachiExhaustionTicks));
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$beforeAttack(Entity entity, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // 脱力软僵直判定：脱力状态下无法攻击敌人！
        if (this.tuanzis_mod$nodachiExhaustionTicks > 0) {
            ci.cancel();
            if (!player.level().isClientSide()) {
                ModLog.debug(player, entity, "【野太刀】玩家处于脱力软僵直状态 (剩余 " + this.tuanzis_mod$nodachiExhaustionTicks + " 刻)，攻击无效！");
            }
            return;
        }

        this.tuanzis_mod$lastAttackStrength = player.getAttackStrengthScale(0.5f);
        this.tuanzis_mod$lastAttackFullyChargedTicks = this.tuanzis_mod$ticksSinceFullyCharged;

        if (!player.level().isClientSide()) {
            ModLog.debug(player, entity, "【玩家攻击】攻击强度: " 
                + String.format("%.4f", this.tuanzis_mod$lastAttackStrength) 
                + ", 冷却已满刻数: " + this.tuanzis_mod$lastAttackFullyChargedTicks);
        }
    }

    @Inject(
        method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;itemAttackInteraction(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/damagesource/DamageSource;Z)V")
    )
    private void tuanzis_mod$onNodachiHitEntity(Entity entity, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof NodachiItem) {
            NodachiItem.onPlayerHitEntity(player, entity, held, 0.0f);
        }
    }

    @ModifyVariable(
        method = "causeExtraKnockback",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private float tuanzis_mod$modifyNodachiKnockback(float knockbackAmount, Entity entity) {
        Player player = (Player) (Object) this;
        if (player.getMainHandItem().getItem() instanceof NodachiItem) {
            double dist = player.distanceTo(entity);
            if (dist <= 1.5) {
                // 刀根贴脸（0 ~ 1.5 格）：攻击击退大幅降低 (降低至 25%)
                ModLog.debug(player, entity, "【野太刀·贴脸】距离 " + String.format("%.2f", dist) + " 格 <= 1.5格，攻击击退大幅降低 (削减 75%)。");
                return knockbackAmount * 0.25f;
            }
        }
        return knockbackAmount;
    }
}
