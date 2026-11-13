package me.tuanzi.mixin;

import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.team.ScavengerTeam;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.world.item.ItemStack tuanzis_mod$currentRod = net.minecraft.world.item.ItemStack.EMPTY;

    @Inject(method = "retrieve", at = @At("HEAD"))
    private void tuanzis_mod$captureRod(net.minecraft.world.item.ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        this.tuanzis_mod$currentRod = rod;
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", ordinal = 0),
            index = 0
    )
    private Entity tuanzis_mod$duplicateFish(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
            net.minecraft.world.item.ItemStack stack = itemEntity.getItem();
            if (stack.is(net.minecraft.tags.ItemTags.FISHES) && !this.tuanzis_mod$currentRod.isEmpty()) {
                FishingHook hook = (FishingHook) (Object) this;
                var registry = hook.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var bountyHolder = registry.getOrThrow(me.tuanzi.init.ModEnchantments.BOUNTY_OF_THE_SEA);
                int level = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(bountyHolder, this.tuanzis_mod$currentRod);
                if (level > 0) {
                    float chance = 0.12f * level;
                    if (hook.getRandom().nextFloat() < chance) {
                        net.minecraft.world.entity.player.Player owner = hook.getPlayerOwner();
                        net.minecraft.world.entity.item.ItemEntity extra = new net.minecraft.world.entity.item.ItemEntity(
                                hook.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack.copyWithCount(1)
                        );
                        extra.setDeltaMovement(itemEntity.getDeltaMovement());
                        hook.level().addFreshEntity(extra);
                        if (owner != null) {
                            ModLog.debug(owner, "【海之馈赠】附魔生效（等级: %d，概率: %.0f%%）！额外钓得一条同种鱼: %s", level, chance * 100, stack.getHoverName().getString());
                        }
                    }
                }
            }
        }
        return entity;
    }

    @Inject(method = "retrieve", at = @At("RETURN"))
    private void tuanzis_mod$clearCapturedRod(net.minecraft.world.item.ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        this.tuanzis_mod$currentRod = net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$canHitEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.level().isClientSide()) {
            return;
        }

        if (entity instanceof ServerPlayer targetPlayer && hook.getOwner() instanceof ServerPlayer ownerPlayer) {
            if (ScavengerTeamManager.areTeammates(ownerPlayer, targetPlayer)) {
                ScavengerTeam team = ScavengerTeamManager.getTeamByPlayer(targetPlayer);
                if (team != null && !team.isFishingHookAffectsTeammates()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "pullEntity", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$pullEntity(Entity entity, CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.level().isClientSide()) {
            return;
        }

        if (entity instanceof ServerPlayer targetPlayer && hook.getOwner() instanceof ServerPlayer ownerPlayer) {
            if (ScavengerTeamManager.areTeammates(ownerPlayer, targetPlayer)) {
                ScavengerTeam team = ScavengerTeamManager.getTeamByPlayer(targetPlayer);
                if (team != null && !team.isFishingHookAffectsTeammates()) {
                    ModLog.debug("【队伍配置】拦截队友钓鱼竿拉扯: 垂钓者=" + ownerPlayer.getName().getString() + ", 目标=" + targetPlayer.getName().getString());
                    ci.cancel();
                }
            }
        }
    }
}
