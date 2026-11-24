package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 使脚手架延展提示使用可热加载的主世界建筑高度上限。
 */
@Mixin(ScaffoldingBlockItem.class)
public abstract class ScaffoldingBlockItemMixin {

    @Redirect(
            method = "updatePlacementContext",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMaxY()I")
    )
    private int tuanzis_mod$getEffectiveMaxY(Level level) {
        return OverworldBuildHeightControl.getEffectiveMaxY(level);
    }
}
