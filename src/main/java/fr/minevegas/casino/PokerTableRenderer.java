package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class PokerTableRenderer implements CasinoBlockEntityRenderer<PokerTableBlockEntity, PokerTableRenderState> {
   private static final Identifier BASE = RouletteMod.id("textures/entity/blackjack_dealer_base.png");
   private static final Identifier TYPE = RouletteMod.id("textures/entity/blackjack_dealer_clothes.png");
   private static final Identifier OUTFIT = RouletteMod.id("textures/entity/villager/profession/croupier.png");
   private static final Identifier BADGE = RouletteMod.id("textures/entity/blackjack_dealer_badge.png");
   private static final Identifier ARMS = RouletteMod.id("textures/entity/blackjack_dealer.png");
   private static final Identifier CARDS = RouletteMod.id("textures/entity/blackjack_cards.png");
   private static final int FULL_LIGHT = 15728880;
   private static final RandomSource ICON_RANDOM = RandomSource.create(0L);
   private static final Quaternionf BLOCK_GUI_INVERSE = (new Quaternionf()).rotationXYZ((float)Math.toRadians((double)30.0F), (float)Math.toRadians((double)225.0F), 0.0F).conjugate();
   private static final int[] RESULT_COLORS = new int[]{-1, -7811194, -7811194, -1, -1599358, -1599358};
   private static final double PANEL_X;
   private static final double PANEL_Z = 16.8;
   private final BlackjackDealerModel model = new BlackjackDealerModel(BlackjackDealerModel.createLayer().bakeRoot(), RenderTypes::entityCutout);
   private final EasyNpcDealerRenderer easyNpc = new EasyNpcDealerRenderer();
   private final ItemModelResolver itemModels;
   private final Font font;
   private final ToIntFunction<FormattedCharSequence> measure;

   public PokerTableRenderer(BlockEntityRendererProvider.Context context) {
      this.itemModels = context.itemModelResolver();
      this.font = context.font();
      Font var10001 = this.font;
      Objects.requireNonNull(var10001);
      this.measure = var10001::width;
   }

   public PokerTableRenderState createRenderState() {
      return new PokerTableRenderState();
   }

   public void extractRenderState(PokerTableBlockEntity table, PokerTableRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
      CasinoBlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPos, breaking);
      state.facing = (Direction)table.getBlockState().getValue(PokerTableBlock.FACING);
      Level level = table.getLevel();
      BlockPos pos = table.getBlockPos();
      long time = level == null ? 0L : level.getGameTime();
      long phase = Math.floorMod((long)pos.getX() * 31L + (long)pos.getY() * 7L + (long)pos.getZ() * 17L, 480L);
      state.dealer.ticks = (float)time + partialTick + (float)phase;
      state.dealerLight = state.lightCoords;
      state.easyNpcDealer = this.easyNpc.prepare(level, table.dealerAppearance());
      if (level != null) {
         for(int column = 1; column <= 2; ++column) {
            BlockPos behind = pos.offset(BlackjackTableBlock.rotatedOffset(state.facing, column - 1, -2));
            state.dealerLight = LightCoordsUtil.max(state.dealerLight, LightCoordsUtil.getLightCoords(level, behind));
         }
      }

      PokerTableState view = table.view();
      state.view = view;
      state.now = (double)time + (double)partialTick;
      view.dealerInput(state.now, state.dealer.play);
      this.extractPanel(table, state, view, cameraPos);
   }

   private void extractPanel(PokerTableBlockEntity table, PokerTableRenderState state, PokerTableState view, Vec3 cameraPos) {
      state.panelVisibility = 0.0F;
      state.stakeKinds = 0;
      state.playerHand = -1;
      state.dealerHand = -1;
      state.result = 0;
      List<BlackjackTableBlockEntity.Stake> wager = table.shownWager();
      if (view.isActive() && !wager.isEmpty()) {
         BlockPos pos = table.getBlockPos();
         double[] centre = BlackjackLayout.toBlock(PANEL_X, (double)0.0F, 16.8, state.facing.get2DDataValue());
         double distance = cameraPos.distanceTo(new Vec3((double)pos.getX() + centre[0], (double)pos.getY() + 2.4, (double)pos.getZ() + centre[2]));
         float approach = (float)Math.clamp(((double)10.0F - distance) / (double)2.5F, (double)0.0F, (double)1.0F);
         state.panelVisibility = approach * approach * (3.0F - 2.0F * approach);
         if (!(state.panelVisibility <= 0.0F)) {
            state.owner = table.ownerName();

            for(int i = 0; i < 3 && i < wager.size(); ++i) {
               ItemStack stack = ((BlackjackTableBlockEntity.Stake)wager.get(i)).display();
               state.stakeCounts[i] = stack.getCount();
               state.stakeBlocks[i] = CasinoBetIcons.drawnByFace(stack);
               this.itemModels.updateForTopItem(state.stakeItems[i], stack, ItemDisplayContext.GUI, table.getLevel(), (ItemOwner)null, i);
               ICON_RANDOM.setSeed((long)i);
               state.stakeSprites[i] = CasinoBetIcons.flatIcon(stack, state.stakeItems[i], ICON_RANDOM);
               state.stakeKinds = i + 1;
            }

            List<Integer> player = new ArrayList();
            List<Integer> dealer = new ArrayList();
            List<Integer> board = new ArrayList();

            for(BlackjackCardMotion.Card card : view.cards) {
               if (!(state.now - (double)card.dealtAt < (double)22.0F)) {
                  int value = card.value;
                  if (card.flipAt >= 0L && state.now - (double)card.flipAt < (double)10.0F) {
                     value = -1;
                  }

                  if (!card.toDealer) {
                     player.add(value);
                  } else if (card.index < 2) {
                     dealer.add(value);
                  } else {
                     board.add(value);
                  }
               }
            }

            if (!player.isEmpty()) {
               player.addAll(board);
               state.playerHand = PokerHands.category(PokerHands.score(player));
            }

            if (dealer.size() == 2 && !dealer.contains(-1)) {
               dealer.addAll(board);
               int score = PokerHands.score(dealer);
               state.dealerHand = PokerHands.category(score);
               state.dealerQualifies = PokerHands.qualifies(score);
            }

            if (view.result != PokerRound.Result.NONE && state.now >= (double)(view.resultAt + 4L)) {
               state.result = view.result.code;
            }

         }
      }
   }

   public void submit(PokerTableRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      CasinoMachineNames.submit(state, pose, output, camera);
      pose.pushPose();
      DealerPlacement.apply(pose, state.facing);
      if (!this.easyNpc.submit(state.easyNpcDealer, state.dealer, pose, output, state.dealerLight)) {
         this.layer(output, pose, state, BASE, 0, 0);
         this.layer(output, pose, state, TYPE, 1, 1);
         this.layer(output, pose, state, OUTFIT, 2, 2);
         this.layer(output, pose, state, BADGE, 3, 3);
         this.layer(output, pose, state, ARMS, 4, 0);
      }

      pose.popPose();
      if (state.view != null && (!state.view.cards.isEmpty() || state.view.gesture == DealerPlayAnimation.Kind.SHUFFLE)) {
         this.cards(state, pose, output);
      }

      if (state.panelVisibility > 0.0F) {
         this.panel(state, pose, output, camera);
      }

   }

   private void layer(SubmitNodeCollector output, PoseStack pose, PokerTableRenderState state, Identifier texture, int order, int step) {
      output.order(order).submitModel(this.model, state.dealer, LayerOffset.of(pose, step), (RenderType)RenderTypes.entityCutout(texture), state.dealerLight, OverlayTexture.NO_OVERLAY, 0);
   }

   private void cards(PokerTableRenderState state, PoseStack pose, SubmitNodeCollector output) {
      List<BlackjackCardGeometry.Quad> quads = new ArrayList();
      BlackjackCardMotion.Pose cardPose = new BlackjackCardMotion.Pose();

      for(BlackjackCardMotion.Card card : state.view.cards) {
         if (BlackjackCardMotion.pose(card, state.view.slot(card), state.now, cardPose)) {
            int var10001 = card.value;
            Objects.requireNonNull(quads);
            BlackjackCardGeometry.quads(cardPose, var10001, quads::add);
         }
      }

      if (state.view.gesture == DealerPlayAnimation.Kind.SHUFFLE) {
         List<BlackjackCardMotion.Pose> deck = new ArrayList();
         BlackjackCardMotion.shuffleDeck(state.now - (double)state.view.gestureAt, deck);

         for(BlackjackCardMotion.Pose p : deck) {
            if (p.visible) {
               Objects.requireNonNull(quads);
               BlackjackCardGeometry.quads(p, -1, quads::add);
            }
         }
      }

      if (!quads.isEmpty()) {
         int turns = state.facing.get2DDataValue();
         int light = state.lightCoords;
         output.order(5).submitCustomGeometry(pose, RenderTypes.entityCutout(CARDS), (transform, vertices) -> {
            for(BlackjackCardGeometry.Quad quad : quads) {
               double nx = quad.normal[0];
               double nz = quad.normal[2];

               for(int k = 0; k < turns; ++k) {
                  double rotated = -nz;
                  nz = nx;
                  nx = rotated;
               }

               for(int i = 0; i < 4; ++i) {
                  double[] p = BlackjackLayout.toBlock(quad.v[i][0], quad.v[i][1], quad.v[i][2], turns);
                  vertices.addVertex(transform, (float)p[0], (float)p[1], (float)p[2]).setColor(-1).setUv((float)quad.uv[i][0], (float)quad.uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transform, (float)nx, (float)quad.normal[1], (float)nz);
               }
            }

         });
      }
   }

   private void panel(PokerTableRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      boolean named = !state.owner.isEmpty();
      boolean decided = state.result != 0;
      FormattedCharSequence you = state.playerHand < 0 ? null : Component.translatable("gui.roulettecasino.blackjack.you").append(" ").append(PokerScreen.handName(state.playerHand)).getVisualOrderText();
      FormattedCharSequence dealer = state.playerHand >= 0 && state.dealerHand >= 0 ? Component.translatable("gui.roulettecasino.blackjack.dealer").append(" ").append(PokerScreen.handName(state.dealerHand)).getVisualOrderText() : null;
      FormattedCharSequence result = !decided ? null : Component.translatable("gui.roulettecasino.poker.result_" + state.result).withStyle(ChatFormatting.BOLD).getVisualOrderText();
      int width = 128;
      if (named) {
         width = Math.max(width, this.measure.applyAsInt(Component.literal(state.owner).getVisualOrderText()) + 30);
      }

      if (you != null) {
         width = Math.max(width, this.measure.applyAsInt(you) + 10);
      }

      if (dealer != null) {
         width = Math.max(width, this.measure.applyAsInt(dealer) + 10);
      }

      if (result != null) {
         width = Math.max(width, this.measure.applyAsInt(result) + 8);
      }

      int height = 20 + (you != null ? 14 : 0) + (dealer != null ? 14 : 0) + (decided ? 14 : 0);
      int left = -width / 2;
      int top = -height / 2;
      double[] centre = BlackjackLayout.toBlock(PANEL_X, (double)0.0F, 16.8, state.facing.get2DDataValue());
      pose.pushPose();
      pose.translate(centre[0], (double)2.5F - 0.09 * (double)(1.0F - state.panelVisibility), centre[2]);
      pose.rotate(camera.orientation);
      float scale = 0.011F * state.panelVisibility;
      pose.scale(scale, -scale, scale);
      output = CasinoFloatingPanels.faded(output, state, camera, centre[0], (double)2.5F - 0.09 * (double)(1.0F - state.panelVisibility), centre[2], width, height, scale);
      CasinoFloatingPanels.badge(state, pose, output, top, width, state.owner);
      background(output, pose, (float)(left - 3), (float)(top - 3), (float)(left + width + 3), (float)(top + height + 3), -123241929);
      pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
      background(output, pose, (float)(left - 2), (float)(top - 2), (float)(left + width + 2), (float)(top + height + 2), -266188488);
      pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
      int y = top;
      background(output, pose, (float)(left + 1), (float)top, (float)(left + width - 1), (float)(top + 19), -1339805653);
      int cell = (width - 2) / Math.max(1, state.stakeKinds);

      for(int i = 0; i < state.stakeKinds; ++i) {
         int x = left + 1 + i * cell + (cell - 33) / 2;
         pose.pushPose();
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         if (state.stakeSprites[i] != null) {
            icon(output, pose, state.stakeSprites[i], (float)(x + 2), (float)(y + 2), (float)(x + 16), (float)(y + 16));
         } else {
            pose.pushPose();
            pose.translate((double)x + (double)9.0F, (double)y + (double)9.5F, (double)1.0F);
            pose.scale(14.0F, -14.0F, 14.0F);
            if (state.stakeBlocks[i]) {
               pose.rotate(BLOCK_GUI_INVERSE);
               pose.scale(1.6F, 1.6F, 1.6F);
            } else {
               CasinoBetIcons.inFrontOfPanel(pose);
               CasinoBetIcons.fullLight(pose);
            }

            state.stakeItems[i].submit(pose, output, 15728880, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
         }

         int var10000 = state.stakeCounts[i];
         FormattedCharSequence count = Component.literal("×" + var10000).getVisualOrderText();
         output.submitText(pose, (float)(x + 19), (float)(y + 6), count, true, DisplayMode.NORMAL, 15728880, -1, 0, 0);
         pose.popPose();
      }

      y += 20;
      if (you != null) {
         background(output, pose, (float)(left + 1), (float)y, (float)(left + width - 1), (float)(y + 13), -1339805653);
         pose.pushPose();
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         output.submitText(pose, (float)(-this.measure.applyAsInt(you)) / 2.0F, (float)(y + 3), you, true, DisplayMode.NORMAL, 15728880, -865166, 0, 0);
         pose.popPose();
         y += 14;
      }

      if (dealer != null) {
         background(output, pose, (float)(left + 1), (float)y, (float)(left + width - 1), (float)(y + 13), -1339805653);
         pose.pushPose();
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         output.submitText(pose, (float)(-this.measure.applyAsInt(dealer)) / 2.0F, (float)(y + 3), dealer, true, DisplayMode.NORMAL, 15728880, state.dealerQualifies ? -1 : -1599358, 0, 0);
         pose.popPose();
         y += 14;
      }

      if (decided) {
         background(output, pose, (float)(left + 1), (float)y, (float)(left + width - 1), (float)(y + 13), -1339805653);
         pose.pushPose();
         pose.translate((double)0.0F, (double)0.0F, (double)1.0F);
         output.submitText(pose, (float)(-this.measure.applyAsInt(result)) / 2.0F, (float)(y + 3), result, true, DisplayMode.NORMAL, 15728880, RESULT_COLORS[Math.min(state.result, RESULT_COLORS.length - 1)], 0, 0);
         pose.popPose();
      }

      pose.popPose();
   }

   private static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      CasinoFloatingPanels.background(output, pose, x1, y1, x2, y2, color);
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

   public AABB getRenderBoundingBox(PokerTableBlockEntity machine) {
      return (new AABB((double)-4.0F, (double)-0.0625F, (double)-4.0F, (double)5.0F, (double)4.75F, (double)5.0F)).move(machine.getBlockPos());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }

   static {
      PANEL_X = PokerLayout.BOARD_X[2];
   }
}
