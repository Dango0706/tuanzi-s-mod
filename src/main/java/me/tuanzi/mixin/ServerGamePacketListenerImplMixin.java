package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 在服务端方块放置校验中使用可热加载的主世界建筑高度上限，
 * 同时允许对高于新上限的已有方块正常进行破坏与右键交互。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    private static boolean wasBlockPlacementAttempt(ServerPlayer player, ItemStack itemStack) {
        throw new AssertionError();
    }

    @Redirect(
            method = "handleUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult tuanzis_mod$checkOverworldPlacementBuildLimit(
            ServerPlayerGameMode gameMode,
            ServerPlayer player,
            Level level,
            ItemStack itemStack,
            InteractionHand hand,
            BlockHitResult blockHit
    ) {
        InteractionResult interactionResult = gameMode.useItemOn(player, level, itemStack, hand, blockHit);
        if (!interactionResult.consumesAction()
                && OverworldBuildHeightControl.isOverworld(level)
                && wasBlockPlacementAttempt(player, itemStack)) {
            int effectiveMaxY = OverworldBuildHeightControl.getEffectiveMaxY(level);
            if (effectiveMaxY < level.getMaxY()) {
                BlockPlaceContext placeContext = new BlockPlaceContext(player, hand, itemStack, blockHit);
                BlockPos targetPos = placeContext.getClickedPos();
                if (targetPos.getY() > effectiveMaxY) {
                    player.sendBuildLimitMessage(true, effectiveMaxY);
                }
            }
        }
        return interactionResult;
    }
}
