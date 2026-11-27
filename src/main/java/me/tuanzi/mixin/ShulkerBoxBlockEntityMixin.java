package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁止将空白蓝图与结构蓝图通过漏斗、投掷器或容器接口自动写入潜影盒。
 */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin extends RandomizableContainerBlockEntity implements WorldlyContainer {

    protected ShulkerBoxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    private long tuanzis_mod$lastRejectLogTick = Long.MIN_VALUE;

    @Unique
    private static boolean tuanzis_mod$isForbiddenBlueprint(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.BLANK_BLUEPRINT) || stack.is(ModItems.STRUCTURE_BLUEPRINT));
    }

    @Unique
    private void tuanzis_mod$logRejectThrottled(String reason, ItemStack stack) {
        long now = this.level != null ? this.level.getGameTime() : 0L;
        if (this.level == null || now - this.tuanzis_mod$lastRejectLogTick >= 40L) {
            this.tuanzis_mod$lastRejectLogTick = now;
            ModLog.debug("[ShulkerBoxBlockEntityMixin] " + reason + ": " + stack.getItem());
        }
    }

    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$blockBlueprintsThroughFace(
            int slot,
            ItemStack itemStack,
            @Nullable Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (tuanzis_mod$isForbiddenBlueprint(itemStack)) {
            tuanzis_mod$logRejectThrottled("拦截蓝图物品从外部面进入潜影盒", itemStack);
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        if (tuanzis_mod$isForbiddenBlueprint(itemStack)) {
            tuanzis_mod$logRejectThrottled("拦截蓝图物品写入潜影盒容器", itemStack);
            return false;
        }
        return super.canPlaceItem(slot, itemStack);
    }
}
