package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在从数据包加载 DIMENSION_TYPE 注册表时，将主世界物理最大高度扩展至原版安全上限 (Y = 2031)。
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    @Shadow
    public abstract Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo registrationInfo);

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$expandOverworldDimensionType(
            ResourceKey<T> key,
            T value,
            RegistrationInfo registrationInfo,
            CallbackInfoReturnable<Holder.Reference<T>> cir
    ) {
        if (OverworldBuildHeightControl.isExpandingDimensionTypeRegistry()
                && BuiltinDimensionTypes.OVERWORLD.equals(key)
                && value instanceof DimensionType dt) {
            DimensionType expanded = OverworldBuildHeightControl.expandOverworldDimensionType(dt);
            if (expanded != dt) {
                @SuppressWarnings("unchecked")
                T expandedValue = (T) expanded;
                cir.setReturnValue(this.register(key, expandedValue, registrationInfo));
            }
        }
    }
}
