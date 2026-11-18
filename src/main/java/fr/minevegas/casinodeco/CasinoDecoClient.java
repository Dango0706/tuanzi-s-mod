package fr.minevegas.casinodeco;

import fr.minevegas.casino.MineVegasClient;
import net.minecraft.client.renderer.entity.NoopRenderer;

public final class CasinoDecoClient {
   private CasinoDecoClient() {
   }

   public static void registerEntityRenderers(MineVegasClient.EntityRendererRegistrar renderers) {
      renderers.register(CasinoDecoMod.SEAT, NoopRenderer::new);
   }
}
