package fr.minevegas.casino;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface CasinoBlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends BlockEntityRenderer<T, S> {
   AABB getRenderBoundingBox(T var1);

   default void extractRenderState(T machine, S state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
      BlockEntityRenderer.super.extractRenderState(machine, state, partialTick, camera, breaking);
      if (state instanceof CasinoNamedRenderState named) {
         CasinoMachineNames.extract(machine, named, camera);
      }

   }
}
