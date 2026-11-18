package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class CasinoMachineNames {
   private CasinoMachineNames() {
   }

   public static void extract(BlockEntity machine, CasinoNamedRenderState state, Vec3 camera) {
      state.panelPos = machine.getBlockPos();
      state.panelPlayer = CasinoFloatingPanels.player(machine);
      state.machineLabel = null;
      LocalPlayer player = Minecraft.getInstance().player;
      if (CasinoSettings.allowed(player) && machine instanceof CasinoConfiguredMachine configured) {
         if (!(player.position().distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) > (double)64.0F) && configured.machineIdentity().number() > 0) {
            CasinoMachineIdentity identity = configured.machineIdentity();
            String type = CasinoMachine.type(machine);
            state.machineLabel = CasinoMachine.displayName(type, identity.name(), identity.number());
            state.machineLabelX = type.equals("roulette") ? (double)1.0F : (double)0.5F;
            state.machineLabelZ = type.equals("roulette") ? (double)1.0F : (double)0.5F;
            double var10001;
            switch (type) {
               case "roulette":
                  var10001 = 2.8;
                  break;
               case "blackjack":
               case "poker":
                  var10001 = 3.4;
                  break;
               case "slots":
                  var10001 = 4.1;
                  break;
               default:
                  var10001 = (double)3.5F;
            }

            state.machineLabelY = var10001;
            return;
         }
      }

   }

   public static void submit(CasinoNamedRenderState state, PoseStack pose, SubmitNodeCollector output, CameraRenderState camera) {
      if (state.machineLabel != null) {
         Font font = Minecraft.getInstance().font;
         FormattedCharSequence text = state.machineLabel.getVisualOrderText();
         int width = font.width(text);
         float scale = Math.min(0.011F, 4.0F / (float)Math.max(1, width));
         pose.pushPose();
         pose.translate(state.machineLabelX, state.machineLabelY, state.machineLabelZ);
         pose.rotate(camera.orientation);
         pose.scale(scale, -scale, scale);
         CasinoPanelStyle.background(output, pose, (float)(-width) / 2.0F - 4.0F, -3.0F, (float)width / 2.0F + 4.0F, 12.0F, -123241929);
         pose.translate(0.0F, 0.0F, 1.0F);
         CasinoPanelStyle.background(output, pose, (float)(-width) / 2.0F - 3.0F, -2.0F, (float)width / 2.0F + 3.0F, 11.0F, -266188488);
         pose.translate(0.0F, 0.0F, 1.0F);
         output.submitText(pose, (float)(-width) / 2.0F, 1.0F, text, true, DisplayMode.NORMAL, 15728880, -10635, 0, 0);
         pose.popPose();
      }
   }
}
