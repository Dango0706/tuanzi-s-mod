package fr.minevegas.casino;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class RouletteBetRenderState extends CasinoNamedRenderState {
   public final ItemStackRenderState[] items = new ItemStackRenderState[9];
   public final TextureAtlasSprite[] iconSprites = new TextureAtlasSprite[9];
   public final int[] counts = new int[9];
   public final boolean[] blockIcons = new boolean[9];
   public float visibility;
   public double phase;
   public double track;
   public double ballPhase;
   public double ballHeight = 12.8;

   public RouletteBetRenderState() {
      for(int i = 0; i < this.items.length; ++i) {
         this.items[i] = new CasinoBetIcons.Stake();
      }

   }
}
