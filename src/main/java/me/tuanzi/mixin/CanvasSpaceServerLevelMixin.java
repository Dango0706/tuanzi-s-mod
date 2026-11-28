package me.tuanzi.mixin;

import me.tuanzi.config.ModConfig;
import me.tuanzi.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 隔离保护画布编辑空间维度：
 * 1. 禁止一切方块/流体随机与计划刻更新（保持完全静止态，防止水/岩浆流动和刷石机）。
 * 2. 禁止任何非玩家生物生成；创造模式画布空间禁止生成物品实体。
 */
@Mixin(ServerLevel.class)
public abstract class CanvasSpaceServerLevelMixin {

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$freezeFluidsInCanvasSpace(BlockPos pos, Fluid type, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (ModDimensions.isCanvasSpaceLevel(level)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$freezeBlocksInCanvasSpace(BlockPos pos, Block type, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (ModDimensions.isCanvasSpaceLevel(level)) {
            ci.cancel();
        }
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$blockSpawnsInCanvasSpace(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!ModDimensions.isCanvasSpaceLevel(level)) {
            return;
        }
        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            cir.setReturnValue(false);
            return;
        }
        if (entity instanceof ItemEntity && ModConfig.isCanvasCreativeMode()) {
            cir.setReturnValue(false);
        }
    }
}
