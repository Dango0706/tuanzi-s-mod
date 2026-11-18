package fr.minevegas.casino;

import java.util.Arrays;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public final class PlinkoRenderState extends CasinoNamedRenderState {
   public Direction facing;
   public final float[] lamps;
   public final float[] bulbs;
   public final float[] pockets;
   public final float[] balls;
   public int ballCount;
   public float panelVisibility;
   public final ItemStackRenderState[] items;
   public final TextureAtlasSprite[] iconSprites;
   public final int[] counts;
   public final boolean[] blockIcons;
   public final int[] landed;
   public boolean resultShown;
   public int bestQuarters;
   public PlinkoGame.Kind kind;

   public PlinkoRenderState() {
      this.facing = Direction.SOUTH;
      this.lamps = new float[PlinkoGame.PIN_COUNT];
      this.bulbs = new float[11];
      this.pockets = new float[9];
      this.balls = new float[8];
      this.items = new ItemStackRenderState[3];
      this.iconSprites = new TextureAtlasSprite[3];
      this.counts = new int[3];
      this.blockIcons = new boolean[3];
      this.landed = new int[3];
      this.kind = PlinkoGame.Kind.NONE;

      for(int i = 0; i < this.items.length; ++i) {
         this.items[i] = new CasinoBetIcons.Stake();
      }

      Arrays.fill(this.landed, -1);
   }
}
