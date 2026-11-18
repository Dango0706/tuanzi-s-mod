package fr.minevegas.casino;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public abstract class CasinoNamedRenderState extends BlockEntityRenderState {
   public BlockPos panelPos;
   public CasinoFloatingPanels.PlayerHead panelPlayer;
   public Component machineLabel;
   public double machineLabelX;
   public double machineLabelY;
   public double machineLabelZ;

   public CasinoNamedRenderState() {
      this.panelPos = BlockPos.ZERO;
      this.machineLabelX = (double)0.5F;
      this.machineLabelY = 3.3;
      this.machineLabelZ = (double)0.5F;
   }
}
