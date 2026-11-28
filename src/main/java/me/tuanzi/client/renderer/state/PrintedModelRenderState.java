package me.tuanzi.client.renderer.state;

import me.tuanzi.block.PrintedModelMountMode;
import me.tuanzi.client.canvas.PrintedModelChunkedMesh;
import me.tuanzi.world.canvas.CanvasType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PrintedModelRenderState extends BlockEntityRenderState {
    public @Nullable PrintedModelChunkedMesh mesh;
    public int sizeX = 16;
    public int sizeY = 1;
    public int sizeZ = 16;
    public int printScale = 1;
    public CanvasType canvasType = CanvasType.FLAT_2D;
    public Direction facing = Direction.NORTH;
    public PrintedModelMountMode mountMode = PrintedModelMountMode.FLOOR;
    public boolean useLod = false;
    public boolean skipRender = false;
}
