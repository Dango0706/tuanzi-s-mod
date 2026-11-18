package fr.minevegas.casino.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.minevegas.casino.LayerOffset;
import fr.minevegas.casino.RouletteMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({VillagerProfessionLayer.class})
public abstract class CroupierLayerMixin {
   @ModifyArg(
      method = {"submit"},
      index = 2,
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/entity/layers/VillagerProfessionLayer;renderColoredCutoutModel(Lnet/minecraft/client/model/Model;Lnet/minecraft/resources/Identifier;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;II)V"
)}
   )
   private PoseStack roulettecasino$separateLayers(Model<?> model, Identifier texture, PoseStack pose, SubmitNodeCollector collector, int light, LivingEntityRenderState state, int color, int order) {
      if (state instanceof VillagerDataHolderRenderState holder) {
         VillagerData data = holder.getVillagerData();
         return data != null && data.profession().is(RouletteMod.CROUPIER_KEY) ? LayerOffset.of(pose, order) : pose;
      } else {
         return pose;
      }
   }
}
