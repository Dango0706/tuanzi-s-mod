package me.tuanzi.client.renderer.blueprint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.tuanzi.block.cannon.BlueprintBuildPlanner;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 蓝图 3D 预烘焙网格缓存：
 * 使用 Minecraft 原生 ModelBlockRenderer + FluidRenderer + SpecialModelRenderer + BlockEntityRenderer
 * 一次性烘焙当前切层下所有完整/非完整方块、流体及模组多方块网格，供 PiP 渲染器零重烘焙重放。
 */
@Environment(EnvType.CLIENT)
public final class BlueprintPreviewMesh {

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final int VIRTUAL_POS_OFFSET = 1_000_000;

    public final LayerBatch solidBatch = new LayerBatch();
    public final LayerBatch cutoutBatch = new LayerBatch();
    public final LayerBatch translucentBatch = new LayerBatch();
    public final List<SpecialModelEntry> specialModels = new ArrayList<>();
    public final List<BlockEntityEntry> blockEntities = new ArrayList<>();

    private BlueprintPreviewMesh() {
    }

    public static BlueprintPreviewMesh build(
            Minecraft mc,
            int width,
            int height,
            int length,
            BlockState[] palette,
            int[] blocks,
            Map<BlockPos, CompoundTag> teNbtMap,
            int currentLayer,
            boolean singleLayerMode
    ) {
        BlueprintPreviewMesh mesh = new BlueprintPreviewMesh();
        if (width <= 0 || height <= 0 || length <= 0 || palette.length == 0 || blocks.length == 0) {
            return mesh;
        }

        BlueprintPreviewLevel previewLevel = new BlueprintPreviewLevel(
                width, height, length, palette, blocks, teNbtMap, currentLayer, singleLayerMode
        );
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(false, true, mc.getBlockColors());
        FluidRenderer fluidRenderer = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
        var stateModelSet = mc.getModelManager().getBlockStateModelSet();
        var blockModelSet = mc.getModelManager().getBlockModelSet();
        var berDispatcher = mc.getBlockEntityRenderDispatcher();
        Level clientLevel = mc.level;

        Map<BlockState, BlockModelRenderState> specialStateCache = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] emittedCounter = new int[1];
        Block[] currentBlockRef = new Block[1];
        int[] currentColorRef = new int[]{-1, -1};

        BlockQuadOutput quadOutput = (qx, qy, qz, quad, instance) -> {
            emittedCounter[0]++;
            LayerBatch target = mesh.batchFor(quad.materialInfo().layer());
            target.addBakedQuad(qx, qy, qz, quad, instance, currentBlockRef[0], currentColorRef[0], currentColorRef[1]);
        };

        FluidVertexCollector fluidCollector = new FluidVertexCollector(mesh);

        int yStart = singleLayerMode ? Math.clamp(currentLayer, 0, height - 1) : 0;
        int yEnd = singleLayerMode ? yStart : Math.clamp(currentLayer, 0, height - 1);

        for (int y = yStart; y <= yEnd; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    long idxLong = (long) y * (long) width * (long) length + (long) z * (long) width + (long) x;
                    if (idxLong < 0 || idxLong >= blocks.length) continue;
                    int idx = (int) idxLong;
                    int pIdx = blocks[idx];
                    if (pIdx < 0 || pIdx >= palette.length) continue;

                    BlockState state = palette[pIdx];
                    if (state == null || state.isAir() || state.is(Blocks.BARRIER) || state.is(Blocks.STRUCTURE_VOID)) {
                        continue;
                    }

                    pos.set(x, y, z);
                    Block block = state.getBlock();
                    currentBlockRef[0] = block;
                    int[] reqCols = BlueprintBuildPlanner.getRequiredColors(state, teNbtMap.get(pos));
                    if (reqCols.length > 0) {
                        currentColorRef[0] = reqCols[0];
                        currentColorRef[1] = reqCols.length > 1 ? reqCols[1] : reqCols[0];
                    } else {
                        currentColorRef[0] = -1;
                        currentColorRef[1] = -1;
                    }
                    emittedCounter[0] = 0;

                    // 1. 流体渲染（支持水、岩浆与含水方块 waterlogged=true）
                    FluidState fluidState = state.getFluidState();
                    if (!fluidState.isEmpty()) {
                        fluidCollector.beginBlock(x, y, z, block);
                        fluidRenderer.tesselate(previewLevel, pos, fluidCollector, state, fluidState);
                    }

                    // 2. 通用方块模型渲染（完整方块、火把、灯笼、花朵、红石粉、模组多方块模型零件等）
                    if (!(block instanceof LiquidBlock)) {
                        BlockStateModel model = stateModelSet.get(state);
                        blockRenderer.tesselateBlock(
                                quadOutput,
                                (float) x,
                                (float) y,
                                (float) z,
                                previewLevel,
                                pos,
                                state,
                                model,
                                state.getSeed(pos)
                        );
                    }

                    // 3. 方块实体渲染（赌场多方块动态部件、告示牌文字、箱子、旗帜等；跳过无 BER 的实体方块如彩色方块以加速大蓝图烘焙）
                    boolean hasBer = false;
                    if (block instanceof EntityBlock entityBlock && clientLevel != null) {
                        BlockPos virtualPos = new BlockPos(x + VIRTUAL_POS_OFFSET, y, z + VIRTUAL_POS_OFFSET);
                        try {
                            BlockEntity be = entityBlock.newBlockEntity(virtualPos, state);
                            if (be != null && berDispatcher.getRenderer(be) != null) {
                                be.setLevel(clientLevel);
                                CompoundTag rawTeNbt = teNbtMap.get(pos);
                                if (rawTeNbt != null) {
                                    CompoundTag loadedNbt = rawTeNbt.copy();
                                    loadedNbt.putInt("x", virtualPos.getX());
                                    loadedNbt.putInt("y", virtualPos.getY());
                                    loadedNbt.putInt("z", virtualPos.getZ());
                                    be.loadWithComponents(TagValueInput.create(
                                            ProblemReporter.DISCARDING, clientLevel.registryAccess(), loadedNbt
                                    ));
                                }
                                mesh.blockEntities.add(new BlockEntityEntry(x, y, z, block, be));
                                hasBer = true;
                            }
                        } catch (Exception e) {
                            ModLog.debug("[BlueprintPreviewMesh] 创建预览 BlockEntity 跳过 " + state + " @" + pos + ": " + e.getMessage());
                        }
                    }

                    // 4. SpecialModelRenderer 兜底（当 BlockStateModel 产生 0 个面且无 BER 接管时）
                    if (emittedCounter[0] == 0 && !(block instanceof LiquidBlock) && !hasBer) {
                        BlockModelRenderState specialRs = specialStateCache.computeIfAbsent(state, st -> {
                            BlockModelRenderState rs = new BlockModelRenderState();
                            blockModelSet.get(st).update(rs, st, DISPLAY_CONTEXT, 42L);
                            rs.blockLightCoords = LightCoordsUtil.FULL_BRIGHT;
                            return rs;
                        });
                        if (!specialRs.isEmpty()) {
                            mesh.specialModels.add(new SpecialModelEntry(x, y, z, block, specialRs));
                        }
                    }
                }
            }
        }

        ModLog.debug("[BlueprintPreviewMesh] 网格烘焙完成: solid=" + mesh.solidBatch.quadCount
                + ", cutout=" + mesh.cutoutBatch.quadCount
                + ", translucent=" + mesh.translucentBatch.quadCount
                + ", special=" + mesh.specialModels.size()
                + ", blockEntities=" + mesh.blockEntities.size()
                + ", tintedColorLookups=" + previewLevel.getTintedBlockLookupCount());
        return mesh;
    }

    public LayerBatch batchFor(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> solidBatch;
            case CUTOUT -> cutoutBatch;
            case TRANSLUCENT -> translucentBatch;
        };
    }

    public record SpecialModelEntry(int x, int y, int z, Block block, BlockModelRenderState renderState) {
    }

    public record BlockEntityEntry(int x, int y, int z, Block block, BlockEntity entity) {
    }

    /**
     * 紧凑型四边形顶点批处理容器，支持按 selectedBlock 零拷贝过滤重绘。
     */
    public static final class LayerBatch {
        private static final int INITIAL_QUADS = 64;
        private float[] xyz = new float[INITIAL_QUADS * 12];
        private float[] uv = new float[INITIAL_QUADS * 8];
        private int[] colors = new int[INITIAL_QUADS * 4];
        private int[] lights = new int[INITIAL_QUADS * 4];
        private Block[] blocks = new Block[INITIAL_QUADS];
        private int[] blockColors = new int[INITIAL_QUADS];
        private int[] altBlockColors = new int[INITIAL_QUADS];
        public int quadCount = 0;

        private void ensureCapacity() {
            if (quadCount < blocks.length) return;
            int newCap = blocks.length * 2;
            xyz = Arrays.copyOf(xyz, newCap * 12);
            uv = Arrays.copyOf(uv, newCap * 8);
            colors = Arrays.copyOf(colors, newCap * 4);
            lights = Arrays.copyOf(lights, newCap * 4);
            blocks = Arrays.copyOf(blocks, newCap);
            blockColors = Arrays.copyOf(blockColors, newCap);
            altBlockColors = Arrays.copyOf(altBlockColors, newCap);
        }

        void addBakedQuad(float ox, float oy, float oz, BakedQuad quad, QuadInstance instance, Block block, int blockColor, int altBlockColor) {
            ensureCapacity();
            int qi = quadCount++;
            blocks[qi] = block;
            blockColors[qi] = blockColor;
            altBlockColors[qi] = altBlockColor;

            int lightEmission = quad.materialInfo().lightEmission();
            int basePos = qi * 12;
            int baseUv = qi * 8;
            int baseCol = qi * 4;

            for (int v = 0; v < 4; v++) {
                Vector3fc p = quad.position(v);
                long packedUv = quad.packedUV(v);
                xyz[basePos + v * 3] = p.x() + ox;
                xyz[basePos + v * 3 + 1] = p.y() + oy;
                xyz[basePos + v * 3 + 2] = p.z() + oz;

                uv[baseUv + v * 2] = UVPair.unpackU(packedUv);
                uv[baseUv + v * 2 + 1] = UVPair.unpackV(packedUv);

                int rawColor = instance.getColor(v);
                colors[baseCol + v] = (quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT)
                        ? rawColor
                        : ARGB.opaque(rawColor);
                lights[baseCol + v] = instance.getLightCoordsWithEmission(v, lightEmission);
            }
        }

        void addFluidQuad(float[] qX, float[] qY, float[] qZ, float[] qU, float[] qV, int[] qCol, int[] qLight, Block block) {
            ensureCapacity();
            int qi = quadCount++;
            blocks[qi] = block;
            blockColors[qi] = -1;
            altBlockColors[qi] = -1;
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
                lights[baseCol + v] = qLight[v];
            }
        }

        private boolean matchesSelection(int idx, @Nullable Block selectedBlock, @Nullable Integer selectedColor) {
            if (selectedBlock == null) return true;
            if (blocks[idx] != selectedBlock) return false;
            if (selectedColor == null) return true;
            int target = selectedColor & 0xFFFFFF;
            return blockColors[idx] == target || altBlockColors[idx] == target;
        }

        public boolean hasAnyMatching(@Nullable Block selectedBlock, @Nullable Integer selectedColor, boolean matchSelected) {
            if (quadCount == 0) return false;
            if (selectedBlock == null) return matchSelected;
            for (int i = 0; i < quadCount; i++) {
                if (matchesSelection(i, selectedBlock, selectedColor) == matchSelected) {
                    return true;
                }
            }
            return false;
        }

        public void writeTo(PoseStack.Pose pose, VertexConsumer vc, @Nullable Block selectedBlock, @Nullable Integer selectedColor, boolean matchSelected, boolean dimmedTranslucent) {
            for (int i = 0; i < quadCount; i++) {
                boolean matches = matchesSelection(i, selectedBlock, selectedColor);
                if (matches != matchSelected) continue;

                int basePos = i * 12;
                int baseUv = i * 8;
                int baseCol = i * 4;

                for (int v = 0; v < 4; v++) {
                    int col = colors[baseCol + v];
                    if (selectedBlock != null) {
                        if (dimmedTranslucent) {
                            int r = Math.round(ARGB.red(col) * 0.55F);
                            int g = Math.round(ARGB.green(col) * 0.55F);
                            int b = Math.round(ARGB.blue(col) * 0.55F);
                            col = ARGB.color(38, r, g, b);
                        } else {
                            int a = ARGB.alpha(col);
                            int r = Math.min(255, ARGB.red(col) + 28);
                            int g = Math.min(255, ARGB.green(col) + 28);
                            int b = Math.min(255, ARGB.blue(col) + 12);
                            col = ARGB.color(a, r, g, b);
                        }
                    }

                    vc.addVertex(
                            pose,
                            xyz[basePos + v * 3],
                            xyz[basePos + v * 3 + 1],
                            xyz[basePos + v * 3 + 2]
                    ).setColor(col).setUv(uv[baseUv + v * 2], uv[baseUv + v * 2 + 1]).setLight(lights[baseCol + v]);
                }
            }
        }
    }

    /**
     * 捕获 FluidRenderer 输出的流体四边形，并将其区块内局部坐标 (& 15) 还原为蓝图绝对局部坐标。
     */
    private static final class FluidVertexCollector implements FluidRenderer.Output {
        private final BlueprintPreviewMesh mesh;
        private int chunkBaseX;
        private int chunkBaseY;
        private int chunkBaseZ;
        private Block currentBlock = Blocks.WATER;

        private final float[] qX = new float[4];
        private final float[] qY = new float[4];
        private final float[] qZ = new float[4];
        private final float[] qU = new float[4];
        private final float[] qV = new float[4];
        private final int[] qCol = new int[4];
        private final int[] qLight = new int[4];
        private int vertexIndex = 0;
        private LayerBatch activeBatch;

        private final VertexConsumer consumer = new VertexConsumer() {
            private float vx, vy, vz;
            private int vCol = -1;
            private float vu, vv;
            private int vLight = LightCoordsUtil.FULL_BRIGHT;

            private void flushIfReady() {
                // addVertex(11-arg) 在最后一步调用 setNormal；而部分路径可能只设前几个属性，我们在每次 11-arg addVertex 或下一次 addVertex 时提交
            }

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
                qLight[i] = lightCoords;
                if (vertexIndex == 4) {
                    activeBatch.addFluidQuad(qX, qY, qZ, qU, qV, qCol, qLight, currentBlock);
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
                vLight = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
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
                qLight[i] = vLight;
                if (vertexIndex == 4) {
                    activeBatch.addFluidQuad(qX, qY, qZ, qU, qV, qCol, qLight, currentBlock);
                    vertexIndex = 0;
                }
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float width) {
                return this;
            }
        };

        FluidVertexCollector(BlueprintPreviewMesh mesh) {
            this.mesh = mesh;
            this.activeBatch = mesh.translucentBatch;
        }

        void beginBlock(int x, int y, int z, Block block) {
            this.chunkBaseX = x & ~15;
            this.chunkBaseY = y & ~15;
            this.chunkBaseZ = z & ~15;
            this.currentBlock = block;
            this.vertexIndex = 0;
        }

        @Override
        public VertexConsumer getBuilder(ChunkSectionLayer layer) {
            this.activeBatch = mesh.batchFor(layer);
            this.vertexIndex = 0;
            return consumer;
        }
    }
}
