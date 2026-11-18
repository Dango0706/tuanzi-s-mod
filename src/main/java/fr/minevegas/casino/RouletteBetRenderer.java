package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class RouletteBetRenderer implements CasinoBlockEntityRenderer<RouletteBlockEntity, RouletteBetRenderState> {
   private static final int LIGHT = 15728880;
   private static final Identifier WHEEL_TEXTURE = Identifier.fromNamespaceAndPath("roulettecasino", "textures/entity/roulette_wheel.png");
   private static final RandomSource ICON_RANDOM = RandomSource.create(0L);
   private static final int[] HEADER = new int[]{-191158735, -199154389, -199518634};
   private static final Quaternionf BLOCK_GUI_INVERSE = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)30.0F), (float)Math.toRadians((double)225.0F), 0.0F).conjugate();
   private static final String[] LABEL = new String[]{"gui.roulettecasino.red", "gui.roulettecasino.black", "gui.roulettecasino.green"};
   private final ItemModelResolver itemModels;
   private final Font font;
   private final ToIntFunction<FormattedCharSequence> measure;

   public RouletteBetRenderer(BlockEntityRendererProvider.Context context) {
      this.itemModels = context.itemModelResolver();
      this.font = context.font();
      Font var10001 = this.font;
      Objects.requireNonNull(var10001);
      this.measure = var10001::width;
   }

   public RouletteBetRenderState createRenderState() {
      return new RouletteBetRenderState();
   }

   public void extractRenderState(RouletteBlockEntity table, RouletteBetRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
      CasinoBlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPos, breaking);
      RouletteSpin.Plan plan = table.plan();
      if (plan != null && table.getLevel() != null) {
         double elapsed = (double)table.getLevel().getGameTime() + (double)partialTick - (double)table.spinStart();
         state.phase = plan.phase(elapsed);
         state.track = plan.track(elapsed);
         state.ballPhase = plan.ballPhase(elapsed);
         state.ballHeight = plan.ballHeight(elapsed);
      } else {
         state.phase = (double)table.restingFrame();
         state.track = (double)0.0F;
         state.ballPhase = (double)2.0F * state.phase;
         state.ballHeight = 12.8;
      }

      double distance = cameraPos.distanceTo(new Vec3((double)table.getBlockPos().getX() + (double)1.0F, (double)table.getBlockPos().getY() + 1.95, (double)table.getBlockPos().getZ() + (double)1.0F));
      float approach = (float)Math.clamp(((double)10.0F - distance) / (double)2.5F, (double)0.0F, (double)1.0F);
      state.visibility = approach * approach * (3.0F - 2.0F * approach);

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = table.displayedBet(i);
         state.counts[i] = stack.getCount();
         state.blockIcons[i] = CasinoBetIcons.drawnByFace(stack);
         this.itemModels.updateForTopItem(state.items[i], stack, ItemDisplayContext.GUI, table.getLevel(), (ItemOwner)null, i);
         ICON_RANDOM.setSeed((long)i);
         state.iconSprites[i] = CasinoBetIcons.flatIcon(stack, state.items[i], ICON_RANDOM);
      }

   }

   public void submit(RouletteBetRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      CasinoMachineNames.submit(state, pose, output, camera);
      submitWheel(state, pose, output);
      int activeCount = 0;
      int maxRows = 0;

      for(int color = 0; color < 3; ++color) {
         int rows = 0;

         for(int row = 0; row < 3; ++row) {
            if (state.counts[row * 3 + color] > 0) {
               ++rows;
            }
         }

         if (rows > 0) {
            ++activeCount;
         }

         maxRows = Math.max(maxRows, rows);
      }

      if (activeCount != 0 && !(state.visibility <= 0.0F)) {
         int width = activeCount * 58;
         int height = 19 + maxRows * 18;
         int left = -width / 2;
         int top = -height / 2;
         pose.pushPose();
         pose.translate((double)1.0F, 1.95 - 0.09 * (double)(1.0F - state.visibility), (double)1.0F);
         pose.rotate(camera.orientation);
         float scale = 0.011F * state.visibility;
         pose.scale(scale, -scale, scale);
         output = CasinoFloatingPanels.faded(output, state, camera, (double)1.0F, 1.95 - 0.09 * (double)(1.0F - state.visibility), (double)1.0F, width, height, scale);
         CasinoFloatingPanels.badge(state, pose, output, top, width);
         background(output, pose, (float)(left - 3), (float)(top - 3), (float)(left + width + 3), (float)(top + height + 3), -123241929);
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         background(output, pose, (float)(left - 2), (float)(top - 2), (float)(left + width + 2), (float)(top + height + 2), -266188488);
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         int visibleColumn = 0;

         for(int color = 0; color < 3; ++color) {
            boolean active = false;

            for(int row = 0; row < 3; ++row) {
               active |= state.counts[row * 3 + color] > 0;
            }

            if (active) {
               int x = left + visibleColumn++ * 58;
               background(output, pose, (float)(x + 1), (float)(top + 1), (float)(x + 57), (float)(top + 17), -118897045);
               pose.pushPose();
               pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
               background(output, pose, (float)(x + 2), (float)(top + 2), (float)(x + 56), (float)(top + 16), HEADER[color]);
               pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
               Component title = Component.translatable(LABEL[color]);
               FormattedCharSequence titleText = title.getVisualOrderText();
               output.submitText(pose, (float)(x + 29) - (float)this.measure.applyAsInt(titleText) / 2.0F, (float)(top + 5), titleText, true, DisplayMode.NORMAL, 15728880, -398143, 0, 0);
               pose.popPose();
               int visibleRow = 0;

               for(int row = 0; row < 3; ++row) {
                  int slot = row * 3 + color;
                  if (state.counts[slot] != 0) {
                     int y = top + 19 + visibleRow++ * 18;
                     background(output, pose, (float)(x + 3), (float)y, (float)(x + 55), (float)(y + 17), -1339805653);
                     pose.pushPose();
                     pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
                     if (state.iconSprites[slot] != null) {
                        icon(output, pose, state.iconSprites[slot], (float)(x + 7), (float)(y + 1), (float)(x + 21), (float)(y + 15));
                     } else {
                        pose.pushPose();
                        pose.translate((double)x + (double)14.0F, (double)y + (double)8.5F, (double)1.0F);
                        pose.scale(14.0F, -14.0F, 14.0F);
                        if (state.blockIcons[slot]) {
                           pose.rotate(BLOCK_GUI_INVERSE);
                           pose.scale(1.6F, 1.6F, 1.6F);
                        } else {
                           CasinoBetIcons.inFrontOfPanel(pose);
                           CasinoBetIcons.fullLight(pose);
                        }

                        state.items[slot].submit(pose, output, 15728880, OverlayTexture.NO_OVERLAY, 0);
                        pose.popPose();
                     }

                     int var28 = state.counts[slot];
                     FormattedCharSequence count = Component.literal("×" + var28).getVisualOrderText();
                     output.submitText(pose, (float)(x + 27), (float)(y + 4), count, true, DisplayMode.NORMAL, 15728880, -1, 0, 0);
                     pose.popPose();
                  }
               }
            }
         }

         pose.popPose();
      }
   }

   private static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      CasinoFloatingPanels.background(output, pose, x1, y1, x2, y2, color);
   }

   static void submitWheel(RouletteBetRenderState state, PoseStack pose, SubmitNodeCollector output) {
      pose.pushPose();
      pose.translate((double)1.0F, (double)0.0F, (double)1.0F);
      pose.rotate((new Quaternionf()).rotationY((float)(state.phase * 0.16981581911296179)));
      int light = state.lightCoords;
      output.submitCustomGeometry(pose, RenderTypes.entityCutout(WHEEL_TEXTURE), (transform, vertices) -> mesh(vertices, transform, RouletteRenderGeometry.rotor(), light));
      pose.popPose();
      double theta = (state.ballPhase + (double)0.5F) * 0.16981581911296179 - (Math.PI / 2D);
      double radius = RouletteSpin.ballRadius(state.track);
      pose.pushPose();
      pose.translate((double)1.0F + radius * Math.cos(theta) / (double)16.0F, state.ballHeight / (double)16.0F, (double)1.0F + radius * Math.sin(theta) / (double)16.0F);
      pose.rotate((new Quaternionf()).rotationXYZ((float)(theta * (double)3.0F), 0.0F, (float)(theta * (double)2.0F)));
      output.submitCustomGeometry(pose, RenderTypes.entityCutout(WHEEL_TEXTURE), (transform, vertices) -> mesh(vertices, transform, RouletteRenderGeometry.ball(), light));
      pose.popPose();
   }

   private static void mesh(VertexConsumer vertices, PoseStack.Pose transform, float[] data, int light) {
      for(int q = 0; q < data.length; q += 23) {
         for(int k = 0; k < 4; ++k) {
            int i = q + k * 5;
            vertices.addVertex(transform, data[i] / 16.0F, data[i + 1] / 16.0F, data[i + 2] / 16.0F).setColor(-1).setUv(data[i + 3], data[i + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transform, data[q + 20], data[q + 21], data[q + 22]);
         }
      }

   }

   private static void icon(SubmitNodeCollector output, PoseStack pose, TextureAtlasSprite sprite, float x1, float y1, float x2, float y2) {
      float u0 = sprite.getU0();
      float u1 = sprite.getU1();
      float v0 = sprite.getV0();
      float v1 = sprite.getV1();
      output.submitCustomGeometry(pose, RenderTypes.text(sprite.atlasLocation()), (transform, vertices) -> {
         iconVertex(vertices, transform, x1, y1, u0, v0);
         iconVertex(vertices, transform, x1, y2, u0, v1);
         iconVertex(vertices, transform, x2, y2, u1, v1);
         iconVertex(vertices, transform, x2, y1, u1, v0);
      });
   }

   private static void iconVertex(VertexConsumer vertices, PoseStack.Pose transform, float x, float y, float u, float v) {
      vertices.addVertex(transform, x, y, 1.0F).setColor(-1).setUv(u, v).setLight(15728880);
   }

   public int getViewDistance() {
      return 64;
   }

   public AABB getRenderBoundingBox(RouletteBlockEntity machine) {
      return (new AABB((double)-2.5F, (double)-0.0625F, (double)-2.5F, (double)4.5F, (double)4.0F, (double)4.5F)).move(machine.getBlockPos());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }
}
