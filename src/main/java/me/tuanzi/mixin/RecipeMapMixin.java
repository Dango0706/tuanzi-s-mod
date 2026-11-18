package me.tuanzi.mixin;

import me.tuanzi.config.CasinoRecipeControl;
import me.tuanzi.config.ModConfig;
import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 在 RecipeMap 构建阶段按模组配置过滤赌场核心合成配方，
 * 同步覆盖合成台、背包合成、自动合成器、原版配方书与 JEI 同步数据。
 */
@Mixin(RecipeMap.class)
public abstract class RecipeMapMixin {

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
    private static HolderLookup<Recipe<?>> tuanzis_mod$filterCasinoRecipes(HolderLookup<Recipe<?>> recipes) {
        ModConfig.load();
        if (ModConfig.isCasinoRecipesEnabled()) {
            return recipes;
        }

        ModLog.debug("赌场配方总开关已关闭，过滤核心赌场合成配方。");

        if (recipes instanceof HolderLookup.RegistryLookup<Recipe<?>> registryLookup) {
            return new HolderLookup.RegistryLookup.Delegate<>() {
                @Override
                public HolderLookup.RegistryLookup<Recipe<?>> parent() {
                    return registryLookup;
                }

                @Override
                public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> id) {
                    if (CasinoRecipeControl.isRecipeBlocked(id.identifier())) {
                        return Optional.empty();
                    }
                    return this.parent().get(id);
                }

                @Override
                public Stream<Holder.Reference<Recipe<?>>> listElements() {
                    return this.parent().listElements()
                            .filter(ref -> !CasinoRecipeControl.isRecipeBlocked(ref.key().identifier()));
                }
            };
        }

        return new HolderLookup<>() {
            @Override
            public Stream<Holder.Reference<Recipe<?>>> listElements() {
                return recipes.listElements()
                        .filter(ref -> !CasinoRecipeControl.isRecipeBlocked(ref.key().identifier()));
            }

            @Override
            public Stream<HolderSet.Named<Recipe<?>>> listTags() {
                return recipes.listTags();
            }

            @Override
            public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> id) {
                if (CasinoRecipeControl.isRecipeBlocked(id.identifier())) {
                    return Optional.empty();
                }
                return recipes.get(id);
            }

            @Override
            public Optional<HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> id) {
                return recipes.get(id);
            }
        };
    }
}
