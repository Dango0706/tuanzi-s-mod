package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
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

public final class PlinkoRenderer implements CasinoBlockEntityRenderer<PlinkoBlockEntity, PlinkoRenderState> {
   private static final Identifier SHEET = Identifier.fromNamespaceAndPath("roulettecasino", "textures/entity/plinko.png");
   private static final int FULL_LIGHT = 15728880;
   private static final RandomSource ICON_RANDOM = RandomSource.create(0L);
   private static final Quaternionf BLOCK_GUI_INVERSE = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)30.0F), (float)Math.toRadians((double)225.0F), 0.0F).conjugate();
   static final double PANEL_Y = 2.7;
   private static final int CELL_W = 46;
   private static final int MIN_PANEL_W = 116;
   private static final int FRAME = -123241929;
   private static final int INSIDE = -266188488;
   private static final int HEADER_EDGE = -118897045;
   private static final int HEADER = -199929794;
   private static final int ROW = -1339805653;
   private static final int TITLE = -398143;
   private static final int[] RESULT_COLOR = new int[]{-1599358, -22016, -7811194, -865166, -22016};
   private static final int[] RESULT_NUMBER = new int[]{16777215, 16777215, 16777215, 16777215, 16777045};
   private static final int POCKET_GOLD = -22016;
   private static final int POCKET_GREEN = -7811194;
   private static final int POCKET_RED = -1599358;
   private static final double GLOW_ALPHA = 0.38;
   private final ItemModelResolver itemModels;
   private final ToIntFunction<FormattedCharSequence> measure;

   public PlinkoRenderer(BlockEntityRendererProvider.Context context) {
      this.itemModels = context.itemModelResolver();
      Font font = context.font();
      Objects.requireNonNull(font);
      this.measure = font::width;
   }

   public PlinkoRenderState createRenderState() {
      return new PlinkoRenderState();
   }

   public void extractRenderState(PlinkoBlockEntity machine, PlinkoRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
      CasinoBlockEntityRenderer.super.extractRenderState(machine, state, partialTick, cameraPos, breaking);
      state.facing = (Direction)machine.getBlockState().getValue(PlinkoBlock.FACING);
      Level level = machine.getLevel();
      double time = level == null ? (double)0.0F : (double)level.getGameTime() + (double)partialTick;
      double elapsed = time - (double)machine.start();
      PlinkoDrop.Plan plan = machine.plan();
      animate(plan, elapsed, time, state);
      this.extractPanel(machine, plan, elapsed, state, cameraPos);
   }

   private static double smooth(double t) {
      double x = Math.max((double)0.0F, Math.min((double)1.0F, t));
      return x * x * ((double)3.0F - (double)2.0F * x);
   }

   static void animate(PlinkoDrop.Plan plan, double elapsed, double time, PlinkoRenderState state) {
      int balls = 0;
      boolean idle = plan == null || elapsed < (double)0.0F || elapsed >= (double)(plan.endTick() + 14);
      if (!idle) {
         boolean over = elapsed >= (double)plan.endTick();
         double since = elapsed - (double)plan.endTick();

         for(int pin = 0; pin < PlinkoGame.PIN_COUNT; ++pin) {
            double game = plan.lamp(pin, elapsed);
            double rest = PlinkoLights.lamp(pin, time);
            state.lamps[pin] = (float)(over ? PlinkoLights.back(game, rest, since) : PlinkoLights.into(rest, game, elapsed));
         }

         for(int b = 0; b < 11; ++b) {
            double game = plan.bulb(b, elapsed);
            double rest = PlinkoLights.bulb(b, time);
            state.bulbs[b] = (float)(over ? PlinkoLights.back(game, rest, since) : PlinkoLights.into(rest, game, elapsed));
         }

         for(int k = 0; k < 9; ++k) {
            double glow = plan.pocket(k, elapsed);
            state.pockets[k] = (float)(over ? glow * ((double)1.0F - smooth(since / (double)14.0F)) : glow);
         }

         for(int b = 0; b < plan.ballCount(); ++b) {
            PlinkoDrop.Ball ball = plan.ball(b);
            if (ball.visible(elapsed)) {
               double[] at = ball.position(elapsed);
               state.balls[2 * balls] = (float)at[0];
               state.balls[2 * balls + 1] = (float)at[1];
               ++balls;
            }
         }

         if (plan.chuteFull(elapsed)) {
            state.balls[2 * balls] = 24.0F;
            state.balls[2 * balls + 1] = 38.5F;
            ++balls;
         }

         state.ballCount = balls;
      } else {
         for(int pin = 0; pin < PlinkoGame.PIN_COUNT; ++pin) {
            state.lamps[pin] = (float)PlinkoLights.lamp(pin, time);
         }

         for(int b = 0; b < 11; ++b) {
            state.bulbs[b] = (float)PlinkoLights.bulb(b, time);
         }

         Arrays.fill(state.pockets, 0.0F);
         state.balls[0] = 24.0F;
         state.balls[1] = 38.5F;
         state.ballCount = 1;
      }
   }

   private void extractPanel(PlinkoBlockEntity machine, PlinkoDrop.Plan plan, double elapsed, PlinkoRenderState state, Vec3 cameraPos) {
      double distance = cameraPos.distanceTo(new Vec3((double)machine.getBlockPos().getX() + (double)0.5F, (double)machine.getBlockPos().getY() + 2.7, (double)machine.getBlockPos().getZ() + (double)0.5F));
      float approach = (float)Math.clamp(((double)10.0F - distance) / (double)2.5F, (double)0.0F, (double)1.0F);
      state.panelVisibility = approach * approach * (3.0F - 2.0F * approach);

      for(int i = 0; i < 3; ++i) {
         ItemStack stack = machine.displayedBet(i);
         state.counts[i] = stack.getCount();
         if (!stack.isEmpty() && !(state.panelVisibility <= 0.0F)) {
            state.blockIcons[i] = CasinoBetIcons.drawnByFace(stack);
            this.itemModels.updateForTopItem(state.items[i], stack, ItemDisplayContext.GUI, machine.getLevel(), (ItemOwner)null, i);
            ICON_RANDOM.setSeed((long)i);
            state.iconSprites[i] = CasinoBetIcons.flatIcon(stack, state.items[i], ICON_RANDOM);
         }
      }

      Objects.requireNonNull(machine);
      landed(plan, elapsed, machine::slotOf, state);
      state.kind = machine.resultKind();
   }

   static void landed(PlinkoDrop.Plan plan, double elapsed, IntUnaryOperator slotOf, PlinkoRenderState state) {
      Arrays.fill(state.landed, -1);
      state.resultShown = false;
      state.bestQuarters = 0;
      if (plan != null && !(elapsed < (double)0.0F)) {
         for(int b = 0; b < plan.ballCount(); ++b) {
            if (elapsed >= plan.ball(b).landTick()) {
               state.landed[slotOf.applyAsInt(b)] = PlinkoGame.quarters(plan.ball(b).pocket());
            }
         }

         state.resultShown = elapsed >= (double)plan.resultTick();
         state.bestQuarters = plan.bestQuarters();
      }
   }

   public void submit(PlinkoRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      CasinoMachineNames.submit(state, pose, output, camera);
      pose.pushPose();
      pose.translate((double)0.5F, (double)0.0F, (double)0.5F);
      pose.rotate((new Quaternionf()).rotationY((float)((double)(-state.facing.get2DDataValue()) * Math.PI / (double)2.0F)));
      pose.translate((double)-1.5F, (double)-1.0F, (double)-0.5F);
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> lights(vertices, transform, state));
      int light = state.lightCoords;
      if (state.ballCount > 0) {
         output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> balls(vertices, transform, state, light));
      }

      boolean glowing = false;

      for(float p : state.pockets) {
         glowing |= glowAlpha(p) > 0;
      }

      if (glowing) {
         output.order(2).submitCustomGeometry(pose, RenderTypes.entityTranslucent(SHEET), (transform, vertices) -> glows(vertices, transform, state));
      }

      pose.popPose();
      if (state.panelVisibility > 0.0F) {
         this.panel(state, pose, output, camera);
      }

   }

   static void lights(VertexConsumer vertices, PoseStack.Pose transform, PlinkoRenderState state) {
      float[] lamp = PlinkoRenderGeometry.lamp();

      for(int pin = 0; pin < 52; ++pin) {
         quads(vertices, transform, lamp, PlinkoRenderGeometry.PINS[2 * pin], PlinkoRenderGeometry.PINS[2 * pin + 1], (double)13.5F, lampTint((double)state.lamps[pin]), 15728880, true);
      }

      float[] bulb = PlinkoRenderGeometry.bulb();

      for(int b = 0; b < 11; ++b) {
         quads(vertices, transform, bulb, PlinkoRenderGeometry.BULBS[2 * b], PlinkoRenderGeometry.BULBS[2 * b + 1], (double)11.5F, lampTint((double)state.bulbs[b]), 15728880, true);
      }

      float[] uv = PlinkoRenderGeometry.RIM_UV;
      double half = (double)1.75F;
      double rim = (double)0.25F;
      double z = 14.1125;
      double bottom = (double)11.0F;
      double top = (double)14.0F;

      for(int k = 0; k < 9; ++k) {
         if (!(state.pockets[k] <= 0.01F)) {
            double x = PlinkoGame.pocketX(k);
            int tint = gray(0.45 + 0.55 * (double)state.pockets[k]);
            flat(vertices, transform, x - half, bottom, x - half + rim, top, z, uv, tint);
            flat(vertices, transform, x + half - rim, bottom, x + half, top, z, uv, tint);
            flat(vertices, transform, x - half + rim, top - rim, x + half - rim, top, z, uv, tint);
            flat(vertices, transform, x - half + rim, bottom, x + half - rim, bottom + rim, z, uv, tint);
         }
      }

   }

   static void balls(VertexConsumer vertices, PoseStack.Pose transform, PlinkoRenderState state, int light) {
      float[] ball = PlinkoRenderGeometry.ball();

      for(int b = 0; b < state.ballCount; ++b) {
         quads(vertices, transform, ball, (double)state.balls[2 * b], (double)state.balls[2 * b + 1], (double)12.0F, -1, light, false);
      }

   }

   static void glows(VertexConsumer vertices, PoseStack.Pose transform, PlinkoRenderState state) {
      float[] uv = PlinkoRenderGeometry.GLOW_UV;
      double half = (double)1.75F;
      double z = 14.0925;

      for(int k = 0; k < 9; ++k) {
         int alpha = glowAlpha(state.pockets[k]);
         if (alpha > 0) {
            double x = PlinkoGame.pocketX(k);
            flat(vertices, transform, x - half, (double)11.0F, x + half, (double)14.0F, z, uv, 16777215 | alpha << 24);
         }
      }

   }

   static int glowAlpha(float p) {
      double wash = Math.max((double)0.0F, Math.min((double)1.0F, ((double)p - 0.45) / 0.55));
      return (int)Math.round(96.9 * wash);
   }

   static int lampTint(double brightness) {
      double t = Math.max((double)0.0F, Math.min((double)1.0F, brightness));
      int r = (int)Math.round((double)255.0F * (0.66 + 0.34 * t));
      int g = (int)Math.round((double)255.0F * (0.46 + 0.54 * t));
      int b = (int)Math.round((double)255.0F * (0.26 + 0.74 * t));
      return -16777216 | r << 16 | g << 8 | b;
   }

   private static int gray(double brightness) {
      int g = (int)Math.round((double)255.0F * Math.max((double)0.0F, Math.min((double)1.0F, brightness)));
      return -16777216 | g << 16 | g << 8 | g;
   }

   private static void flat(VertexConsumer vertices, PoseStack.Pose transform, double x0, double y0, double x1, double y1, double z, float[] uv, int tint) {
      vertex(vertices, transform, x0, y1, z, uv[0], uv[1], tint, 15728880, 0.0F, 1.0F, 0.0F);
      vertex(vertices, transform, x0, y0, z, uv[0], uv[3], tint, 15728880, 0.0F, 1.0F, 0.0F);
      vertex(vertices, transform, x1, y0, z, uv[2], uv[3], tint, 15728880, 0.0F, 1.0F, 0.0F);
      vertex(vertices, transform, x1, y1, z, uv[2], uv[1], tint, 15728880, 0.0F, 1.0F, 0.0F);
   }

   private static void quads(VertexConsumer vertices, PoseStack.Pose transform, float[] data, double x, double y, double z, int tint, int light, boolean selfLit) {
      int per = 23;

      for(int q = 0; q + per <= data.length; q += per) {
         float nx = selfLit ? 0.0F : data[q + 20];
         float ny = selfLit ? 1.0F : data[q + 21];
         float nz = selfLit ? 0.0F : data[q + 22];

         for(int v = 0; v < 4; ++v) {
            int o = q + v * 5;
            vertex(vertices, transform, x + (double)data[o], y + (double)data[o + 1], z + (double)data[o + 2], data[o + 3], data[o + 4], tint, light, nx, ny, nz);
         }
      }

   }

   private static void vertex(VertexConsumer vertices, PoseStack.Pose transform, double x, double y, double z, float u, float v, int color, int light, float nx, float ny, float nz) {
      vertices.addVertex(transform, (float)(x / (double)16.0F), (float)(y / (double)16.0F), (float)(z / (double)16.0F)).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transform, nx, ny, nz);
   }

   private void panel(PlinkoRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      int stakes = 0;
      boolean anyLanded = false;

      for(int i = 0; i < 3; ++i) {
         if (state.counts[i] != 0) {
            ++stakes;
            anyLanded |= state.landed[i] >= 0;
         }
      }

      if (stakes != 0) {
         boolean decided = state.resultShown;
         int width = Math.max(Math.max(116, stakes * 46 + 8), decided ? this.textWidth(resultText(state).getVisualOrderText()) + 12 : 0);
         int height = 39 + (anyLanded ? 16 : 0) + (decided ? 18 : 0);
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
         FormattedCharSequence title = Component.translatable("gui.roulettecasino.plinko.panel_title").getVisualOrderText();
         output.submitText(pose, (float)(-this.textWidth(title)) / 2.0F, (float)(top + 5), title, true, DisplayMode.NORMAL, 15728880, -398143, 0, 0);
         pose.popPose();
         int rowTop = top + 19;
         background(output, pose, (float)(left + 3), (float)rowTop, (float)(left + width - 3), (float)(rowTop + 18), -1339805653);
         int pocketTop = rowTop + 20;
         if (anyLanded) {
            background(output, pose, (float)(left + 3), (float)pocketTop, (float)(left + width - 3), (float)(pocketTop + 14), -1339805653);
         }

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
               if (state.landed[slot] >= 0) {
                  FormattedCharSequence odds = Component.translatable(PlinkoGame.multiplierKey(state.landed[slot])).withStyle(ChatFormatting.BOLD).getVisualOrderText();
                  output.submitText(pose, (float)x + 23.0F - (float)this.textWidth(odds) / 2.0F, (float)(pocketTop + 3), odds, true, DisplayMode.NORMAL, 15728880, pocketColor(state.landed[slot]), 0, 0);
               }

               pose.popPose();
            }
         }

         if (decided) {
            int resultTop = pocketTop + (anyLanded ? 16 : 0);
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

   static int pocketColor(int quarters) {
      return quarters >= 32 ? -22016 : (quarters >= 8 ? -7811194 : -1599358);
   }

   static Component resultText(PlinkoRenderState state) {
      String var10000;
      switch (state.kind) {
         case NONE -> var10000 = "gui.roulettecasino.slot.result_lose";
         case PARTIAL -> var10000 = "gui.roulettecasino.plinko.result_partial";
         case WIN -> var10000 = "gui.roulettecasino.plinko.result_win";
         case BIG -> var10000 = "gui.roulettecasino.plinko.result_big";
         case JACKPOT -> var10000 = "gui.roulettecasino.plinko.result_jackpot";
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      String key = var10000;
      Component odds = Component.translatable(PlinkoGame.multiplierKey(state.bestQuarters)).withColor(RESULT_NUMBER[state.kind.ordinal()]);
      return Component.translatable(key, odds).copy().withStyle(ChatFormatting.BOLD);
   }

   private int textWidth(FormattedCharSequence text) {
      return this.measure.applyAsInt(text);
   }

   private static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      CasinoFloatingPanels.background(output, pose, x1, y1, x2, y2, color);
   }

   private static void spriteIcon(SubmitNodeCollector output, PoseStack pose, TextureAtlasSprite sprite, float x1, float y1, float x2, float y2) {
      output.submitCustomGeometry(pose, RenderTypes.text(sprite.atlasLocation()), (transform, vertices) -> {
         iconVertex(vertices, transform, x1, y1, sprite.getU0(), sprite.getV0());
         iconVertex(vertices, transform, x1, y2, sprite.getU0(), sprite.getV1());
         iconVertex(vertices, transform, x2, y2, sprite.getU1(), sprite.getV1());
         iconVertex(vertices, transform, x2, y1, sprite.getU1(), sprite.getV0());
      });
   }

   private static void iconVertex(VertexConsumer vertices, PoseStack.Pose transform, float x, float y, float u, float v) {
      vertices.addVertex(transform, x, y, 1.0F).setColor(-1).setUv(u, v).setLight(15728880);
   }

   public int getViewDistance() {
      return 64;
   }

   public AABB getRenderBoundingBox(PlinkoBlockEntity machine) {
      return (new AABB((double)-2.5F, (double)-1.0625F, (double)-2.5F, (double)3.5F, (double)4.5F, (double)3.5F)).move(machine.getBlockPos());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }
}
