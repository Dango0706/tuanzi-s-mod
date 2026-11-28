package me.tuanzi.mixin.voxy;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import me.cortex.voxy.common.voxelization.ILightingSupplier;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.voxelization.WorldConversionFactory;
import me.cortex.voxy.common.world.other.Mapper;
import me.tuanzi.integration.voxy.VoxyColorIngestContext;
import me.tuanzi.integration.voxy.VoxyColorSectionSnapshot;
import me.tuanzi.integration.voxy.VoxyColorSupport;
import me.tuanzi.integration.voxy.VoxyMapperAccess;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = WorldConversionFactory.class, remap = false)
public class VoxyWorldConversionFactoryMixin {
    @Inject(
            method = "convert(Lme/cortex/voxy/common/voxelization/VoxelizedSection;Lme/cortex/voxy/common/world/other/Mapper;Lnet/minecraft/world/level/chunk/PalettedContainer;Lnet/minecraft/world/level/chunk/PalettedContainerRO;Lme/cortex/voxy/common/voxelization/ILightingSupplier;ZJ)Lme/cortex/voxy/common/voxelization/VoxelizedSection;",
            at = @At("RETURN"),
            require = 0
    )
    private static void tuanzis_mod$applyColoredBlockEntities(
            VoxelizedSection section,
            Mapper stateMapper,
            PalettedContainer<BlockState> blockContainer,
            PalettedContainerRO<Holder<Biome>> biomeContainer,
            ILightingSupplier lightSupplier,
            boolean shouldZoom,
            long zoomSeed,
            CallbackInfoReturnable<VoxelizedSection> cir
    ) {
        VoxyColorSectionSnapshot snapshot = VoxyColorIngestContext.getCurrent();
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        VoxelizedSection result = cir.getReturnValue();
        if (result == null) {
            return;
        }

        long[] data = result.section;
        VoxyMapperAccess mapperAccess = (VoxyMapperAccess) stateMapper;
        for (Int2LongMap.Entry entry : snapshot.entries()) {
            int localIdx = entry.getIntKey();
            int lx = localIdx & 0xF;
            int lz = (localIdx >> 4) & 0xF;
            int ly = (localIdx >> 8) & 0xF;

            BlockState state = blockContainer.get(lx, ly, lz);
            if (!VoxyColorSupport.isColorBlockState(state)) {
                continue;
            }
            long packed = entry.getLongValue();
            int coloredId = mapperAccess.tuanzis_mod$getIdForColoredBlockState(
                    state,
                    VoxyColorSupport.unpackPrimary(packed),
                    VoxyColorSupport.unpackTop(packed)
            );
            if (coloredId > 0) {
                data[localIdx] = Mapper.withBlock(data[localIdx], coloredId);
            }
        }
    }
}
