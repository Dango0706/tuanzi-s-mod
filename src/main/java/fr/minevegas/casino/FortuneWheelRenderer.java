package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class FortuneWheelRenderer implements CasinoBlockEntityRenderer<FortuneWheelBlockEntity, FortuneWheelRenderState> {
   private static final Identifier SHEET = Identifier.fromNamespaceAndPath("roulettecasino", "textures/entity/fortune_wheel.png");
   private static final int FULL_LIGHT = 15728880;
   private static final RandomSource ICON_RANDOM = RandomSource.create(0L);
   private static final Quaternionf BLOCK_GUI_INVERSE = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)30.0F), (float)Math.toRadians((double)225.0F), 0.0F).conjugate();
   static final double PANEL_Y = 2.7;
   private static final int CELL_W = 46;
   private static final int MIN_PANEL_W = 146;
   private static final int FRAME = -123241929;
   private static final int INSIDE = -266188488;
   private static final int HEADER_EDGE = -118897045;
   private static final int HEADER = -199929794;
   private static final int ROW = -1339805653;
   private static final int TITLE = -398143;
   private static final int[] RESULT_COLOR = new int[]{-1599358, -22016, -865166, -7811194, -22016};
   private static final int[] RESULT_NUMBER = new int[]{16777215, 16777215, 16777215, 16777045, 16777215};
   private final ItemModelResolver itemModels;
   private final ToIntFunction<FormattedCharSequence> measure;

   public FortuneWheelRenderer(BlockEntityRendererProvider.Context context) {
      this.itemModels = context.itemModelResolver();
      Font font = context.font();
      Objects.requireNonNull(font);
      this.measure = font::width;
   }

   public FortuneWheelRenderState createRenderState() {
      return new FortuneWheelRenderState();
   }

   public void extractRenderState(FortuneWheelBlockEntity wheel, FortuneWheelRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
      CasinoBlockEntityRenderer.super.extractRenderState(wheel, state, partialTick, cameraPos, breaking);
      state.facing = (Direction)wheel.getBlockState().getValue(FortuneWheelBlock.FACING);
      Level level = wheel.getLevel();
      FortuneWheelSpin.Plan plan = wheel.plan();
      double idleTime = CasinoIdleLights.clock(level == null ? (double)0.0F : (double)level.getGameTime() + (double)partialTick, wheel.getBlockPos().getX(), wheel.getBlockPos().getY(), wheel.getBlockPos().getZ());
      if (plan != null && level != null) {
         double elapsed = (double)level.getGameTime() + (double)partialTick - (double)wheel.spinStart();
         state.turn = plan.turn(elapsed);
         state.speed = plan.speed(elapsed);
         state.flapper = plan.flapper(elapsed);
         state.brightness = (float)plan.flash(elapsed);

         for(int i = 0; i < state.bulbs.length; ++i) {
            state.bulbs[i] = (float)CasinoIdleLights.afterGame(plan.bulb(i, elapsed), CasinoIdleLights.bulb(i, state.bulbs.length, idleTime), elapsed - (double)plan.endTick());
         }

         state.resultShown = elapsed >= (double)plan.resultTick();
      } else {
         state.turn = FortuneWheelSpin.restTurn(wheel.stop());
         state.speed = state.flapper = (double)0.0F;
         state.brightness = 1.0F;

         for(int i = 0; i < state.bulbs.length; ++i) {
            state.bulbs[i] = (float)CasinoIdleLights.bulb(i, state.bulbs.length, idleTime);
         }

         state.resultShown = false;
      }

      state.quarters = FortuneWheelGame.quarters(wheel.stop());
      state.kind = wheel.resultKind();
      this.extractPanel(wheel, state, cameraPos);
   }

   private void extractPanel(FortuneWheelBlockEntity wheel, FortuneWheelRenderState state, Vec3 cameraPos) {
      double distance = cameraPos.distanceTo(new Vec3((double)wheel.getBlockPos().getX() + (double)0.5F, (double)wheel.getBlockPos().getY() + 2.7, (double)wheel.getBlockPos().getZ() + (double)0.5F));
      float approach = (float)Math.clamp(((double)10.0F - distance) / (double)2.5F, (double)0.0F, (double)1.0F);
      state.panelVisibility = approach * approach * (3.0F - 2.0F * approach);

      for(int i = 0; i < 3; ++i) {
         ItemStack stack = wheel.displayedBet(i);
         state.counts[i] = stack.getCount();
         if (!stack.isEmpty() && !(state.panelVisibility <= 0.0F)) {
            state.blockIcons[i] = CasinoBetIcons.drawnByFace(stack);
            this.itemModels.updateForTopItem(state.items[i], stack, ItemDisplayContext.GUI, wheel.getLevel(), (ItemOwner)null, i);
            ICON_RANDOM.setSeed((long)i);
            state.iconSprites[i] = CasinoBetIcons.flatIcon(stack, state.items[i], ICON_RANDOM);
         }
      }

   }

   public void submit(FortuneWheelRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      CasinoMachineNames.submit(state, pose, output, camera);
      pose.pushPose();
      pose.translate((double)0.5F, (double)0.0F, (double)0.5F);
      pose.rotate((new Quaternionf()).rotationY((float)((double)(-state.facing.get2DDataValue()) * Math.PI / (double)2.0F)));
      pose.translate((double)-0.5F, (double)0.0F, (double)-0.5F);
      pose.pushPose();
      pose.translate(FortuneWheelRenderGeometry.AXLE[0] / (double)16.0F, FortuneWheelRenderGeometry.AXLE[1] / (double)16.0F, (double)0.0F);
      pose.rotate((new Quaternionf()).rotationZ((float)Math.toRadians(-state.turn * (double)22.5F)));
      FortuneWheelBlur.Blend blur = FortuneWheelBlur.at(state.speed);
      int alpha = blur.alpha();
      int base = alpha == 255 ? blur.upper() : blur.lower();
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> face(vertices, transform, state, base, 255));
      if (alpha > 0 && alpha < 255) {
         output.order(2).submitCustomGeometry(pose, RenderTypes.entityTranslucent(SHEET), (transform, vertices) -> face(vertices, transform, state, blur.upper(), alpha));
      }

      pose.popPose();
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> lights(vertices, transform, state));
      pose.pushPose();
      pose.translate(FortuneWheelRenderGeometry.HINGE[0] / (double)16.0F, FortuneWheelRenderGeometry.HINGE[1] / (double)16.0F, (double)0.0F);
      pose.rotate((new Quaternionf()).rotationZ((float)Math.toRadians(state.flapper)));
      int light = state.lightCoords;
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> quads(vertices, transform, FortuneWheelRenderGeometry.pennant(), -1, light, false));
      pose.popPose();
      pose.popPose();
      if (state.panelVisibility > 0.0F) {
         this.panel(state, pose, output, camera);
      }

   }

   static void face(VertexConsumer vertices, PoseStack.Pose transform, FortuneWheelRenderState state, int blur, int alpha) {
      float[] uv = FortuneWheelRenderGeometry.PLATE_UV;
      int i = blur * 4;
      double half = (double)20.0F;
      double z = (double)11.0F;
      int tint = gray((double)state.brightness) & 16777215 | alpha << 24;
      float turned = (float)Math.toRadians(-state.turn * (double)22.5F);
      float nx = (float)Math.sin((double)turned);
      float ny = (float)Math.cos((double)turned);
      vertex(vertices, transform, -half, half, z, uv[i], uv[i + 1], tint, 15728880, nx, ny, 0.0F);
      vertex(vertices, transform, -half, -half, z, uv[i], uv[i + 3], tint, 15728880, nx, ny, 0.0F);
      vertex(vertices, transform, half, -half, z, uv[i + 2], uv[i + 3], tint, 15728880, nx, ny, 0.0F);
      vertex(vertices, transform, half, half, z, uv[i + 2], uv[i + 1], tint, 15728880, nx, ny, 0.0F);
   }

   static void lights(VertexConsumer vertices, PoseStack.Pose transform, FortuneWheelRenderState state) {
      float[] uv = FortuneWheelRenderGeometry.BULB_UV;
      double half = (double)0.5F;

      for(int i = 0; i < 16; ++i) {
         double x = FortuneWheelRenderGeometry.BULBS[3 * i];
         double y = FortuneWheelRenderGeometry.BULBS[3 * i + 1];
         double z = FortuneWheelRenderGeometry.BULBS[3 * i + 2];
         int tint = gray(0.28 + 0.72 * (double)state.bulbs[i]);
         vertex(vertices, transform, x - half, y + half, z, uv[0], uv[1], tint, 15728880, 0.0F, 1.0F, 0.0F);
         vertex(vertices, transform, x - half, y - half, z, uv[0], uv[3], tint, 15728880, 0.0F, 1.0F, 0.0F);
         vertex(vertices, transform, x + half, y - half, z, uv[2], uv[3], tint, 15728880, 0.0F, 1.0F, 0.0F);
         vertex(vertices, transform, x + half, y + half, z, uv[2], uv[1], tint, 15728880, 0.0F, 1.0F, 0.0F);
      }

   }

   private static int gray(double brightness) {
      int g = (int)Math.round((double)255.0F * Math.max((double)0.0F, Math.min((double)1.0F, brightness)));
      return -16777216 | g << 16 | g << 8 | g;
   }

   private static void quads(VertexConsumer vertices, PoseStack.Pose transform, float[] data, int tint, int light, boolean selfLit) {
      int per = 23;

      for(int q = 0; q + per <= data.length; q += per) {
         float nx = selfLit ? 0.0F : data[q + 20];
         float ny = selfLit ? 1.0F : data[q + 21];
         float nz = selfLit ? 0.0F : data[q + 22];

         for(int v = 0; v < 4; ++v) {
            int o = q + v * 5;
            vertex(vertices, transform, (double)data[o], (double)data[o + 1], (double)data[o + 2], data[o + 3], data[o + 4], tint, light, nx, ny, nz);
         }
      }

   }

   private static void vertex(VertexConsumer vertices, PoseStack.Pose transform, double x, double y, double z, float u, float v, int color, int light, float nx, float ny, float nz) {
      vertices.addVertex(transform, (float)(x / (double)16.0F), (float)(y / (double)16.0F), (float)(z / (double)16.0F)).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transform, nx, ny, nz);
   }

   private void panel(FortuneWheelRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      int stakes = 0;

      for(int i = 0; i < 3; ++i) {
         if (state.counts[i] > 0) {
            ++stakes;
         }
      }

      if (stakes != 0) {
         boolean decided = state.resultShown;
         int width = Math.max(Math.max(146, stakes * 46 + 8), decided ? this.textWidth(resultText(state).getVisualOrderText()) + 12 : 0);
         int height = 39 + (decided ? 18 : 0);
         int left = -width / 2;
         int top = -height / 2;
         pose.pushPose();
         pose.translate((double)0.5F, 2.7 - 0.09 * (double)(1.0F - state.panelVisibility), (double)0.5F);
         pose.rotate(camera.orientation);
         float scale = 0.011F * state.panelVisibility;
         pose.scale(scale, -scale, scale);
         output = CasinoFloatingPanels.faded(output, state, camera, (double)0.5F, 2.7 - 0.09 * (double)(1.0F - state.panelVisibility), (double)0.5F, width, height, scale);
         CasinoFloatingPanels.badge(state, pose, output, top, width);
         background(output, pose, (float)(left - 3), (float)(top - 3), (float)(left + width + 3), (float)(top + height + 3), -123241929);
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         background(output, pose, (float)(left - 2), (float)(top - 2), (float)(left + width + 2), (float)(top + height + 2), -266188488);
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         background(output, pose, (float)(left + 1), (float)(top + 1), (float)(left + width - 1), (float)(top + 17), -118897045);
         pose.pushPose();
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         background(output, pose, (float)(left + 2), (float)(top + 2), (float)(left + width - 2), (float)(top + 16), -199929794);
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         FormattedCharSequence title = Component.translatable("gui.roulettecasino.wheel.panel_title").getVisualOrderText();
         output.submitText(pose, (float)(-this.textWidth(title)) / 2.0F, (float)(top + 5), title, true, DisplayMode.NORMAL, 15728880, -398143, 0, 0);
         pose.popPose();
         int rowTop = top + 19;
         background(output, pose, (float)(left + 3), (float)rowTop, (float)(left + width - 3), (float)(rowTop + 18), -1339805653);
         int cellLeft = -stakes * 46 / 2;
         int cell = 0;

         for(int slot = 0; slot < 3; ++slot) {
            if (state.counts[slot] != 0) {
               int x = cellLeft + cell++ * 46;
               pose.pushPose();
               pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
               if (state.iconSprites[slot] != null) {
                  spriteIcon(output, pose, state.iconSprites[slot], (float)(x + 4), (float)(rowTop + 2), (float)(x + 18), (float)(rowTop + 16));
               } else {
                  pose.pushPose();
                  pose.translate((double)x + (double)11.0F, (double)rowTop + (double)9.5F, (double)1.0F);
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

               int var10000 = state.counts[slot];
               FormattedCharSequence count = Component.literal("×" + var10000).getVisualOrderText();
               output.submitText(pose, (float)(x + 21), (float)(rowTop + 5), count, true, DisplayMode.NORMAL, 15728880, -1, 0, 0);
               pose.popPose();
            }
         }

         if (decided) {
            int resultTop = rowTop + 20;
            background(output, pose, (float)(left + 1), (float)resultTop, (float)(left + width - 1), (float)(resultTop + 15), -1339805653);
            pose.pushPose();
            pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
            FormattedCharSequence line = resultText(state).getVisualOrderText();
            output.submitText(pose, (float)(-this.textWidth(line)) / 2.0F, (float)(resultTop + 4), line, true, DisplayMode.NORMAL, 15728880, RESULT_COLOR[state.kind.ordinal()], 0, 0);
            pose.popPose();
         }

         pose.popPose();
      }
   }

   static Component resultText(FortuneWheelRenderState state) {
      String var10000;
      switch (state.kind) {
         case NONE -> var10000 = "gui.roulettecasino.wheel.result_lose";
         case PARTIAL -> var10000 = "gui.roulettecasino.wheel.result_partial";
         case PUSH -> var10000 = "gui.roulettecasino.wheel.result_push";
         case WIN -> var10000 = "gui.roulettecasino.wheel.result_win";
         case JACKPOT -> var10000 = "gui.roulettecasino.wheel.result_jackpot";
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      String key = var10000;
      Component odds = Component.translatable(FortuneWheelGame.multiplierKey(state.quarters)).withColor(RESULT_NUMBER[state.kind.ordinal()]);
      Component text = Component.translatable(key, odds);
      return text.copy().withStyle(ChatFormatting.BOLD);
   }

   private int textWidth(FormattedCharSequence text) {
      return this.measure.applyAsInt(text);
   }

   private static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      CasinoFloatingPanels.background(output, pose, x1, y1, x2, y2, color);
   }

   private static void spriteIcon(SubmitNodeCollector output, PoseStack pose, TextureAtlasSprite sprite, float x1, float y1, float x2, float y2) {
      picture(output, pose, sprite.atlasLocation(), sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), x1, y1, x2, y2);
   }

   private static void sheetIcon(SubmitNodeCollector output, PoseStack pose, float[] uv, float x1, float y1, float x2, float y2) {
      picture(output, pose, SHEET, uv[0], uv[1], uv[2], uv[3], x1, y1, x2, y2);
   }

   private static void picture(SubmitNodeCollector output, PoseStack pose, Identifier texture, float u0, float v0, float u1, float v1, float x1, float y1, float x2, float y2) {
      output.submitCustomGeometry(pose, RenderTypes.text(texture), (transform, vertices) -> {
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

   public AABB getRenderBoundingBox(FortuneWheelBlockEntity machine) {
      return (new AABB((double)-2.5F, (double)-1.0625F, (double)-2.5F, (double)3.5F, (double)4.5F, (double)3.5F)).move(machine.getBlockPos());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }
}
