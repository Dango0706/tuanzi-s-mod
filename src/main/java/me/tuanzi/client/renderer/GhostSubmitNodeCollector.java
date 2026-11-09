package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.render.submit.ExtendedBlockModelSubmit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class GhostSubmitNodeCollector implements SubmitNodeCollector {
    private static final Function<ChunkSectionLayer, RenderType> GHOST_BLOCK_RENDER_TYPE = layer -> Sheets.translucentBlockItemSheet();

    private final SubmitNodeCollector parent;
    private final float alpha;

    public GhostSubmitNodeCollector(SubmitNodeCollector parent, float alpha) {
        this.parent = parent;
        this.alpha = alpha;
    }

    private static @Nullable SubmitNodeCollection resolveCollection(OrderedSubmitNodeCollector collector) {
        if (collector instanceof SubmitNodeStorage storage) {
            return storage.order(0);
        }
        if (collector instanceof SubmitNodeCollection collection) {
            return collection;
        }
        return null;
    }

    private static int[] ensureOpaqueTints(int[] tintLayers) {
        if (tintLayers == null || tintLayers.length == 0) {
            return tintLayers;
        }
        int[] normalized = new int[tintLayers.length];
        for (int i = 0; i < tintLayers.length; i++) {
            normalized[i] = ARGB.opaque(tintLayers[i]);
        }
        return normalized;
    }

    private static int scaleAlphaColor(int color, float alpha) {
        if (color == -1) {
            return ARGB.white(alpha);
        }
        int baseAlpha = ARGB.alpha(color);
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }
        int scaledAlpha = Math.clamp(Math.round(baseAlpha * alpha), 0, 255);
        return ARGB.color(scaledAlpha, color);
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new GhostOrderedSubmitNodeCollector(this.parent.order(order), this.alpha);
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        this.parent.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {
        this.parent.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        this.parent.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
    }

    @Override
    public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {
        this.parent.submitTextBackground(poseStack, x0, y0, x1, y1, color, displayMode, lightCoords);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.parent.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.parent.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor) {
        RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
        int ghostColor = scaleAlphaColor(tintedColor, this.alpha);
        this.parent.submitModel(model, state, poseStack, ghostType, lightCoords, overlayCoords, ghostColor, uvMapping, outlineColor);
    }

    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
        int ghostColor = scaleAlphaColor(tintedColor, this.alpha);
        this.parent.submitCrumblingOverlay(model, state, poseStack, ghostType, lightCoords, overlayCoords, ghostColor, crumblingOverlay);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        this.parent.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
    }

    @Override
    public void submitBlockModel(
        PoseStack poseStack,
        Function<ChunkSectionLayer, RenderType> renderTypeFunction,
        boolean hasTranslucent,
        List<BlockStateModelPart> modelParts,
        @Nullable Mesh mesh,
        int[] tintLayers,
        int lightCoords,
        int overlayCoords,
        int outlineColor
    ) {
        SubmitNodeCollection col = resolveCollection(this.parent);
        if (col != null) {
            col.translucentBlocksAndItems.submit(new ExtendedBlockModelSubmit(
                poseStack.last().copy(),
                GHOST_BLOCK_RENDER_TYPE,
                modelParts,
                mesh,
                ensureOpaqueTints(tintLayers),
                lightCoords,
                overlayCoords,
                ARGB.white(this.alpha),
                null
            ));
        } else {
            this.parent.submitBlockModel(poseStack, GHOST_BLOCK_RENDER_TYPE, true, modelParts, mesh, tintLayers, lightCoords, overlayCoords, outlineColor);
        }
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        SubmitNodeCollection col = resolveCollection(this.parent);
        if (col != null) {
            col.translucentBlocksAndItems.submit(new BlockModelFeatureRenderer.Submit(
                poseStack.last().copy(),
                Sheets.translucentBlockItemSheet(),
                parts,
                ensureOpaqueTints(tintLayers),
                lightCoords,
                overlayCoords,
                ARGB.white(this.alpha),
                null
            ));
        } else {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            this.parent.submitBlockModel(poseStack, ghostType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
        }
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
        this.parent.submitBreakingBlockModel(poseStack, parts, progress, isBlockTranslucent);
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
        this.parent.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
        this.parent.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
        ModLog.debug("[GhostSubmitNodeCollector] submitCustomGeometry: renderType=" + renderType + ", ghostType=" + ghostType);
        this.parent.submitCustomGeometry(poseStack, ghostType, customGeometryRenderer);
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {
        this.parent.submitQuadParticleGroup(particles);
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
        this.parent.submitGizmoPrimitives(group, camera, onTop);
    }

    private static class GhostOrderedSubmitNodeCollector implements OrderedSubmitNodeCollector {
        private final OrderedSubmitNodeCollector delegate;
        private final float alpha;

        public GhostOrderedSubmitNodeCollector(OrderedSubmitNodeCollector delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
            this.delegate.submitShadow(poseStack, radius, pieces);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {
            this.delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
            this.delegate.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
        }

        @Override
        public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {
            this.delegate.submitTextBackground(poseStack, x0, y0, x1, y1, color, displayMode, lightCoords);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
            this.delegate.submitFlame(poseStack, renderState, rotation);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            this.delegate.submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor) {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            int ghostColor = scaleAlphaColor(tintedColor, this.alpha);
            this.delegate.submitModel(model, state, poseStack, ghostType, lightCoords, overlayCoords, ghostColor, uvMapping, outlineColor);
        }

        @Override
        public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            int ghostColor = scaleAlphaColor(tintedColor, this.alpha);
            this.delegate.submitCrumblingOverlay(model, state, poseStack, ghostType, lightCoords, overlayCoords, ghostColor, crumblingOverlay);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
            this.delegate.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
        }

        @Override
        public void submitBlockModel(
            PoseStack poseStack,
            Function<ChunkSectionLayer, RenderType> renderTypeFunction,
            boolean hasTranslucent,
            List<BlockStateModelPart> modelParts,
            @Nullable Mesh mesh,
            int[] tintLayers,
            int lightCoords,
            int overlayCoords,
            int outlineColor
        ) {
            SubmitNodeCollection col = resolveCollection(this.delegate);
            if (col != null) {
                col.translucentBlocksAndItems.submit(new ExtendedBlockModelSubmit(
                    poseStack.last().copy(),
                    GHOST_BLOCK_RENDER_TYPE,
                    modelParts,
                    mesh,
                    ensureOpaqueTints(tintLayers),
                    lightCoords,
                    overlayCoords,
                    ARGB.white(this.alpha),
                    null
                ));
            } else {
                this.delegate.submitBlockModel(poseStack, GHOST_BLOCK_RENDER_TYPE, true, modelParts, mesh, tintLayers, lightCoords, overlayCoords, outlineColor);
            }
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
            SubmitNodeCollection col = resolveCollection(this.delegate);
            if (col != null) {
                col.translucentBlocksAndItems.submit(new BlockModelFeatureRenderer.Submit(
                    poseStack.last().copy(),
                    Sheets.translucentBlockItemSheet(),
                    parts,
                    ensureOpaqueTints(tintLayers),
                    lightCoords,
                    overlayCoords,
                    ARGB.white(this.alpha),
                    null
                ));
            } else {
                RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
                this.delegate.submitBlockModel(poseStack, ghostType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
            }
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
            this.delegate.submitBreakingBlockModel(poseStack, parts, progress, isBlockTranslucent);
        }

        @Override
        public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
            this.delegate.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
            this.delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            this.delegate.submitCustomGeometry(poseStack, ghostType, customGeometryRenderer);
        }

        @Override
        public void submitQuadParticleGroup(QuadParticleRenderState particles) {
            this.delegate.submitQuadParticleGroup(particles);
        }

        @Override
        public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
            this.delegate.submitGizmoPrimitives(group, camera, onTop);
        }
    }
}
