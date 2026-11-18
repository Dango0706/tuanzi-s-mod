package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.minevegas.casinodeco.CasinoChandelierBlock;
import fr.minevegas.casinodeco.CasinoWallSconceBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public final class GuideFlames {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/particle/flame.png");

   private GuideFlames() {
   }

   public static List<Flame> positions(List<GuideScene.Placed> blocks) {
      final ArrayList<Flame> result = new ArrayList();
      var sink = new CasinoChandelierBlock.FlameSink() {
         public void smoke(double x, double y, double z) {
         }

         public void crackle(double x, double y, double z, float volume, float pitch) {
         }

         public void flame(double x, double y, double z) {
            result.add(new Flame(x, y, z));
         }
      };
      RandomSource random = RandomSource.create(0L);

      for(GuideScene.Placed placed : blocks) {
         BlockState state = placed.state();
         BlockPos pos = new BlockPos(placed.x(), placed.y(), placed.z());
         if (state.getBlock() instanceof CasinoChandelierBlock) {
            CasinoChandelierBlock.animate((Integer)state.getValue(CasinoChandelierBlock.PART), pos, random, sink);
         } else if (state.getBlock() instanceof CasinoWallSconceBlock) {
            CasinoWallSconceBlock.animate(state, pos, random, sink);
         }
      }

      return List.copyOf(result);
   }

   public static void draw(List<Flame> flames, float age, float yaw, float pitch, PoseStack pose, SubmitNodeCollector output) {
      int index = 0;

      for(Flame flame : flames) {
         pose.pushPose();
         pose.translate(flame.x(), flame.y(), flame.z());
         pose.rotate(Axis.YP.rotationDegrees(-yaw));
         pose.rotate(Axis.XP.rotationDegrees(pitch));
         float size = 0.95F + 0.05F * (float)Math.sin((double)(age * 0.7F + (float)(index++)));
         pose.scale(size, size, size);
         output.submitCustomGeometry(pose, RenderTypes.text(TEXTURE), (matrix, vertices) -> {
            vertex(vertices, matrix, -0.055F, -0.04F, 0.0F, 1.0F);
            vertex(vertices, matrix, 0.055F, -0.04F, 1.0F, 1.0F);
            vertex(vertices, matrix, 0.055F, 0.12F, 1.0F, 0.0F);
            vertex(vertices, matrix, -0.055F, 0.12F, 0.0F, 0.0F);
            vertex(vertices, matrix, -0.055F, 0.12F, 0.0F, 0.0F);
            vertex(vertices, matrix, 0.055F, 0.12F, 1.0F, 0.0F);
            vertex(vertices, matrix, 0.055F, -0.04F, 1.0F, 1.0F);
            vertex(vertices, matrix, -0.055F, -0.04F, 0.0F, 1.0F);
         });
         pose.popPose();
      }

   }

   private static void vertex(VertexConsumer target, PoseStack.Pose matrix, float x, float y, float u, float v) {
      target.addVertex(matrix, x, y, 0.0F).setColor(-1).setUv(u, v).setLight(15728880);
   }

   public static record Flame(double x, double y, double z) {
   }
}

