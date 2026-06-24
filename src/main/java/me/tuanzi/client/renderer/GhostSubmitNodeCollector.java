package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class GhostSubmitNodeCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector parent;
    private final float alpha;

    public GhostSubmitNodeCollector(SubmitNodeCollector parent, float alpha) {
        this.parent = parent;
        this.alpha = alpha;
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
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.parent.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.parent.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
        me.tuanzi.util.ModLog.debug("GhostSubmitNodeCollector: submitModel called! model=" + model.getClass().getName() + ", renderType=" + renderType + ", ghostType=" + ghostType);
        this.parent.submitModel(model, state, poseStack, ghostType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        this.parent.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
        me.tuanzi.util.ModLog.debug("GhostSubmitNodeCollector: submitBlockModel called! parts size=" + parts.size() + ", renderType=" + renderType + ", ghostType=" + ghostType);
        this.parent.submitBlockModel(poseStack, ghostType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
        this.parent.submitBreakingBlockModel(poseStack, parts, progress);
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
        this.parent.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        this.parent.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
        me.tuanzi.util.ModLog.debug("GhostSubmitNodeCollector: submitCustomGeometry called! renderType=" + renderType + ", ghostType=" + ghostType);
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
        public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
            this.delegate.submitFlame(poseStack, renderState, rotation);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            this.delegate.submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            me.tuanzi.util.ModLog.debug("GhostOrderedSubmitNodeCollector: submitModel called! model=" + model.getClass().getName() + ", renderType=" + renderType + ", ghostType=" + ghostType);
            this.delegate.submitModel(model, state, poseStack, ghostType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
            this.delegate.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            me.tuanzi.util.ModLog.debug("GhostOrderedSubmitNodeCollector: submitBlockModel called! parts size=" + parts.size() + ", renderType=" + renderType + ", ghostType=" + ghostType);
            this.delegate.submitBlockModel(poseStack, ghostType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
            this.delegate.submitBreakingBlockModel(poseStack, parts, progress);
        }

        @Override
        public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
            this.delegate.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
            this.delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
            RenderType ghostType = GhostRenderRegistry.getOrCreateGhostType(renderType, this.alpha);
            me.tuanzi.util.ModLog.debug("GhostOrderedSubmitNodeCollector: submitCustomGeometry called! renderType=" + renderType + ", ghostType=" + ghostType);
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
