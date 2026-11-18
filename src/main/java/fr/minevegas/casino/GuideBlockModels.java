package fr.minevegas.casino;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class GuideBlockModels {
   private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

   private GuideBlockModels() {
   }

   public static long seed(BlockState state, int x, int y, int z) {
      return state.getSeed(new BlockPos(x, y, z));
   }

   public static void fill(BlockModelRenderState target, GuideScene.Placed block) {
      target.clear();
      Minecraft.getInstance().getModelManager().getBlockModelSet().get(block.state()).update(target, block.state(), CONTEXT, seed(block.state(), block.x(), block.y(), block.z()));
      target.blockLightCoords = 15728880;
   }
}
