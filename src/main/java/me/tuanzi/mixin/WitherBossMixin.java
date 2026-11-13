package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(WitherBoss.class)
public abstract class WitherBossMixin extends Monster {

    @Unique
    private float tuanzis_mod$totalDamageTaken = 0.0f;
    @Unique
    private float tuanzis_mod$totalPlayerDamage = 0.0f;
    @Unique
    private boolean tuanzis_mod$isMarkedGlowing = false;

    protected WitherBossMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowModStatusEffects(MobEffectInstance newEffect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        Holder<MobEffect> effect = newEffect.getEffect();
        if (effect.is(ModStatusEffects.TEARING)
                || effect.is(ModStatusEffects.BEE_POISON)
                || effect.is(ModStatusEffects.BEE_POISON_COOLDOWN)
                || effect.is(ModStatusEffects.RESONANCE)
                || effect.is(ModStatusEffects.TIDE_EROSION)) {
            me.tuanzi.util.ModLog.debug(source, this, "【凋零效果免疫解除】允许对凋零施加效果: " + effect.getRegisteredName());
            cir.setReturnValue(super.addEffect(newEffect, source));
        }
    }

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void tuanzis_mod$trackDamageAndGlow(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && damage > 0.0f) {
            tuanzis_mod$totalDamageTaken += damage;

            if (source.getEntity() instanceof Player player) {
                tuanzis_mod$totalPlayerDamage += damage;
                float ratio = tuanzis_mod$totalDamageTaken > 0 ? (tuanzis_mod$totalPlayerDamage / tuanzis_mod$totalDamageTaken) : 0f;
                ModLog.debug(player, this, String.format("玩家 %s 对凋灵造成 %.2f 伤害 (玩家总伤害: %.2f, 凋灵总承伤: %.2f, 玩家总体占比: %.2f%%)",
                        player.getScoreboardName(), damage, tuanzis_mod$totalPlayerDamage, tuanzis_mod$totalDamageTaken, ratio * 100));

                if (ratio >= 0.90f) {
                    if (!tuanzis_mod$isMarkedGlowing) {
                        tuanzis_mod$isMarkedGlowing = true;
                        this.setGlowingTag(true);
                        ModLog.debug(player, this, "【凋灵发光提示】玩家总体伤害占比达到 " + String.format("%.2f%%", ratio * 100) + " (>= 90%)，凋灵已激活发光提示！");
                    }
                }
            } else {
                float ratio = tuanzis_mod$totalDamageTaken > 0 ? (tuanzis_mod$totalPlayerDamage / tuanzis_mod$totalDamageTaken) : 0f;
                ModLog.debug(null, this, String.format("凋灵受到非玩家伤害 %.2f (来源: %s, 玩家总伤害: %.2f, 凋灵总承伤: %.2f, 玩家总体占比: %.2f%%)",
                        damage, source.getMsgId(), tuanzis_mod$totalPlayerDamage, tuanzis_mod$totalDamageTaken, ratio * 100));

                // 若非玩家伤害导致玩家总体占比跌破 90%，解除发光
                if (ratio < 0.90f && tuanzis_mod$isMarkedGlowing) {
                    tuanzis_mod$isMarkedGlowing = false;
                    this.setGlowingTag(false);
                    ModLog.debug(null, this, "【凋灵发光提示解除】受非玩家伤害影响，玩家总体伤害占比跌破 90% (当前: " + String.format("%.2f%%", ratio * 100) + ")，凋灵发光已解除。");
                }
            }
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
    private void tuanzis_mod$onWitherDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer, CallbackInfo ci) {
        if (killedByPlayer && source.getEntity() instanceof ServerPlayer killer) {
            float ratio = tuanzis_mod$totalDamageTaken > 0 ? (tuanzis_mod$totalPlayerDamage / tuanzis_mod$totalDamageTaken) : 0f;
            ModLog.debug(killer, this, String.format("【凋灵被击杀】最后一击杀死凋灵的玩家: %s, 玩家总体伤害: %.2f, 凋灵总承伤: %.2f, 玩家总体占比: %.2f%%",
                    killer.getScoreboardName(), tuanzis_mod$totalPlayerDamage, tuanzis_mod$totalDamageTaken, ratio * 100));

            // 只需玩家总体伤害 >= 90%，积分奖励给将凋灵杀死的玩家
            if (ratio >= 0.90f) {
                int newKills = me.tuanzi.util.WitherKillManager.addKills(killer, 1);
                int currentProgress = newKills % 1000;
                if (currentProgress == 0 && newKills > 0) {
                    currentProgress = 1000;
                }
                ModLog.debug(killer, this, "【凋灵有效击杀】最后一击杀死凋灵的玩家 " + killer.getScoreboardName() + " 获得击杀计数！当前累计击杀数: " + newKills);

                // 动作栏（ActionBar）简短提示当前进度
                killer.sendSystemMessage(Component.translatable("chat.tuanzis_mod.witherkills.actionbar", currentProgress, 1000), true);

                // 每达到 1000 次掉落 1 本灵魂绑定附魔书
                if (newKills % 1000 == 0) {
                    var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var soulboundHolder = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);

                    ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                    EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(soulboundHolder, 1));

                    killer.spawnAtLocation(level, book);
                    ModLog.debug(killer, this, "【灵魂绑定掉落】玩家 " + killer.getScoreboardName() + " 累计击杀达到 " + newKills + " 次！已在玩家身旁掉落 1 本灵魂绑定附魔书！");

                    // 播放特殊音效
                    level.playSound(null, killer.getX(), killer.getY(), killer.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

                    // 聊天栏高亮祝贺
                    killer.sendSystemMessage(Component.translatable("chat.tuanzis_mod.witherkills.reward", newKills));
                }
            } else {
                ModLog.debug(killer, this, "【凋灵击杀无效】玩家总体伤害占比不足 90% (当前: " + String.format("%.2f%%", ratio * 100) + ")，不计入凋灵击杀数。");
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$writeWitherTrackingData(ValueOutput output, CallbackInfo ci) {
        output.putFloat("tuanzis_mod$totalDamageTaken", this.tuanzis_mod$totalDamageTaken);
        output.putFloat("tuanzis_mod$totalPlayerDamage", this.tuanzis_mod$totalPlayerDamage);
        output.putBoolean("tuanzis_mod$isMarkedGlowing", this.tuanzis_mod$isMarkedGlowing);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$readWitherTrackingData(ValueInput input, CallbackInfo ci) {
        this.tuanzis_mod$totalDamageTaken = input.getFloatOr("tuanzis_mod$totalDamageTaken", 0.0f);
        this.tuanzis_mod$totalPlayerDamage = input.getFloatOr("tuanzis_mod$totalPlayerDamage", 0.0f);
        this.tuanzis_mod$isMarkedGlowing = input.getBooleanOr("tuanzis_mod$isMarkedGlowing", false);
        if (this.tuanzis_mod$isMarkedGlowing) {
            this.setGlowingTag(true);
        }
    }
}
