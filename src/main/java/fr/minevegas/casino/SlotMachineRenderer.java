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

public final class SlotMachineRenderer implements CasinoBlockEntityRenderer<SlotMachineBlockEntity, SlotMachineRenderState> {
   private static final Identifier SHEET = Identifier.fromNamespaceAndPath("roulettecasino", "textures/entity/slot_machine.png");
   private static final int FULL_LIGHT = 15728880;
   private static final float[] LIT = new float[]{0.0F, 1.0F, 0.0F};
   private static final RandomSource ICON_RANDOM = RandomSource.create(0L);
   private static final Quaternionf BLOCK_GUI_INVERSE = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)30.0F), (float)Math.toRadians((double)225.0F), 0.0F).conjugate();
   static final double PANEL_Y = 3.3;
   private static final int CELL_W = 46;
   private static final int FRAME = -123241929;
   private static final int INSIDE = -266188488;
   private static final int HEADER_EDGE = -118897045;
   private static final int HEADER = -199929794;
   private static final int ROW = -1339805653;
   private static final int TITLE = -398143;
   private static final int[] RESULT_COLOR = new int[]{-1599358, -865166, -7811194, -5906485, -22016};
   private static final int[] RESULT_NUMBER = new int[]{16777215, 16777215, 16777045, 16777045, 16777215};
   private static final int MIN_PANEL_W = 116;
   private final ItemModelResolver itemModels;
   private final ToIntFunction<FormattedCharSequence> measure;

   public SlotMachineRenderer(BlockEntityRendererProvider.Context context) {
      this.itemModels = context.itemModelResolver();
      Font font = context.font();
      Objects.requireNonNull(font);
      this.measure = font::width;
   }

   public SlotMachineRenderState createRenderState() {
      return new SlotMachineRenderState();
   }

   public void extractRenderState(SlotMachineBlockEntity machine, SlotMachineRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
      CasinoBlockEntityRenderer.super.extractRenderState(machine, state, partialTick, cameraPos, breaking);
      state.facing = (Direction)machine.getBlockState().getValue(SlotMachineBlock.FACING);
      Level level = machine.getLevel();
      SlotMachineSpin.Plan plan = machine.plan();
      double idleTime = CasinoIdleLights.clock(level == null ? (double)0.0F : (double)level.getGameTime() + (double)partialTick, machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
      int[] stops = machine.stops();
      double elapsed = plan != null && level != null ? (double)level.getGameTime() + (double)partialTick - (double)machine.spinStart() : (double)0.0F;

      for(int k = 0; k < 3; ++k) {
         if (plan != null && level != null) {
            state.turn[k] = plan.turn(k, elapsed);
            state.speed[k] = plan.speed(k, elapsed);
         } else {
            state.turn[k] = (double)stops[k];
            state.speed[k] = (double)0.0F;
         }
      }

      if (plan != null && level != null) {
         state.lever = plan.lever(elapsed);
         state.lamp = (float)CasinoIdleLights.afterGame(plan.lamp(elapsed), CasinoIdleLights.beacon(idleTime), elapsed - (double)plan.endTick());
         state.drumBrightness = (float)plan.flash(elapsed);

         for(int i = 0; i < 15; ++i) {
            state.bulbs[i] = (float)CasinoIdleLights.afterGame(plan.bulb(i, elapsed), CasinoIdleLights.bulb(i, 15, idleTime), elapsed - (double)plan.endTick());
         }

         state.resultShown = elapsed >= (double)plan.resultTick();
         state.kind = plan.result().kind();
         state.multiplier = plan.result().multiplier();
         System.arraycopy(SlotMachineGame.symbols(plan.stops()), 0, state.symbols, 0, 3);
      } else {
         state.lever = (double)-14.0F;
         state.lamp = (float)CasinoIdleLights.beacon(idleTime);
         state.drumBrightness = 1.0F;

         for(int i = 0; i < 15; ++i) {
            state.bulbs[i] = (float)CasinoIdleLights.bulb(i, 15, idleTime);
         }

         state.resultShown = false;
      }

      this.extractPanel(machine, state, cameraPos);
   }

   private void extractPanel(SlotMachineBlockEntity machine, SlotMachineRenderState state, Vec3 cameraPos) {
      double distance = cameraPos.distanceTo(new Vec3((double)machine.getBlockPos().getX() + (double)0.5F, (double)machine.getBlockPos().getY() + 3.3, (double)machine.getBlockPos().getZ() + (double)0.5F));
      float approach = (float)Math.clamp(((double)10.0F - distance) / (double)2.5F, (double)0.0F, (double)1.0F);
      state.panelVisibility = approach * approach * (3.0F - 2.0F * approach);

      for(int i = 0; i < 1; ++i) {
         ItemStack stack = machine.displayedBet(i);
         state.counts[i] = stack.getCount();
         if (!stack.isEmpty() && !(state.panelVisibility <= 0.0F)) {
            state.blockIcons[i] = CasinoBetIcons.drawnByFace(stack);
            this.itemModels.updateForTopItem(state.items[i], stack, ItemDisplayContext.GUI, machine.getLevel(), (ItemOwner)null, i);
            ICON_RANDOM.setSeed((long)i);
            state.iconSprites[i] = CasinoBetIcons.flatIcon(stack, state.items[i], ICON_RANDOM);
         }
      }

   }

   public void submit(SlotMachineRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      CasinoMachineNames.submit(state, pose, output, camera);
      int turns = state.facing.get2DDataValue();
      pose.pushPose();
      pose.translate((double)0.5F, (double)0.0F, (double)0.5F);
      pose.rotate((new Quaternionf()).rotationY((float)((double)(-turns) * Math.PI / (double)2.0F)));
      pose.translate((double)-0.5F, (double)0.0F, (double)-0.5F);
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> drums(vertices, transform, state));
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> lights(vertices, transform, state));
      pose.pushPose();
      pose.translate(SlotMachineRenderGeometry.LEVER_PIVOT[0] / (double)16.0F, SlotMachineRenderGeometry.LEVER_PIVOT[1] / (double)16.0F, SlotMachineRenderGeometry.LEVER_PIVOT[2] / (double)16.0F);
      pose.rotate((new Quaternionf()).rotationX((float)Math.toRadians(state.lever)));
      int light = state.lightCoords;
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> {
         quads(vertices, transform, SlotMachineRenderGeometry.arm(), -1, light, false);
         quads(vertices, transform, SlotMachineRenderGeometry.ball(), -1, light, false);
      });
      pose.popPose();
      pose.popPose();
      if (state.panelVisibility > 0.0F) {
         this.panel(state, pose, output, camera);
      }

   }

   static double shade(double theta) {
      return 0.42 + 0.58 * Math.pow(Math.cos(theta), 3.2);
   }

   static void drums(VertexConsumer vertices, PoseStack.Pose transform, SlotMachineRenderState state) {
      int facets = 16;
      double span = Math.toRadians((double)60.0F);
      double radius = 7.002817496043395;
      float[] cap = SlotMachineRenderGeometry.CAP_UV;
      float capU = (cap[0] + cap[2]) / 2.0F;
      float capV = (cap[1] + cap[3]) / 2.0F;

      for(int k = 0; k < 3; ++k) {
         double turn = state.turn[k];
         double within = turn - (double)8.0F * Math.floor(turn / (double)8.0F);
         double rowCentre = (double)682.0F - (double)44.0F * within;
         double speed = state.speed[k];
         int level = speed < 0.35 ? 0 : (speed < 0.62 ? 1 : 2);
         int sheetX = SlotMachineRenderGeometry.STRIP_X[3 * k + level];
         float u0 = ((float)sheetX + 0.5F) / 512.0F;
         float u1 = ((float)(sheetX + 40) - 0.5F) / 512.0F;
         float x0 = (float)SlotMachineRenderGeometry.DRUM_X0[k];
         float x1 = x0 + 5.0F;

         for(int j = 0; j < facets; ++j) {
            double a0 = -span + (double)2.0F * span * (double)j / (double)facets;
            double a1 = -span + (double)2.0F * span * (double)(j + 1) / (double)facets;
            double y0 = (double)25.75F - radius * Math.sin(a0);
            double z0 = 4.497182503956605 + radius * Math.cos(a0);
            double y1 = (double)25.75F - radius * Math.sin(a1);
            double z1 = 4.497182503956605 + radius * Math.cos(a1);
            float v0 = (float)((rowCentre + (double)8.0F * radius * a0) / (double)1056.0F);
            float v1 = (float)((rowCentre + (double)8.0F * radius * a1) / (double)1056.0F);
            int c0 = gray(shade(a0) * (double)state.drumBrightness);
            int c1 = gray(shade(a1) * (double)state.drumBrightness);
            vertex(vertices, transform, (double)x0, y0, z0, u0, v0, c0, 15728880, LIT[0], LIT[1], LIT[2]);
            vertex(vertices, transform, (double)x0, y1, z1, u0, v1, c1, 15728880, LIT[0], LIT[1], LIT[2]);
            vertex(vertices, transform, (double)x1, y1, z1, u1, v1, c1, 15728880, LIT[0], LIT[1], LIT[2]);
            vertex(vertices, transform, (double)x1, y0, z0, u1, v0, c0, 15728880, LIT[0], LIT[1], LIT[2]);
         }

         int dark = gray(0.45 * (double)state.drumBrightness);

         for(int side = 0; side < 2; ++side) {
            float x = side == 0 ? x0 : x1;

            for(int j = 0; j + 2 <= facets; j += 2) {
               double a = -span + (double)2.0F * span * (double)j / (double)facets;
               double b = -span + (double)2.0F * span * (double)(j + 1) / (double)facets;
               double c = -span + (double)2.0F * span * (double)(j + 2) / (double)facets;
               vertex(vertices, transform, (double)x, (double)25.75F, 4.497182503956605, capU, capV, dark, 15728880, LIT[0], LIT[1], LIT[2]);
               vertex(vertices, transform, (double)x, (double)25.75F - radius * Math.sin(a), 4.497182503956605 + radius * Math.cos(a), capU, capV, dark, 15728880, LIT[0], LIT[1], LIT[2]);
               vertex(vertices, transform, (double)x, (double)25.75F - radius * Math.sin(b), 4.497182503956605 + radius * Math.cos(b), capU, capV, dark, 15728880, LIT[0], LIT[1], LIT[2]);
               vertex(vertices, transform, (double)x, (double)25.75F - radius * Math.sin(c), 4.497182503956605 + radius * Math.cos(c), capU, capV, dark, 15728880, LIT[0], LIT[1], LIT[2]);
            }
         }
      }

   }

   static void lights(VertexConsumer vertices, PoseStack.Pose transform, SlotMachineRenderState state) {
      int lampTint = gray(0.3 + 0.7 * (double)state.lamp);
      quads(vertices, transform, SlotMachineRenderGeometry.lamp(), lampTint, 15728880, true);
      float[] uv = SlotMachineRenderGeometry.BULB_UV;
      float half = 0.5F;

      for(int i = 0; i < 15; ++i) {
         float cx = (float)SlotMachineRenderGeometry.BULBS[3 * i];
         float cy = (float)SlotMachineRenderGeometry.BULBS[3 * i + 1];
         float cz = (float)SlotMachineRenderGeometry.BULBS[3 * i + 2];
         int tint = gray(0.28 + 0.72 * (double)state.bulbs[i]);
         vertex(vertices, transform, (double)(cx - half), (double)(cy + half), (double)cz, uv[0], uv[1], tint, 15728880, LIT[0], LIT[1], LIT[2]);
         vertex(vertices, transform, (double)(cx - half), (double)(cy - half), (double)cz, uv[0], uv[3], tint, 15728880, LIT[0], LIT[1], LIT[2]);
         vertex(vertices, transform, (double)(cx + half), (double)(cy - half), (double)cz, uv[2], uv[3], tint, 15728880, LIT[0], LIT[1], LIT[2]);
         vertex(vertices, transform, (double)(cx + half), (double)(cy + half), (double)cz, uv[2], uv[1], tint, 15728880, LIT[0], LIT[1], LIT[2]);
      }

   }

   private static int gray(double brightness) {
      int g = (int)Math.round((double)255.0F * Math.max((double)0.0F, Math.min((double)1.0F, brightness)));
      return -16777216 | g << 16 | g << 8 | g;
   }

   private static void quads(VertexConsumer vertices, PoseStack.Pose transform, float[] data, int tint, int light, boolean selfLit) {
      int per = 23;

      for(int q = 0; q + per <= data.length; q += per) {
         float nx = selfLit ? LIT[0] : data[q + 20];
         float ny = selfLit ? LIT[1] : data[q + 21];
         float nz = selfLit ? LIT[2] : data[q + 22];

         for(int v = 0; v < 4; ++v) {
            int o = q + v * 5;
            vertex(vertices, transform, (double)data[o], (double)data[o + 1], (double)data[o + 2], data[o + 3], data[o + 4], tint, light, nx, ny, nz);
         }
      }

   }

   private static void vertex(VertexConsumer vertices, PoseStack.Pose transform, double x, double y, double z, float u, float v, int color, int light, float nx, float ny, float nz) {
      vertices.addVertex(transform, (float)(x / (double)16.0F), (float)(y / (double)16.0F), (float)(z / (double)16.0F)).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transform, nx, ny, nz);
   }

   private void panel(SlotMachineRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      int stakes = 0;

      for(int i = 0; i < 1; ++i) {
         if (state.counts[i] > 0) {
            ++stakes;
         }
      }

      if (stakes != 0) {
         boolean decided = state.resultShown;
         int width = Math.max(Math.max(116, stakes * 46 + 8), decided ? this.textWidth(resultText(state).getVisualOrderText()) + 12 : 0);
         int height = 39 + (decided ? 39 : 0);
         int left = -width / 2;
         int top = -height / 2;
         pose.pushPose();
         pose.translate((double)0.5F, 3.3 - 0.09 * (double)(1.0F - state.panelVisibility), (double)0.5F);
         pose.rotate(camera.orientation);
         float scale = 0.011F * state.panelVisibility;
         pose.scale(scale, -scale, scale);
         output = CasinoFloatingPanels.faded(output, state, camera, (double)0.5F, 3.3 - 0.09 * (double)(1.0F - state.panelVisibility), (double)0.5F, width, height, scale);
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
         FormattedCharSequence title = Component.translatable("gui.roulettecasino.slot.panel_title").getVisualOrderText();
         output.submitText(pose, (float)(-this.textWidth(title)) / 2.0F, (float)(top + 5), title, true, DisplayMode.NORMAL, 15728880, -398143, 0, 0);
         pose.popPose();
         int rowTop = top + 19;
         background(output, pose, (float)(left + 3), (float)rowTop, (float)(left + width - 3), (float)(rowTop + 18), -1339805653);
         int cellLeft = -stakes * 46 / 2;
         int cell = 0;

         for(int slot = 0; slot < 1; ++slot) {
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
            int symbolsTop = rowTop + 20;
            background(output, pose, (float)(left + 3), (float)symbolsTop, (float)(left + width - 3), (float)(symbolsTop + 19), -1339805653);
            pose.pushPose();
            pose.translate((double)0.0F, (double)0.0F, (double)1.0F);

            for(int k = 0; k < 3; ++k) {
               float[] uv = new float[]{SlotMachineRenderGeometry.ICON_UV[4 * state.symbols[k]], SlotMachineRenderGeometry.ICON_UV[4 * state.symbols[k] + 1], SlotMachineRenderGeometry.ICON_UV[4 * state.symbols[k] + 2], SlotMachineRenderGeometry.ICON_UV[4 * state.symbols[k] + 3]};
               float x = (float)(-25 + k * 18);
               sheetIcon(output, pose, uv, x, (float)symbolsTop + 2.5F, x + 14.0F, (float)symbolsTop + 16.5F);
            }

            pose.popPose();
            int resultTop = symbolsTop + 21;
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

   static Component resultText(SlotMachineRenderState state) {
      String var10000;
      switch (state.kind) {
         case NONE:
            var10000 = "gui.roulettecasino.slot.result_lose";
            break;
         case PUSH:
            var10000 = "gui.roulettecasino.slot.result_push";
            break;
         case WIN:
         case BIG:
            var10000 = "gui.roulettecasino.slot.result_win";
            break;
         case JACKPOT:
            var10000 = "gui.roulettecasino.slot.result_jackpot";
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      String key = var10000;
      Component text = state.kind != SlotMachineGame.Kind.NONE && state.kind != SlotMachineGame.Kind.PUSH ? Component.translatable(key, Component.literal("×" + state.multiplier).withColor(RESULT_NUMBER[state.kind.ordinal()])) : Component.translatable(key);
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

   public AABB getRenderBoundingBox(SlotMachineBlockEntity machine) {
      return (new AABB((double)-2.5F, (double)-0.0625F, (double)-2.5F, (double)3.5F, (double)5.0F, (double)3.5F)).move(machine.getBlockPos());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }
}
