package me.tuanzi.mixin;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.world.inventory.ScavengerEnderChestContainer;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public abstract class EnderChestBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$openScavengerEnderChest(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            if (level.getBlockEntity(pos) instanceof EnderChestBlockEntity enderChest) {
                BlockPos above = pos.above();
                if (level.getBlockState(above).isRedstoneConductor(level, above)) {
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                if (level instanceof ServerLevel) {
                    if (player instanceof ScavengerPlayerTracker tracker) {
                        ScavengerEnderChestContainer container = tracker.tuanzis_mod$getScavengerEnderChest();
                        container.setActiveChest(enderChest);
                        player.openMenu(new SimpleMenuProvider(
                            (containerId, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, container, 1),
                            Component.translatable("container.tuanzis_mod.scavenger_enderchest")
                        ));
                        player.awardStat(Stats.OPEN_ENDERCHEST);
                        cir.setReturnValue(InteractionResult.SUCCESS);
                        return;
                    }
                }

                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
