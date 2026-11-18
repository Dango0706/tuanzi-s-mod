package fr.minevegas.casino;

import com.mojang.blaze3d.platform.Lighting.Entry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class GuideSceneRenderer extends PictureInPictureRenderer<GuideSceneRenderer.State> {
   private static final int LIGHT = 15728880;
   private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
   private BlockModelResolver resolver;
   private static Field renderState;
   private static boolean warned;

   public static void submit(GuiGraphicsExtractor graphics, GuideScene scene, List<GuideScene.Placed> blocks, int age, float partial, float yaw, float pitch, int x0, int y0, int x1, int y1, float scale) {
      ScreenRectangle area = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
      State state = new State(blocks, scene.machines(), scene.hiringFrame((float)age + partial), scene.flames(), (float)age + partial, partial, yaw, pitch, scene.centerX, scene.centerY, scene.centerZ, x0, y0, x1, y1, scale, (ScreenRectangle)null, area);
      target(graphics).addPicturesInPictureState(state);
   }

   private static GuiRenderState target(GuiGraphicsExtractor graphics) {
      try {
         if (renderState == null) {
            for(Field field : graphics.getClass().getDeclaredFields()) {
               if (field.getType() == GuiRenderState.class) {
                  field.setAccessible(true);
                  renderState = field;
               }
            }
         }

         return (GuiRenderState)renderState.get(graphics);
      } catch (RuntimeException | ReflectiveOperationException exception) {
         throw new IllegalStateException("Cannot reach the interface state for the guide's 3D scenes", exception);
      }
   }

   public Class<State> getRenderStateClass() {
      return State.class;
   }

   protected String getTextureLabel() {
      return "minevegas casino guide scene";
   }

   protected float getTranslateY(int height, int guiScale) {
      return (float)height / 2.0F;
   }

   protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.gameRenderer.lighting().setupFor(Entry.ENTITY_IN_UI);
      if (this.resolver == null) {
         this.resolver = new BlockModelResolver(minecraft.getModelManager());
      }

      place(state, pose);

      for(GuideScene.Placed placed : state.blocks()) {
         pose.pushPose();
         pose.translate((float)placed.x(), (float)placed.y(), (float)placed.z());
         BlockModelRenderState block = new BlockModelRenderState();
         GuideBlockModels.fill(block, placed);
         block.submit(pose, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);
         pose.popPose();
      }

      machines(state, pose, collector);
      GuideFlames.draw(state.flames(), state.age(), state.yaw(), state.pitch(), pose, collector);
      this.hiring(state, pose, collector);
      pose.popPose();
   }

   private static void place(State state, PoseStack pose) {
      pose.pushPose();
      pose.rotate(Axis.ZP.rotation((float)Math.PI));
      pose.rotate(Axis.XP.rotationDegrees(-state.pitch()));
      pose.rotate(Axis.YP.rotationDegrees(state.yaw()));
      pose.translate(-state.centerX(), -state.centerY(), -state.centerZ());
   }

   private void hiring(State picture, PoseStack pose, SubmitNodeCollector collector) {
      Minecraft minecraft = Minecraft.getInstance();
      CameraRenderState camera = new CameraRenderState();
      camera.orientation = new Quaternionf();

      for(GuideHiringPreview.Actor actor : picture.hiring().actors()) {
         pose.pushPose();
         pose.translate(actor.x(), actor.y(), actor.z());
         EntityRenderer renderer = minecraft.getEntityRenderDispatcher().getRenderer(actor.entity());
         EntityRenderState state = renderer.createRenderState(actor.entity(), 1.0F);
         state.lightCoords = 15728880;
         state.nameTag = null;
         state.shadowPieces.clear();
         renderer.submit(state, pose, collector, camera);
         pose.popPose();
      }

      for(GuideHiringPreview.Spark spark : picture.hiring().sparks()) {
         pose.pushPose();
         pose.translate(spark.x(), spark.y(), spark.z());
         pose.scale(spark.size(), spark.size(), spark.size());
         BlockModelRenderState model = new BlockModelRenderState();
         this.resolver.update(model, spark.state(), CONTEXT);
         model.submit(pose, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);
         pose.popPose();
      }

   }

   private static void machines(State state, PoseStack pose, SubmitNodeCollector collector) {
      if (!state.machines().isEmpty()) {
         CameraRenderState camera = new CameraRenderState();
         camera.orientation = new Quaternionf();

         for(GuideScene.Machine machine : state.machines()) {
            pose.pushPose();
            pose.translate((float)machine.placed().x(), (float)machine.placed().y(), (float)machine.placed().z());
            machine(machine.entity(), state.partial(), pose, collector, camera);
            pose.popPose();
         }

      }
   }

   private static void machine(BlockEntity entity, float partial, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      try {
         BlockEntityRenderer renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
         if (renderer == null) {
            return;
         }

         BlockEntityRenderState state = renderer.createRenderState();
         renderer.extractRenderState(entity, state, partial, Vec3.ZERO, (ModelFeatureRenderer.CrumblingOverlay)null);
         state.lightCoords = 15728880;
         renderer.submit(state, pose, collector, camera);
      } catch (RuntimeException exception) {
         if (!warned) {
            LogUtils.getLogger().warn((String)"[MineVegas Casino] A machine's own renderer could not draw in the guide", (Throwable)exception);
         }

         warned = true;
      }

   }

   public static record State(List<GuideScene.Placed> blocks, List<GuideScene.Machine> machines, GuideHiringPreview.Frame hiring, List<GuideFlames.Flame> flames, float age, float partial, float yaw, float pitch, float centerX, float centerY, float centerZ, int x0, int y0, int x1, int y1, float scale, ScreenRectangle scissorArea, ScreenRectangle bounds) implements PictureInPictureRenderState {
   }
}

