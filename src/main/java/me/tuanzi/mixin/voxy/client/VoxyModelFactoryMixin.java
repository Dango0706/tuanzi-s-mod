package me.tuanzi.mixin.voxy.client;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import me.cortex.voxy.client.core.model.ColourDepthTextureData;
import me.cortex.voxy.client.core.model.ModelFactory;
import me.cortex.voxy.client.core.model.ModelQueries;
import me.cortex.voxy.common.world.other.Mapper;
import me.tuanzi.integration.voxy.VoxyColorSupport;
import me.tuanzi.integration.voxy.VoxyMapperAccess;
import me.tuanzi.integration.voxy.VoxyStateEntryAccess;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(value = ModelFactory.class, remap = false)
public abstract class VoxyModelFactoryMixin {
    @Shadow
    @Final
    private Mapper mapper;
    @Shadow
    @Final
    private int[] idMappings;
    @Shadow
    @Final
    private long[] metadataCache;

    @Shadow
    private static List<BlockTintSource> getTintSources(BlockState block) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean isBiomeDependentColour(List<BlockTintSource> tintSources, BlockState state) {
        throw new AssertionError();
    }

    @Shadow
    private static int captureColourConstant(List<BlockTintSource> tintSources, BlockState state, Biome biome) {
        throw new AssertionError();
    }

    @Unique
    private Int2IntOpenHashMap tuanzis_mod$biomeModelCustomColors;

    /**
     * +1 偏移存储当前正在烘焙的颜色（0 表示无自定义颜色），防止仅零初始化时误判。
     */
    @Unique
    private int tuanzis_mod$currentColorPlusOne;

    @Unique
    private boolean tuanzis_mod$currentDualPremultiplied;

    /**
     * +1 偏移存储 addBiome0 当前处理的 modelId（0 表示无）。
     */
    @Unique
    private int tuanzis_mod$currentBiomeModelIdPlusOne;

    @Unique
    private Int2IntOpenHashMap tuanzis_mod$getBiomeModelCustomColors() {
        if (this.tuanzis_mod$biomeModelCustomColors == null) {
            Int2IntOpenHashMap map = new Int2IntOpenHashMap();
            map.defaultReturnValue(-1);
            this.tuanzis_mod$biomeModelCustomColors = map;
        }
        return this.tuanzis_mod$biomeModelCustomColors;
    }

    @Inject(method = "processTextureBakeResult", at = @At("HEAD"), require = 0)
    private void tuanzis_mod$prepareColoredBake(
            int blockId,
            BlockState blockState,
            ColourDepthTextureData[] textureData,
            boolean isShaded,
            boolean darkenedTinting,
            ChunkSectionLayer layer,
            CallbackInfoReturnable<Object> cir
    ) {
        this.tuanzis_mod$currentColorPlusOne = 0;
        this.tuanzis_mod$currentDualPremultiplied = false;

        VoxyStateEntryAccess entry = ((VoxyMapperAccess) this.mapper).tuanzis_mod$getStateEntryByBlockId(blockId);
        if (entry == null) {
            return;
        }
        int customColor = entry.tuanzis_mod$getCustomColor();
        if (customColor < 0) {
            return;
        }

        int customTopColor = entry.tuanzis_mod$getCustomTopColor();
        this.tuanzis_mod$currentColorPlusOne = (customColor & 0xFFFFFF) + 1;
        if (customTopColor >= 0 && customTopColor != customColor) {
            for (int face = 0; face < 6; face++) {
                ColourDepthTextureData old = textureData[face];
                int[] tinted = VoxyColorSupport.applyDualSlabColorsToFace(
                        face, old.colour(), old.width(), old.height(), customColor, customTopColor
                );
                textureData[face] = new ColourDepthTextureData(tinted, old.depth(), old.width(), old.height());
            }
            this.tuanzis_mod$currentDualPremultiplied = true;
        }
    }

    @Redirect(
            method = "processTextureBakeResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/client/core/model/ModelFactory;getTintSources(Lnet/minecraft/world/level/block/state/BlockState;)Ljava/util/List;"
            ),
            require = 0
    )
    private List<BlockTintSource> tuanzis_mod$disableTintForDualSlab(BlockState blockState) {
        if (this.tuanzis_mod$currentDualPremultiplied) {
            return null;
        }
        return getTintSources(blockState);
    }

    @Redirect(
            method = "processTextureBakeResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/client/core/model/ModelFactory;isBiomeDependentColour(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            ),
            require = 0
    )
    private boolean tuanzis_mod$overrideBiomeDependent(List<BlockTintSource> tintSources, BlockState state) {
        if (this.tuanzis_mod$currentColorPlusOne > 0) {
            return false;
        }
        return isBiomeDependentColour(tintSources, state);
    }

    @Redirect(
            method = "processTextureBakeResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/client/core/model/ModelFactory;captureColourConstant(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/biome/Biome;)I"
            ),
            require = 0
    )
    private int tuanzis_mod$overrideCapturedColor(List<BlockTintSource> tintSources, BlockState state, Biome biome) {
        int c = this.tuanzis_mod$currentColorPlusOne - 1;
        if (c >= 0 && !this.tuanzis_mod$currentDualPremultiplied) {
            return 0xFF000000 | c;
        }
        return captureColourConstant(tintSources, state, biome);
    }

    @Inject(method = "processTextureBakeResult", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$finishColoredBake(
            int blockId,
            BlockState blockState,
            ColourDepthTextureData[] textureData,
            boolean isShaded,
            boolean darkenedTinting,
            ChunkSectionLayer layer,
            CallbackInfoReturnable<Object> cir
    ) {
        int c = this.tuanzis_mod$currentColorPlusOne - 1;
        if (c >= 0 && !this.tuanzis_mod$currentDualPremultiplied) {
            int modelId = this.idMappings[blockId];
            if (modelId >= 0 && ModelQueries.isBiomeColoured(this.metadataCache[modelId])) {
                tuanzis_mod$getBiomeModelCustomColors().put(modelId, c);
            }
        }
        this.tuanzis_mod$currentColorPlusOne = 0;
        this.tuanzis_mod$currentDualPremultiplied = false;
    }

    @Redirect(
            method = "addBiome0",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/system/MemoryUtil;memPutLong(JJ)V"
            ),
            require = 0
    )
    private void tuanzis_mod$trackBiomeEntryModel(long ptr, long value) {
        this.tuanzis_mod$currentBiomeModelIdPlusOne = ((int) (value & 0xFFFFFFFFL)) + 1;
        MemoryUtil.memPutLong(ptr, value);
    }

    @Redirect(
            method = "addBiome0",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/client/core/model/ModelFactory;captureColourConstant(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/biome/Biome;)I"
            ),
            require = 0
    )
    private int tuanzis_mod$overrideBiomeLutColor(List<BlockTintSource> tintSources, BlockState state, Biome biome) {
        int modelId = this.tuanzis_mod$currentBiomeModelIdPlusOne - 1;
        if (modelId >= 0) {
            int customColor = tuanzis_mod$getBiomeModelCustomColors().get(modelId);
            if (customColor >= 0) {
                return 0xFF000000 | customColor;
            }
        }
        return captureColourConstant(tintSources, state, biome);
    }

    @Inject(method = "addBiome0", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$resetTrackedBiomeModel(int id, Biome biome, CallbackInfoReturnable<Object> cir) {
        this.tuanzis_mod$currentBiomeModelIdPlusOne = 0;
    }
}
