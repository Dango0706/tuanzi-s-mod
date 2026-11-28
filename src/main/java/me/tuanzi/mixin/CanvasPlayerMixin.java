package me.tuanzi.mixin;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.world.canvas.CanvasPlayerTracker;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 持久化玩家画布编辑空间会话状态（返回坐标、原游戏模式与双背包快照）。
 */
@Mixin(ServerPlayer.class)
public abstract class CanvasPlayerMixin implements CanvasPlayerTracker {
    @Unique
    private CompoundTag tuanzis_mod$canvasSessionData = new CompoundTag();

    @Override
    public CompoundTag tuanzis_mod$getCanvasSessionData() {
        return this.tuanzis_mod$canvasSessionData;
    }

    @Override
    public void tuanzis_mod$setCanvasSessionData(CompoundTag tag) {
        this.tuanzis_mod$canvasSessionData = tag != null ? tag : new CompoundTag();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$saveCanvasSessionData(ValueOutput output, CallbackInfo ci) {
        if (!this.tuanzis_mod$canvasSessionData.isEmpty()) {
            output.store("tuanzis_mod$canvas_session", CompoundTag.CODEC, this.tuanzis_mod$canvasSessionData);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$readCanvasSessionData(ValueInput input, CallbackInfo ci) {
        input.read("tuanzis_mod$canvas_session", CompoundTag.CODEC)
                .ifPresent(tag -> this.tuanzis_mod$canvasSessionData = tag.copy());
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void tuanzis_mod$copyCanvasSessionOnRestore(ServerPlayer oldPlayer, boolean wonGame, CallbackInfo ci) {
        if (oldPlayer instanceof CanvasPlayerTracker tracker) {
            this.tuanzis_mod$canvasSessionData = tracker.tuanzis_mod$getCanvasSessionData().copy();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickCanvasSpacePlayer(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.level().isClientSide() && ModDimensions.isCanvasSpaceLevel(player.level())) {
            CanvasSpaceManager.tickPlayerInCanvasSpace(player);
        }
    }
}
