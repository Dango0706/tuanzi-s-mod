package me.tuanzi.client.canvas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import me.tuanzi.world.canvas.CanvasPaletteEntry;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.canvas.CanvasVoxelData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打印模型 16³ 分块静态烘焙网格与远距 4× 降采样 LOD 网格：
 * 利用 Minecraft 原生 ModelBlockRenderer + FluidRenderer 一次烘焙，零逐帧重烘焙。
 */
@Environment(EnvType.CLIENT)
public final class PrintedModelChunkedMesh {
    public static final int SUBCHUNK_SIZE = 16;

    public final List<SubChunkMesh> subChunks = new ArrayList<>();
    public final SubChunkMesh lodMesh = new SubChunkMesh();

    private PrintedModelChunkedMesh() {
    }

    public static PrintedModelChunkedMesh build(Minecraft mc, CanvasVoxelData data) {
        PrintedModelChunkedMesh result = new PrintedModelChunkedMesh();
        if (data == null || data.getNonEmptyCount() == 0) {
            return result;
        }

        List<CanvasPaletteEntry> paletteEntries = data.getPalette();
        BlockState[] resolvedPalette = new BlockState[paletteEntries.size()];
        for (int i = 0; i < paletteEntries.size(); i++) {
            resolvedPalette[i] = paletteEntries.get(i).resolveBlockState();
        }

        CanvasVoxelRenderLevel renderLevel = new CanvasVoxelRenderLevel(data, resolvedPalette);
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(false, true, mc.getBlockColors());
        FluidRenderer fluidRenderer = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
        var stateModelSet = mc.getModelManager().getBlockStateModelSet();

        Map<Long, SubChunkMesh> chunkMap = new HashMap<>();
        SubChunkMesh[] activeSubChunk = new SubChunkMesh[1];
        int[] currentLightEmission = new int[1];

        BlockQuadOutput quadOutput = (qx, qy, qz, quad, instance) -> {
            SubChunkMesh chunk = activeSubChunk[0];
            if (chunk == null) return;
            QuadBatch batch = chunk.batchFor(quad.materialInfo().layer());
            batch.addBakedQuad(qx, qy, qz, quad, instance, currentLightEmission[0]);
        };

        FluidCollector fluidCollector = new FluidCollector(activeSubChunk);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (Int2IntMap.Entry entry : data.getVoxelMap().int2IntEntrySet()) {
            int packed = entry.getIntKey();
            int pIdx = entry.getIntValue();
            if (pIdx < 0 || pIdx >= resolvedPalette.length) {
                continue;
            }
            BlockState state = resolvedPalette[pIdx];
            if (state == null || state.isAir()) {
                continue;
            }
            int x = CanvasVoxelData.unpackX(packed);
            int y = CanvasVoxelData.unpackY(packed);
            int z = CanvasVoxelData.unpackZ(packed);
            mpos.set(x, y, z);

            long chunkKey = (((long) (x >> 4) & 0xFFL) << 16)
                    | (((long) (y >> 4) & 0xFFL) << 8)
                    | ((long) (z >> 4) & 0xFFL);
            SubChunkMesh chunk = chunkMap.computeIfAbsent(chunkKey, k -> new SubChunkMesh());
            activeSubChunk[0] = chunk;
            currentLightEmission[0] = state.getLightEmission();

            FluidState fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
                fluidCollector.beginBlock(x, y, z);
                fluidRenderer.tesselate(renderLevel, mpos, fluidCollector, state, fluidState);
            }
            if (!(state.getBlock() instanceof LiquidBlock)) {
                BlockStateModel model = stateModelSet.get(state);
                blockRenderer.tesselateBlock(
                        quadOutput,
                        (float) x,
                        (float) y,
                        (float) z,
                        renderLevel,
                        mpos,
                        state,
                        model,
                        state.getSeed(mpos)
                );
            }
        }

        result.subChunks.addAll(chunkMap.values());
        buildLodMesh(mc, data, resolvedPalette, result.lodMesh);
        return result;
    }

    private static void buildLodMesh(
            Minecraft mc,
            CanvasVoxelData data,
            BlockState[] resolvedPalette,
            SubChunkMesh lodTarget
    ) {
        int step = (Math.max(data.getSizeX(), data.getSizeZ()) <= 16) ? 2 : 4;
        int lx = Math.max(1, (data.getSizeX() + step - 1) / step);
        int ly = data.getType() == CanvasType.FLAT_2D ? 1 : Math.max(1, (data.getSizeY() + step - 1) / step);
        int lz = Math.max(1, (data.getSizeZ() + step - 1) / step);

        CanvasVoxelData lodData = new CanvasVoxelData(lx, ly, lz, data.getType());
        for (Int2IntMap.Entry entry : data.getVoxelMap().int2IntEntrySet()) {
            int packed = entry.getIntKey();
            int pIdx = entry.getIntValue();
            if (pIdx < 0 || pIdx >= data.getPalette().size()) continue;
            int dx = CanvasVoxelData.unpackX(packed) / step;
            int dy = data.getType() == CanvasType.FLAT_2D ? 0 : (CanvasVoxelData.unpackY(packed) / step);
            int dz = CanvasVoxelData.unpackZ(packed) / step;
            if (lodData.getPaletteIndex(dx, dy, dz) < 0) {
                lodData.setVoxel(dx, dy, dz, data.getPalette().get(pIdx));
            }
        }

        BlockState[] lodPalette = new BlockState[lodData.getPalette().size()];
        for (int i = 0; i < lodPalette.length; i++) {
            lodPalette[i] = lodData.getPalette().get(i).resolveBlockState();
        }
        CanvasVoxelRenderLevel lodLevel = new CanvasVoxelRenderLevel(lodData, lodPalette);
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(false, true, mc.getBlockColors());
        var stateModelSet = mc.getModelManager().getBlockStateModelSet();

        int[] curEmit = new int[1];
        BlockQuadOutput lodOut = (qx, qy, qz, quad, instance) -> {
            QuadBatch batch = lodTarget.batchFor(quad.materialInfo().layer());
            batch.addScaledBakedQuad(qx * step, qy * step, qz * step, step, quad, instance, curEmit[0]);
        };

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (Int2IntMap.Entry entry : lodData.getVoxelMap().int2IntEntrySet()) {
            int packed = entry.getIntKey();
            int pIdx = entry.getIntValue();
            if (pIdx < 0 || pIdx >= lodPalette.length) continue;
            BlockState state = lodPalette[pIdx];
            if (state == null || state.isAir()) continue;
            int x = CanvasVoxelData.unpackX(packed);
            int y = CanvasVoxelData.unpackY(packed);
            int z = CanvasVoxelData.unpackZ(packed);
            mpos.set(x, y, z);
            curEmit[0] = state.getLightEmission();
            BlockStateModel model = stateModelSet.get(state);
            blockRenderer.tesselateBlock(lodOut, (float) x, (float) y, (float) z, lodLevel, mpos, state, model, state.getSeed(mpos));
        }
    }

    public boolean hasSolid() {
        for (SubChunkMesh c : subChunks) {
            if (c.solid.quadCount > 0) return true;
        }
        return false;
    }

    public boolean hasCutout() {
        for (SubChunkMesh c : subChunks) {
            if (c.cutout.quadCount > 0) return true;
        }
        return false;
    }

    public boolean hasTranslucent() {
        for (SubChunkMesh c : subChunks) {
            if (c.translucent.quadCount > 0) return true;
        }
        return false;
    }

    public void emitLayer(ChunkSectionLayer layer, PoseStack.Pose pose, VertexConsumer vc, int worldLight, boolean useLod) {
        if (useLod) {
            lodMesh.batchFor(layer).writeTo(pose, vc, worldLight);
            return;
        }
        for (SubChunkMesh chunk : subChunks) {
            chunk.batchFor(layer).writeTo(pose, vc, worldLight);
        }
    }

    public static final class SubChunkMesh {
        public final QuadBatch solid = new QuadBatch();
        public final QuadBatch cutout = new QuadBatch();
        public final QuadBatch translucent = new QuadBatch();

        public QuadBatch batchFor(ChunkSectionLayer layer) {
            return switch (layer) {
                case SOLID -> solid;
                case CUTOUT -> cutout;
                case TRANSLUCENT -> translucent;
            };
        }
    }

    public static final class QuadBatch {
        private static final int INITIAL_QUADS = 32;
        private float[] xyz = new float[INITIAL_QUADS * 12];
        private float[] uv = new float[INITIAL_QUADS * 8];
        private int[] colors = new int[INITIAL_QUADS * 4];
        private boolean[] emissive = new boolean[INITIAL_QUADS];
        public int quadCount = 0;

        private void ensureCapacity() {
            if (quadCount < emissive.length) return;
            int newCap = emissive.length * 2;
            xyz = Arrays.copyOf(xyz, newCap * 12);
            uv = Arrays.copyOf(uv, newCap * 8);
            colors = Arrays.copyOf(colors, newCap * 4);
            emissive = Arrays.copyOf(emissive, newCap);
        }

        void addBakedQuad(float ox, float oy, float oz, BakedQuad quad, QuadInstance instance, int blockEmission) {
            addScaledBakedQuad(ox, oy, oz, 1.0f, quad, instance, blockEmission);
        }

        void addScaledBakedQuad(float ox, float oy, float oz, float scale, BakedQuad quad, QuadInstance instance, int blockEmission) {
            ensureCapacity();
            int qi = quadCount++;
            emissive[qi] = blockEmission > 0 || quad.materialInfo().lightEmission() > 0;
            int basePos = qi * 12;
            int baseUv = qi * 8;
            int baseCol = qi * 4;
            for (int v = 0; v < 4; v++) {
                Vector3fc p = quad.position(v);
                long packedUv = quad.packedUV(v);
                xyz[basePos + v * 3] = p.x() * scale + ox;
                xyz[basePos + v * 3 + 1] = p.y() * scale + oy;
                xyz[basePos + v * 3 + 2] = p.z() * scale + oz;
                uv[baseUv + v * 2] = UVPair.unpackU(packedUv);
                uv[baseUv + v * 2 + 1] = UVPair.unpackV(packedUv);
                int rawColor = instance.getColor(v);
                colors[baseCol + v] = (quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT)
                        ? rawColor
                        : ARGB.opaque(rawColor);
            }
        }

        void addFluidQuad(float[] qX, float[] qY, float[] qZ, float[] qU, float[] qV, int[] qCol, boolean isEmissive) {
            ensureCapacity();
            int qi = quadCount++;
            emissive[qi] = isEmissive;
            int basePos = qi * 12;
            int baseUv = qi * 8;
            int baseCol = qi * 4;
            for (int v = 0; v < 4; v++) {
                xyz[basePos + v * 3] = qX[v];
                xyz[basePos + v * 3 + 1] = qY[v];
                xyz[basePos + v * 3 + 2] = qZ[v];
                uv[baseUv + v * 2] = qU[v];
                uv[baseUv + v * 2 + 1] = qV[v];
                colors[baseCol + v] = qCol[v];
            }
        }

        public void writeTo(PoseStack.Pose pose, VertexConsumer vc, int worldLight) {
            for (int i = 0; i < quadCount; i++) {
                int basePos = i * 12;
                int baseUv = i * 8;
                int baseCol = i * 4;
                int light = emissive[i] ? LightCoordsUtil.FULL_BRIGHT : worldLight;
                for (int v = 0; v < 4; v++) {
                    vc.addVertex(
                            pose,
                            xyz[basePos + v * 3],
                            xyz[basePos + v * 3 + 1],
                            xyz[basePos + v * 3 + 2]
                    ).setColor(colors[baseCol + v]).setUv(uv[baseUv + v * 2], uv[baseUv + v * 2 + 1]).setLight(light);
                }
            }
        }
    }

    private static final class FluidCollector implements FluidRenderer.Output {
        private final SubChunkMesh[] activeSubChunk;
        private int chunkBaseX;
        private int chunkBaseY;
        private int chunkBaseZ;
        private final float[] qX = new float[4];
        private final float[] qY = new float[4];
        private final float[] qZ = new float[4];
        private final float[] qU = new float[4];
        private final float[] qV = new float[4];
        private final int[] qCol = new int[4];
        private int vertexIndex = 0;
        private QuadBatch activeBatch;

        private final VertexConsumer consumer = new VertexConsumer() {
            private float vx, vy, vz;
            private int vCol = -1;
            private float vu, vv;

            @Override
            public void addVertex(
                    float x, float y, float z,
                    int color, float u, float v,
                    int overlayCoords, int lightCoords,
                    float nx, float ny, float nz
            ) {
                int i = vertexIndex++;
                qX[i] = x + chunkBaseX;
                qY[i] = y + chunkBaseY;
                qZ[i] = z + chunkBaseZ;
                qU[i] = u;
                qV[i] = v;
                qCol[i] = color;
                if (vertexIndex == 4) {
                    activeBatch.addFluidQuad(qX, qY, qZ, qU, qV, qCol, false);
                    vertexIndex = 0;
                }
            }

            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                vx = x;
                vy = y;
                vz = z;
                return this;
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                vCol = ARGB.color(a, r, g, b);
                return this;
            }

            @Override
            public VertexConsumer setColor(int color) {
                vCol = color;
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                vu = u;
                vv = v;
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer setUv3(float u, float v) {
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                int i = vertexIndex++;
                qX[i] = vx + chunkBaseX;
                qY[i] = vy + chunkBaseY;
                qZ[i] = vz + chunkBaseZ;
                qU[i] = vu;
                qV[i] = vv;
                qCol[i] = vCol;
                if (vertexIndex == 4) {
                    activeBatch.addFluidQuad(qX, qY, qZ, qU, qV, qCol, false);
                    vertexIndex = 0;
                }
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float width) {
                return this;
            }
        };

        FluidCollector(SubChunkMesh[] activeSubChunk) {
            this.activeSubChunk = activeSubChunk;
        }

        void beginBlock(int x, int y, int z) {
            this.chunkBaseX = x & ~15;
            this.chunkBaseY = y & ~15;
            this.chunkBaseZ = z & ~15;
            this.vertexIndex = 0;
        }

        @Override
        public VertexConsumer getBuilder(ChunkSectionLayer layer) {
            SubChunkMesh chunk = activeSubChunk[0];
            this.activeBatch = chunk != null ? chunk.batchFor(layer) : new QuadBatch();
            this.vertexIndex = 0;
            return consumer;
        }
    }
}
