package me.tuanzi.mixin;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 画布系统破坏保护与系统底面无掉落逻辑：
 * 1. 当画板的画布空间内有玩家时，禁止破坏该画板。
 * 2. 在画布编辑空间内校验编辑权限与边界；系统白色底面允许挖掘且直接清除（不掉落物品）。
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class CanvasBreakProtectionMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$protectCanvasAndEasel(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = this.level.getBlockState(pos);

        // 1. 画板保护：画布空间有人时禁止破坏画板
        if (state.is(ModBlocks.EASEL)) {
            if (CanvasSpaceManager.isEaselOccupied(this.level, pos)) {
                this.level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                this.player.sendSystemMessage(
                        Component.translatable("message.tuanzis_mod.easel.occupied_break_denied").withStyle(ChatFormatting.RED),
                        true
                );
                cir.setReturnValue(false);
                return;
            }
        }

        // 2. 画布编辑空间破坏权限、边界及系统白色底面零掉落处理
        if (ModDimensions.isCanvasSpaceLevel(this.level)) {
            if (!CanvasSpaceManager.canBreakAt(this.player, this.level, pos, state)) {
                this.level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                cir.setReturnValue(false);
                return;
            }
            if (state.is(ModBlocks.CANVAS_BASE_BLOCK)) {
                this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                cir.setReturnValue(true);
            }
        }
    }
}
