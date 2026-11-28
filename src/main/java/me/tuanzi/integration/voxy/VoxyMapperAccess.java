package me.tuanzi.integration.voxy;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Duck 接口，由 {@code VoxyMapperMixin} 实现于 {@code me.cortex.voxy.common.world.other.Mapper}。
 */
public interface VoxyMapperAccess {
    int tuanzis_mod$getIdForColoredBlockState(BlockState state, int primaryColor, int topColor);

    @Nullable
    VoxyStateEntryAccess tuanzis_mod$getStateEntryByBlockId(int blockId);
}
