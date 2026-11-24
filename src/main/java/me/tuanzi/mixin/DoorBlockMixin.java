package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 防止双格门方块的上半部分越过主世界配置的建筑高度上限。
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    @Redirect(
            method = "getStateForPlacement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMaxY()I")
    )
    private int tuanzis_mod$getEffectiveMaxY(Level level) {
        return OverworldBuildHeightControl.getEffectiveMaxY(level);
    }
}
