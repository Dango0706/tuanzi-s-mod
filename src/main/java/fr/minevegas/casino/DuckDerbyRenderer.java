package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.DefaultPlayerSkin;
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
import org.joml.Vector3f;

public final class DuckDerbyRenderer implements CasinoBlockEntityRenderer<DuckDerbyBlockEntity, DuckDerbyRenderState> {
   private static final Identifier SHEET = Identifier.fromNamespaceAndPath("roulettecasino", "textures/entity/duck_derby.png");
   private static final Identifier SIGN = Identifier.fromNamespaceAndPath("roulettecasino", "textures/block/dd_sign.png");
   private static final Identifier BULB_SHEET = Identifier.fromNamespaceAndPath("roulettecasino", "textures/entity/slot_machine.png");
   private static final int FULL_LIGHT = 15728880;
   private static final RandomSource ICON_RANDOM = RandomSource.create(0L);
   private static final Quaternionf BLOCK_GUI_INVERSE = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)30.0F), (float)Math.toRadians((double)225.0F), 0.0F).conjugate();
   private static final double PANEL_Y = 2.6;
   private static final int COLUMN_W = 58;
   private static final int FRAME = -123241929;
   private static final int INSIDE = -266188488;
   private static final int HEADER_EDGE = -118897045;
   private static final int ROW = -1339805653;
   private static final int SECOND_EDGE = -188100906;
   private static final int WINNER_EDGE = -1456533;
   private static final int[] HEADER = new int[]{-186730673, -191158735, -185932596, -199154389};
   private static final int[] HEADER_TEXT = new int[]{-15254482, -398143, -15254482, -398143};
   private static final int TITLE = -398143;
   private static final int[] WINNER_NAME = new int[]{16777045, 16733525, 16777215, 11184810};
   private static final String[] NAME = new String[]{"gui.roulettecasino.derby.duck_0", "gui.roulettecasino.derby.duck_1", "gui.roulettecasino.derby.duck_2", "gui.roulettecasino.derby.duck_3"};
   private static final int MIN_PANEL_W = 116;
   private final ItemModelResolver itemModels;
   private final ToIntFunction<FormattedCharSequence> measure;

   public DuckDerbyRenderer(BlockEntityRendererProvider.Context context) {
      this.itemModels = context.itemModelResolver();
      Font font = context.font();
      Objects.requireNonNull(font);
      this.measure = font::width;
   }

   public DuckDerbyRenderState createRenderState() {
      return new DuckDerbyRenderState();
   }

   public void extractRenderState(DuckDerbyBlockEntity machine, DuckDerbyRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
      CasinoBlockEntityRenderer.super.extractRenderState(machine, state, partialTick, cameraPos, breaking);
      state.facing = (Direction)machine.getBlockState().getValue(DuckDerbyBlock.FACING);
      Level level = machine.getLevel();
      state.lightTime = CasinoIdleLights.clock(level == null ? (double)0.0F : (double)level.getGameTime() + (double)partialTick, machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
      DuckDerbyRace.Plan plan = machine.plan();
      double elapsed = (double)-1.0F;
      if (plan != null && level != null && machine.raceStart() >= 0L) {
         elapsed = (double)level.getGameTime() + (double)partialTick - (double)machine.raceStart();
         if (elapsed < (double)0.0F || elapsed > (double)(plan.endTick() + 1)) {
            elapsed = (double)-1.0F;
         }
      }

      for(int lane = 0; lane < 4; ++lane) {
         DuckDerbyRace.Pose pose = state.poses[lane];
         if (elapsed >= (double)0.0F) {
            plan.pose(lane, elapsed, pose);
         } else {
            pose.z = (double)56.5F;
            pose.pitch = (double)0.0F;
            pose.lift = (double)0.0F;
         }
      }

      state.gate = elapsed >= (double)0.0F ? (float)plan.gate(elapsed) : 0.0F;
      state.winnerShown = elapsed >= (double)0.0F && elapsed >= (double)plan.resultTick() ? plan.winner() : -1;
      state.secondShown = state.winnerShown >= 0 ? DuckDerbyBlockEntity.runnerUp(plan) : -1;
      this.extractPanel(machine, state, cameraPos, partialTick);
   }

   private void extractPanel(DuckDerbyBlockEntity machine, DuckDerbyRenderState state, Vec3 cameraPos, float partialTick) {
      state.shared = machine.groupSession();
      state.enrolling = machine.enrolling();
      state.members = machine.participantCount();
      state.seconds = machine.enrollmentSeconds();
      state.enrollmentProgress = machine.enrollmentProgress(partialTick);
      int turns = state.facing.get2DDataValue();
      double[] centre = DuckDerbyLayout.toBlock((double)24.0F, (double)0.0F, (double)32.0F, turns);
      double distance = cameraPos.distanceTo(new Vec3((double)machine.getBlockPos().getX() + centre[0], (double)machine.getBlockPos().getY() + 2.6, (double)machine.getBlockPos().getZ() + centre[2]));
      float approach = (float)Math.clamp(((double)10.0F - distance) / (double)2.5F, (double)0.0F, (double)1.0F);
      state.panelVisibility = approach * approach * (3.0F - 2.0F * approach);
      state.bettorCount = 0;
      if (state.shared && state.panelVisibility > 0.0F) {
         for(DerbyLobby.Member member : DerbyLobby.visible(machine.members(), Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID(), 8)) {
            DuckDerbyRenderState.Bettor bettor = (DuckDerbyRenderState.Bettor)state.bettors.get(state.bettorCount++);
            Arrays.fill(bettor.counts, 0);
            bettor.name = member.name();
            bettor.lane = Math.floorMod(member.lane(), 4);
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            PlayerInfo info = connection == null ? null : connection.getPlayerInfo(member.id());
            bettor.skin = (info == null ? DefaultPlayerSkin.get(member.id()) : info.getSkin()).body().texturePath();

            for(int i = 0; i < Math.min(3, member.stakes().size()); ++i) {
               ItemStack stack = (ItemStack)member.stakes().get(i);
               bettor.counts[i] = stack.getCount();
               bettor.blocks[i] = CasinoBetIcons.drawnByFace(stack);
               this.itemModels.updateForTopItem(bettor.items[i], stack, ItemDisplayContext.GUI, machine.getLevel(), (ItemOwner)null, i);
               ICON_RANDOM.setSeed((long)i);
               bettor.sprites[i] = CasinoBetIcons.flatIcon(stack, bettor.items[i], ICON_RANDOM);
            }
         }
      }

      for(int i = 0; i < 12; ++i) {
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

   public void submit(DuckDerbyRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      CasinoMachineNames.submit(state, pose, output, camera);
      int turns = state.facing.get2DDataValue();
      pose.pushPose();
      pose.translate((double)0.5F, (double)0.0F, (double)0.5F);
      pose.rotate((new Quaternionf()).rotationY((float)((double)(-turns) * Math.PI / (double)2.0F)));
      float sink = (float)((double)state.gate * 3.6);

      for(int lane = 0; lane < 4; ++lane) {
         float ox = (float)(DuckDerbyGeometry.GATE_X[lane] - (double)24.0F);
         float oy = -sink;
         float oz = -56.0F;
         int light = state.lightCoords;
         output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> quads(vertices, transform, DuckDerbyGeometry.GATE, ox, oy, -56.0F, 0.0F, 0.0F, light));
      }

      for(int lane = 0; lane < 4; ++lane) {
         DuckDerbyRace.Pose p = state.poses[lane];
         pose.pushPose();
         pose.translate((DuckDerbyGeometry.LANE_CENTERS[lane] - (double)24.0F) / (double)16.0F, ((double)13.0F + p.lift) / (double)16.0F, (p.z - (double)56.0F) / (double)16.0F);
         if (p.pitch != (double)0.0F) {
            pose.rotate((new Quaternionf()).rotationX((float)p.pitch));
         }

         float du = DuckDerbyGeometry.LANE_UV[2 * lane];
         float dv = DuckDerbyGeometry.LANE_UV[2 * lane + 1];
         float oy = -0.5F;
         int light = state.lightCoords;
         output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SHEET), (transform, vertices) -> quads(vertices, transform, DuckDerbyGeometry.DUCK, 0.0F, -0.5F, 0.0F, du, dv, light));
         pose.popPose();
      }

      signLights(state, pose, output);
      pose.popPose();
      if (state.panelVisibility > 0.0F) {
         this.panel(state, pose, output, camera);
      }

   }

   static void signLights(DuckDerbyRenderState state, PoseStack pose, SubmitNodeCollector output) {
      output.order(1).submitCustomGeometry(pose, RenderTypes.entityCutout(SIGN), (transform, vertices) -> signQuad(vertices, transform, (double)4.0F, (double)17.0F, (double)44.0F, (double)31.0F, 3.025, 0.0F, 0.0F, 1.0F, 0.875F, -1));
      output.order(2).submitCustomGeometry(pose, RenderTypes.entityCutout(BULB_SHEET), (transform, vertices) -> {
         float[] uv = SlotMachineRenderGeometry.BULB_UV;

         for(int i = 0; i < 12; ++i) {
            double x = DuckDerbySignLights.x(i);
            double y = DuckDerbySignLights.y(i);
            double brightness = 0.28 + 0.72 * CasinoIdleLights.bulb(i, 12, state.lightTime);
            int g = (int)Math.round((double)255.0F * brightness);
            int tint = -16777216 | g << 16 | g << 8 | g;
            double half = (double)0.375F;
            signQuad(vertices, transform, x - half, y - half, x + half, y + half, 3.08, uv[0], uv[1], uv[2], uv[3], tint);
         }

      });
   }

   private static void signQuad(VertexConsumer vertices, PoseStack.Pose transform, double x0, double y0, double x1, double y1, double z, float u0, float v0, float u1, float v1, int tint) {
      signVertex(vertices, transform, x0, y1, z, u0, v0, tint);
      signVertex(vertices, transform, x0, y0, z, u0, v1, tint);
      signVertex(vertices, transform, x1, y0, z, u1, v1, tint);
      signVertex(vertices, transform, x1, y1, z, u1, v0, tint);
   }

   private static void signVertex(VertexConsumer vertices, PoseStack.Pose transform, double x, double y, double z, float u, float v, int tint) {
      vertices.addVertex(transform, (float)((x - (double)24.0F) / (double)16.0F), (float)(y / (double)16.0F), (float)((z - (double)56.0F) / (double)16.0F)).setColor(tint).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(transform, 0.0F, 1.0F, 0.0F);
   }

   private static void quads(VertexConsumer vertices, PoseStack.Pose transform, float[] data, float ox, float oy, float oz, float du, float dv, int light) {
      int per = 23;

      for(int q = 0; q + per <= data.length; q += per) {
         float nx = data[q + 20];
         float ny = data[q + 21];
         float nz = data[q + 22];

         for(int v = 0; v < 4; ++v) {
            int o = q + v * 5;
            vertices.addVertex(transform, (data[o] + ox) / 16.0F, (data[o + 1] + oy) / 16.0F, (data[o + 2] + oz) / 16.0F).setColor(-1).setUv(data[o + 3] + du, data[o + 4] + dv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transform, nx, ny, nz);
         }
      }

   }

   private void sharedPanel(DuckDerbyRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      Component title = Component.translatable(DerbyPanelPresentation.peopleKey(state.members, Minecraft.getInstance().getLanguageManager().getSelected()), state.members);
      Component phase = state.enrolling ? Component.translatable("gui.roulettecasino.derby.countdown", state.seconds) : Component.translatable("gui.roulettecasino.derby.racing");
      List<Component> lines = new ArrayList();
      if (state.winnerShown >= 0) {
         lines.add(title);
         lines.add(Component.translatable("gui.roulettecasino.derby.result_line", Component.translatable("gui.roulettecasino.derby.payout"), Component.translatable(NAME[state.winnerShown])).append(" · ").append((Component)Component.translatable("gui.roulettecasino.derby.result_line", Component.translatable("gui.roulettecasino.derby.second"), Component.translatable(NAME[state.secondShown]))));
      } else {
         lines.add(state.enrolling ? title : title.copy().append(" · ").append(phase));
      }

      int cellWidth = 52;

      for(int n = 0; n < state.bettorCount; ++n) {
         DuckDerbyRenderState.Bettor bettor = (DuckDerbyRenderState.Bettor)state.bettors.get(n);
         int stakesWidth = 20;

         for(int k = 0; k < 3; ++k) {
            if (bettor.counts[k] > 0) {
               int var10003 = bettor.counts[k];
               stakesWidth += 16 + this.textWidth(Component.literal("×" + var10003).getVisualOrderText());
            }
         }

         cellWidth = Math.max(cellWidth, Math.max(stakesWidth, 20 + this.textWidth(Component.literal(bettor.name).getVisualOrderText())));
      }

      int titleWidth = 0;

      for(Component line : lines) {
         titleWidth = Math.max(titleWidth, this.textWidth(line.getVisualOrderText()));
      }

      DerbyPanelPresentation.Layout layout = DerbyPanelPresentation.layout(cellWidth, state.bettorCount, lines.size(), titleWidth, state.members > state.bettorCount);
      int width = layout.width();
      int height = layout.height();
      double[] centre = DuckDerbyLayout.toBlock((double)24.0F, (double)0.0F, (double)32.0F, state.facing.get2DDataValue());
      pose.pushPose();
      pose.translate(centre[0], 2.6 + (double)((float)Math.max(0, height - 70) * 0.011F / 2.0F), centre[2]);
      pose.rotate(camera.orientation);
      float scale = 0.011F * state.panelVisibility;
      pose.scale(scale, -scale, scale);
      float opacity = DerbyPanelPresentation.opacity(state.enrolling, backingOpacity(state, camera, centre, 2.6 + (double)((float)Math.max(0, height - 70) * 0.011F / 2.0F), width, height, scale));
      SubmitNodeCollector itemOutput = (SubmitNodeCollector)(opacity >= 0.9999F ? output : new DerbyPanelItems(output, opacity));
      if (state.enrolling) {
         this.countdown(state, pose, output, -height / 2, opacity);
      }

      CasinoFloatingPanels.background(output, pose, (float)(-width / 2 - 3), (float)(-height / 2 - 3), (float)(width / 2 + 3), (float)(height / 2 + 3), -123241929, opacity);
      pose.translate(0.0F, 0.0F, 1.0F);
      CasinoFloatingPanels.background(output, pose, (float)(-width / 2 - 2), (float)(-height / 2 - 2), (float)(width / 2 + 2), (float)(height / 2 + 2), -266188488, opacity);
      pose.translate(0.0F, 0.0F, 1.0F);

      for(int n = 0; n < lines.size(); ++n) {
         FormattedCharSequence line = ((Component)lines.get(n)).getVisualOrderText();
         float fit = Math.min(1.0F, (float)layout.textWidth() / (float)Math.max(1, this.textWidth(line)));
         pose.pushPose();
         pose.translate(0.0F, (float)(layout.top() + n * 12), 0.0F);
         pose.scale(fit, fit, fit);
         output.submitText(pose, (float)(-this.textWidth(line)) / 2.0F, 0.0F, line, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-398143, opacity), 0, 0);
         pose.popPose();
      }

      for(int n = 0; n < state.bettorCount; ++n) {
         DuckDerbyRenderState.Bettor bettor = (DuckDerbyRenderState.Bettor)state.bettors.get(n);
         int x = layout.x(n);
         int y = layout.y(n);
         CasinoFloatingPanels.background(output, pose, (float)x, (float)(y + 19), (float)(x + 14), (float)(y + 22), HEADER[bettor.lane], opacity);
         pose.pushPose();
         head(output, pose, bettor.skin, (float)x, (float)(y + 2), 14.0F, opacity);
         pose.popPose();
         FormattedCharSequence name = Component.literal(bettor.name).getVisualOrderText();
         output.submitText(pose, (float)(x + 20), (float)(y + 1), name, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-398143, opacity), 0, 0);
         int ix = x + 20;
         int iy = y + 12;

         for(int k = 0; k < 3; ++k) {
            if (bettor.counts[k] > 0) {
               if (bettor.sprites[k] != null) {
                  icon(output, pose, bettor.sprites[k], (float)ix, (float)iy, (float)(ix + 11), (float)(iy + 11), opacity);
               } else {
                  pose.pushPose();
                  pose.translate((double)ix + (double)5.5F, (double)iy + (double)5.5F, (double)1.0F);
                  pose.scale(11.0F, -11.0F, 11.0F);
                  if (bettor.blocks[k]) {
                     pose.rotate(BLOCK_GUI_INVERSE);
                     pose.scale(1.6F, 1.6F, 1.6F);
                  } else {
                     CasinoBetIcons.inFrontOfPanel(pose);
                     CasinoBetIcons.fullLight(pose);
                  }

                  bettor.items[k].submit(pose, itemOutput, 15728880, OverlayTexture.NO_OVERLAY, 0);
                  pose.popPose();
               }

               int var10000 = bettor.counts[k];
               FormattedCharSequence count = Component.literal("×" + var10000).getVisualOrderText();
               output.submitText(pose, (float)(ix + 13), (float)(iy + 2), count, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-1, opacity), 0, 0);
               ix += 16 + this.textWidth(count);
            }
         }
      }

      if (state.members > state.bettorCount) {
         FormattedCharSequence more = Component.translatable("gui.roulettecasino.derby.more", state.members - state.bettorCount).getVisualOrderText();
         output.submitText(pose, (float)(-this.textWidth(more)) / 2.0F, (float)(height / 2 - 16), more, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-398143, opacity), 0, 0);
      }

      pose.popPose();
   }

   private void countdown(DuckDerbyRenderState state, PoseStack pose, SubmitNodeCollector output, int top, float opacity) {
      FormattedCharSequence message = Component.translatable("gui.roulettecasino.derby.countdown", state.seconds).getVisualOrderText();
      float textScale = Math.min(1.5F, 220.0F / (float)Math.max(1, this.textWidth(message)));
      float barWidth = Math.max(100.0F, (float)this.textWidth(message) * textScale + 8.0F);
      pose.pushPose();
      pose.translate(0.0F, (float)(top - 34), 4.0F);
      pose.scale(textScale, textScale, textScale);
      output.submitText(pose, (float)(-this.textWidth(message)) / 2.0F, 0.0F, message, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-398143, opacity), 0, 0);
      pose.popPose();
      pose.pushPose();
      pose.translate(0.0F, 0.0F, 4.0F);
      background(output, pose, -barWidth / 2.0F, (float)(top - 15), barWidth / 2.0F, (float)(top - 10), -123241929, opacity);
      pose.translate(0.0F, 0.0F, 1.0F);
      background(output, pose, -barWidth / 2.0F + 1.0F, (float)(top - 14), barWidth / 2.0F - 1.0F, (float)(top - 11), -266188488, opacity);
      pose.translate(0.0F, 0.0F, 1.0F);
      float fill = (barWidth - 2.0F) * state.enrollmentProgress;
      if (fill > 0.0F) {
         background(output, pose, -barWidth / 2.0F + 1.0F, (float)(top - 14), -barWidth / 2.0F + 1.0F + fill, (float)(top - 11), -866724, opacity);
      }

      pose.popPose();
   }

   private void panel(DuckDerbyRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      if (state.shared && state.members > 0) {
         this.sharedPanel(state, pose, output, camera);
      } else {
         int active = 0;
         int maxRows = 0;

         for(int lane = 0; lane < 4; ++lane) {
            int rows = 0;

            for(int row = 0; row < 3; ++row) {
               if (state.counts[row * 4 + lane] > 0) {
                  ++rows;
               }
            }

            if (rows > 0) {
               ++active;
            }

            maxRows = Math.max(maxRows, rows);
         }

         if (active != 0) {
            boolean decided = state.winnerShown >= 0;
            int width = Math.max(active * 58, 116);
            String[] resultKeys = new String[]{"gui.roulettecasino.derby.payout", "gui.roulettecasino.derby.second"};
            int[] resultLanes = new int[]{state.winnerShown, state.secondShown};
            FormattedCharSequence[] results = new FormattedCharSequence[2];

            for(int k = 0; decided && k < 2; ++k) {
               if (resultLanes[k] >= 0) {
                  results[k] = Component.translatable("gui.roulettecasino.derby.result_line", Component.translatable(resultKeys[k]), Component.translatable(NAME[resultLanes[k]]).withColor(WINNER_NAME[resultLanes[k]])).withStyle(ChatFormatting.BOLD).getVisualOrderText();
                  width = Math.max(width, this.textWidth(results[k]) + 12);
               }
            }

            int height = 19 + maxRows * 18 + (decided ? 16 : 0) + (decided && state.secondShown >= 0 ? 16 : 0);
            int left = -width / 2;
            int top = -height / 2;
            int columnsLeft = left + (width - active * 58) / 2;
            double[] centre = DuckDerbyLayout.toBlock((double)24.0F, (double)0.0F, (double)32.0F, state.facing.get2DDataValue());
            pose.pushPose();
            pose.translate(centre[0], 2.6 - 0.09 * (double)(1.0F - state.panelVisibility), centre[2]);
            pose.rotate(camera.orientation);
            float scale = 0.011F * state.panelVisibility;
            pose.scale(scale, -scale, scale);
            float opacity = backingOpacity(state, camera, centre, 2.6 - 0.09 * (double)(1.0F - state.panelVisibility), width, height, scale);
            SubmitNodeCollector itemOutput = (SubmitNodeCollector)(opacity >= 0.9999F ? output : new DerbyPanelItems(output, opacity));
            CasinoFloatingPanels.badge(state, pose, itemOutput, top, width);
            CasinoFloatingPanels.background(output, pose, (float)(left - 3), (float)(top - 3), (float)(left + width + 3), (float)(top + height + 3), -123241929, opacity);
            pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
            CasinoFloatingPanels.background(output, pose, (float)(left - 2), (float)(top - 2), (float)(left + width + 2), (float)(top + height + 2), -266188488, opacity);
            pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
            int visibleColumn = 0;

            for(int lane = 0; lane < 4; ++lane) {
               boolean any = false;

               for(int row = 0; row < 3; ++row) {
                  any |= state.counts[row * 4 + lane] > 0;
               }

               if (any) {
                  int x = columnsLeft + visibleColumn++ * 58;
                  CasinoFloatingPanels.background(output, pose, (float)(x + 1), (float)(top + 1), (float)(x + 58 - 1), (float)(top + 17), -118897045, opacity);
                  pose.pushPose();
                  pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
                  CasinoFloatingPanels.background(output, pose, (float)(x + 2), (float)(top + 2), (float)(x + 58 - 2), (float)(top + 16), HEADER[lane], opacity);
                  pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
                  FormattedCharSequence title = Component.translatable(NAME[lane]).getVisualOrderText();
                  output.submitText(pose, (float)x + 29.0F - (float)this.textWidth(title) / 2.0F, (float)(top + 5), title, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(HEADER_TEXT[lane], opacity), 0, 0);
                  pose.popPose();
                  int visibleRow = 0;

                  for(int row = 0; row < 3; ++row) {
                     int slot = row * 4 + lane;
                     if (state.counts[slot] != 0) {
                        int y = top + 19 + visibleRow++ * 18;
                        CasinoFloatingPanels.background(output, pose, (float)(x + 3), (float)y, (float)(x + 58 - 3), (float)(y + 17), -1339805653, opacity);
                        pose.pushPose();
                        pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
                        if (state.iconSprites[slot] != null) {
                           icon(output, pose, state.iconSprites[slot], (float)(x + 7), (float)(y + 1), (float)(x + 21), (float)(y + 15), opacity);
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

                           state.items[slot].submit(pose, itemOutput, 15728880, OverlayTexture.NO_OVERLAY, 0);
                           pose.popPose();
                        }

                        int var40 = state.counts[slot];
                        FormattedCharSequence count = Component.literal("×" + var40).getVisualOrderText();
                        output.submitText(pose, (float)(x + 27), (float)(y + 4), count, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-1, opacity), 0, 0);
                        pose.popPose();
                     }
                  }

                  if (state.winnerShown == lane || state.secondShown == lane) {
                     int edge = state.winnerShown == lane ? -1456533 : -188100906;
                     int bottom = top + 19 + maxRows * 18;
                     pose.pushPose();
                     pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
                     CasinoFloatingPanels.background(output, pose, (float)x, (float)top, (float)(x + 58), (float)(top + 2), edge, opacity);
                     CasinoFloatingPanels.background(output, pose, (float)x, (float)(bottom - 2), (float)(x + 58), (float)bottom, edge, opacity);
                     CasinoFloatingPanels.background(output, pose, (float)x, (float)(top + 2), (float)(x + 2), (float)(bottom - 2), edge, opacity);
                     CasinoFloatingPanels.background(output, pose, (float)(x + 58 - 2), (float)(top + 2), (float)(x + 58), (float)(bottom - 2), edge, opacity);
                     pose.popPose();
                  }
               }
            }

            if (decided) {
               for(int k = 0; k < 2; ++k) {
                  if (results[k] != null) {
                     int y = top + 19 + maxRows * 18 + 1 + 16 * k;
                     CasinoFloatingPanels.background(output, pose, (float)(left + 1), (float)y, (float)(left + width - 1), (float)(y + 14), -1339805653, opacity);
                     pose.pushPose();
                     pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
                     FormattedCharSequence line = results[k];
                     output.submitText(pose, (float)(-this.textWidth(line)) / 2.0F, (float)(y + 3), line, true, DisplayMode.NORMAL, 15728880, DerbyPanelFocus.color(-1, opacity), 0, 0);
                     pose.popPose();
                  }
               }
            }

            pose.popPose();
         }
      }
   }

   private static float backingOpacity(DuckDerbyRenderState state, CameraRenderState camera, double[] centre, double y, int width, int height, float scale) {
      Vector3f relative = new Vector3f((float)((double)state.blockPos.getX() + centre[0] - camera.pos.x), (float)((double)state.blockPos.getY() + y - camera.pos.y), (float)((double)state.blockPos.getZ() + centre[2] - camera.pos.z));
      relative.rotate((new Quaternionf(camera.orientation)).conjugate());
      return DerbyPanelFocus.opacity((double)relative.x, (double)relative.y, (double)(-relative.z), ((double)width / (double)2.0F + (double)3.0F) * (double)scale, ((double)height / (double)2.0F + (double)3.0F) * (double)scale);
   }

   private int textWidth(FormattedCharSequence text) {
      return this.measure.applyAsInt(text);
   }

   private static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color, float opacity) {
      CasinoPanelStyle.background(output, pose, x1, y1, x2, y2, color, opacity);
   }

   private static void icon(SubmitNodeCollector output, PoseStack pose, TextureAtlasSprite sprite, float x1, float y1, float x2, float y2, float opacity) {
      float u0 = sprite.getU0();
      float u1 = sprite.getU1();
      float v0 = sprite.getV0();
      float v1 = sprite.getV1();
      output.submitCustomGeometry(pose, RenderTypes.text(sprite.atlasLocation()), (transform, vertices) -> {
         iconVertex(vertices, transform, x1, y1, u0, v0, opacity);
         iconVertex(vertices, transform, x1, y2, u0, v1, opacity);
         iconVertex(vertices, transform, x2, y2, u1, v1, opacity);
         iconVertex(vertices, transform, x2, y1, u1, v0, opacity);
      });
   }

   private static void head(SubmitNodeCollector output, PoseStack pose, Identifier skin, float x, float y, float size, float opacity) {
      for(int layer = 0; layer < 2; ++layer) {
         float u = (layer == 0 ? 8.0F : 40.0F) / 64.0F;
         float v = 0.125F;
         float uv = 0.125F;
         output.submitCustomGeometry(pose, RenderTypes.text(skin), (transform, vertices) -> {
            iconVertex(vertices, transform, x, y, u, 0.125F, opacity);
            iconVertex(vertices, transform, x, y + size, u, 0.25F, opacity);
            iconVertex(vertices, transform, x + size, y + size, u + 0.125F, 0.25F, opacity);
            iconVertex(vertices, transform, x + size, y, u + 0.125F, 0.125F, opacity);
         });
         pose.translate(0.0F, 0.0F, 0.05F);
      }

   }

   private static void iconVertex(VertexConsumer vertices, PoseStack.Pose transform, float x, float y, float u, float v, float opacity) {
      vertices.addVertex(transform, x, y, 1.0F).setColor(DerbyPanelFocus.color(-1, opacity)).setUv(u, v).setLight(15728880);
   }

   public int getViewDistance() {
      return 64;
   }

   public AABB getRenderBoundingBox(DuckDerbyBlockEntity machine) {
      return (new AABB((double)-4.0F, (double)-0.0625F, (double)-4.0F, (double)5.0F, (double)6.0F, (double)5.0F)).move(machine.getBlockPos());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }
}
