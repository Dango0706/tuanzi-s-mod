package me.tuanzi.client.renderer.state;

import me.tuanzi.block.cannon.LaunchedItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class BlueprintCannonRenderState extends BlockEntityRenderState {
    public double yaw;
    public double pitch;
    public double recoil;
    public BlockPos cannonPos = BlockPos.ZERO;
    public final List<LaunchedItem> flyingBlocks = new ArrayList<>();
    public boolean firstRenderTick = true;
}
