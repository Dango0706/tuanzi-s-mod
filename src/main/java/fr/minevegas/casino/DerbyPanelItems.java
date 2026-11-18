package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.minevegas.casino.mixin.PanelRenderSetupAccess;
import fr.minevegas.casino.mixin.PanelRenderTypeAccess;
import fr.minevegas.casino.mixin.PanelTextureBindingAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

final class DerbyPanelItems implements SubmitNodeCollector {
   private static final Map<ItemQuads, Map<Integer, ItemQuads>> QUADS = new WeakHashMap();
   private final SubmitNodeCollector root;
   private final OrderedSubmitNodeCollector target;
   private final float opacity;

   DerbyPanelItems(SubmitNodeCollector target, float opacity) {
      this(target, target, opacity);
   }

   private DerbyPanelItems(SubmitNodeCollector root, OrderedSubmitNodeCollector target, float opacity) {
      this.root = root;
      this.target = target;
      this.opacity = opacity;
   }

   private int color(int color) {
      return DerbyPanelFocus.color(color, this.opacity);
   }

   public OrderedSubmitNodeCollector order(int order) {
      return new DerbyPanelItems(this.root, this.root.order(order), this.opacity);
   }

   private static ItemQuads translucent(ItemQuads source, int extraTint) {
      return (ItemQuads)((Map)QUADS.computeIfAbsent(source, (k) -> new HashMap())).computeIfAbsent(extraTint, (k) -> {
         List<BakedQuad> quads = new ArrayList(source.all().size());

         for(BakedQuad q : source.all()) {
            BakedQuad.MaterialInfo m = q.materialInfo();
            Identifier atlas = m.sprite().atlasLocation();
            BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(m.sprite(), ChunkSectionLayer.TRANSLUCENT, RenderTypes.itemTranslucent(atlas), RenderTypes.itemTranslucentGlint(atlas), RenderTypes.itemTranslucentGlintSpecial(atlas), m.tintIndex() < 0 ? extraTint : m.tintIndex(), m.shadeDirectionOverride(), m.lightEmission());
            quads.add(new BakedQuad(q.position0(), q.position1(), q.position2(), q.position3(), q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(), q.direction(), material));
         }

         return new ItemQuads(quads, List.of(), quads);
      });
   }

   public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads, ItemStackRenderState.FoilType foil) {
      int[] faded = Arrays.copyOf(tints, tints.length + 1);

      for(int n = 0; n < tints.length; ++n) {
         faded[n] = this.color(tints[n]);
      }

      faded[tints.length] = this.color(-1);
      this.target.submitItem(pose, context, light, overlay, outline, faded, translucent(quads, tints.length), foil);
   }

   private RenderType translucent(RenderType original) {
      RenderSetup state = ((PanelRenderTypeAccess)original).minevegas$state();
      Object binding = ((PanelRenderSetupAccess)(Object)state).minevegas$textures().get("Sampler0");
      if (binding instanceof PanelTextureBindingAccess texture) {
         RenderType faded = RenderTypes.entityTranslucent(texture.minevegas$location());
         if (original.format().equals(faded.format())) {
            return faded;
         }
      }

      return original;
   }

   public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int color, UvMapping uv, int outline) {
      this.target.submitModel(model, state, pose, this.translucent(type), light, overlay, this.color(color), uv, outline);
   }

   public void submitCustomGeometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
      this.target.submitCustomGeometry(pose, this.translucent(type), (transform, vertices) -> renderer.render(transform, new FadedVertices(vertices, this.opacity)));
   }

   public void submitShadow(PoseStack p, float r, List<EntityRenderState.ShadowPiece> s) {
      this.target.submitShadow(p, r, s);
   }

   public void submitNameTag(PoseStack p, Vec3 v, int y, Component c, boolean b, int l, CameraRenderState camera) {
      this.target.submitNameTag(p, v, y, c, b, l, camera);
   }

   public void submitText(PoseStack p, float x, float y, FormattedCharSequence t, boolean b, Font.DisplayMode m, int l, int c, int bg, int o) {
      this.target.submitText(p, x, y, t, b, m, l, this.color(c), this.color(bg), o);
   }

   public void submitTextBackground(PoseStack p, float x, float y, float x2, float y2, int c, Font.DisplayMode m, int l) {
      this.target.submitTextBackground(p, x, y, x2, y2, this.color(c), m, l);
   }

   public void submitFlame(PoseStack p, EntityRenderState s, Quaternionf q) {
      this.target.submitFlame(p, s, q);
   }

   public void submitLeash(PoseStack p, EntityRenderState.LeashState s) {
      this.target.submitLeash(p, s);
   }

   public <S> void submitCrumblingOverlay(Model<? super S> m, S s, PoseStack p, RenderType t, int l, int o, int c, ModelFeatureRenderer.CrumblingOverlay b) {
      this.target.submitCrumblingOverlay(m, s, p, this.translucent(t), l, o, this.color(c), b);
   }

   public void submitMovingBlock(PoseStack p, MovingBlockRenderState s, int outline) {
      this.target.submitMovingBlock(p, s, outline);
   }

   public void submitBlockModel(PoseStack p, RenderType t, List<BlockStateModelPart> parts, int[] colors, int l, int o, int outline) {
      this.target.submitBlockModel(p, t, parts, colors, l, o, outline);
   }

   public void submitBreakingBlockModel(PoseStack p, List<BlockStateModelPart> parts, int i, boolean b) {
      this.target.submitBreakingBlockModel(p, parts, i, b);
   }

   public void submitShapeOutline(PoseStack p, VoxelShape s, RenderType t, int c, float w, boolean b) {
      this.target.submitShapeOutline(p, s, t, this.color(c), w, b);
   }

   public void submitQuadParticleGroup(QuadParticleRenderState s) {
      this.target.submitQuadParticleGroup(s);
   }

   public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group g, CameraRenderState c, boolean b) {
      this.target.submitGizmoPrimitives(g, c, b);
   }

   private static record FadedVertices(VertexConsumer target, float opacity) implements VertexConsumer {
      public VertexConsumer addVertex(float x, float y, float z) {
         this.target.addVertex(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.target.setColor(r, g, b, Math.round((float)a * this.opacity));
         return this;
      }

      public VertexConsumer setColor(int color) {
         this.target.setColor(DerbyPanelFocus.color(color, this.opacity));
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.target.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.target.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.target.setUv2(u, v);
         return this;
      }

      public VertexConsumer setUv3(float u, float v) {
         this.target.setUv3(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.target.setNormal(x, y, z);
         return this;
      }

      public VertexConsumer setLineWidth(float width) {
         this.target.setLineWidth(width);
         return this;
      }
   }
}

