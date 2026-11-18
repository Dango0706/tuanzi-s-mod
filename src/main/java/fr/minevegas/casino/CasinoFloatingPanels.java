package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CasinoFloatingPanels {
   private static final int LIGHT = 15728880;
   private static final Identifier FELT = Identifier.fromNamespaceAndPath("roulettecasino", "textures/gui/floating_felt.png");

   private CasinoFloatingPanels() {
   }

   static PlayerHead player(BlockEntity machine) {
      CasinoPlayerDisplay var10000;
      if (machine instanceof CasinoConfiguredMachine configured) {
         var10000 = configured.panelPlayer();
      } else {
         var10000 = CasinoPlayerDisplay.EMPTY;
      }

      CasinoPlayerDisplay display = var10000;
      if (machine instanceof DuckDerbyBlockEntity derby) {
         if (!derby.groupSession()) {
            List<DerbyLobby.Member> members = derby.members();
            if (!members.isEmpty()) {
               display = new CasinoPlayerDisplay(((DerbyLobby.Member)members.get(0)).id().toString(), ((DerbyLobby.Member)members.get(0)).name());
            }
         }
      }

      if (display.id().isEmpty()) {
         return null;
      } else {
         UUID id = UUID.fromString(display.id());
         ClientPacketListener connection = Minecraft.getInstance().getConnection();
         PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
         Identifier skin = (info == null ? DefaultPlayerSkin.get(id) : info.getSkin()).body().texturePath();
         return new PlayerHead(display.name(), skin);
      }
   }

   static SubmitNodeCollector faded(SubmitNodeCollector output, CasinoNamedRenderState state, CameraRenderState camera, double x, double y, double z, int width, int height, float scale) {
      Vector3f relative = new Vector3f((float)((double)state.panelPos.getX() + x - camera.pos.x), (float)((double)state.panelPos.getY() + y - camera.pos.y), (float)((double)state.panelPos.getZ() + z - camera.pos.z));
      relative.rotate((new Quaternionf(camera.orientation)).conjugate());
      float opacity = DerbyPanelFocus.opacity((double)relative.x, (double)relative.y, (double)(-relative.z), ((double)width / (double)2.0F + (double)3.0F) * (double)scale, ((double)height / (double)2.0F + (double)(state.panelPlayer == null ? 3 : 22)) * (double)scale);
      return (SubmitNodeCollector)(opacity >= 0.9999F ? output : new DerbyPanelItems(output, opacity));
   }

   static void badge(CasinoNamedRenderState state, PoseStack pose, SubmitNodeCollector output, int top, int width) {
      badge(state, pose, output, top, width, "");
   }

   static void badge(CasinoNamedRenderState state, PoseStack pose, SubmitNodeCollector output, int top, int width, String fallbackName) {
      String fullName = state.panelPlayer == null ? fallbackName : state.panelPlayer.name();
      if (!fullName.isEmpty()) {
         Font font = Minecraft.getInstance().font;
         int faceWidth = state.panelPlayer == null ? 0 : 18;
         String name = font.plainSubstrByWidth(fullName, Math.max(1, width - faceWidth - 6));
         float left = (float)(-(font.width(name) + faceWidth)) / 2.0F;
         pose.pushPose();
         pose.translate(0.0F, 0.0F, 2.0F);
         head(state.panelPlayer, pose, output, left, (float)(top - 18), 12.0F);
         output.submitText(pose, left + (float)faceWidth, (float)(top - 16), Component.literal(name).getVisualOrderText(), true, DisplayMode.NORMAL, 15728880, -792629, 0, 0);
         pose.popPose();
      }
   }

   static void head(PlayerHead player, PoseStack pose, SubmitNodeCollector output, float x, float y, float size) {
      if (player != null) {
         pose.pushPose();
         pose.translate(0.0F, 0.0F, 1.0F);

         for(int layer = 0; layer < 2; ++layer) {
            float u = (layer == 0 ? 8.0F : 40.0F) / 64.0F;
            float v = 0.125F;
            picture(output, pose, player.skin(), x, y, x + size, y + size, u, v, u + 0.125F, v + 0.125F, -1);
            pose.translate(0.0F, 0.0F, 0.05F);
         }

         pose.popPose();
      }
   }

   static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      background(output, pose, x1, y1, x2, y2, color, 1.0F);
   }

   static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color, float opacity) {
      SubmitNodeCollector target = (SubmitNodeCollector)(opacity >= 0.9999F ? output : new DerbyPanelItems(output, opacity));
      if (color == -123241929) {
         frame(target, pose, x1, y1, x2, y2);
      } else if (color == -266188488) {
         felt(target, pose, x1, y1, x2, y2, 0, 0);
      } else if (color == -118897045) {
         rect(target, pose, x1, y2 - 1.0F, x2, y2, -5010875);
      } else if (color != -199929794 && color != -1339805653) {
         rect(target, pose, x1, y1, x2, y2, color);
      } else {
         felt(target, pose, x1, y1, x2, y2, color == -1339805653 ? 32 : 0, color == -199929794 ? 32 : 0);
      }

   }

   private static void frame(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2) {
      pose.pushPose();
      pose.translate(0.0F, 0.0F, 1.02F);
      int[] brass = new int[]{-9548506, -2771851, -9283281};

      for(int i = 0; i < 3; ++i) {
         float a = x1 + (float)i;
         float b = y1 + (float)i;
         float c = x2 - (float)i;
         float d = y2 - (float)i;
         rect(output, pose, a, b, c, b + 1.06F, brass[i]);
         rect(output, pose, a, d - 1.06F, c, d, brass[i]);
         rect(output, pose, a, b + 1.0F, a + 1.06F, d - 1.0F, brass[i]);
         rect(output, pose, c - 1.06F, b + 1.0F, c, d - 1.0F, brass[i]);
      }

      rect(output, pose, x1 + 4.0F, y1 + 1.0F, x2 - 4.0F, y1 + 2.0F, -599666);
      pose.translate(0.0F, 0.0F, 0.03F);

      for(float x : new float[]{x1 + 1.0F, x2 - 3.0F}) {
         for(float y : new float[]{y1 + 1.0F, y2 - 3.0F}) {
            rect(output, pose, x, y, x + 2.0F, y + 2.0F, -1388406);
         }
      }

      pose.popPose();
   }

   private static void felt(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int tileX, int tileY) {
      output.submitCustomGeometry(pose, RenderTypes.text(FELT), (transform, vertices) -> {
         for(float y = y1; y < y2; y += 32.0F) {
            for(float x = x1; x < x2; x += 32.0F) {
               float right = Math.min(x + 32.0F, x2);
               float bottom = Math.min(y + 32.0F, y2);
               float u1 = (float)tileX / 64.0F;
               float v1 = (float)tileY / 64.0F;
               float u2 = ((float)tileX + right - x) / 64.0F;
               float v2 = ((float)tileY + bottom - y) / 64.0F;
               vertices.addVertex(transform, x, y, 0.0F).setColor(-1).setUv(u1, v1).setLight(15728880);
               vertices.addVertex(transform, x, bottom, 0.0F).setColor(-1).setUv(u1, v2).setLight(15728880);
               vertices.addVertex(transform, right, bottom, 0.0F).setColor(-1).setUv(u2, v2).setLight(15728880);
               vertices.addVertex(transform, right, y, 0.0F).setColor(-1).setUv(u2, v1).setLight(15728880);
            }
         }

      });
   }

   private static void rect(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      picture(output, pose, FELT, x1, y1, x2, y2, 0.75F, 0.75F, 0.75F, 0.75F, color);
   }

   private static void picture(SubmitNodeCollector output, PoseStack pose, Identifier texture, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, int color) {
      output.submitCustomGeometry(pose, RenderTypes.text(texture), (transform, vertices) -> {
         vertices.addVertex(transform, x1, y1, 0.0F).setColor(color).setUv(u1, v1).setLight(15728880);
         vertices.addVertex(transform, x1, y2, 0.0F).setColor(color).setUv(u1, v2).setLight(15728880);
         vertices.addVertex(transform, x2, y2, 0.0F).setColor(color).setUv(u2, v2).setLight(15728880);
         vertices.addVertex(transform, x2, y1, 0.0F).setColor(color).setUv(u2, v1).setLight(15728880);
      });
   }

   public static record PlayerHead(String name, Identifier skin) {
   }
}
