package me.tuanzi.mixin;

import me.tuanzi.config.OverworldBuildHeightControl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

/**
 * 标记从服务端/本地资源包加载 DIMENSION_TYPE 注册表的阶段，以便扩展主世界物理高度上限。
 */
@Mixin(RegistryLoadTask.class)
public abstract class RegistryLoadTaskMixin<T> {

    @Shadow
    protected abstract ResourceKey<?> registryKey();

    @Inject(method = "registerElements", at = @At("HEAD"))
    private void tuanzis_mod$onRegisterElementsStart(Stream<?> elements, CallbackInfo ci) {
        if ((Object) this instanceof ResourceManagerRegistryLoadTask && Registries.DIMENSION_TYPE.equals(this.registryKey())) {
            OverworldBuildHeightControl.setExpandingDimensionTypeRegistry(true);
        }
    }

    @Inject(method = "registerElements", at = @At("RETURN"))
    private void tuanzis_mod$onRegisterElementsEnd(Stream<?> elements, CallbackInfo ci) {
        if (OverworldBuildHeightControl.isExpandingDimensionTypeRegistry()) {
            OverworldBuildHeightControl.setExpandingDimensionTypeRegistry(false);
        }
    }
}
